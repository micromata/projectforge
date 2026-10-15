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

package org.projectforge.rest.pub

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.ical.ICalGenerator
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.time.PFDay
import java.io.ByteArrayInputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

/**
 * The holiday and week-of-year feeds: one event per holiday / per week, each spanning exactly its own day(s)
 * with the exclusive DTEND of RFC 5545.
 */
class CalendarSubscriptionServiceRestTest : AbstractTestBase() {
    @Test
    fun `each holiday is a single all-day event`() {
        val generator = ICalGenerator()
        val from = PFDay.of(2026, 1, 1)
        CalendarSubscriptionServiceRest.addHolidays(generator, from, PFDay.of(2026, 12, 31))
        val events = parse(generator)
        // At least New Year's day, Christmas and the Easter holidays.
        assert(events.size >= 8) { "Too few holidays: ${events.size}" }
        events.forEach { (start, end) ->
            assertEquals(1, ChronoUnit.DAYS.between(start, end), "Holiday $start must span one day")
            assertEquals(2026, start.year)
        }
        assertEquals(LocalDate.of(2026, 1, 1), events.first().first)
        assertEquals(events.size, uids(generator).toSet().size)
    }

    @Test
    fun `weeks of year are consecutive weeks`() {
        val generator = ICalGenerator()
        val from = PFDay.of(2024, 1, 1)
        CalendarSubscriptionServiceRest.addWeeksOfYear(generator, from, from.plusYears(6))
        val events = parse(generator)
        assert(events.size in 312..314) { "Unexpected number of weeks: ${events.size}" }
        var previous: LocalDate? = null
        events.forEach { (start, end) ->
            assertEquals(7, ChronoUnit.DAYS.between(start, end), "Week $start must span seven days")
            previous?.let { assertEquals(7, ChronoUnit.DAYS.between(it, start)) }
            previous = start
        }
        assertEquals(events.first().first.dayOfWeek, events.last().first.dayOfWeek)
        assert(events.first().first.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY))
        assertEquals(events.size, uids(generator).toSet().size)
    }

    private fun vEvents(generator: ICalGenerator): List<VEvent> {
        val calendar = CalendarBuilder().build(ByteArrayInputStream(generator.asByteArray!!))
        return calendar.getComponents<VEvent>(Component.VEVENT)
    }

    private fun parse(generator: ICalGenerator): List<Pair<LocalDate, LocalDate>> = vEvents(generator).map {
        val start = it.getDateTimeStart<Temporal>()!!.date as LocalDate
        val end = it.getDateTimeEnd<Temporal>()!!.date as LocalDate
        start to end
    }.sortedBy { it.first }

    private fun uids(generator: ICalGenerator): List<String> = vEvents(generator).map { it.uid.get().value }
}
