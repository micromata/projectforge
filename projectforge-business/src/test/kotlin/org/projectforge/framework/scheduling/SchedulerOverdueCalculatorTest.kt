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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.common.scheduling.SchedulerTrigger
import java.time.ZoneId
import java.time.ZonedDateTime

class SchedulerOverdueCalculatorTest {
    private val delay = ResolvedSchedule.FixedDelay(60_000, 10_000)
    private val nightly = ResolvedSchedule.resolve(SchedulerSchedule.Cron("0 0 2 * * *"), { it }, ZoneId.of("UTC"))

    @Test
    fun `fixed delay job is overdue after delay and grace`() {
        val last = T0
        // Grace: max(2 minutes, delay).
        Assertions.assertFalse(calc(delay, last, now = last + 60_000 + 120_000).overdue)
        val result = calc(delay, last, now = last + 60_000 + 120_001)
        Assertions.assertTrue(result.overdue)
        Assertions.assertEquals(last + 60_000, result.nextRun)
    }

    @Test
    fun `fixed delay job without run is due after the initial delay since the start`() {
        val result = calc(delay, null, readySince = T0, now = T0 + 5_000)
        Assertions.assertEquals(T0 + 10_000, result.nextRun)
        Assertions.assertFalse(result.overdue)
        Assertions.assertTrue(calc(delay, null, readySince = T0, now = T0 + 10_000 + 120_001).overdue)
    }

    @Test
    fun `cron job is overdue after its grace (a tenth of the period, at most 30 minutes)`() {
        val readySince = utc(2026, 10, 10, 1, 0)
        val due = utc(2026, 10, 10, 2, 0)
        Assertions.assertEquals(due, calc(nightly, null, readySince, now = readySince).nextRun)
        Assertions.assertFalse(calc(nightly, null, readySince, now = due + 30 * MINUTE).overdue)
        Assertions.assertTrue(calc(nightly, null, readySince, now = due + 30 * MINUTE + 1).overdue)
        // Ran in time: next run tomorrow.
        val ran = calc(nightly, due, readySince, now = due + 60 * MINUTE)
        Assertions.assertFalse(ran.overdue)
        Assertions.assertEquals(utc(2026, 10, 11, 2, 0), ran.nextRun)
    }

    @Test
    fun `runs before a restart don't make a job overdue`() {
        // Last run 3 days ago, restarted at 03:00 today: due tomorrow at 02:00, not overdue.
        val readySince = utc(2026, 10, 10, 3, 0)
        val result = calc(nightly, utc(2026, 10, 7, 2, 0), readySince, now = readySince + 60 * MINUTE)
        Assertions.assertFalse(result.overdue)
        Assertions.assertEquals(utc(2026, 10, 11, 2, 0), result.nextRun)
    }

    @Test
    fun `running, inactive, disabled jobs and a system not yet up are never overdue`() {
        val now = T0 + 3600_000
        Assertions.assertFalse(calc(delay, T0, now = now, running = true).overdue)
        Assertions.assertFalse(calc(delay, T0, now = now, inactive = true).overdue)
        Assertions.assertFalse(SchedulerOverdueCalculator.calculate(delay, T0, false, false, null, now).overdue)
        val disabled = calc(ResolvedSchedule.Disabled, T0, now = now)
        Assertions.assertFalse(disabled.overdue)
        Assertions.assertNull(disabled.nextRun)
        Assertions.assertTrue(calc(delay, T0, now = now).overdue)
    }

    @Test
    fun `slow threshold`() {
        val min = 60_000L
        Assertions.assertEquals(5_000L, SchedulerSlowDetector.thresholdMillis(state(ResolvedSchedule.FixedDelay(3600_000, 0), 5_000), min, 3.0))
        val hourly = state(ResolvedSchedule.FixedDelay(3600_000, 0))
        Assertions.assertEquals(min, SchedulerSlowDetector.thresholdMillis(hourly, min, 3.0))
        hourly.baselineAvgMs = 30_000
        Assertions.assertEquals(90_000L, SchedulerSlowDetector.thresholdMillis(hourly, min, 3.0))
        hourly.baselineAvgMs = 10_000
        Assertions.assertEquals(min, SchedulerSlowDetector.thresholdMillis(hourly, min, 3.0))
        // Longer than the interval between two runs is slow, but never below the minimum.
        val often = state(ResolvedSchedule.FixedDelay(120_000, 0))
        often.baselineAvgMs = 100_000
        Assertions.assertEquals(120_000L, SchedulerSlowDetector.thresholdMillis(often, min, 3.0))
        val veryOften = state(ResolvedSchedule.FixedDelay(30_000, 0))
        veryOften.baselineAvgMs = 30_000
        Assertions.assertEquals(min, SchedulerSlowDetector.thresholdMillis(veryOften, min, 3.0))
    }

    @Test
    fun `job status, the worst first`() {
        fun snapshot(running: Boolean = false, last: SchedulerRunStatus? = null, latest: SchedulerRunStatus? = last) =
            SchedulerJobState.Snapshot(
                lastStart = last?.let { T0 }, lastDurationMs = null, lastStatus = last, lastError = null,
                lastErrorTime = null, runs = 0, errors = 0, skipped = 0, running = running, runningSince = null,
                inactiveReason = null, lastScheduledReference = null,
                recentRuns = listOfNotNull(latest?.let { SchedulerRunRecord("job", T0, 0, it, SchedulerTrigger.SCHEDULED) }),
            )
        val status = SchedulerJobAdminService.Companion::statusOf
        Assertions.assertEquals(SchedulerJobStatus.RUNNING, status(snapshot(running = true), false, true, true))
        Assertions.assertEquals(SchedulerJobStatus.INACTIVE, status(snapshot(), false, true, true))
        Assertions.assertEquals(SchedulerJobStatus.OVERDUE, status(snapshot(last = SchedulerRunStatus.SUCCESS), false, false, true))
        Assertions.assertEquals(SchedulerJobStatus.SKIPPED, status(snapshot(last = SchedulerRunStatus.ERROR, latest = SchedulerRunStatus.SKIPPED), false, false, false))
        Assertions.assertEquals(SchedulerJobStatus.FAILED, status(snapshot(last = SchedulerRunStatus.ERROR), true, false, false))
        Assertions.assertEquals(SchedulerJobStatus.OK, status(snapshot(last = SchedulerRunStatus.SUCCESS), false, false, false))
        Assertions.assertEquals(SchedulerJobStatus.DISABLED, status(snapshot(last = SchedulerRunStatus.SUCCESS), true, false, false))
        Assertions.assertEquals(SchedulerJobStatus.DISABLED, status(snapshot(), true, false, false))
        Assertions.assertEquals(SchedulerJobStatus.PENDING, status(snapshot(), false, false, false))
    }

    private fun calc(
        schedule: ResolvedSchedule,
        last: Long?,
        readySince: Long? = T0 - 3600_000,
        now: Long,
        running: Boolean = false,
        inactive: Boolean = false,
    ) = SchedulerOverdueCalculator.calculate(schedule, last, running, inactive, readySince, now)

    private fun state(schedule: ResolvedSchedule, slowThresholdMillis: Long? = null) = SchedulerJobState(
        SchedulerJobDefinition(
            "test.slow", SchedulerJobArea.SYSTEM, javaClass, "run", SchedulerSchedule.FixedDelay(1_000L),
            slowThresholdMillis,
        ),
        schedule,
    )

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

    companion object {
        private const val MINUTE = 60_000L
        private val T0 = ZonedDateTime.of(2026, 10, 10, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
    }
}
