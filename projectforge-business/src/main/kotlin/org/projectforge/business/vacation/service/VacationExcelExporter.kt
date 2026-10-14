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

import de.micromata.merlin.excel.ExcelRow
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.util.CellRangeAddress
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.xmlstream.XmlHelper
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Exports vacation entries of users.
 * @author K. Reinhard
 */
object VacationExcelExporter {
  private class Context(val workbook: ExcelWorkbook) {
    lateinit var currentSheet: ExcelSheet
      private set
    val monthSeparatorCols = mutableListOf<Int>()
    val monthSeparationStyle = ExcelUtils.createCellStyle(
      workbook,
      "monthSeparation",
      fillForegroundColor = IndexedColors.BLACK,
    )
    val boldFont = ExcelUtils.createFont(workbook, "bold", bold = true, heightInPoints = 18)
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
    // Half vacation days (half day begin/end, or a half working day such as 24.12.) are lighter and marked by ½.
    val halfVacationStyle = ExcelUtils.createCellStyle(
      workbook,
      "halfVacationDay",
      alignment = HorizontalAlignment.CENTER,
      fillForegroundColor = IndexedColors.LIGHT_GREEN,
      borderStyle = BorderStyle.THIN,
    )
    val halfUnapprovedVacationStyle = ExcelUtils.createCellStyle(
      workbook,
      "halfUnapprovedVacationDay",
      alignment = HorizontalAlignment.CENTER,
      fillForegroundColor = IndexedColors.GREY_25_PERCENT,
      borderStyle = BorderStyle.THIN,
    )

    fun vacationStyle(cell: VacationCell) = when {
      cell.approved && cell.half -> halfVacationStyle
      cell.approved -> vacationStyle
      cell.half -> halfUnapprovedVacationStyle
      else -> unapprovedVacationStyle
    }

    /** Weekends and holidays off work are coloured; a holiday configured as (half) working day (24.12.) isn't. */
    fun dayStyle(day: PFDay) = if (Holidays.instance.isWorkingDay(day)) standardDayStyle else holidayAndWeekendStyle

    fun withSheet(sheet: ExcelSheet) {
      currentSheet = sheet
      monthSeparatorCols.clear()
    }
  }

  internal class SheetData(var sheetName: String, var startDate: PFDay, var endDate: PFDay)

