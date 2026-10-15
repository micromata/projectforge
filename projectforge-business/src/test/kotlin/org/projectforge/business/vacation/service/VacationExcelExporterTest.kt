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

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
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

  /**
   * Only working days of a vacation are painted (weekends and holidays inside keep their colour), 24.12. (a working
   * day by default) isn't coloured as holiday, a half day end is lighter and marked by ½, and an approved vacation
   * wins over an overlapping one in progress, whatever the order of the list.
   */
  @Test
  fun cellStylesTest() {
    val user = PFUserDO()
    user.locale = Locale.ENGLISH
    ThreadLocalUserContext.userContext = UserContext.createTestInstance(user)
    runCatching { ConfigXml.getInstance() }.onFailure { ConfigXml.internalSetInstance("<config><holidays></holidays></config>") }
    val employee = EmployeeDO().also {
      it.user = PFUserDO().also { u ->
        u.firstname = "Berta"
        u.lastname = "Smith"
      }
    }
    val approved = VacationDO().also {
      it.employee = employee
      it.startDate = LocalDate.of(2024, Month.DECEMBER, 20) // Friday
      it.endDate = LocalDate.of(2024, Month.DECEMBER, 27) // Friday
      it.halfDayEnd = true
      it.status = VacationStatus.APPROVED
    }
    val inProgress = VacationDO().also {
      it.employee = employee
      it.startDate = LocalDate.of(2024, Month.DECEMBER, 23)
      it.endDate = LocalDate.of(2024, Month.DECEMBER, 23)
      it.status = VacationStatus.IN_PROGRESS
    }
    val excel = VacationExcelExporter.export(
      LocalDate.of(2024, Month.OCTOBER, 1),
      listOf(VacationService.VacationsByEmployee(employee, listOf(approved, inProgress))),
    )
    XSSFWorkbook(excel.inputStream()).use { workbook ->
      val sheet = workbook.getSheet("Q4 2024")
      val columns = dayColumns(sheet.getRow(1), LocalDate.of(2024, Month.OCTOBER, 1))
      val employeeRow = sheet.getRow(3)
      fun fill(day: Int) = employeeRow.getCell(columns.getValue(LocalDate.of(2024, Month.DECEMBER, day)))
        .cellStyle.fillForegroundColor
      fun text(day: Int) = employeeRow.getCell(columns.getValue(LocalDate.of(2024, Month.DECEMBER, day)))
        .stringCellValue
      Assertions.assertEquals(IndexedColors.GREEN.index, fill(20))
      Assertions.assertEquals(IndexedColors.TAN.index, fill(21), "Saturday inside the vacation")
      Assertions.assertEquals(IndexedColors.TAN.index, fill(22), "Sunday inside the vacation")
      Assertions.assertEquals(IndexedColors.GREEN.index, fill(23), "approved wins over in progress")
      Assertions.assertTrue(
        fill(24) == IndexedColors.GREEN.index || fill(24) == IndexedColors.LIGHT_GREEN.index,
        "24.12. is a (half) working day, so a vacation day",
      )
      Assertions.assertNotEquals(
        IndexedColors.TAN.index,
        sheet.getRow(1).getCell(columns.getValue(LocalDate.of(2024, Month.DECEMBER, 24))).cellStyle.fillForegroundColor,
        "24.12. isn't coloured as holiday in the header",
      )
      Assertions.assertEquals(IndexedColors.TAN.index, fill(25), "holiday inside the vacation")
      Assertions.assertEquals(IndexedColors.LIGHT_GREEN.index, fill(27), "half day end")
      Assertions.assertEquals(VacationExcelExporter.HALF_DAY_MARKER, text(27))
      Assertions.assertEquals("", text(20))
    }
    // The order of the list doesn't matter.
    val start = PFDay.withDate(2024, Month.DECEMBER, 1)
    val end = PFDay.withDate(2024, Month.DECEMBER, 31)
    listOf(listOf(approved, inProgress), listOf(inProgress, approved)).forEach { vacations ->
      val cells = VacationExcelExporter.vacationCells(vacations, start, end)
      Assertions.assertEquals(VacationExcelExporter.VacationCell(approved = true, half = false), cells[LocalDate.of(2024, Month.DECEMBER, 23)])
      Assertions.assertNull(cells[LocalDate.of(2024, Month.DECEMBER, 21)])
      Assertions.assertEquals(VacationExcelExporter.VacationCell(approved = true, half = true), cells[LocalDate.of(2024, Month.DECEMBER, 27)])
    }
  }

  /** The column of each day, read from the date row (day numbers, interrupted by the month separator columns). */
  private fun dayColumns(dateRow: Row, firstDay: LocalDate): Map<LocalDate, Int> {
    val map = mutableMapOf<LocalDate, Int>()
    var day = firstDay
    for (col in 0..dateRow.lastCellNum) {
      val value = dateRow.getCell(col)?.takeIf { it.cellType == CellType.STRING }?.stringCellValue
      if (value == "${day.dayOfMonth}") {
        map[day] = col
        day = day.plusDays(1)
      }
    }
    return map
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
