/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.lanesandplanes

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.logging.error
import org.projectforge.common.logging.warn
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.integration.IntegrationConfig
import org.projectforge.framework.integration.IntegrationErrors
import org.projectforge.framework.integration.PooledHttpClients
import org.projectforge.framework.integration.SyncStatsRegistry
import org.projectforge.framework.json.JsonUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPOutputStream

private val log = KotlinLogging.logger {}

/**
 * Pushes the active employees with their bookable cost units to the user import of Lanes & Planes
 * (`POST /ext/users`, see https://support.lanes-planes.com/hc/de/articles/31245986692370). Lanes & Planes replaces its
 * complete user list by every push and deactivates all users not sent, so a push is refused, if the user list is
 * suspiciously small. The import is processed asynchronously by Lanes & Planes: data errors of single users are
 * only shown in the user import log of the Lanes & Planes company account.
 */
@Service
@ConditionalOnProperty(name = ["projectforge.lanesandplanes.enabled"], havingValue = "true")
open class LanesAndPlanesSyncService(
    private val config: LanesAndPlanesConfig,
    private val payloadBuilder: LanesAndPlanesPayloadBuilder,
    private val settingsService: LanesAndPlanesSettingsService,
) {
    private val syncStats = SyncStatsRegistry.get(SYNC_TYPE)

    private val pushLock = ReentrantLock()

    private val restTemplate by lazy { RestTemplate(PooledHttpClients.requestFactory(IntegrationConfig.LANES_AND_PLANES)) }

    /** Hash of the last payload accepted by Lanes & Planes (in memory: the first run after a restart always pushes). */
    private var lastPushedHash: Int? = null

    private var lastPushedUsers: Int? = null

    open fun push() {
        if (!pushLock.tryLock()) {
            log.info { "Lanes & Planes push already running, skipped." }
            return
        }
        try {
            val run = syncStats.startRun(if (config.dryRun) "dry run" else null)
            val settings = settingsService.settings()
            val result = try {
                run.step("build") { counts ->
                    payloadBuilder.build(settings).also {
                        counts.unchanged = it.request.users.size
                        counts.errors = it.skippedUsers
                    }
                }
            } catch (ex: Exception) {
                log.error(ex) { "Lanes & Planes push: building the user list failed: ${ex.message}" }
                run.abort("build failed: ${ex.message}")
                return
            }
            val users = result.request.users
            refuseReason(users.size, settings)?.let { reason ->
                log.error(LanesAndPlanesLogEvents.PUSH_REFUSED) { "Lanes & Planes push refused: $reason" }
                run.abort(reason)
                return
            }
            val json = JsonUtils.toJson(result.request, ignoreNullableProps = true)
            if (config.dryRun) {
                val file = File(ConfigXml.getInstance().workingDirectory, DRY_RUN_FILENAME)
                file.writeText(json)
                log.info {
                    "Lanes & Planes dry run (projectforge.lanesandplanes.dryRun=true): ${users.size} users with " +
                            "${users.sumOf { it.costCenters?.size ?: 0 }} cost centers " +
                            "(${settings.generalKost1Numbers.size} general Kost1) and " +
                            "${users.sumOf { it.costUnits?.size ?: 0 }} cost units " +
                            "(${settings.generalKost2Numbers.size} general Kost2, patterns ${settings.kost2Patterns}), " +
                            "${users.count { it.creditorAccount != null }} with creditor account, " +
                            "${settings.additionalUserList.size} additional users, " +
                            "written to ${file.absolutePath}, nothing sent."
                }
                run.finish()
                return
            }
            val hash = json.hashCode()
            if (hash == lastPushedHash) {
                run.skip("no changes since the last push")
                return
            }
            try {
                run.step("push") { counts ->
                    post(json)
                    counts.updated = users.size
                }
            } catch (ex: Exception) {
                val message = when (ex) {
                    is HttpStatusCodeException -> "HTTP ${ex.statusCode.value()}: ${ex.responseBodyAsString.take(500)}"
                    else -> ex.message ?: ex.javaClass.simpleName
                }
                log.warn(LanesAndPlanesLogEvents.PUSH_FAILED) { "Lanes & Planes push of ${users.size} users failed: $message" }
                run.abort(message, IntegrationErrors.isTimeout(ex))
                return
            }
            lastPushedHash = hash
            lastPushedUsers = users.size
            run.finish()
        } finally {
            pushLock.unlock()
        }
    }

    /**
     * @return The reason, why the push mustn't be sent, or null.
     */
    internal fun refuseReason(userCount: Int, settings: LanesAndPlanesSettings): String? {
        if (config.apiKey.isBlank() && !config.dryRun) {
            return "projectforge.lanesandplanes.apiKey not configured."
        }
        if (settings.invoiceProfileIds.isEmpty()) {
            return "no invoice profile ids in the configuration parameter 'lanesAndPlanes' (required by Lanes & Planes)."
        }
        if (userCount < config.minUsers) {
            return "only $userCount users found (projectforge.lanesandplanes.minUsers=${config.minUsers}), all " +
                    "other users would be deactivated in Lanes & Planes."
        }
        val last = lastPushedUsers
        if (last != null && userCount < last / 2) {
            return "only $userCount users found, $last users were sent by the last push. All other users would be " +
                    "deactivated in Lanes & Planes. Restart ProjectForge to force the push, if this is intended."
        }
        return null
    }

    private fun post(json: String) {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.CONTENT_ENCODING, "gzip")
        headers.set(HttpHeaders.AUTHORIZATION, "Token token=${config.apiKey}")
        restTemplate.exchange(config.url, HttpMethod.POST, HttpEntity(gzip(json), headers), String::class.java)
    }

    companion object {
        const val SYNC_TYPE = "lanes-and-planes"

        private const val DRY_RUN_FILENAME = "lanes-and-planes-users.json"

        internal fun gzip(json: String): ByteArray {
            val bytes = ByteArrayOutputStream()
            GZIPOutputStream(bytes).use { it.write(json.toByteArray(Charsets.UTF_8)) }
            return bytes.toByteArray()
        }
    }
}
