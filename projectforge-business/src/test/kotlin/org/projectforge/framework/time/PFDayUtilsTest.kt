/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.test.TestSetup
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class PFDayUtilsTest {

    @Test
    fun numberOfWorkingDaysTest() {
        assertBigDecimal(0.5, PFDayUtils.getNumberOfWorkingDays(LocalDate.of(2019, Month.DECEMBER, 24), LocalDate.of(2019, Month.DECEMBER, 24)))
        assertBigDecimal(0.5, PFDayUtils.getNumberOfWorkingDays(LocalDate.of(2019, Month.DECEMBER, 31), LocalDate.of(2019, Month.DECEMBER, 31)))
        assertBigDecimal(1.0, PFDayUtils.getNumberOfWorkingDays(LocalDate.of(2019, Month.DECEMBER, 30), LocalDate.of(2019, Month.DECEMBER, 30)))
        assertBigDecimal(3.0, PFDayUtils.getNumberOfWorkingDays(LocalDate.of(2019, Month.DECEMBER, 24), LocalDate.of(2019, Month.DECEMBER, 31)))
    }

    @Test
    fun nextWorkingDayTest() {
        checkNextWorkingDay(LocalDate.of(2020, Month.JANUARY, 10), 2020, Month.JANUARY, 10)
        checkNextWorkingDay(LocalDate.of(2020, Month.JANUARY, 11), 2020, Month.JANUARY, 13)
        checkNextWorkingDay(LocalDate.of(2019, Month.DECEMBER, 25), 2019, Month.DECEMBER, 27)
    }

    private fun checkNextWorkingDay(date: LocalDate, year: Int, month: Month, dayOfMonth: Int) {
        checkDate(PFDayUtils.getNextWorkingDay(date), year, month, dayOfMonth)
        checkDate(PFDayUtils.getNextWorkingDay(PFDay.from(date)).localDate, year, month, dayOfMonth)

    }

    private fun checkDate(date: LocalDate, year: Int, month: Month, dayOfMonth: Int) {
        assertEquals(year, date.year, "Year check failed.")
        assertEquals(month, date.month, "Month check failed.")
        assertEquals(dayOfMonth, date.dayOfMonth, "Day check failed.")
    }

    private fun assertBigDecimal(expected: Double, actual: BigDecimal, msg: String = "") {
        Assertions.assertEquals(expected, actual.toDouble(), msg)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
