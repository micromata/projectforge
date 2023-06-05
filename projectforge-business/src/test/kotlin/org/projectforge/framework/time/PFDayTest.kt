/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.test.TestSetup
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.util.*

class PFDayTest {

    @Test
    fun convertTest() {
        // User's time zone is "Europe/Berlin": "UTC+2". Therefore local date should be 2019-04-01 00:00:00
        var date = PFDay.from(LocalDate.of(2019, Month.APRIL, 10))

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
        formatter.timeZone = TimeZone.getTimeZone("UTC")

        val sqlDate = date.sqlDate
        assertEquals("2019-04-10", sqlDate.toString())

        date = PFDay.from(sqlDate)
        checkDate(date.date, 2019, Month.APRIL, 10)

        // 1581206400000 is UTC midnight
        val utcMidnight = Date(1581206400000)
        val europeBerlinMidnight = Date(1581202800000)
        assertEquals("2020-02-09 00:00:00.000 +0000", DateHelper.formatAsUTC(utcMidnight))
        assertEquals("2020-02-08 23:00:00.000 +0000", DateHelper.formatAsUTC(europeBerlinMidnight))
        assertEquals("2020-02-09", PFDay.fromOrNullUTC(utcMidnight)!!.isoString)
        assertEquals("2020-02-08", PFDay.fromOrNullUTC(europeBerlinMidnight)!!.isoString)
        assertEquals("2020-02-09 00:00:00.000 +0000", DateHelper.formatAsUTC(PFDay.of(2020, Month.FEBRUARY, 9).utilDateUTC))
    }

    @Test
    fun baseTest() {
        var date = PFDay.from(LocalDate.of(2019, Month.APRIL, 10))
        assertEquals(2019, date.year)
        assertEquals(Month.APRIL, date.month)
        assertEquals(4, date.monthValue)
        assertEquals(1, date.beginOfMonth.dayOfMonth)
        assertEquals(30, date.endOfMonth.dayOfMonth)

        date = PFDay.from(LocalDate.of(2019, Month.JANUARY, 1))
        assertEquals(2019, date.year)
        assertEquals(Month.JANUARY, date.month)
        assertEquals(1, date.monthValue)
        assertEquals(1, date.beginOfMonth.dayOfMonth)

        date = PFDay.from(LocalDate.of(2019, Month.JANUARY, 31)).plusMonths(1)
        assertEquals(2019, date.year)
        assertEquals(Month.FEBRUARY, date.month)
        assertEquals(28, date.dayOfMonth)

        val dateTime = PFDateTimeUtils.parseAndCreateDateTime("2019-11-30 23:00")!!
        date = PFDay.from(dateTime.utilDate)
        assertEquals(2019, date.year)
        assertEquals(Month.DECEMBER, date.month)
        assertEquals(1, date.dayOfMonth)
    }

    @Test
    fun weekOfTest() {
        ConfigurationServiceAccessor.internalSetMinimalDaysInFirstWeekForJunitTests(4)
        PFDay._weekFields = null // Force recalculation of weekFields

        val date = PFDay.withDate(2020, Month.OCTOBER, 4)
        assertEquals(40, date.weekOfYear)

        ConfigurationServiceAccessor.internalSetMinimalDaysInFirstWeekForJunitTests(null)
        PFDay._weekFields = null // Force recalculation of weekFields
    }

    private fun checkDate(date: LocalDate, year: Int, month: Month, dayOfMonth: Int) {
        assertEquals(year, date.year, "Year check failed.")
        assertEquals(month, date.month, "Month check failed.")
        assertEquals(dayOfMonth, date.dayOfMonth, "Day check failed.")
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
