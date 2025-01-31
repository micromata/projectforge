/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.timesheet

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.Constants
import org.projectforge.business.test.TestSetup
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

class AITimeSavingsTest {
    @Test
    fun testBuildStats() {
        val timesheets = listOf(
            createTimesheet(120, "1", TimesheetDO.TimeSavedByAIUnit.HOURS),      // 1 hour saved by AI.
            createTimesheet(60, "50", TimesheetDO.TimeSavedByAIUnit.PERCENTAGE), // 1 hour saved by AI.
            createTimesheet(60, "0", TimesheetDO.TimeSavedByAIUnit.PERCENTAGE), // No AI time saved.
            createTimesheet(60, "0", TimesheetDO.TimeSavedByAIUnit.HOURS), // No AI time saved.
            createTimesheet(60, "50", null), // No unit specified (shouldn't happen).
            createTimesheet(60, null, TimesheetDO.TimeSavedByAIUnit.HOURS), // No AI time saved.
            createTimesheet(60, null, TimesheetDO.TimeSavedByAIUnit.PERCENTAGE), // No AI time saved.
        )
        val stats = AITimeSavings.buildStats(timesheets)
        Assertions.assertEquals(8 * Constants.MILLIS_PER_HOUR, stats.totalDurationMillis, "9 hours in total.")
        Assertions.assertEquals(
            2 * Constants.MILLIS_PER_HOUR,
            stats.totalTimeSavedByAIMillis,
            "1 hours saved by AI (1 hour + 50% of 2 hours)."
        )
    }

    @Test
    fun `test getTimeSavedByAIMillis`() {
        createTimesheet(120, "1", TimesheetDO.TimeSavedByAIUnit.HOURS).also {
            Assertions.assertEquals(Constants.MILLIS_PER_HOUR, AITimeSavings.getTimeSavedByAIMillis(it))
        }
        createTimesheet(120, ".5", TimesheetDO.TimeSavedByAIUnit.HOURS).also {
            Assertions.assertEquals(Constants.MILLIS_PER_HOUR / 2, AITimeSavings.getTimeSavedByAIMillis(it))
        }
        createTimesheet(9 * 60, "10", TimesheetDO.TimeSavedByAIUnit.PERCENTAGE).also {
            Assertions.assertEquals(Constants.MILLIS_PER_HOUR, AITimeSavings.getTimeSavedByAIMillis(it))
        }
        createTimesheet(60, "50", TimesheetDO.TimeSavedByAIUnit.PERCENTAGE).also {
            Assertions.assertEquals(Constants.MILLIS_PER_HOUR, AITimeSavings.getTimeSavedByAIMillis(it))
        }
        createTimesheet(60, "0", TimesheetDO.TimeSavedByAIUnit.PERCENTAGE).also {
            Assertions.assertEquals(0, AITimeSavings.getTimeSavedByAIMillis(it))
        }
        createTimesheet(60, "0", TimesheetDO.TimeSavedByAIUnit.HOURS).also {
            Assertions.assertEquals(0, AITimeSavings.getTimeSavedByAIMillis(it))
        }
        createTimesheet(60, "50", null).also {
            Assertions.assertEquals(0, AITimeSavings.getTimeSavedByAIMillis(it))
        }
        createTimesheet(60, null, TimesheetDO.TimeSavedByAIUnit.HOURS).also {
            Assertions.assertEquals(0, AITimeSavings.getTimeSavedByAIMillis(it))
        }
        createTimesheet(60, null, TimesheetDO.TimeSavedByAIUnit.PERCENTAGE).also {
            Assertions.assertEquals(0, AITimeSavings.getTimeSavedByAIMillis(it))
        }
    }

    @Test
    fun `test formatting of duration and time savings`() {
        createTimesheet(120, "1", TimesheetDO.TimeSavedByAIUnit.HOURS).also {
            Assertions.assertEquals("1:00h, 33 %", AITimeSavings.getFormattedTimeSavedByAI(it))
        }
        createTimesheet(120, "10", TimesheetDO.TimeSavedByAIUnit.PERCENTAGE).also {
            Assertions.assertEquals("0:13h, 10 %", AITimeSavings.getFormattedTimeSavedByAI(it))
        }
        createTimesheet(120, "12", TimesheetDO.TimeSavedByAIUnit.PERCENTAGE).also {
            // 15,84 minutes -> 16 minutes (half round up)
            Assertions.assertEquals("0:16h, 12 %", AITimeSavings.getFormattedTimeSavedByAI(it))
        }
        createTimesheet(7 * 60, "3", TimesheetDO.TimeSavedByAIUnit.PERCENTAGE).also {
            // 12,98 minutes n-> 13 minutes (half round up)
            Assertions.assertEquals("0:13h, 3,0 %", AITimeSavings.getFormattedTimeSavedByAI(it))
        }
    }

    private fun createTimesheet(
        durationMinutes: Long,
        timeSavedByAIString: String?,
        timeSavedByAIUnit: TimesheetDO.TimeSavedByAIUnit?
    ): TimesheetDO {
        val timeSavedByAI = timeSavedByAIString?.toBigDecimalOrNull()
        return TimesheetDO().also {
            it.startTime = START_TIME
            it.stopTime = Date(START_TIME.time + durationMinutes * Constants.MILLIS_PER_MINUTE)
            it.timeSavedByAI = timeSavedByAI
            it.timeSavedByAIUnit = timeSavedByAIUnit
        }
    }

    companion object {
        private val START_TIME =
            Date.from(LocalDateTime.of(2025, Month.JANUARY, 25, 22, 43).atZone(ZoneId.of("UTC")).toInstant())

        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
