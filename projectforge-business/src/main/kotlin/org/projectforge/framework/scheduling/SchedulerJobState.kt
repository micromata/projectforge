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

import org.projectforge.common.scheduling.SchedulerActiveCheck
import org.projectforge.common.scheduling.SchedulerJobDefinition
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The state of a scheduled job since the start (in memory), see [SchedulerJobRegistry]. The last run is restored
 * from the database after a restart ([SchedulerJobStatsService]).
 */
class SchedulerJobState(val definition: SchedulerJobDefinition, val schedule: ResolvedSchedule) {
    /** The `@Scheduled` method, for "run now". Null, if the job wasn't registered (only run). */
    @Volatile
    var entry: Runnable? = null
        internal set

    @Volatile
    var activeCheck: SchedulerActiveCheck? = null
        internal set

    internal val running = AtomicBoolean(false)

    @Volatile
    var runningSince: Long? = null
        internal set

    /** The reason of the last check, null if active (or not yet checked). */
    @Volatile
    var inactiveReason: String? = null
        internal set

    /**
     * The time the scheduler measures the next run from: the start of the last scheduled call of a job running in
     * its own thread, the end of a job running in the scheduler's thread. Manual runs don't count.
     */
    @Volatile
    var lastScheduledReference: Long? = null
        internal set

    /** The average duration of the last days ([SchedulerJobStatsService]), for detecting slow runs. */
    @Volatile
    var baselineAvgMs: Long? = null
        internal set

    private var lastStart: Long? = null
    private var lastDurationMs: Long? = null
    private var lastStatus: SchedulerRunStatus? = null
    private var lastError: String? = null
    private var lastErrorTime: Long? = null
    private var runs = 0
    private var errors = 0
    private var skipped = 0
    private val recentRuns = ArrayDeque<SchedulerRunRecord>()

    @Synchronized
    internal fun record(record: SchedulerRunRecord) {
        recentRuns.addFirst(record)
        while (recentRuns.size > MAX_RECENT_RUNS) {
            recentRuns.removeLast()
        }
        if (record.status == SchedulerRunStatus.SKIPPED) {
            skipped++
            return
        }
        runs++
        lastStart = record.start
        lastDurationMs = record.durationMs
        lastStatus = record.status
        if (record.status == SchedulerRunStatus.ERROR) {
            errors++
            lastError = record.errorMessage
            lastErrorTime = record.start
        }
    }

    /** Takes over the last run stored before the restart, unless the job already ran since. */
    @Synchronized
    internal fun restore(stats: SchedulerStats) {
        if (lastStart == null && stats.lastRunStart != null) {
            lastStart = stats.lastRunStart
            lastDurationMs = stats.lastDurationMs
            lastStatus = stats.lastStatus
        }
        if (lastErrorTime == null && stats.lastErrorTime != null) {
            lastError = stats.lastErrorMessage
            lastErrorTime = stats.lastErrorTime
        }
    }

    @Synchronized
    fun snapshot(): Snapshot = Snapshot(
        lastStart = lastStart,
        lastDurationMs = lastDurationMs,
        lastStatus = lastStatus,
        lastError = lastError,
        lastErrorTime = lastErrorTime,
        runs = runs,
        errors = errors,
        skipped = skipped,
        running = running.get(),
        runningSince = runningSince,
        inactiveReason = inactiveReason,
        lastScheduledReference = lastScheduledReference,
        recentRuns = recentRuns.toList(),
    )

    /** A consistent copy of the state. The counts are those since the start. */
    class Snapshot(
        val lastStart: Long?,
        val lastDurationMs: Long?,
        val lastStatus: SchedulerRunStatus?,
        val lastError: String?,
        val lastErrorTime: Long?,
        val runs: Int,
        val errors: Int,
        val skipped: Int,
        val running: Boolean,
        val runningSince: Long?,
        val inactiveReason: String?,
        val lastScheduledReference: Long?,
        /** Newest first. */
        val recentRuns: List<SchedulerRunRecord>,
    )

    companion object {
        const val MAX_RECENT_RUNS = 20
    }
}
