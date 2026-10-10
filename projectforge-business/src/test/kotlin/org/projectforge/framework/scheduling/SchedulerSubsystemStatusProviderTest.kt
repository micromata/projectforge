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
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.integration.SubsystemState
import org.projectforge.framework.integration.SyncStats

class SchedulerSubsystemStatusProviderTest : AbstractTestBase() {
    @Test
    fun `state of the scheduler tile`() {
        Assertions.assertEquals(SubsystemState.UNKNOWN, state(job("a", SchedulerJobStatus.PENDING)))
        Assertions.assertEquals(
            SubsystemState.OK,
            state(job("a", SchedulerJobStatus.OK, lastRun = 1), job("b", SchedulerJobStatus.PENDING)),
        )
        Assertions.assertEquals(
            SubsystemState.DEGRADED,
            state(job("a", SchedulerJobStatus.OK, lastRun = 1), job("b", SchedulerJobStatus.FAILED, lastRun = 1)),
        )
        Assertions.assertEquals(SubsystemState.DEGRADED, state(job("a", SchedulerJobStatus.OVERDUE, overdue = true)))
        Assertions.assertEquals(
            SubsystemState.DOWN,
            state(job("a", SchedulerJobStatus.OK, lastRun = 1), job("jobs.tidyUp", SchedulerJobStatus.OVERDUE, overdue = true)),
        )
    }

    @Test
    fun `failed and overdue jobs are shown as syncs`() {
        val status = SchedulerSubsystemStatusProvider.statusOf(
            listOf(
                job("a", SchedulerJobStatus.OK, lastRun = 1),
                job("b", SchedulerJobStatus.FAILED, lastRun = 5, lastError = "broken"),
                job("support.logAggregationFlush", SchedulerJobStatus.OVERDUE, overdue = true, nextRun = 7),
            ),
        )
        Assertions.assertEquals(2, status.syncs.size)
        status.syncs[0].let {
            Assertions.assertEquals("title b", it.type)
            Assertions.assertEquals(SyncStats.Status.ERRORS, it.lastStatus)
            Assertions.assertEquals("broken", it.lastError)
            Assertions.assertEquals(5L, it.lastErrorDate)
        }
        status.syncs[1].let {
            Assertions.assertEquals(SyncStats.Status.ABORTED, it.lastStatus)
            Assertions.assertTrue(it.lastError!!.contains("title support.logAggregationFlush"), it.lastError)
            Assertions.assertEquals(7L, it.lastErrorDate)
        }
        Assertions.assertTrue(status.detail!!.contains("3"), status.detail)
    }

    private fun state(vararg jobs: SchedulerJobEntry) = SchedulerSubsystemStatusProvider.statusOf(jobs.toList()).state

    private fun job(
        id: String,
        status: SchedulerJobStatus,
        lastRun: Long? = null,
        overdue: Boolean = false,
        lastError: String? = null,
        nextRun: Long? = null,
    ) = SchedulerJobEntry(
        id = id, title = "title $id", description = null, area = "SYSTEM", areaTitle = "System",
        scheduleType = "FIXED_DELAY", cron = null, delayMillis = 60_000, initialDelayMillis = 0, zone = "UTC",
        status = status, inactiveReason = null, runningSince = null, lastRun = lastRun, lastDurationMs = null,
        lastStatus = null, lastError = lastError, lastErrorTime = lastRun.takeIf { lastError != null },
        nextRun = nextRun, overdue = overdue, runs7d = 0, errors7d = 0, skipped7d = 0, slow7d = 0,
        avgDurationMs7d = null, maxDurationMs7d = null, runNowAllowed = true,
    )
}
