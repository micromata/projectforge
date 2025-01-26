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
import org.junit.jupiter.api.Test
import org.projectforge.Constants
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

class AITimeSavingsTest {
    @Test
    fun testBuildStats() {
        val timesheets = listOf(
            createTimesheet(120, BigDecimal(1), TimesheetDO.TimeSavedByAIUnit.HOURS),
            createTimesheet(120, BigDecimal(50), TimesheetDO.TimeSavedByAIUnit.PERCENTAGE),
            createTimesheet(60, BigDecimal(0), TimesheetDO.TimeSavedByAIUnit.PERCENTAGE), // No AI time saved.
            createTimesheet(60, BigDecimal(0), TimesheetDO.TimeSavedByAIUnit.HOURS), // No AI time saved.
            createTimesheet(60, BigDecimal(50), null), // No unit specified (shouldn't happen).
            createTimesheet(60, null, TimesheetDO.TimeSavedByAIUnit.HOURS), // No AI time saved.
            createTimesheet(60, null, TimesheetDO.TimeSavedByAIUnit.PERCENTAGE), // No AI time saved.
        )
        val stats = AITimeSavings.buildStats(timesheets)
        Assertions.assertEquals(9 * Constants.MILLIS_PER_HOUR, stats.totalDurationMillis, "9 hours in total.")
        Assertions.assertEquals(
            2 * Constants.MILLIS_PER_HOUR,
            stats.totalTimeSavedByAIMillis,
            "2 hours saved by AI (1 hour + 50% of 2 hours)."
        )
    }

    private fun createTimesheet(
        durationMinutes: Long,
        timeSavedByAI: BigDecimal?,
        timeSavedByAIUnit: TimesheetDO.TimeSavedByAIUnit?
    ): TimesheetDO {
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
    }
}
