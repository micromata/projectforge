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


package org.projectforge.framework.calendar

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.business.test.TestConfigurationParams
import org.projectforge.business.test.TestSetup
import org.projectforge.framework.configuration.ConfigurationJsonValues
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.time.PFDay
import java.math.BigDecimal
import java.time.Month

class HolidaysConfigTest {
    @Test
    fun configuredHolidays() {
        val holidays = Holidays.instance
        Assertions.assertTrue(holidays.isHoliday(PFDay.of(2009, Month.MAY, 1)), "Added by configuration.")
        Assertions.assertTrue(holidays.isHoliday(PFDay.of(2009, Month.FEBRUARY, 23)), "Shrove Monday.")
        Assertions.assertFalse(holidays.isHoliday(PFDay.of(2009, Month.FEBRUARY, 24)), "Shrove Tuesday ignored.")
        Assertions.assertEquals(0, BigDecimal("0.5").compareTo(holidays.getWorkFraction(PFDay.of(2009, Month.DECEMBER, 24))))
    }

    @Test
    fun recomputedOnChange() {
        val holidays = Holidays.instance
        try {
            Assertions.assertTrue(holidays.isHoliday(PFDay.of(2009, Month.OCTOBER, 3)))
            ConfigurationJsonValues.internalSetForTest(ConfigurationParam.HOLIDAYS, "[]")
            Assertions.assertFalse(holidays.isHoliday(PFDay.of(2009, Month.OCTOBER, 3)))
            Assertions.assertTrue(holidays.isHoliday(PFDay.of(2009, Month.FEBRUARY, 24)), "Shrove Tuesday not ignored anymore.")
        } finally {
            ConfigurationJsonValues.internalSetForTest(ConfigurationParam.HOLIDAYS, TestConfigurationParams.HOLIDAYS_JSON)
        }
    }

    @Test
    fun parse() {
        Assertions.assertTrue(HolidaysConfig.parse(null).isEmpty())
        Assertions.assertTrue(HolidaysConfig.parse("no json").isEmpty())
        HolidaysConfig.parse(ConfigurationParam.HOLIDAYS.defaultStringValue).let {
            Assertions.assertEquals(listOf(HolidayDefinition.XMAS_EVE, HolidayDefinition.NEW_YEARS_EVE), it.map { h -> h.id })
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            TestSetup.init()
        }
    }
}
