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

package org.projectforge.framework.scheduling

import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemState
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.projectforge.framework.integration.SubsystemSync
import org.projectforge.framework.integration.SyncStats
import org.springframework.stereotype.Component

/**
 * The scheduled jobs as tile of the overview: degraded by failed or overdue jobs, down if one of the jobs running
 * every minute or more often ([CANARY_JOBS]) is overdue: the scheduler itself is stuck then. Unknown before the first
 * run. The failed and overdue jobs are shown as its syncs.
 */
@Component
class SchedulerSubsystemStatusProvider(private val adminService: SchedulerJobAdminService) : SubsystemStatusProvider {
    override val id = "scheduler"

    override val titleKey = "system.admin.adminErrors.subsystem.scheduler"

    override val problems = SubsystemProblemMatch(
        locationPrefixes = listOf("SchedulerJobRegistry", "SchedulerJobStatsService"),
    )

    override fun status(): SubsystemStatus = statusOf(adminService.overview().map { it.second })

    companion object {
        internal val CANARY_JOBS = setOf("jobs.tidyUp", "support.logAggregationFlush")

        internal fun statusOf(jobs: List<SchedulerJobEntry>): SubsystemStatus {
            val detail = translateMsg("system.scheduler.subsystem.detail", jobs.size)
            val problems = jobs.filter { it.status == SchedulerJobStatus.FAILED || it.status == SchedulerJobStatus.OVERDUE }
            val state = when {
                problems.any { it.overdue && it.id in CANARY_JOBS } -> SubsystemState.DOWN
                problems.isNotEmpty() -> SubsystemState.DEGRADED
                jobs.none { it.lastRun != null } -> SubsystemState.UNKNOWN
                else -> SubsystemState.OK
            }
            return SubsystemStatus(state, detail, problems.map { syncOf(it) })
        }

        private fun syncOf(job: SchedulerJobEntry) = SubsystemSync(
            type = job.title,
            runs = job.runs7d,
            lastRun = job.lastRun,
            lastStatus = if (job.overdue && job.id in CANARY_JOBS) SyncStats.Status.ABORTED else SyncStats.Status.ERRORS,
            lastDurationMs = job.lastDurationMs,
            lastError = if (job.overdue) translateMsg("system.scheduler.subsystem.overdue", job.title) else job.lastError,
            lastErrorDate = if (job.overdue) job.nextRun else job.lastErrorTime,
        )
    }
}
