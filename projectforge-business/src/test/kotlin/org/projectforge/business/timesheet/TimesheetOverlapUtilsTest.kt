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

package org.projectforge.business.timesheet

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.Constants
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

class TimesheetOverlapUtilsTest {
    @Test
    fun `disjoint time sheets keep their full duration`() {
        val a = createTimesheet(1, 0, 60) // 1 hour
        val b = createTimesheet(2, 120, 60) // 1 hour, starting after a
        val split = TimesheetOverlapUtils.splitDurations(listOf(a, b))
        Assertions.assertEquals(HOUR, split[1L])
        Assertions.assertEquals(HOUR, split[2L])
        Assertions.assertEquals(2 * HOUR, TimesheetOverlapUtils.unionDurationMillis(listOf(a, b)))
    }

    @Test
    fun `two fully overlapping time sheets are split 50-50`() {
        val a = createTimesheet(1, 0, 60)
        val b = createTimesheet(2, 0, 60)
        val split = TimesheetOverlapUtils.splitDurations(listOf(a, b))
        Assertions.assertEquals(HOUR / 2, split[1L])
        Assertions.assertEquals(HOUR / 2, split[2L])
        Assertions.assertEquals(HOUR, TimesheetOverlapUtils.unionDurationMillis(listOf(a, b)))
    }

    @Test
    fun `three fully overlapping time sheets are split into thirds`() {
        val sheets = listOf(createTimesheet(1, 0, 60), createTimesheet(2, 0, 60), createTimesheet(3, 0, 60))
        val split = TimesheetOverlapUtils.splitDurations(sheets)
        // Sum must be exactly one hour despite the remainder of the integer division.
        Assertions.assertEquals(HOUR, split.values.sum())
        Assertions.assertEquals(HOUR, TimesheetOverlapUtils.unionDurationMillis(sheets))
        // Each gets roughly a third.
        split.values.forEach { Assertions.assertTrue(it in (HOUR / 3)..(HOUR / 3 + 1)) }
    }

    @Test
    fun `partial overlap splits only the shared interval`() {
        // a: 0..60min, b: 30..90min -> overlap 30..60 (30min shared).
        val a = createTimesheet(1, 0, 60)
        val b = createTimesheet(2, 30, 60)
        val split = TimesheetOverlapUtils.splitDurations(listOf(a, b))
        // a: 30min alone + 15min (half of shared 30min) = 45min. Same for b.
        Assertions.assertEquals(45 * MINUTE, split[1L])
        Assertions.assertEquals(45 * MINUTE, split[2L])
        // Union = 0..90min = 90min.
        Assertions.assertEquals(90 * MINUTE, TimesheetOverlapUtils.unionDurationMillis(listOf(a, b)))
    }

    private fun createTimesheet(id: Long, startOffsetMinutes: Long, durationMinutes: Long): TimesheetDO {
        return TimesheetDO().also {
            it.id = id
            it.startTime = Date(START_TIME.time + startOffsetMinutes * MINUTE)
            it.stopTime = Date(START_TIME.time + (startOffsetMinutes + durationMinutes) * MINUTE)
        }
    }

    companion object {
        private const val MINUTE = Constants.MILLIS_PER_MINUTE
        private const val HOUR = Constants.MILLIS_PER_HOUR
        private val START_TIME =
            Date.from(LocalDateTime.of(2026, Month.JANUARY, 5, 8, 0).atZone(ZoneId.of("UTC")).toInstant())
    }
}
