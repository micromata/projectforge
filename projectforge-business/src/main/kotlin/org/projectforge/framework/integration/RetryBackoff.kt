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


package org.projectforge.framework.integration

/**
 * Retry schedule for a permanently failing remote resource (e.g. a subscribed calendar): the longer the failure
 * lasts, the less often it is retried. The schedule is computed only from the start of the failure series, so it
 * survives restarts if the start is persisted.
 *
 * Example: `RetryBackoff(listOf(Phase(15.minutes, until = 45.minutes), Phase(1.hours, until = 24.hours)), 24.hours)`
 * retries 3 times every 15 minutes, then hourly until one day after the first failure, then daily.
 *
 * @param phases Retry phases in ascending order.
 * @param finalIntervalMs Interval after the last phase.
 * @param toleranceMs A retry is already due this time before its slot. Needed if the retries are triggered by a
 * periodic job: otherwise a slot a few seconds after the job's tick would be missed and retried a whole tick later.
 */
class RetryBackoff(
    private val phases: List<Phase>,
    private val finalIntervalMs: Long,
    private val toleranceMs: Long = 0,
) {
    /**
     * Retries every [intervalMs] as long as the elapsed time since the first failure doesn't exceed [untilMs].
     */
    class Phase(val intervalMs: Long, val untilMs: Long)

    init {
        require(finalIntervalMs > 0) { "finalIntervalMs must be positive." }
        require(phases.all { it.intervalMs > 0 }) { "Intervals must be positive." }
    }

    /**
     * Next retry after the given attempt.
     *
     * @param failingSince Time of the first failure of the series (epoch millis).
     * @param lastAttempt Time of the last attempt, or null if unknown (e.g. after a restart). If unknown, the
     * last due retry is regarded as done, so a restart doesn't cause an immediate retry.
     * @return Time of the next retry (epoch millis).
     */
    fun nextAttempt(failingSince: Long, lastAttempt: Long?, now: Long = System.currentTimeMillis()): Long {
        val elapsed = (lastAttempt ?: now) - failingSince
        var offset = 0L
        for (phase in phases) {
            while (offset + phase.intervalMs <= phase.untilMs) {
                offset += phase.intervalMs
                if (offset > elapsed) {
                    return failingSince + offset
                }
            }
        }
        // Final phase: next multiple of finalIntervalMs after the elapsed time.
        val steps = (elapsed - offset) / finalIntervalMs + 1
        return failingSince + offset + steps * finalIntervalMs
    }

    fun isDue(failingSince: Long, lastAttempt: Long?, now: Long = System.currentTimeMillis()): Boolean {
        return now + toleranceMs >= nextAttempt(failingSince, lastAttempt, now)
    }
}
