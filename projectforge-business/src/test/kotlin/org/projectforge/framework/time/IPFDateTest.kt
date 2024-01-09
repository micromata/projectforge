/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.time

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.projectforge.test.TestSetup
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Month
import java.time.ZonedDateTime

class IPFDateTest {
    enum class TimeCheck { NONE, BEGIN_OF_DAY, END_OF_DAY }

    @Test
    fun dayTest() {
        allTests(PFDay.withDate(2019, Month.DECEMBER, 24))
    }

    @Test
    fun dateTimeTest() {
        allTests(PFDateTime.withDate(2019, Month.DECEMBER, 24, 8, 17, 26, 123))
    }

    private fun <T : IPFDate<T>> allTests(date: T) {
        baseTests(date)
        testSameDay(date)
        testGetNumberOfWorkingDays(date)
        testJavaDates(date)
        testDaysBetween(date)
        testAddWorkingDays(date)
    }

    private fun <T : IPFDate<T>> baseTests(date: T) {
        val checkBeginOfDay = if (date is PFDateTime) TimeCheck.BEGIN_OF_DAY else TimeCheck.NONE
        val checkEndOfDay = if (date is PFDateTime) TimeCheck.END_OF_DAY else TimeCheck.NONE

        checkDate(date.beginOfYear, 2019, Month.JANUARY, 1, checkBeginOfDay)
        checkDate(date.endOfYear, 2019, Month.DECEMBER, 31, checkEndOfDay)

        checkDate(date.beginOfMonth, 2019, Month.DECEMBER, 1, checkBeginOfDay)
        checkDate(date.endOfMonth, 2019, Month.DECEMBER, 31, checkEndOfDay)

        checkDate(date.beginOfWeek, 2019, Month.DECEMBER, 23, checkBeginOfDay)
        checkDate(date.endOfWeek, 2019, Month.DECEMBER, 29, checkEndOfDay)

        val myDate = date.withDayOfMonth(30)
        checkDate(myDate.beginOfWeek, 2019, Month.DECEMBER, 30, checkBeginOfDay)
        checkDate(myDate.endOfWeek, 2020, Month.JANUARY, 5, checkEndOfDay)

        try {
            date.withDayOfWeek(0)
            fail("IllegalArgumentException expected")
        } catch (ex: IllegalArgumentException) {
            // OK
        }
        try {
            date.withDayOfWeek(8)
            fail("IllegalArgumentException expected")
        } catch (ex: IllegalArgumentException) {
            // OK
        }
        assertEquals(DayOfWeek.MONDAY, date.withDayOfWeek(1).dayOfWeek)
        assertEquals(DayOfWeek.TUESDAY, date.withDayOfWeek(2).dayOfWeek)
        assertEquals(DayOfWeek.WEDNESDAY, date.withDayOfWeek(3).dayOfWeek)
        assertEquals(DayOfWeek.THURSDAY, date.withDayOfWeek(4).dayOfWeek)
        assertEquals(DayOfWeek.FRIDAY, date.withDayOfWeek(5).dayOfWeek)
        assertEquals(DayOfWeek.SATURDAY, date.withDayOfWeek(6).dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, date.withDayOfWeek(7).dayOfWeek)

        assertTrue(date.withDayOfWeek(1).isFirstDayOfWeek)
        assertFalse(date.isBeginOfWeek)
        assertTrue(date.beginOfWeek.isBeginOfWeek)

        for (day in 1..5) {
            assertFalse(date.withDayOfWeek(day).isWeekend())
        }
        assertTrue(date.withDayOfWeek(6).isWeekend())
        assertTrue(date.withDayOfWeek(7).isWeekend())
    }

    private fun <T : IPFDate<T>> testSameDay(date: T) {
        val myDate = date.withYear(2008).withMonth(Month.MARCH).withDayOfMonth(5)
        var otherDate = myDate.plusDays(1)
        assertFalse(myDate.isSameDay(otherDate))
        otherDate = otherDate.minusDays(2)
        assertFalse(myDate.isSameDay(otherDate))
        otherDate = otherDate.plusDays(1)
        assertTrue(myDate.isSameDay(otherDate))
    }

