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

package org.projectforge.birthdaybutler

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.Month

class BithdayButlerServiceTest {
  @Test
  fun testGetYear() {
    val year = LocalDateTime.now().year
    testGetYear(currentMonth = Month.JANUARY, month = Month.JANUARY, year)
    testGetYear(currentMonth = Month.JANUARY, month = Month.DECEMBER, year)
    testGetYear(currentMonth = Month.FEBRUARY, month = Month.JANUARY, year)
    testGetYear(currentMonth = Month.FEBRUARY, month = Month.FEBRUARY, year)
    testGetYear(currentMonth = Month.FEBRUARY, month = Month.DECEMBER, year)
    testGetYear(currentMonth = Month.JUNE, month = Month.JANUARY, year + 1)
    testGetYear(currentMonth = Month.JUNE, month = Month.MARCH, year + 1)
    testGetYear(currentMonth = Month.JUNE, month = Month.APRIL, year)
    testGetYear(currentMonth = Month.JUNE, month = Month.MAY, year)
    testGetYear(currentMonth = Month.JUNE, month = Month.DECEMBER, year)
    testGetYear(currentMonth = Month.DECEMBER, month = Month.JANUARY, year + 1)
    testGetYear(currentMonth = Month.DECEMBER, month = Month.SEPTEMBER, year + 1)
    testGetYear(currentMonth = Month.DECEMBER, month = Month.OCTOBER, year)
    testGetYear(currentMonth = Month.DECEMBER, month = Month.NOVEMBER, year)
    testGetYear(currentMonth = Month.DECEMBER, month = Month.DECEMBER, year)
  }

  private fun testGetYear(currentMonth: Month, month: Month, expectedYear: Int) {
    Assertions.assertEquals(expectedYear, BirthdayButlerService.getYear(month, currentMonth))
  }
}
