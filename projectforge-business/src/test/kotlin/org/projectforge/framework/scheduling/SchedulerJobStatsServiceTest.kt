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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.common.scheduling.SchedulerTrigger
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class SchedulerJobStatsServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var statsService: SchedulerJobStatsService

    @Autowired
    private lateinit var registry: SchedulerJobRegistry

    private lateinit var savedZone: ZoneId
    private var savedMaxRuns = 0

    @BeforeEach
    fun setUpZone() {
        savedZone = statsService.zone
        savedMaxRuns = statsService.maxStoredRunsPerJobAndDay
        statsService.zone = ZoneOffset.UTC
    }

    @AfterEach
    fun restore() {
        statsService.zone = savedZone
        statsService.maxStoredRunsPerJobAndDay = savedMaxRuns
    }

    @Test
    fun `days are merged across flushes, the runs not yet written included`() {
        val job = "test.stats.merge"
        val day = LocalDate.of(2026, 9, 1)
        record(job, day, 10, 100)
        record(job, day, 11, 300, SchedulerTrigger.MANUAL)
        statsService.flush()
        record(job, day, 12, 200, status = SchedulerRunStatus.ERROR, error = "broken")
        record(job, day, 13, 0, status = SchedulerRunStatus.SKIPPED)
        // Not yet written, but shown:
        statsService.days(job, day).single().second.let {
            Assertions.assertEquals(3, it.runCount)
            Assertions.assertEquals(1, it.skippedCount)
        }
        statsService.flush()
        val (periodStart, stats) = statsService.days(job, day).single()
        Assertions.assertEquals(day, periodStart)
        Assertions.assertEquals(3, stats.runCount)
        Assertions.assertEquals(2, stats.successCount)
        Assertions.assertEquals(1, stats.errorCount)
        Assertions.assertEquals(1, stats.skippedCount)
        Assertions.assertEquals(1, stats.manualCount)
        Assertions.assertEquals(100L, stats.minDurationMs)
        Assertions.assertEquals(300L, stats.maxDurationMs)
        Assertions.assertEquals(200L, stats.avgDurationMs)
        Assertions.assertEquals(SchedulerRunStatus.ERROR, stats.lastStatus)
        Assertions.assertEquals("broken", stats.lastErrorMessage)
        Assertions.assertEquals(millis(day, 12), stats.lastErrorTime)
        Assertions.assertEquals(3, statsService.statsSince(day, job)[job]!!.runCount)
        Assertions.assertTrue(statsService.days(job, day.plusDays(1)).isEmpty())

        val runs = statsService.storedRuns(job, 10)
        Assertions.assertEquals(listOf(SchedulerRunStatus.ERROR), runs.map { it.status }, "Only failed and slow runs.")
        Assertions.assertEquals("broken", runs.single().errorMessage)
    }

    @Test
    fun `the single runs stored per job and day are capped`() {
        val job = "test.stats.cap"
        val day = LocalDate.of(2026, 9, 2)
        statsService.maxStoredRunsPerJobAndDay = 3
        repeat(5) { record(job, day, it, 10, status = SchedulerRunStatus.ERROR, error = "error $it") }
        record(job, day.plusDays(1), 1, 70_000, slow = true)
        statsService.flush()
        val runs = statsService.storedRuns(job, 100)
        Assertions.assertEquals(4, runs.size)
        Assertions.assertTrue(runs.first().slow)
        Assertions.assertEquals(listOf("error 2", "error 1", "error 0"), runs.drop(1).map { it.errorMessage })
        Assertions.assertEquals(5, statsService.days(job, day).first().second.errorCount, "All runs are counted.")
    }

    @Test
    fun `old days are aggregated into months, old months and runs deleted`() {
        val job = "test.stats.cleanup"
        statsService.dailyRetentionDays = 90
        statsService.monthlyRetentionMonths = 60
        statsService.runRetentionDays = 90
        record(job, LocalDate.of(2026, 1, 10), 1, 100)
        record(job, LocalDate.of(2026, 1, 20), 1, 300, status = SchedulerRunStatus.ERROR, error = "january")
        record(job, LocalDate.of(2026, 2, 5), 1, 200)
        record(job, LocalDate.of(2026, 3, 2), 1, 400) // Kept: cutoff is 2026-03-01.
        record(job, LocalDate.of(2026, 3, 20), 1, 50, status = SchedulerRunStatus.ERROR, error = "march")
        record(job, LocalDate.of(2020, 1, 5), 1, 100) // Older than 60 months.
        statsService.flush()

        val now = millis(LocalDate.of(2026, 6, 15), 12)
        val first = statsService.cleanup(now)
        Assertions.assertTrue(first.aggregatedDays >= 4)
        Assertions.assertTrue(first.deletedMonths >= 1)
        Assertions.assertTrue(first.deletedRuns >= 1)
        checkAfterCleanup(job)

        val second = statsService.cleanup(now)
        Assertions.assertEquals(0, second.aggregatedDays, "Nothing aggregated twice.")
        checkAfterCleanup(job)
    }

    private fun checkAfterCleanup(job: String) {
        val months = statsService.months(job)
        Assertions.assertEquals(listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)), months.map { it.first })
        months[0].second.let {
            Assertions.assertEquals(2, it.runCount)
            Assertions.assertEquals(1, it.errorCount)
            Assertions.assertEquals(100L, it.minDurationMs)
            Assertions.assertEquals(300L, it.maxDurationMs)
            Assertions.assertEquals("january", it.lastErrorMessage)
        }
        Assertions.assertEquals(1, months[1].second.runCount)
        Assertions.assertEquals(
            listOf(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 20)),
            statsService.days(job, LocalDate.of(2000, 1, 1)).map { it.first },
        )
        Assertions.assertEquals(listOf("march"), statsService.storedRuns(job, 10).map { it.errorMessage })
    }

    @Test
    fun `the last run is restored after a restart`() {
        val definition = SchedulerJobDefinition(
            "test.stats.restore", SchedulerJobArea.SYSTEM, javaClass, "run", SchedulerSchedule.FixedDelay(60_000L),
        )
        registry.register(definition, {})
        val state = registry.get(definition.id)!!
        val start = System.currentTimeMillis() - 3_600_000
        statsService.recordRun(
            state, SchedulerRunRecord(definition.id, start, 1_234, SchedulerRunStatus.ERROR, SchedulerTrigger.SCHEDULED, errorMessage = "failed"),
        )
        statsService.flush()
        Assertions.assertNull(state.snapshot().lastStart, "Only counted by the registry.")
        statsService.onApplicationReady()
        state.snapshot().let {
            Assertions.assertEquals(start, it.lastStart)
            Assertions.assertEquals(1_234L, it.lastDurationMs)
            Assertions.assertEquals(SchedulerRunStatus.ERROR, it.lastStatus)
            Assertions.assertEquals("failed", it.lastError)
            Assertions.assertEquals(0, it.runs, "The counts are those since the start.")
        }
    }

    private fun record(
        job: String,
        day: LocalDate,
        hour: Int,
        durationMs: Long,
        trigger: SchedulerTrigger = SchedulerTrigger.SCHEDULED,
        status: SchedulerRunStatus = SchedulerRunStatus.SUCCESS,
        error: String? = null,
        slow: Boolean = false,
    ) {
        val record = SchedulerRunRecord(job, millis(day, hour), durationMs, status, trigger, slow, errorMessage = error)
        statsService.recordRun(state, record)
    }

    private fun millis(day: LocalDate, hour: Int): Long =
        day.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private val state = SchedulerJobState(
        SchedulerJobDefinition("test.stats", SchedulerJobArea.SYSTEM, javaClass, "run", SchedulerSchedule.FixedDelay(60_000L)),
        ResolvedSchedule.FixedDelay(60_000, 0),
    )
}
