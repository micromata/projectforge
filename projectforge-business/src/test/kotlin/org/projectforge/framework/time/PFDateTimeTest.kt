/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.test.TestSetup
import java.text.SimpleDateFormat
import java.time.DateTimeException
import java.time.Month
import java.time.ZonedDateTime
import java.util.*

class PFDateTimeTest {

    @Test
    fun beginAndEndOfIntervalsTest() {
        // User's time zone is "Europe/Berlin": "UTC+2". Therefore local date should be 2019-04-01 00:00:00
        val date = PFDateTimeUtils.parseAndCreateDateTime("2019-03-31 22:00:00")!!
        checkDate(date.dateTime, 2019, Month.APRIL, 1, false)

        val beginOfDay = date.beginOfDay.dateTime
        checkDate(beginOfDay, 2019, Month.APRIL, 1, true)
        val endOfDay = date.endOfDay.dateTime
        checkDate(endOfDay, 2019, Month.APRIL, 1, true)

        val beginOfWeek = date.beginOfWeek.dateTime
        checkDate(beginOfWeek, 2019, Month.APRIL, 1, true)
        val endOfWeek = date.endOfWeek.dateTime
        checkDate(endOfWeek, 2019, Month.APRIL, 7, true) // Midnight of first day of next week

        val beginOfMonth = date.beginOfMonth.dateTime
        checkDate(beginOfMonth, 2019, Month.APRIL, 1, true)
        val endOfMonth = date.endOfMonth.dateTime
        checkDate(endOfMonth, 2019, Month.APRIL, 30, true) // Midnight of first day of next month
    }

    @Test
    fun convertTest() {
        // User's time zone is "Europe/Berlin": "UTC+2". Therefore local date should be 2019-04-01 00:00:00
        var date = PFDateTimeUtils.parseAndCreateDateTime("2019-03-31 22:00:00")!!

        var localDate = date.localDate
        assertEquals(2019, localDate.year)
        assertEquals(Month.APRIL, localDate.month)
        assertEquals(1, localDate.dayOfMonth)

        val utilDate = date.utilDate
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        assertEquals("2019-03-31 22:00:00 +0000", formatter.format(utilDate))
        assertEquals(1554069600000, utilDate.time)

        date = PFDateTimeUtils.parseAndCreateDateTime("2019-04-01 15:00:00")!!

        localDate = date.localDate
        assertEquals(2019, localDate.year)
        assertEquals(Month.APRIL, localDate.month)
        assertEquals(1, localDate.dayOfMonth)
    }

    @Test
    fun parseTest() {
        assertEquals("2019-03-31 22:00", PFDateTimeUtils.parseAndCreateDateTime("1554069600")!!.isoString)
        assertEquals("2019-03-31 22:00", PFDateTimeUtils.parseAndCreateDateTime("2019-03-31 22:00:00")!!.isoString)
        assertEquals("2019-03-31 22:00", PFDateTimeUtils.parseAndCreateDateTime("2019-03-31 22:00")!!.isoString)
        assertEquals("2019-03-31 22:00", PFDateTimeUtils.parseAndCreateDateTime("2019-03-31T22:00:00.000Z")!!.isoString)
        try {
            PFDateTimeUtils.parseAndCreateDateTime("2019-03-31")
            fail("Exception expected, because 2019-03-31 isn't parseable due to missing time of day.")
        } catch (ex: DateTimeException) {
            // OK
        }
    }

    @Test
    fun daysOfYearTest() {
        var dateTime = PFDateTimeUtils.parseAndCreateDateTime("2020-01-10 10:00")
        assertEquals(366, dateTime!!.numberOfDaysInYear)
        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2019-12-31 23:00")
        assertEquals(366, dateTime!!.numberOfDaysInYear, "Europe-Berlin: 2020! UTC: ${dateTime.isoString}")
        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2019-12-31 22:00")
        assertEquals(365, dateTime!!.numberOfDaysInYear, "Europe-Berlin: 2020! UTC: ${dateTime.isoString}")
    }

    @Test
    fun sqlDateTest() {
        var sqlDate = PFDateTimeUtils.parseAndCreateDateTime("2019-12-06 23:30")!!.sqlDate
        var localDate = sqlDate.toLocalDate()
        assertEquals(2019, localDate.year)
        assertEquals(Month.DECEMBER, localDate.month)
        assertEquals(7, localDate.dayOfMonth)
        sqlDate = PFDateTimeUtils.parseAndCreateDateTime("2019-12-06 22:30")!!.sqlDate
        localDate = sqlDate.toLocalDate()
        assertEquals(2019, localDate.year)
        assertEquals(Month.DECEMBER, localDate.month)
        assertEquals(6, localDate.dayOfMonth)
    }

