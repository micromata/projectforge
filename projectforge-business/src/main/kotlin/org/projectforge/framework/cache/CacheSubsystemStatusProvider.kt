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


package org.projectforge.framework.cache

import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemState
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.projectforge.framework.integration.SubsystemSync
import org.projectforge.framework.integration.SyncStats
import org.springframework.stereotype.Component

/**
 * The caches as tile of the overview: down if a refresh is stuck, degraded if the last refresh of a cache failed.
 * Unknown while no cache is filled yet. The stuck and failed caches are shown as its syncs.
 */
@Component
class CacheSubsystemStatusProvider(private val adminService: CacheAdminService) : SubsystemStatusProvider {
    override val id = "caches"

    override val titleKey = "system.admin.adminErrors.subsystem.caches"

    override val problems = SubsystemProblemMatch(
        locationPrefixes = listOf("AbstractCache"),
    )

    override fun status(): SubsystemStatus = statusOf(adminService.list())

    companion object {
        internal fun statusOf(caches: List<CacheEntry>): SubsystemStatus {
            val detail = translateMsg("system.caches.subsystem.detail", caches.size)
            val problems = caches.filter { it.state == CacheState.STUCK || it.state == CacheState.FAILED }
            val state = when {
                problems.any { it.state == CacheState.STUCK } -> SubsystemState.DOWN
                problems.isNotEmpty() -> SubsystemState.DEGRADED
                caches.none { it.status.everFilled } -> SubsystemState.UNKNOWN
                else -> SubsystemState.OK
            }
            return SubsystemStatus(state, detail, problems.map { syncOf(it) })
        }

        private fun syncOf(cache: CacheEntry): SubsystemSync {
            val status = cache.status
            val stuck = cache.state == CacheState.STUCK
            return SubsystemSync(
                type = status.name,
                runs = status.refreshCount.toInt(),
                lastRun = status.lastRefreshStart,
                lastStatus = if (stuck) SyncStats.Status.ABORTED else SyncStats.Status.ERRORS,
                lastDurationMs = status.lastRefreshDurationMs,
                lastError = if (stuck) translateMsg("system.caches.subsystem.stuck", status.name) else status.lastRefreshError,
                lastErrorDate = if (stuck) status.refreshRunningSince else status.lastRefreshEnd,
            )
        }
    }
}
