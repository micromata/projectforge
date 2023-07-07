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

package org.projectforge.business.timesheet

import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.projectforge.business.common.OutputType
import org.projectforge.business.task.TaskFormatter.Companion.getTaskPath
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.DateFormatType
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats.getFormatString
import org.projectforge.framework.time.DateTimeFormatter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

private val log = KotlinLogging.logger {}

/**
 * For excel export.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class TimesheetExport {
  @Autowired
  private lateinit var dateTimeFormatter: DateTimeFormatter

  @Autowired
  private lateinit var taskTree: TaskTree

  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  /**
   * Exports the filtered list as table with almost all fields.
   */
  open fun export(list: List<TimesheetDO>): ByteArray {
    log.info("Exporting timesheet list.")
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale!!).use { workbook ->
      val sheet = workbook.createOrGetSheet(translate("timesheet.timesheets"))
      val boldFont = workbook.createOrGetFont("bold", bold = true)
      val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
      val wrapTextStyle = workbook.createOrGetCellStyle("wrap")
      val timeFormat = workbook.ensureDateCellStyle("HH:mm")
      val durationFormat = workbook.ensureDateCellStyle("[h]:mm")
      val hoursFormat = workbook.createOrGetCellStyle("hours")
      hoursFormat.dataFormat = workbook.createDataFormat().getFormat("#,##0.00");
      val idFormat = workbook.createOrGetCellStyle("id")
      idFormat.dataFormat = workbook.createDataFormat().getFormat("0");

      wrapTextStyle.wrapText = true
      sheet.registerColumn(translate("timesheet.user"), "user").withSize(ExcelUtils.Size.USER)
      sheet.registerColumn(translate("fibu.kunde"), "kunde").withSize(ExcelUtils.Size.STANDARD)
      sheet.registerColumn(translate("fibu.projekt"), "projekt").withSize(ExcelUtils.Size.STANDARD)
      sheet.registerColumn(translate("fibu.kost2"), "kost2").withSize(ExcelUtils.Size.KOSTENTRAEGER)
      sheet.registerColumn(translate("calendar.weekOfYearShortLabel"), "weekOfYearShortLabel").withSize(4)
      sheet.registerColumn(translate("calendar.dayOfWeekShortLabel"), "dayOfWeekShortLabel").withSize(4)
      ExcelUtils.registerColumn(sheet, TimesheetDO::startTime, ExcelUtils.Size.TIMESTAMP)
      val stopTimeColDef = ExcelUtils.registerColumn(sheet, TimesheetDO::stopTime, 8)
      sheet.registerColumn(translate("timesheet.duration"), "duration").withSize(ExcelUtils.Size.DURATION)
      sheet.registerColumn(translate("hours"), "hours").withSize(ExcelUtils.Size.DURATION)
      ExcelUtils.registerColumn(sheet, TimesheetDO::location)
      ExcelUtils.registerColumn(sheet, TimesheetDO::reference)
      sheet.registerColumn(translate("task"), "task.title").withSize(ExcelUtils.Size.STANDARD)
      sheet.registerColumn(translate("timesheet.taskReference"), "taskReference").withSize(ExcelUtils.Size.STANDARD)
      sheet.registerColumn(translate("shortDescription"), "shortDescription").withSize(ExcelUtils.Size.EXTRA_LONG)
      val descriptionColDef = ExcelUtils.registerColumn(sheet, TimesheetDO::description).withSize(ExcelUtils.Size.EXTRA_LONG)
      sheet.registerColumn(translate("task.path"), "taskPath").withSize(ExcelUtils.Size.TASK_PATH)
      sheet.registerColumn(translate("id"), "id")
      sheet.registerColumn(translate("created"), "created").withSize(ExcelUtils.Size.TIMESTAMP)
      sheet.registerColumn(translate("lastUpdate"), "lastUpdate").withSize(ExcelUtils.Size.TIMESTAMP)
      ExcelUtils.addHeadRow(sheet, boldStyle)

      list.forEach { timesheet ->
        val row = sheet.createRow()
        row.autoFillFromObject(timesheet)
        val node = taskTree.getTaskNodeById(timesheet.taskId)
        val user = userGroupCache.getUser(timesheet.userId)
        row.getCell("user")?.setCellValue(user?.getFullname())
        row.getCell("kunde")?.setCellValue(timesheet.kost2?.projekt?.kunde?.name)
        row.getCell("projekt")?.setCellValue(timesheet.kost2?.projekt?.name)
        row.getCell("kost2")?.setCellValue(timesheet.kost2?.displayName)
        row.getCell("task.title")?.setCellValue(node.task.title)
        row.getCell("taskPath")?.setCellValue(getTaskPath(timesheet.taskId, null, true, OutputType.PLAIN))
        row.getCell("weekOfYearShortLabel")?.setCellValue(timesheet.getFormattedWeekOfYear())
        row.getCell("dayOfWeekShortLabel")?.setCellValue(
          dateTimeFormatter.getFormattedDate(
            timesheet.startTime, getFormatString(DateFormatType.DAY_OF_WEEK_SHORT)
          )
        )
        row.getCell(stopTimeColDef).cell.cellStyle = timeFormat
        // ExcelUtils.getCell(row, TimesheetDO::startTime)?.setCellValue(startTime)
        // ExcelUtils.getCell(row, TimesheetDO::stopTime)?.setCellValue(stopTime)
        val seconds = BigDecimal(timesheet.getDuration() / 1000) // Seconds
        val duration = seconds.divide(BigDecimal(60 * 60 * 24), 8, RoundingMode.HALF_UP) // Fraction of day (24 hours)
        row.getCell("duration")?.setCellValue(duration.toDouble())?.setCellStyle(durationFormat)
        val hours = seconds.divide(BigDecimal(60 * 60), 2, RoundingMode.HALF_UP)
        row.getCell("hours")?.setCellValue(hours.toDouble())?.setCellStyle(hoursFormat)
        row.getCell("taskReference")?.setCellValue(node.reference)
        row.getCell("shortDescription")?.setCellValue(timesheet.getShortDescription())?.setCellStyle(wrapTextStyle)
        row.getCell(descriptionColDef).cell.cellStyle = wrapTextStyle
        row.getCell("created")?.setCellValue(timesheet.created)
        row.getCell("lastUpdate")?.setCellValue(timesheet.lastUpdate)
        ExcelUtils.getCell(row, TimesheetDO::description)?.setCellStyle(wrapTextStyle)
      }
      sheet.setAutoFilter()
      sheet.createFreezePane(stopTimeColDef.columnNumber + 1, 1)
      return workbook.asByteArrayOutputStream.toByteArray()
    }
  }
}
