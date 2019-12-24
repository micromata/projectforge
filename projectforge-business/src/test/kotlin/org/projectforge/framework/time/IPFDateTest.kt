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
import org.projectforge.test.TestSetup
import java.time.Month
import java.time.ZonedDateTime

class IPFDateTest {
    enum class TimeCheck { NONE, BEGIN_OF_DAY, END_OF_DAY }

    @Test
    fun dayTest() {
        baseTests(PFDay.withDate(2019, Month.DECEMBER, 24))
    }

    @Test
    fun dateTimeTest() {
        baseTests(PFDateTime.withDate(2019, Month.DECEMBER, 24, 8, 17, 26, 123))
    }

    private fun <T : IPFDate<T>> baseTests(date: T) {
        val checkBeginOfDay = if (date is PFDateTime) TimeCheck.BEGIN_OF_DAY else TimeCheck.NONE
        val checkEndOfDay = if (date is PFDateTime) TimeCheck.END_OF_DAY else TimeCheck.NONE

        checkDate(date.beginOfYear, 2019, Month.JANUARY, 1, checkBeginOfDay)
        checkDate(date.endOfYear, 2019, Month.DECEMBER, 31, checkEndOfDay)

        checkDate(date.beginOfMonth, 2019, Month.DECEMBER, 1, checkBeginOfDay)
        checkDate(date.endOfMonth, 2019, Month.DECEMBER, 31, checkEndOfDay)
    }

    private fun checkDate(date: IPFDate<*>, year: Int, month: Month, dayOfMonth: Int, checkMode: TimeCheck = TimeCheck.NONE) {
        assertEquals(year, date.year, "Year check failed.")
        assertEquals(month, date.month, "Month check failed.")
        assertEquals(dayOfMonth, date.dayOfMonth, "Day check failed.")
        if (date is PFDateTime) {
            when (checkMode) {
                TimeCheck.BEGIN_OF_DAY -> checkTime(date.dateTime, 0, 0, 0, 0)
                TimeCheck.END_OF_DAY -> checkTime(date.dateTime, 23, 59, 59, 999999999)
            }
        }
    }

    private fun checkTime(date: ZonedDateTime, hour: Int, minute: Int, second: Int, nanos: Int) {
        assertEquals(hour, date.hour, "Hour check failed.")
        assertEquals(minute, date.minute)
        assertEquals(second, date.second)
        assertEquals(nanos, date.nano )
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