  fun export(
    date: LocalDate = LocalDate.now(),
    vacationsByEmployee: List<VacationService.VacationsByEmployee>,
  ): ByteArray {
    log.info { "Exporting Excel sheet with vacations of users: ${vacationsByEmployee.joinToString { it.employee.user?.getFullname() ?: "???" }}" }
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale).use { workbook ->
      val context = Context(workbook)
      val startDate = PFDay.from(date).beginOfMonth
      getSheetsData(startDate).forEach { sheetData ->
        createSheet(context, sheetData, vacationsByEmployee)
      }
      // Legend
      val sheet = workbook.createOrGetSheet(translate("legend"))
      var row = sheet.createRow()
      row.getCell(0).setCellStyle(context.unapprovedVacationStyle)
      row.getCell(1).setCellValue(translate("vacation.status.inProgress"))
      row = sheet.createRow()
      row.getCell(0).setCellStyle(context.vacationStyle)
      row.getCell(1).setCellValue(translate("vacation.status.approved"))
      row = sheet.createRow()
      row.getCell(0).setCellValue(HALF_DAY_MARKER).setCellStyle(context.halfUnapprovedVacationStyle)
      row.getCell(1).setCellValue("${translate("vacation.status.inProgress")} (${translate("vacation.halfDay")})")
      row = sheet.createRow()
      row.getCell(0).setCellValue(HALF_DAY_MARKER).setCellStyle(context.halfVacationStyle)
      row.getCell(1).setCellValue("${translate("vacation.status.approved")} (${translate("vacation.halfDay")})")
      row = sheet.createRow()
      row.getCell(0).setCellStyle(context.holidayAndWeekendStyle)
      row.getCell(1).setCellValue(translate("vacation.export.legend.holidayOrWeekend"))
      sheet.setColumnWidth(0, COL_WIDTH_DAY)
      return workbook.asByteArrayOutputStream.toByteArray()
    }
  }

  private fun createSheet(
    context: Context,
    sheetData: SheetData,
    vacationsByEmployee: List<VacationService.VacationsByEmployee>,
  ) {
    val startDate = sheetData.startDate
    val endDate = sheetData.endDate
    val workbook = context.workbook
    val sheet = workbook.createOrGetSheet(sheetData.sheetName)
    context.withSheet(sheet)
    sheet.poiSheet.printSetup.landscape = true
    sheet.poiSheet.printSetup.fitWidth = 1.toShort()  // Doesn't work
    sheet.poiSheet.printSetup.fitHeight = 0.toShort() // Doesn't work
    val firstRow = sheet.createRow()
    val dateRow = sheet.createRow()
    val weekDayRow = sheet.createRow()
    var currentDate = startDate
    var columnIndex = 0
    firstRow.getCell(columnIndex).setCellValue(translate("fibu.employees"))
    sheet.setColumnWidth(columnIndex, COL_WIDTH_USER) // Column of vacationers.
    firstRow.getCell(++columnIndex).setCellValue(translate("vacation"))
    sheet.setColumnWidth(columnIndex, 500) // Column if employee has any vacation or not.
    createMonthSeparationCells(context, ++columnIndex, dateRow, weekDayRow, firstRow)
    var paranoiaCounter = 0
    var firstDayOfMonthCol = columnIndex + 1
    val columnIndexMap = mutableMapOf<LocalDate, Int>()
    while (currentDate <= endDate && paranoiaCounter++ < 500) {
      // Add day columns.
      val style = context.dayStyle(currentDate)
      columnIndexMap[currentDate.date] = ++columnIndex // Store for finding column indexes by vacation dates.
      sheet.setColumnWidth(columnIndex, COL_WIDTH_DAY)
      dateRow.getCell(columnIndex).setCellValue("${currentDate.dayOfMonth}").setCellStyle(style)
      weekDayRow.getCell(columnIndex).setCellValue(getWeekDayString(currentDate)).setCellStyle(style)
      currentDate = currentDate.plusDays(1)
      if (currentDate.dayOfMonth == 1) {
        // New month started.
        val monthCell =
          sheet.setMergedRegion(
            firstRow.rowNum,
            firstRow.rowNum,
            firstDayOfMonthCol,
            columnIndex,
            getMonthString(currentDate.minusMonths(1)),
          )
        monthCell.setCellStyle(context.monthStyle)
        createMonthSeparationCells(context, ++columnIndex, dateRow, weekDayRow, firstRow)
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
          employeeRow.getCell(col).setCellStyle(context.dayStyle(currentDate))
        }
        currentDate = currentDate.plusDays(1)
      }
      val cells = vacationCells(entry.vacations, startDate, endDate)
      if (cells.isNotEmpty()) {
        // Mark row as user with vacation entries:
        employeeRow.getCell(1).setCellValue("X")
      }
      cells.forEach { (date, cell) ->
        columnIndexMap[date]?.let { col ->
          val excelCell = employeeRow.getCell(col).setCellStyle(context.vacationStyle(cell))
          if (cell.half) {
            excelCell.setCellValue(HALF_DAY_MARKER)
          }
        }
      }
    }
    sheet.createRow().getCell(0).setCellValue("(${PFDay.now().isoString})")
    sheet.createFreezePane(1, 3)
    sheet.setAutoFilter()
    sheet.poiSheet.setAutoFilter(CellRangeAddress(0, 0, 0, 1))
  }

  /** A painted vacation day: approved wins over in progress, a full day over a half one. */
  internal data class VacationCell(val approved: Boolean, val half: Boolean) {
    fun outranks(other: VacationCell): Boolean =
      if (approved != other.approved) approved else !half && other.half
  }

  /**
   * The painted days of the given vacations between [startDate] and [endDate], by date. Only working days are
   * painted, so weekends and holidays inside a vacation keep their own colour (they aren't vacation days). A day
   * is half if it's the half day begin/end of its vacation or a half working day (24.12.), as counted by
   * [VacationService.getVacationDays]. Overlapping vacations are merged by [VacationCell.outranks], whatever the
   * order of the list.
   */
  internal fun vacationCells(
    vacations: List<VacationDO>,
    startDate: PFDay,
    endDate: PFDay,
  ): Map<LocalDate, VacationCell> {
    val cells = mutableMapOf<LocalDate, VacationCell>()
    vacations.forEach { vacation ->
      val vacationStart = PFDay.fromOrNull(vacation.startDate) ?: return@forEach
      val vacationEnd = PFDay.fromOrNull(vacation.endDate) ?: return@forEach
      val approved = vacation.status == VacationStatus.APPROVED
      var paranoiaCounter = 0
      // A vacation beginning before the sheet is painted from the sheet's first day on.
      var current: PFDay = if (vacationStart < startDate) startDate else vacationStart
      while (current <= vacationEnd && current <= endDate && paranoiaCounter++ < 500) {
        val fraction = PFDayUtils.getNumberOfWorkingDays(current, current)
        if (fraction > BigDecimal.ZERO) {
          val halfDayFlag = current.isSameDay(vacationStart) && vacation.halfDayBegin == true ||
              current.isSameDay(vacationEnd) && vacation.halfDayEnd == true
          val cell = VacationCell(approved, half = fraction < BigDecimal.ONE || halfDayFlag)
          val existing = cells[current.date]
          if (existing == null || cell.outranks(existing)) {
            cells[current.date] = cell
          }
        }
        current = current.plusDays(1)
      }
    }
    return cells
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

  /**
   * Gets list of sheets to create.
   */
  internal fun getSheetsData(startDate: PFDay): List<SheetData> {
    val list = mutableListOf<SheetData>()
    // Create 3 month sheet:
    list.add(getSheetData(startDate, startDate.plusMonths(2)))
    // Create sheet for rest of year:
    list.add(getSheetData(startDate, startDate.endOfYear))
    // 4 quarters:
    val startMonth = startDate.monthValue // (1-12)
    var quarterStart = when (startMonth) {
      1, 2, 3 -> startDate.withMonth(1)
      4, 5, 6 -> startDate.withMonth(4)
      7, 8, 9 -> startDate.withMonth(7)
      else -> startDate.withMonth(10)
    }
    for (count in 1..4) {
      list.add(getSheetData(quarterStart, quarterStart.plusMonths(2)))
      quarterStart = quarterStart.plusMonths(3)
    }
    // 2 years:
    var yearBegin = startDate.beginOfYear
    for (count in 1..2) {
      list.add(getSheetData(yearBegin, yearBegin.endOfYear))
      yearBegin = yearBegin.plusYears(1)
    }
    return list.distinctBy { it.sheetName }
  }

  private fun getSheetData(startDate: PFDay, endDate: PFDay): SheetData {
    val numberOfMonths = startDate.monthsBetween(endDate).toInt() + 1
    val quarter = when (startDate.monthValue) {
      1 -> 1
      4 -> 2
      7 -> 3
      10 -> 4
      else -> -1
    }
    val sheetName = if (startDate.month == Month.JANUARY && numberOfMonths == 12) {
      // Show all months of one year.
      startDate.year.toString()
    } else if (numberOfMonths == 3 && quarter > 0) {
      // Show quarter
      "Q$quarter ${startDate.year}"
    } else {
      val fromMonth = startDate.month.getDisplayName(TextStyle.SHORT, ThreadLocalUserContext.locale)
      val untilMonth =
        "${endDate.month.getDisplayName(TextStyle.SHORT, ThreadLocalUserContext.locale)} ${endDate.year - 2000}"
      "$fromMonth-$untilMonth"
    }
    return SheetData(sheetName, startDate.beginOfMonth, endDate.endOfMonth)
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
    vacationsByEmployee.add(VacationService.VacationsByEmployee(createEmployee("Hard", "Worker"), emptyList()))
    val workbook = export(
      date = LocalDate.of(2023, Month.FEBRUARY, 27),
      vacationsByEmployee = vacationsByEmployee,
    )
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

  internal const val HALF_DAY_MARKER = "½"

  private const val COL_WIDTH_DAY = 600
  private const val COL_WIDTH_USER = 20 * 256
}
