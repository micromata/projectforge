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

package org.projectforge.plugins.cveradar

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.plugins.cveradar.model.CvrClientAnswer
import org.projectforge.plugins.cveradar.model.CvrConfirmationDO
import org.projectforge.plugins.cveradar.model.CvrDeviceDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * Devices and confirmations: written by the sync and by the employees' confirmations, read by the
 * closure overview. No access checks here, the callers check (devices are personal data, VIS-15).
 */
@Service
class CvrDeviceService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /**
     * A message of the client agent from the log server (EXT-04, EXT-05). Values are cut to the column sizes: one
     * overlong message would otherwise fail the whole sync.
     */
    class AgentRecord(
        val timestamp: Date,
        username: String,
        serialNumber: String? = null,
        hostname: String? = null,
        version: String? = null,
        checkId: String? = null,
        val checkPassed: Boolean? = null,
    ) {
        val username = username.take(255)
        val serialNumber = serialNumber?.take(100)
        val hostname = hostname?.take(255)
        val version = version?.take(50)
        val checkId = checkId?.take(100)
    }

    /** The latest result of a check of the client agent. */
    class AgentCheck(var passed: Boolean = false, var at: Date? = null)

    fun selectDevices(): List<CvrDeviceDO> {
        return persistenceService.executeQuery("from CvrDeviceDO d order by d.username", CvrDeviceDO::class.java)
    }

    fun selectDevicesOfUser(userId: Long): List<CvrDeviceDO> {
        return persistenceService.executeQuery(
            "from CvrDeviceDO d where d.userId = :userId order by d.hostname",
            CvrDeviceDO::class.java,
            Pair("userId", userId),
        )
    }

    fun agentChecks(device: CvrDeviceDO): Map<String, AgentCheck> {
        val json = device.agentChecks?.takeIf { it.isNotBlank() } ?: return emptyMap()
        return try {
            objectMapper.readValue(json, CHECKS_TYPE)
        } catch (ex: Exception) {
            log.warn { "Can't read the agent checks of device #${device.id}: ${ex.message}" }
            emptyMap()
        }
    }

    /**
     * Stores the messages of the client agents: per device the newest version and the newest result of every
     * check. Older messages than the stored values are ignored.
     *
     * @return The number of devices updated.
     */
    fun applyAgentRecords(records: List<AgentRecord>): Int {
        val byDevice = records.filter { it.username.isNotBlank() }
            .groupBy { Triple(it.serialNumber?.takeIf { s -> s.isNotBlank() }, it.username, it.hostname) }
        var updated = 0
        persistenceService.runInTransaction { context ->
            byDevice.forEach { (key, deviceRecords) ->
                val (serial, username, hostname) = key
                val device = findOrCreateDevice(context, serial, username, hostname)
                device.username = username
                device.userId = resolveUserId(username)
                hostname?.takeIf { it.isNotBlank() }?.let { device.hostname = it }
                val newest = deviceRecords.maxOf { it.timestamp }
                device.lastSeen = maxOf(device.lastSeen ?: newest, newest)
                deviceRecords.filter { !it.version.isNullOrBlank() }.maxByOrNull { it.timestamp }?.let {
                    if (device.agentVersionAt == null || !it.timestamp.before(device.agentVersionAt)) {
                        device.agentVersion = it.version!!.trim().take(50)
                        device.agentVersionAt = it.timestamp
                    }
                }
                val checks = agentChecks(device).toMutableMap()
                deviceRecords.filter { !it.checkId.isNullOrBlank() && it.checkPassed != null }
                    .groupBy { it.checkId!!.trim() }
                    .forEach { (checkId, checkRecords) ->
                        val latest = checkRecords.maxBy { it.timestamp }
                        val stored = checks[checkId]
                        if (stored?.at == null || !latest.timestamp.before(stored.at)) {
                            checks[checkId] = AgentCheck(latest.checkPassed!!, latest.timestamp)
                        }
                    }
                device.agentChecks = checksJson(device, checks)
                context.update(device)
                updated++
            }
        }
        return updated
    }

    /** Deletes the devices not reported for [retentionDays] (EXT-07). */
    fun deleteOutdated(retentionDays: Int, now: Date = Date()): Int {
        val limit = Date(now.time - retentionDays * MILLIS_PER_DAY)
        return persistenceService.runInTransaction { context ->
            val ids = context.executeQuery(
                "select d.id from CvrDeviceDO d where d.lastSeen < :limit",
                Long::class.javaObjectType,
                Pair("limit", limit),
            )
            if (ids.isNotEmpty()) {
                context.executeUpdate("delete from CvrDeviceDO d where d.id in :ids", Pair("ids", ids))
            }
            ids.size
        }
    }

    fun selectConfirmations(caseId: Long): List<CvrConfirmationDO> {
        return persistenceService.executeQuery(
            "from CvrConfirmationDO c where c.caseId = :caseId",
            CvrConfirmationDO::class.java,
            Pair("caseId", caseId),
        )
    }

    fun selectConfirmationsOfUser(userId: Long): List<CvrConfirmationDO> {
        return persistenceService.executeQuery(
            "from CvrConfirmationDO c where c.userId = :userId",
            CvrConfirmationDO::class.java,
            Pair("userId", userId),
        )
    }

    /** Stores the answer of the user for the case, replacing an earlier one. */
    fun confirm(caseId: Long, userId: Long, answer: CvrClientAnswer, comment: String?): CvrConfirmationDO {
        return persistenceService.runInTransaction { context ->
            val confirmation = context.selectSingleResult(
                "from CvrConfirmationDO c where c.caseId = :caseId and c.userId = :userId",
                CvrConfirmationDO::class.java,
                Pair("caseId", caseId),
                Pair("userId", userId),
                attached = true,
            ) ?: CvrConfirmationDO().also {
                it.caseId = caseId
                it.userId = userId
            }
            confirmation.answer = answer
            confirmation.comment = comment?.trim()?.take(4000)?.takeIf { it.isNotEmpty() }
            confirmation.lastUpdate = Date()
            if (confirmation.id == null) context.insert(confirmation) else context.update(confirmation)
            confirmation
        }
    }

    /**
     * The device of the serial number, or (without serial number) the only device of the user with this host name or
     * the only device of the user at all; otherwise a new one.
     */
    private fun findOrCreateDevice(
        context: PfPersistenceContext,
        serialNumber: String?,
        username: String,
        hostname: String?,
    ): CvrDeviceDO {
        val device = if (serialNumber != null) {
            context.selectSingleResult(
                "from CvrDeviceDO d where d.serialNumber = :serial",
                CvrDeviceDO::class.java,
                Pair("serial", serialNumber),
                attached = true,
            )
        } else {
            val ofUser = context.executeQuery(
                "from CvrDeviceDO d where d.username = :username",
                CvrDeviceDO::class.java,
                Pair("username", username),
                attached = true,
            )
            ofUser.filter { hostname != null && it.hostname.equals(hostname, ignoreCase = true) }.singleOrNull()
                ?: ofUser.singleOrNull()
        }
        if (device != null) {
            return device
        }
        return CvrDeviceDO().also {
            it.created = Date()
            it.serialNumber = serialNumber
            it.username = username
            it.hostname = hostname
            context.insert(it)
        }
    }

    /**
     * The checks as json fitting into the column: if they are too many, the oldest are dropped (never cut, the json
     * would be unreadable and all checks lost).
     */
    internal fun checksJson(device: CvrDeviceDO, checks: Map<String, AgentCheck>): String? {
        var kept = checks.entries.sortedByDescending { it.value.at }.map { it.key to it.value }
        while (kept.isNotEmpty()) {
            val json = objectMapper.writeValueAsString(kept.toMap())
            if (json.length <= MAX_CHECKS_JSON) {
                if (kept.size < checks.size) {
                    log.info { "Device #${device.id}: ${checks.size - kept.size} oldest agent checks dropped." }
                }
                return json
            }
            kept = kept.dropLast(maxOf(1, kept.size / 10))
        }
        return null
    }

    private fun resolveUserId(username: String): Long? {
        return (userGroupCache.getUser(username) ?: userGroupCache.getUser(username.lowercase()))?.id
    }

    companion object {
        private const val MILLIS_PER_DAY = 24L * 3600 * 1000

        /** The size of the column agent_checks. */
        private const val MAX_CHECKS_JSON = 4000

        private val objectMapper = ObjectMapper()

        private val CHECKS_TYPE = object : TypeReference<Map<String, AgentCheck>>() {}
    }
}
