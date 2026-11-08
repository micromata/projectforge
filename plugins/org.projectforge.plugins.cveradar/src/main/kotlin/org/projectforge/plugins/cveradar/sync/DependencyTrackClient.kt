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

package org.projectforge.plugins.cveradar.sync

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SyncStatsRegistry
import org.projectforge.plugins.cveradar.CveRadarConfig
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Client of the REST API v1 of Dependency-Track (PF-CVR-DT-MIG 4.1), optional ([CveRadarConfig.DependencyTrack]):
 * the API key goes in the header `X-Api-Key` only, never into a message or the log. Lists are paged (at most
 * [PAGE_SIZE] entries per page, the total in the header `X-Total-Count`): [getAll] reads all pages. Timeouts and the
 * size limit of the answers: integration id [CveRadarHttp.DEPTRACK].
 */
@Service
class DependencyTrackClient(private val config: CveRadarConfig) {
    /** The team of the API key with its permissions. */
    class Team(val name: String?, val permissions: Set<String>)

    /** The version of Dependency-Track found by the last [probe], null before or if it failed. */
    @Volatile
    var lastVersion: String? = null
        private set

    /** The version of the API server (`/api/version`, readable without key). */
    fun version(): String? = get("/api/version").path("version").textValue()

    /** The team of the API key (`/api/v1/team/self`): an invalid key fails with status 401. */
    fun team(): Team {
        val node = get("/api/v1/team/self")
        val permissions = node.path("permissions").mapNotNull { it.path("name").textValue() }.toSet()
        return Team(node.path("name").textValue(), permissions)
    }

    fun get(path: String): JsonNode = objectMapper.readTree(execute(HttpGet(url(path))).body)

    /** All entries of a paged list, e.g. `/api/v1/finding/project/<uuid>`. */
    fun getAll(path: String): List<JsonNode> {
        val result = mutableListOf<JsonNode>()
        val separator = if (path.contains('?')) '&' else '?'
        var page = 1
        while (true) {
            val response = execute(HttpGet(url("$path${separator}pageSize=$PAGE_SIZE&pageNumber=$page")))
            val entries = objectMapper.readTree(response.body)
            check(entries.isArray) { "Dependency-Track: no list received for $path." }
            entries.forEach { result.add(it) }
            // Without total (not a paged list) the first page is all.
            val total = response.header("X-Total-Count")?.toIntOrNull() ?: return result
            if (entries.isEmpty || result.size >= total) {
                return result
            }
            check(page < MAX_PAGES) { "Dependency-Track: more than $MAX_PAGES pages for $path." }
            page++
        }
    }

    /**
     * Tests the connection, recorded as a run of the sync stats [SYNC_TYPE] (tile on the system dashboard): version,
     * key valid, the permissions of the team ([PERMISSIONS], a missing one counts as error).
     * @return The version of Dependency-Track.
     */
    fun probe(): String? {
        lastVersion = null
        return SyncStatsRegistry.get(SYNC_TYPE).execute(SubsystemStatus.hostOf(config.deptrack.url)) { run ->
            val version = run.step("version") { counts ->
                version().also { counts.unchanged++ }
            }
            run.step("team") { counts ->
                val missing = PERMISSIONS - team().permissions
                counts.unchanged += PERMISSIONS.size - missing.size
                counts.errors += missing.size
                if (missing.isNotEmpty()) {
                    log.warn { "Dependency-Track: the team of the API key misses the permissions ${missing.sorted().joinToString()}." }
                }
            }
            lastVersion = version
            version
        }
    }

    private fun url(path: String): String {
        check(config.deptrack.isConfigured()) { "Dependency-Track isn't configured." }
        return CveRadarHttp.url(config.deptrack.url, path)
    }

    private fun execute(request: HttpUriRequestBase): CveRadarHttp.Response {
        request.setHeader("X-Api-Key", config.deptrack.apiKey)
        request.setHeader("Accept", "application/json")
        return CveRadarHttp.executeForResponse(CveRadarHttp.DEPTRACK, request)
    }

    companion object {
        /** Dependency-Track answers 100 entries per page at most. */
        const val PAGE_SIZE = 100

        /** Guard against a list that never ends (100,000 entries). */
        private const val MAX_PAGES = 1000

        const val SYNC_TYPE = "cveradar-deptrack"

        /** The permissions the team of ProjectForge needs (admin guide). */
        val PERMISSIONS = setOf(
            "BOM_UPLOAD", "PORTFOLIO_MANAGEMENT", "PROJECT_CREATION_UPLOAD", "VIEW_POLICY_VIOLATION", "VIEW_PORTFOLIO",
            "VIEW_VULNERABILITY", "VULNERABILITY_ANALYSIS",
        )

        private val objectMapper = ObjectMapper()
    }
}
