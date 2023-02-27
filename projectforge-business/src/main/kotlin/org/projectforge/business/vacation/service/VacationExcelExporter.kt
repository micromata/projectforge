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

package org.projectforge.business.vacation.service

import de.micromata.merlin.excel.ExcelRow
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.xmlstream.XmlHelper
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import java.io.File
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*

/**
 * Exports vacation entries of users.
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
object VacationExcelExporter {
  private enum class Mode { NORMAL, QUARTER, YEAR }
  private class Context(val workbook: ExcelWorkbook) {
    lateinit var currentSheet: ExcelSheet
      private set
    var mode = Mode.NORMAL
    val monthSeparatorCols = mutableListOf<Int>()
    val monthSeparationStyle = ExcelUtils.createCellStyle(
      workbook,
      "monthSeparation",
      fillForegroundColor = IndexedColors.BLACK,
    )
    val boldFont = workbook.createOrGetFont("bold", bold = true, heightInPoints = 18)
    val monthStyle =
      ExcelUtils.createCellStyle(workbook, "month", font = boldFont, alignment = HorizontalAlignment.CENTER)
    val standardStyle = ExcelUtils.createCellStyle(workbook, "standard", borderStyle = BorderStyle.THIN)
    val standardDayStyle = ExcelUtils.createCellStyle(
      workbook,
      "standardDay",
      alignment = HorizontalAlignment.CENTER,
      borderStyle = BorderStyle.THIN,
    )
    val holidayAndWeekendStyle = ExcelUtils.createCellStyle(
      workbook,
      "holidayWeekendDay",
      alignment = HorizontalAlignment.CENTER,
      fillForegroundColor = IndexedColors.TAN,
      borderStyle = BorderStyle.THIN,
    )
    val vacationStyle = ExcelUtils.createCellStyle(
      workbook,
      "vacationDay",
      fillForegroundColor = IndexedColors.GREEN,
      borderStyle = BorderStyle.THIN,
    )
    val unapprovedVacationStyle = ExcelUtils.createCellStyle(
      workbook,
      "unapprovedVacationDay",
      fillForegroundColor = IndexedColors.GREY_40_PERCENT,
      borderStyle = BorderStyle.THIN,
    )

    fun withSheet(sheet: ExcelSheet, mode: Mode) {
      currentSheet = sheet
      this.mode = mode
      monthSeparatorCols.clear()
    }
  }

  fun export(
    date: LocalDate = LocalDate.now(),
    vacationsByEmployee: List<VacationService.VacationsByEmployee>,
    numberOfMonths: Int = 3,
  ): ByteArray {
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale!!).use { workbook ->
      val context = Context(workbook)
      val startDate = PFDay.from(date).beginOfMonth
      createSheet(context, startDate, vacationsByEmployee, numberOfMonths)
      // 4 quarters:
      val startMonth = startDate.monthValue // (1-12)
      var quarterStart = when (startMonth) {
        1, 2, 3 -> startDate.withMonth(1)
        4, 5, 6 -> startDate.withMonth(4)
        7, 8, 9 -> startDate.withMonth(7)
        else -> startDate.withMonth(10)
      }
      for (count in 1..4) {
        createSheet(context, quarterStart, vacationsByEmployee, 3, Mode.QUARTER)
        quarterStart = quarterStart.plusMonths(3)
      }
      // 2 years:
      var yearBegin = startDate.beginOfYear
      for (count in 1..2) {
        createSheet(context, yearBegin, vacationsByEmployee, 12, Mode.YEAR)
        yearBegin = yearBegin.plusYears(1)
      }
      // Legend
      val sheet = workbook.createOrGetSheet(translate("legend"))
      var row = sheet.createRow()
      row.getCell(0).setCellStyle(context.unapprovedVacationStyle)
      row.getCell(1).setCellValue(translate("vacation.status.inProgress"))
      row = sheet.createRow()
      row.getCell(0).setCellStyle(context.vacationStyle)
      row.getCell(1).setCellValue(translate("vacation.status.approved"))
      sheet.setColumnWidth(0, COL_WIDTH_DAY)
      return workbook.asByteArrayOutputStream.toByteArray()
    }
  }

  /**
   * @param startDate must be the beginning of a month.
   */
  private fun createSheet(
    context: Context,
    startDate: PFDay,
    vacationsByEmployee: List<VacationService.VacationsByEmployee>,
    numberOfMonths: Int = 3,
    mode: Mode = Mode.NORMAL
  ) {
    val endDate = startDate.plusMonths(numberOfMonths.toLong() - 1).endOfMonth
    val sheetName = if (mode == Mode.YEAR) {
      startDate.year.toString()
    } else if (mode == Mode.QUARTER) {
      val q = when(startDate.monthValue) {
        1, 2, 3 -> 1
        4, 5, 6 -> 2
        7, 8, 9 -> 3
        else -> 4
      }
      "Q$q ${startDate.year}"
    } else {
      val fromMonth = startDate.month.getDisplayName(TextStyle.SHORT, ThreadLocalUserContext.locale)
      val untilMonth =
        "${endDate.month.getDisplayName(TextStyle.SHORT, ThreadLocalUserContext.locale)} ${endDate.year - 2000}"
      "$fromMonth-$untilMonth"
    }
    val workbook = context.workbook
    val sheet = workbook.createOrGetSheet(sheetName)
    context.withSheet(sheet, mode)
    sheet.poiSheet.printSetup.landscape = true
    sheet.poiSheet.printSetup.fitWidth = 1.toShort()  // Doesn't work
    sheet.poiSheet.printSetup.fitHeight = 0.toShort() // Doesn't work
    val monthRow = sheet.createRow()
    val dateRow = sheet.createRow()
    val weekDayRow = sheet.createRow()
    var currentDate = startDate
    var columnIndex = 0
    sheet.setColumnWidth(columnIndex, COL_WIDTH_USER) // Column of vacationers.
    createMonthSeparationCells(context, ++columnIndex, dateRow, weekDayRow, monthRow)
    var paranoiaCounter = 0
    var firstDayOfMonthCol = columnIndex + 1
    val columnIndexMap = mutableMapOf<LocalDate, Int>()
    while (currentDate <= endDate && paranoiaCounter++ < 500) {
      // Add day columns.
      val style = if (currentDate.isHolidayOrWeekend()) {
        context.holidayAndWeekendStyle
      } else {
        context.standardDayStyle
      }
      columnIndexMap[currentDate.date] = ++columnIndex // Store for finding column indexes by vacation dates.
      sheet.setColumnWidth(columnIndex, COL_WIDTH_DAY)
      dateRow.getCell(columnIndex).setCellValue("${currentDate.dayOfMonth}").setCellStyle(style)
      weekDayRow.getCell(columnIndex).setCellValue(getWeekDayString(currentDate)).setCellStyle(style)
      currentDate = currentDate.plusDays(1)
      if (currentDate.dayOfMonth == 1) {
        // New month started.
        val monthCell =
          sheet.setMergedRegion(
            monthRow.rowNum,
            monthRow.rowNum,
            firstDayOfMonthCol,
            columnIndex,
            getMonthString(currentDate.minusMonths(1)),
          )
        monthCell.setCellStyle(context.monthStyle)
        createMonthSeparationCells(context, ++columnIndex, dateRow, weekDayRow, monthRow)
        firstDayOfMonthCol = columnIndex + 1 // Store column for setMergedRegion of month name.
      }
    }
    vacationsByEmployee.sortedBy { it.employee.user?.getFullname() }.forEach { entry ->
      val employee = entry.employee
      val employeeRow = sheet.createRow()
      employeeRow.getCell(0).setCellValue(employee.user?.getFullname() ?: "???").setCellStyle(context.standardStyle)
      createMonthSeparationCells(context, employeeRow)
      paranoiaCounter = 0
      currentDate = startDate
      while (currentDate <= endDate && paranoiaCounter++ < 500) {
        columnIndexMap[currentDate.date]?.let { col ->
          if (currentDate.isHolidayOrWeekend()) {
            employeeRow.getCell(col).setCellStyle(context.holidayAndWeekendStyle)
          } else {
            employeeRow.getCell(col).setCellStyle(context.standardDayStyle)
          }
        }
        currentDate = currentDate.plusDays(1)
      }
      entry.vacations.forEach { vacation ->
        val vacationStart = PFDay.fromOrNull(vacation.startDate)
        val vacationEnd = PFDay.fromOrNull(vacation.endDate)
        if (vacationStart != null && vacationEnd != null) {
          val style = if (vacation.status == VacationStatus.APPROVED) {
            context.vacationStyle
          } else {
            context.unapprovedVacationStyle
          }
          paranoiaCounter = 0
          var current: PFDay = vacationStart
          while (current <= vacationEnd && current <= endDate && paranoiaCounter++ < 500) {
            val col = columnIndexMap[current.date] ?: continue
            employeeRow.getCell(col).setCellStyle(style)
            current = current.plusDays(1)
          }
        }
      }
    }
    sheet.createRow().getCell(0).setCellValue("(${PFDay.now().isoString})")
    sheet.createFreezePane(1, 3)
  }

  private fun getWeekDayString(day: PFDay): String {
    return day.dayOfWeek.getDisplayName(TextStyle.SHORT, ThreadLocalUserContext.locale).take(1)
  }

  private fun getMonthString(day: PFDay): String {
    return "${day.month.getDisplayName(TextStyle.FULL, ThreadLocalUserContext.locale)} ${day.year}"
  }

  private fun createMonthSeparationCells(context: Context, col: Int, vararg rows: ExcelRow) {
    rows.forEach { row ->
      row.getCell(col).setCellStyle(context.monthSeparationStyle)
    }
    if (!context.monthSeparatorCols.contains(col)) {
      context.monthSeparatorCols.add(col)
      context.currentSheet.setColumnWidth(col, 100)
    }
  }

  private fun createMonthSeparationCells(context: Context, row: ExcelRow) {
    context.monthSeparatorCols.forEach { col ->
      row.getCell(col).setCellStyle(context.monthSeparationStyle)
    }
  }

  fun download(vacationsByEmployee: List<VacationService.VacationsByEmployee>): ResponseEntity<ByteArrayResource> {
    val workbook = export(vacationsByEmployee = vacationsByEmployee)
    val filename = ("Vacation-${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx")
    val resource = ByteArrayResource(workbook)
    return ResponseEntity.ok()
      .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
      .body(resource)
  }

  @JvmStatic
  fun main(vararg args: String) {
    val xml = XmlHelper.replaceQuotes(
      """<config>
  <holidays>
    <holiday label='Erster Mai' month='5' dayOfMonth='1'/>
    <holiday label='Dritter Oktober' month='10' dayOfMonth='3'/>
    <holiday id='XMAS_EVE' workingDay='true' workFraction='0.5'/>
    <holiday id='SHROVE_TUESDAY' ignore='true'/>
    <holiday id='NEW_YEARS_EVE' workingDay='true' workFraction='0.5'/>
  </holidays>
</config>"""
    )
    ConfigXml.internalSetInstance(xml)
    val vacationsByEmployee = mutableListOf<VacationService.VacationsByEmployee>()
    val kai = createEmployee("Kai", "Reinhard")
    var vacations = mutableListOf<VacationDO>()
    vacations.add(createVacation(kai, LocalDate.of(2023, Month.FEBRUARY, 25), 20))
    vacations.add(createVacation(kai, LocalDate.of(2023, Month.APRIL, 2), 1, status = VacationStatus.IN_PROGRESS))
    vacationsByEmployee.add(VacationService.VacationsByEmployee(kai, vacations))
    val berta = createEmployee("Berta", "Müller")
    vacations = mutableListOf()
    vacations.add(
      createVacation(
        berta,
        LocalDate.of(2023, Month.FEBRUARY, 27),
        10,
        status = VacationStatus.IN_PROGRESS,
      )
    )
    vacations.add(createVacation(berta, LocalDate.of(2023, Month.APRIL, 2), 1))
    vacationsByEmployee.add(VacationService.VacationsByEmployee(berta, vacations))
    val workbook = export(date = LocalDate.of(2023, Month.FEBRUARY, 27), vacationsByEmployee = vacationsByEmployee, numberOfMonths = 3)
    val file = File("/tmp", "vacation.xlsx")
    println("Writing excel file `${file.absolutePath}'...")
    file.writeBytes(workbook)
  }

  private fun createEmployee(firstname: String, lastname: String): EmployeeDO {
    val employee = EmployeeDO()
    val user = PFUserDO()
    user.firstname = firstname
    user.lastname = lastname
    employee.user = user
    return employee
  }

  private fun createVacation(
    employee: EmployeeDO,
    startDate: LocalDate,
    days: Long,
    status: VacationStatus? = VacationStatus.APPROVED,
  ): VacationDO {
    val vacation = VacationDO()
    vacation.employee = employee
    vacation.startDate = startDate
    vacation.endDate = startDate.plusDays(days - 1)
    vacation.status = status
    return vacation
  }

  private const val COL_WIDTH_DAY = 600
  private const val COL_WIDTH_USER = 20 * 256
}
