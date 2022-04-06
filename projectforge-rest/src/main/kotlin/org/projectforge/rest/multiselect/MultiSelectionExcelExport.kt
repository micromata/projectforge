/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.multiselect

import de.micromata.merlin.excel.ExcelCell
import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.CellStyle
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import java.io.File
import java.io.Serializable


private val log = KotlinLogging.logger {}

/**
 * Exports the results of mass updates
 */
object MultiSelectionExcelExport {
  private class Context<T : IdObject<out Serializable>>(
    val multiSelectedPage: AbstractMultiSelectedPage<T>,
    val wrapStyle: CellStyle,
    val boldStyle: CellStyle,
  )

  fun <T : IdObject<out Serializable>> export(
    massUpdateContext: MassUpdateContext<T>,
    multiSelectedPage: AbstractMultiSelectedPage<T>
  ): ByteArray {
    log.info("Exporting results of mass update as Excel file.")
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.getLocale()).use { workbook ->
      val sheet = workbook.createOrGetSheet(translate("massUpdate.result.excel.title"))
      val boldFont = workbook.createOrGetFont("bold", bold = true)
      val boldStyle = workbook.createOrGetCellStyle("boldStyle")
      boldStyle.setFont(boldFont)
      val decimalStyle = workbook.createOrGetCellStyle("decimal")
      decimalStyle.dataFormat = workbook.createDataFormat().getFormat("0.0")
      val wrapStyle = workbook.createOrGetCellStyle("wrapText")
      wrapStyle.wrapText = true
      val context = Context(multiSelectedPage, wrapStyle, boldStyle)
      val firstRow = sheet.createRow() // First row
      val identifierHeadCols = multiSelectedPage.customizeExcelIdentifierHeadCells()
      sheet.registerColumns(*identifierHeadCols)
      for (i in 0 until identifierHeadCols.size) {
        firstRow.createCell() // 2 empty cells.
      }
      val modifiedFields = mutableSetOf<String>()
      val colWidth = mutableMapOf<String, Int>() // key is field name.
      massUpdateContext.massUpdateObjects.forEach { massUpdateObject ->
        massUpdateObject.fieldModifications.forEach { (field, modification) ->
          if (!modifiedFields.contains(field)) {
            if (modification.oldValue != modification.newValue) {
              modifiedFields.add(field)
            }
          }
        }
      }
      val headRow = sheet.createRow().fillHeadRow(boldStyle)
      massUpdateContext.massUpdateData.forEach { (field, param) ->
        if (modifiedFields.contains(field)) {
          val colNumber = firstRow.createCell().setCellValue(multiSelectedPage.getFieldTranslation(field)).colNumber
          firstRow.createCell()
          firstRow.addMergeRegion(colNumber, colNumber + 1)
          headRow.createCell().setCellValue(translate("massUpdate.excel.column.old"))
          headRow.createCell().setCellValue(translate("massUpdate.excel.column.new"))
          sheet.setColumnWidth(colNumber, 30 * 256)
          sheet.setColumnWidth(colNumber + 1, 30 * 256)
        }
      }
      sheet.setAutoFilter()
      massUpdateContext.massUpdateObjects.forEach { massUpdateObject ->
        val row = sheet.createRow()
        multiSelectedPage.getExcelIdentifierCells(massUpdateObject).forEach {
          row.createCell().setCellValue(it)
        }
        massUpdateObject.fieldModifications.forEach { (field, modification) ->
          if (modifiedFields.contains(field)) {
            val oldValueCell = row.createCell()
            val newValueCell = row.createCell()
            if (modification.oldValue != modification.newValue) {
              displayValue(context, oldValueCell, field, modification.oldValue)
              displayValue(context, newValueCell, field, modification.newValue)
            }
          }
        }
      }
      return workbook.asByteArrayOutputStream.toByteArray()
    }
  }

  private fun <T : IdObject<out Serializable>> displayValue(
    context: Context<T>,
    cell: ExcelCell,
    field: String,
    value: Any?,
  ) {
    if (context.multiSelectedPage.handleValue(cell, field, value)) {
      // Value already handled by multi page.
      return
    }
    var cellValue = value
    when (value) {
      is String -> cell.setCellStyle(context.wrapStyle)
      is DisplayNameCapable -> cellValue = value.displayName
      else -> {}
    }
    cell.setCellValue(cellValue)
  }
/*
  fun download() {
    scriptExecution.getDownloadFile(request)?.let { download ->
      script.scriptDownload = Script.ScriptDownload(download.filename, download.sizeHumanReadable)
      val availableUntil = translateMsg("scripting.download.filename.additional", download.availableUntil)
      layout.add(
        UIRow().add(
          UIFieldset(title = "scripting.download.filename.info").add(
            UIReadOnlyField(
              "scriptDownload.filenameAndSize",
              label = "scripting.download.filename",
              additionalLabel = "'$availableUntil",
            )
          )
            .add(
              UIButton.createDownloadButton(
                responseAction = ResponseAction(
                  url = "${getRestPath()}/download",
                  targetType = TargetType.DOWNLOAD
                )
              )
            )
        )
      )
    }

  }

  internal fun getDownloadFile(request: HttpServletRequest): ScriptExecution.DownloadFile? {
    return ExpiringSessionAttributes.getAttribute(
      request,
      EXPIRING_SESSION_ATTRIBUTE, ScriptExecution.DownloadFile::class.java
    )
  }

  data class DownloadFile(val filename: String, val bytes: ByteArray, val availableUntil: String) {
    val sizeHumanReadable
      get() = NumberHelper.formatBytes(bytes.size)
  }

  internal fun createFilename(multiSelectedPage: AbstractMultiSelectedPage<*>): String {
    return ReplaceUtils.encodeFilename(
      "${translate(multiSelectedPage.getTitleKey())}_${
        PFDateTime.now().format4Filenames()
      }.xslx", true
    )
  }

  private val EXPIRING_SESSION_ATTRIBUTE = "${this::class.java.name}.downloadResultExcel"
  */
}
