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

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.util.Date

/** The period of a [SchedulerJobStatsDO]. */
enum class SchedulerStatsPeriod { DAY, MONTH }

/**
 * The runs of a scheduled job in one day or, after the retention of the days, in one month (see
 * [SchedulerJobStatsService.cleanup]). Without history, written in batches by [SchedulerJobStatsService.flush].
 */
@Entity
@Table(
    name = "T_PF_SCHEDULER_JOB_STATS",
    uniqueConstraints = [UniqueConstraint(
        name = "unique_t_pf_scheduler_job_stats_period",
        columnNames = ["job_id", "period_type", "period_start"],
    )],
    indexes = [Index(name = "idx_t_pf_scheduler_job_stats_period", columnList = "period_type, period_start")],
)
@NamedQueries(
    NamedQuery(
        name = SchedulerJobStatsDO.FIND_BY_JOB_PERIOD,
        query = "from SchedulerJobStatsDO s where s.jobId = :jobId and s.periodType = :periodType and s.periodStart = :periodStart",
    ),
    NamedQuery(
        name = SchedulerJobStatsDO.SELECT_BY_JOB_SINCE,
        query = "from SchedulerJobStatsDO s where s.jobId = :jobId and s.periodType = :periodType and s.periodStart >= :since order by s.periodStart",
    ),
    NamedQuery(
        name = SchedulerJobStatsDO.SELECT_SINCE,
        query = "from SchedulerJobStatsDO s where s.periodType = :periodType and s.periodStart >= :since order by s.periodStart",
    ),
    NamedQuery(
        name = SchedulerJobStatsDO.SELECT_BEFORE,
        query = "from SchedulerJobStatsDO s where s.periodType = :periodType and s.periodStart < :before",
    ),
    NamedQuery(
        name = SchedulerJobStatsDO.DELETE_BEFORE,
        query = "delete from SchedulerJobStatsDO s where s.periodType = :periodType and s.periodStart < :before",
    ),
)
open class SchedulerJobStatsDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    /** The [org.projectforge.common.scheduling.SchedulerJobDefinition.id]. */
    @get:Column(name = "job_id", length = 100, nullable = false)
    open var jobId: String? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "period_type", length = 10, nullable = false)
    open var periodType: SchedulerStatsPeriod? = null

    /** The day or the first day of the month (system time zone). */
    @get:Column(name = "period_start", nullable = false)
    open var periodStart: LocalDate? = null

    /** Successful and failed runs, without the skipped ones. */
    @get:Column(name = "run_count", nullable = false)
    open var runCount: Int = 0

    @get:Column(name = "success_count", nullable = false)
    open var successCount: Int = 0

    @get:Column(name = "error_count", nullable = false)
    open var errorCount: Int = 0

    /** Skipped, because the previous run was still running. */
    @get:Column(name = "skipped_count", nullable = false)
    open var skippedCount: Int = 0

    @get:Column(name = "manual_count", nullable = false)
    open var manualCount: Int = 0

    @get:Column(name = "slow_count", nullable = false)
    open var slowCount: Int = 0

    @get:Column(name = "min_duration_ms")
    open var minDurationMs: Long? = null

    @get:Column(name = "max_duration_ms")
    open var maxDurationMs: Long? = null

    @get:Column(name = "sum_duration_ms", nullable = false)
    open var sumDurationMs: Long = 0

    @get:Column(name = "last_run_start")
    open var lastRunStart: Date? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "last_status", length = 20)
    open var lastStatus: SchedulerRunStatus? = null

    @get:Column(name = "last_duration_ms")
    open var lastDurationMs: Long? = null

    @get:Column(name = "last_error_message", length = MAX_MESSAGE)
    open var lastErrorMessage: String? = null

    @get:Column(name = "last_error_time")
    open var lastErrorTime: Date? = null

    companion object {
        internal const val FIND_BY_JOB_PERIOD = "SchedulerJobStatsDO_FindByJobPeriod"
        internal const val SELECT_BY_JOB_SINCE = "SchedulerJobStatsDO_SelectByJobSince"
        internal const val SELECT_SINCE = "SchedulerJobStatsDO_SelectSince"
        internal const val SELECT_BEFORE = "SchedulerJobStatsDO_SelectBefore"
        internal const val DELETE_BEFORE = "SchedulerJobStatsDO_DeleteBefore"

        internal const val MAX_MESSAGE = 4000
    }
}
