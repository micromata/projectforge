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

package org.projectforge.business.vacation.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay
import java.time.Month
import java.util.*

class VacationExcelExporterTest {
  @Test
  fun sheetListTest() {
    val user = PFUserDO()
    user.locale = Locale.ENGLISH
    ThreadLocalUserContext.setUser(user)
    assert(VacationExcelExporter.getSheetsData(PFDay.withDate(2023, Month.JANUARY, 5)),
      "Q1 2023", "2023", "Q2 2023", "Q3 2023", "Q4 2023", "2024")
    assert(VacationExcelExporter.getSheetsData(PFDay.withDate(2023, Month.FEBRUARY, 5)),
      "Feb-Apr 23", "Feb-Dec 23", "Q1 2023", "Q2 2023", "Q3 2023", "Q4 2023", "2023", "2024")
    assert(VacationExcelExporter.getSheetsData(PFDay.withDate(2023, Month.MARCH, 5)),
      "Mar-May 23", "Mar-Dec 23", "Q1 2023", "Q2 2023", "Q3 2023", "Q4 2023", "2023", "2024")
    assert(VacationExcelExporter.getSheetsData(PFDay.withDate(2023, Month.APRIL, 5)),
      "Q2 2023", "Apr-Dec 23", "Q3 2023", "Q4 2023", "Q1 2024", "2023", "2024")
    assert(VacationExcelExporter.getSheetsData(PFDay.withDate(2023, Month.OCTOBER, 5)),
      "Q4 2023", "Q1 2024", "Q2 2024", "Q3 2024", "2023", "2024")
    assert(VacationExcelExporter.getSheetsData(PFDay.withDate(2023, Month.NOVEMBER, 5)),
      "Nov-Jan 24", "Nov-Dec 23", "Q4 2023", "Q1 2024", "Q2 2024", "Q3 2024", "2023", "2024")
    assert(VacationExcelExporter.getSheetsData(PFDay.withDate(2023, Month.DECEMBER, 5)),
      "Dec-Feb 24", "Dec-Dec 23", "Q4 2023", "Q1 2024", "Q2 2024", "Q3 2024", "2023", "2024")
  }

  private fun assert(list: List<VacationExcelExporter.SheetData>, vararg sheetNames: String) {
    // println(list.joinToString { it.sheetName })
    list.forEach { data ->
      Assertions.assertTrue(data.startDate.dayOfMonth == 1, "startDate must be begin of month.")
      Assertions.assertTrue(data.endDate.isSameDay(data.endDate.endOfMonth), "endDate must be end of month.")
    }
    Assertions.assertEquals(sheetNames.size, list.size, "Unexpected number of sheets.")
    sheetNames.forEachIndexed { index, s ->
      Assertions.assertEquals(s, list[index].sheetName)
    }
  }
}
