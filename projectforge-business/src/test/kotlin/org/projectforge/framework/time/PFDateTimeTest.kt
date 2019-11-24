/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.test.TestSetup
import java.text.SimpleDateFormat
import java.time.DateTimeException
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class PFDateTimeTest {

    @Test
    fun beginAndEndOfIntervalsTest() {
        // User's time zone is "Europe/Berlin": "UTC+2". Therefore local date should be 2019-04-01 00:00:00
        val date = PFDateTime.parseUTCDate("2019-03-31 22:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!
        checkDate(date.dateTime, 2019, Month.APRIL, 1, false)

        val beginOfDay = date.beginOfDay.dateTime
        checkDate(beginOfDay, 2019, Month.APRIL, 1, true)
        val endOfDay = date.endOfDay.dateTime
        checkDate(endOfDay, 2019, Month.APRIL, 2, true)

        val beginOfWeek = date.beginOfWeek.dateTime
        checkDate(beginOfWeek, 2019, Month.APRIL, 1, true)
        val endOfWeek = date.endOfWeek.dateTime
        checkDate(endOfWeek, 2019, Month.APRIL, 8, true) // Midnight of first day of next week

        val beginOfMonth = date.beginOfMonth.dateTime
        checkDate(beginOfMonth, 2019, Month.APRIL, 1, true)
        val endOfMonth = date.endOfMonth.dateTime
        checkDate(endOfMonth, 2019, Month.MAY, 1, true) // Midnight of first day of next month
    }

    @Test
    fun convertTest() {
        // User's time zone is "Europe/Berlin": "UTC+2". Therefore local date should be 2019-04-01 00:00:00
        var date = PFDateTime.parseUTCDate("2019-03-31 22:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!

        var localDate = date.localDate
        assertEquals(2019, localDate.year)
        assertEquals(Month.APRIL, localDate.month)
        assertEquals(1, localDate.dayOfMonth)

        val utilDate = date.utilDate
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        assertEquals("2019-03-31 22:00:00 +0000", formatter.format(utilDate))
        assertEquals(1554069600000, utilDate.time)

        date = PFDateTime.parseUTCDate("2019-04-01 15:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!

        localDate = date.localDate
        assertEquals(2019, localDate.year)
        assertEquals(Month.APRIL, localDate.month)
        assertEquals(1, localDate.dayOfMonth)
    }

    @Test
    fun parseTest() {
        assertEquals("2019-03-31 22:00", PFDateTime.parseUTCDate("1554069600")!!.isoString)
        assertEquals("2019-03-31 22:00", PFDateTime.parseUTCDate("2019-03-31 22:00:00")!!.isoString)
        assertEquals("2019-03-31 22:00", PFDateTime.parseUTCDate("2019-03-31 22:00")!!.isoString)
        assertEquals("2019-03-31 22:00", PFDateTime.parseUTCDate("2019-03-31T22:00:00.000Z")!!.isoString)
        try {
            PFDateTime.parseUTCDate("2019-03-31")
            fail("Exception expected, because 2019-03-31 isn't parseable due to missing time of day.")
        } catch (ex: DateTimeException) {
            // OK
        }
    }

    @Test
    fun daysOfYearTest() {
        var dateTime = PFDateTime.parseUTCDate("2020-01-10 10:00")
        assertEquals(366, dateTime!!.numberOfDaysInYear)
        dateTime = PFDateTime.parseUTCDate("2019-12-31 23:00")
        assertEquals(366, dateTime!!.numberOfDaysInYear, "Europe-Berlin: 2020! UTC: ${dateTime.isoString}")
        dateTime = PFDateTime.parseUTCDate("2019-12-31 22:00")
        assertEquals(365, dateTime!!.numberOfDaysInYear, "Europe-Berlin: 2020! UTC: ${dateTime.isoString}")
    }

    @Test
    fun weekOfYearTest() {
        // German weeks:
        var dateTime = PFDateTime.parseUTCDate("2020-12-31 10:00")
        assertEquals(53, dateTime!!.weekOfYear)
        dateTime = PFDateTime.parseUTCDate("2021-01-02 10:00")
        assertEquals(53, dateTime!!.weekOfYear)
        dateTime = PFDateTime.parseUTCDate("2021-01-04 10:00")
        assertEquals(1, dateTime!!.weekOfYear)

        dateTime = PFDateTime.parseUTCDate("2019-12-31 10:00")
        assertEquals(1, dateTime!!.weekOfYear)
        dateTime = PFDateTime.parseUTCDate("2020-01-02 10:00")
        assertEquals(1, dateTime!!.weekOfYear)

        // US weeks:
        val contextUser = ThreadLocalUserContext.getUser();
        val storedLocale = contextUser.locale
        contextUser.locale = Locale.US
        dateTime = PFDateTime.parseUTCDate("2020-12-31 10:00")
        assertEquals(1, dateTime!!.weekOfYear)
        dateTime = PFDateTime.parseUTCDate("2021-01-02 10:00")
        assertEquals(1, dateTime!!.weekOfYear)
        dateTime = PFDateTime.parseUTCDate("2021-01-04 10:00")
        assertEquals(2, dateTime!!.weekOfYear)

        dateTime = PFDateTime.parseUTCDate("2019-12-31 10:00")
        assertEquals(1, dateTime!!.weekOfYear)
        dateTime = PFDateTime.parseUTCDate("2020-01-02 10:00")
        assertEquals(1, dateTime!!.weekOfYear)

        contextUser.locale = storedLocale
    }

    private fun checkDate(date: ZonedDateTime, year: Int, month: Month, dayOfMonth: Int, checkMidnight: Boolean = true) {
        assertEquals(year, date.year, "Year check failed.")
        assertEquals(month, date.month, "Month check failed.")
        assertEquals(dayOfMonth, date.dayOfMonth, "Day check failed.")
        if (checkMidnight)
            checkMidnight(date)
    }

    private fun checkMidnight(date: ZonedDateTime) {
        checkTime(date, 0, 0, 0)
    }

    private fun checkTime(date: ZonedDateTime, hour: Int, minute: Int, second: Int) {
        assertEquals(hour, date.hour, "Hour check failed.")
        assertEquals(minute, date.minute)
        assertEquals(second, date.second)
        assertEquals(0, date.nano)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
