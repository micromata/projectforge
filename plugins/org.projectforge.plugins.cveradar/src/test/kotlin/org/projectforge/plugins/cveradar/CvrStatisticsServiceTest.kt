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

package org.projectforge.plugins.cveradar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.plugins.cveradar.CvrStatisticsService.Companion.bucketize
import org.projectforge.plugins.cveradar.CvrStatisticsService.Granularity
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/** The periods of the statistics: window, week starting on Monday, empty periods, time zone of the user. */
class CvrStatisticsServiceTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 10, 28) // A Wednesday.

    private fun at(day: String, time: String = "12:00", zone: ZoneId = berlin) =
        Date.from(LocalDate.parse(day).atTime(java.time.LocalTime.parse(time)).atZone(zone).toInstant())

    @Test
    fun `days of the window are all there, also without entries`() {
        val buckets = bucketize(listOf(at("2026-10-28"), at("2026-10-28"), at("2026-10-27")), Granularity.DAY, berlin, today)
        assertEquals(CvrStatisticsService.DAYS.toInt(), buckets.size)
        assertEquals(today, buckets.last().periodStart)
        assertEquals(2, buckets.last().count)
        assertEquals(1, buckets[buckets.size - 2].count)
        assertEquals(0, buckets.first().count)
    }

    @Test
    fun `a week starts on Monday`() {
        val buckets = bucketize(listOf(at("2026-10-26"), at("2026-10-25")), Granularity.WEEK, berlin, today)
        assertEquals(CvrStatisticsService.WEEKS.toInt(), buckets.size)
        assertEquals(LocalDate.of(2026, 10, 26), buckets.last().periodStart)
        assertEquals(1, buckets.last().count, "Monday belongs to the current week")
        assertEquals(1, buckets[buckets.size - 2].count, "Sunday to the week before")
    }

    @Test
    fun `months are counted by the first day`() {
        val buckets = bucketize(listOf(at("2026-10-01", "00:30"), at("2026-09-30", "23:30")), Granularity.MONTH, berlin, today)
        assertEquals(CvrStatisticsService.MONTHS.toInt(), buckets.size)
        assertEquals(LocalDate.of(2026, 10, 1), buckets.last().periodStart)
        assertEquals(1, buckets.last().count)
        assertEquals(1, buckets[buckets.size - 2].count)
    }

    @Test
    fun `the day is the one of the time zone of the user`() {
        // 23:30 in Berlin is already the next day in Tokyo.
        val date = at("2026-10-27", "23:30")
        val tokyo = ZoneId.of("Asia/Tokyo")
        assertEquals(1, bucketize(listOf(date), Granularity.DAY, berlin, today).single { it.periodStart == LocalDate.of(2026, 10, 27) }.count)
        assertEquals(1, bucketize(listOf(date), Granularity.DAY, tokyo, today).single { it.periodStart == LocalDate.of(2026, 10, 28) }.count)
    }
}