    @Test
    fun weekOfYearTest() {
        val storedDefaultLocale = ConfigurationServiceAccessor.get().defaultLocale
        ConfigurationServiceAccessor.internalSetLocaleForJunitTests(Locale("de", "DE"))
        PFDay._weekFields = null // Force recalculation of weekFields

        checkISOWeeks()

        ConfigurationServiceAccessor.internalSetLocaleForJunitTests(Locale("en", "US"))
        PFDay._weekFields = null // Force recalculation of weekFields
        // US weeks:
        var dateTime = PFDateTimeUtils.parseAndCreateDateTime("2020-12-31 10:00")
        assertEquals(1, dateTime!!.weekOfYear)
        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2021-01-02 10:00")
        assertEquals(1, dateTime!!.weekOfYear)
        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2021-01-04 10:00")
        assertEquals(2, dateTime!!.weekOfYear)

        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2019-12-31 10:00")
        assertEquals(1, dateTime!!.weekOfYear)
        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2020-01-02 10:00")
        assertEquals(1, dateTime!!.weekOfYear)

        ConfigurationServiceAccessor.internalSetMinimalDaysInFirstWeekForJunitTests(4)
        PFDay._weekFields = null // Force recalculation of weekFields
        checkISOWeeks()
        ConfigurationServiceAccessor.internalSetMinimalDaysInFirstWeekForJunitTests(null)

        ConfigurationServiceAccessor.internalSetLocaleForJunitTests(storedDefaultLocale)
        PFDay._weekFields = null // Force recalculation of weekFields
    }

    private fun checkISOWeeks() {
        // German weeks:
        var dateTime = PFDateTimeUtils.parseAndCreateDateTime("2020-12-31 10:00")
        assertEquals(53, dateTime!!.weekOfYear)
        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2021-01-02 10:00")
        assertEquals(53, dateTime!!.weekOfYear)
        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2021-01-04 10:00")
        assertEquals(1, dateTime!!.weekOfYear)

        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2019-12-31 10:00")
        assertEquals(1, dateTime!!.weekOfYear)
        dateTime = PFDateTimeUtils.parseAndCreateDateTime("2020-01-02 10:00")
        assertEquals(1, dateTime!!.weekOfYear)
    }

    @Test
    fun ensurePrecision() {
        assertPrecision("1970-11-20 23:00:00", "1970-11-21 21:04:50", DatePrecision.DAY)

        assertPrecision("1970-11-21 04:00:00", "1970-11-21 04:50:23", DatePrecision.HOUR_OF_DAY)

        assertPrecision("1970-11-21 04:50:00", "1970-11-21 04:50:23", DatePrecision.MINUTE)

        assertPrecision("1970-11-21 04:00:00", "1970-11-21 04:00:00", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:00:00", "1970-11-21 04:07:59", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:15:00", "1970-11-21 04:08:00", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:15:00", "1970-11-21 04:15:00", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:15:00", "1970-11-21 04:22:59", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:30:00", "1970-11-21 04:23:00", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:30:00", "1970-11-21 04:30:00", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:30:00", "1970-11-21 04:37:59", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:45:00", "1970-11-21 04:38:00", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:45:00", "1970-11-21 04:45:00", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 04:45:00", "1970-11-21 04:52:59", DatePrecision.MINUTE_15)
        assertPrecision("1970-11-21 05:00:00", "1970-11-21 04:53:00", DatePrecision.MINUTE_15)

        assertPrecision("1970-11-21 04:00:00", "1970-11-21 04:02:59", DatePrecision.MINUTE_5)
        assertPrecision("1970-11-21 04:05:00", "1970-11-21 04:03:00", DatePrecision.MINUTE_5)
        assertPrecision("1970-11-21 04:50:00", "1970-11-21 04:48:00", DatePrecision.MINUTE_5)
        assertPrecision("1970-11-21 04:50:00", "1970-11-21 04:52:59", DatePrecision.MINUTE_5)
        assertPrecision("1970-11-21 04:55:00", "1970-11-21 04:53:00", DatePrecision.MINUTE_5)
        assertPrecision("1970-11-21 04:55:00", "1970-11-21 04:57:59", DatePrecision.MINUTE_5)
        assertPrecision("1970-11-21 05:00:00", "1970-11-21 04:58:00", DatePrecision.MINUTE_5)

        assertPrecision("1970-11-21 04:50:23", "1970-11-21 04:50:23", DatePrecision.SECOND)
    }

    private fun assertPrecision(expected: String, dateString: String, precision: DatePrecision) {
        val dt = PFDateTimeUtils.parseAndCreateDateTime(dateString)!!.withNano(123456).withPrecision(precision)
        if (precision == DatePrecision.MILLISECOND) {
            assertEquals(123000, dt.nano)
        } else {
            assertEquals(0, dt.nano)
        }
        assertEquals(expected, dt.isoStringSeconds)
    }


    private fun checkDate(date: ZonedDateTime, year: Int, month: Month, dayOfMonth: Int, checkMidnight: Boolean = true) {
        assertEquals(year, date.year, "Year check failed.")
        assertEquals(month, date.month, "Month check failed.")
        assertEquals(dayOfMonth, date.dayOfMonth, "Day check failed.")
        if (checkMidnight)
            checkMidnight(date)
    }

    private fun checkMidnight(date: ZonedDateTime) {
        if (date.second == 59) {
            checkTime(date, 23, 59, 59, 999999999)
        } else {
            checkTime(date, 0, 0, 0, 0)
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
