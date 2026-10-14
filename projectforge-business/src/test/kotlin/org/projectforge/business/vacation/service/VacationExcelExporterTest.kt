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

package org.projectforge.business.vacation.service

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay
import java.time.LocalDate
import java.time.Month
import java.util.*

class VacationExcelExporterTest {
  @Test
  fun sheetListTest() {
    val user = PFUserDO()
    user.locale = Locale.ENGLISH
    // Without a Spring context: setUser would refresh the user from the UserGroupCache, which isn't there.
    ThreadLocalUserContext.userContext = UserContext.createTestInstance(user)
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

  /** A vacation beginning before a sheet's first month is painted on that sheet, too (from its first day on). */
  @Test
  fun vacationBeginningBeforeSheetTest() {
    val user = PFUserDO()
    user.locale = Locale.ENGLISH
    ThreadLocalUserContext.userContext = UserContext.createTestInstance(user)
    // The holidays need a configuration; without a Spring context there is none unless another test set it.
    runCatching { ConfigXml.getInstance() }.onFailure { ConfigXml.internalSetInstance("<config><holidays></holidays></config>") }
    val employee = EmployeeDO().also {
      it.user = PFUserDO().also { u ->
        u.firstname = "Berta"
        u.lastname = "Smith"
      }
    }
    val vacation = VacationDO().also {
      it.employee = employee
      it.startDate = LocalDate.of(2023, Month.MARCH, 28)
      it.endDate = LocalDate.of(2023, Month.APRIL, 4)
      it.status = VacationStatus.APPROVED
    }
    val excel = VacationExcelExporter.export(
      LocalDate.of(2023, Month.APRIL, 5),
      listOf(VacationService.VacationsByEmployee(employee, listOf(vacation))),
    )
    XSSFWorkbook(excel.inputStream()).use { workbook ->
      // Q2 2023 starts in April: the vacation shows there although it began in March.
      val employeeRow = workbook.getSheet("Q2 2023").getRow(3)
      Assertions.assertEquals("Berta Smith", employeeRow.getCell(0).stringCellValue)
      Assertions.assertEquals("X", employeeRow.getCell(1).stringCellValue)
      // The Q3 sheet doesn't contain it.
      Assertions.assertNotEquals("X", workbook.getSheet("Q3 2023").getRow(3).getCell(1)?.stringCellValue)
    }
  }

  @AfterEach
  fun clearUser() {
    ThreadLocalUserContext.clear()
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
