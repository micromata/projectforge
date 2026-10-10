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

import org.projectforge.Constants

/**
 * When a job is expected to run next and whether it is overdue (it didn't run, although it should have).
 */
object SchedulerOverdueCalculator {
    class Result(val nextRun: Long?, val overdue: Boolean)

    /**
     * @param lastScheduledReference See [SchedulerJobState.lastScheduledReference].
     * @param readySince Since when the system is up (runs before a restart don't count). Null if not yet up: nothing
     * is overdue then.
     */
    fun calculate(
        schedule: ResolvedSchedule,
        lastScheduledReference: Long?,
        running: Boolean,
        inactive: Boolean,
        readySince: Long?,
        now: Long,
    ): Result {
        val start = readySince ?: now
        val next = when (schedule) {
            is ResolvedSchedule.FixedDelay ->
                lastScheduledReference?.let { it + schedule.delayMillis } ?: (start + schedule.initialDelayMillis)

            is ResolvedSchedule.Cron -> schedule.nextAfter(maxOf(lastScheduledReference ?: start, start))
            else -> null
        }
        if (next == null || running || inactive || readySince == null) {
            return Result(next, false)
        }
        return Result(next, now > next + graceMillis(schedule, next))
    }

    internal fun graceMillis(schedule: ResolvedSchedule, next: Long): Long = when (schedule) {
        is ResolvedSchedule.FixedDelay -> maxOf(MIN_GRACE, schedule.delayMillis)
        else -> ((schedule.periodAround(next) ?: 0L) / 10).coerceIn(MIN_GRACE, MAX_CRON_GRACE)
    }

    private const val MIN_GRACE = 2 * Constants.MILLIS_PER_MINUTE
    private const val MAX_CRON_GRACE = 30 * Constants.MILLIS_PER_MINUTE
}

/**
 * Whether a run is slow: it took longer than the threshold of its definition or, by default, longer than
 * [slowFactor] times its average duration (of the last days, if known) and longer than [minSlowMillis]. A run taking
 * longer than the interval between two runs is slow as well (if longer than [minSlowMillis]).
 */
object SchedulerSlowDetector {
    fun thresholdMillis(
        state: SchedulerJobState,
        minSlowMillis: Long,
        slowFactor: Double,
        now: Long = System.currentTimeMillis(),
    ): Long {
        state.definition.slowThresholdMillis?.let { return it }
        val byAverage = state.baselineAvgMs?.let { (it * slowFactor).toLong() } ?: 0L
        val threshold = maxOf(minSlowMillis, byAverage)
        val period = state.schedule.periodAround(now) ?: return threshold
        return maxOf(minSlowMillis, minOf(threshold, period))
    }
}