    private fun <T : IPFDate<T>> testJavaDates(date: T) {
        val myDate = date.withYear(2008).withMonth(Month.MARCH).withDayOfMonth(5)
        assertEquals("2008-03-05", PFDateTimeUtils.formatUTCDate(myDate.sqlDate))
        if (date is PFDateTime) {
            assertEquals(myDate.format(PFDateTime.isoDateTimeFormatterMilli), PFDateTimeUtils.formatUTCDate(myDate.utilDate))
        } else {
            // UTC-Date of Europe/Berlin (GMT + 1)
            assertEquals("2008-03-04 23:00:00.000", PFDateTimeUtils.formatUTCDate(myDate.utilDate))
        }
    }

    private fun <T : IPFDate<T>> testGetNumberOfWorkingDays(date: T) {
        var fromDay = date.withYear(2009).withMonth(Month.JANUARY).withDayOfMonth(1)
        var toDay = fromDay.withDayOfMonth(31)
        var days = PFDayUtils.getNumberOfWorkingDays(fromDay, toDay)
        assertTrue(BigDecimal(21).compareTo(days) == 0, "Unexpected number of days: $days")
        toDay = toDay.withMonth(Month.FEBRUARY).withDayOfMonth(28)
        days = PFDayUtils.getNumberOfWorkingDays(fromDay, toDay)
        assertTrue(BigDecimal(41).compareTo(days) == 0, "Unexpected number of days: $days")

        fromDay = fromDay.withMonth(Month.DECEMBER).withDayOfMonth(24)
        toDay = fromDay.plusDays(4) // Until 28.12.
        days = PFDayUtils.getNumberOfWorkingDays(fromDay, toDay)
        // 24.12. is a half working day and the 28.12. is a full working day.
        assertTrue(BigDecimal(1.5).compareTo(days) == 0, "Unexpected number of days: $days")
    }

    private fun <T : IPFDate<T>> testAddWorkingDays(date: T) {
        var myDate = date.withYear(2010).withMonth( Month.MAY).withDayOfMonth( 21) // Friday
        myDate = PFDayUtils.addWorkingDays(myDate, 0)
        checkDate(myDate, 2010, Month.MAY, 21)

        myDate = PFDayUtils.addWorkingDays(myDate, 1) // Skip saturday, sunday and whit monday and weekend.
        checkDate(myDate, 2010, Month.MAY, 25)

        myDate = PFDayUtils.addWorkingDays(myDate, -1) // Skip saturday, sunday and whit monday and weekend.
        checkDate(myDate, 2010, Month.MAY, 21)

        myDate = PFDayUtils.addWorkingDays(myDate, 1) // Skip saturday, sunday and whit monday and weekend.
        myDate = PFDayUtils.addWorkingDays(myDate, -6) // Skip saturday, sunday and whit monday and weekends.
        checkDate(myDate, 2010, Month.MAY, 14)
    }

    private fun <T : IPFDate<T>> testDaysBetween(date: T) {
        var date1 = date.withYear(2008).withMonth(Month.MARCH).withDayOfMonth(23)
        var date2 = date.withYear(2008).withMonth(Month.MARCH).withDayOfMonth(23)
        assertEquals(0, date1.daysBetween(date2))
        date2 = date2.withDayOfMonth(24)
        assertEquals(1, date1.daysBetween(date2))

        date2 = date2.withDayOfMonth(22)
        assertEquals(-1, date1.daysBetween(date2))

        date2 = date1.plusDays(364)
        assertEquals(364, date1.daysBetween(date2))

        date1 = date1.withYear(2010).withMonth(Month.JANUARY).withDayOfMonth(1)
        date2 = date1.withMonth(Month.DECEMBER).withDayOfMonth(31)
        assertEquals(364, date1.daysBetween(date2))
    }


    private fun checkDate(date: IPFDate<*>, year: Int, month: Month, dayOfMonth: Int, checkMode: TimeCheck = TimeCheck.NONE) {
        assertEquals(year, date.year, "Year check failed.")
        assertEquals(month, date.month, "Month check failed.")
        assertEquals(dayOfMonth, date.dayOfMonth, "Day check failed.")
        if (date is PFDateTime) {
            when (checkMode) {
                TimeCheck.BEGIN_OF_DAY -> checkTime(date.dateTime, 0, 0, 0, 0)
                TimeCheck.END_OF_DAY -> checkTime(date.dateTime, 23, 59, 59, 999999999)
                else -> {}
            }
        }
    }

    private fun checkTime(date: ZonedDateTime, hour: Int, minute: Int, second: Int, nanos: Int) {
        assertEquals(hour, date.hour, "Hour check failed.")
        assertEquals(minute, date.minute)
        assertEquals(second, date.second)
        assertEquals(nanos, date.nano)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
