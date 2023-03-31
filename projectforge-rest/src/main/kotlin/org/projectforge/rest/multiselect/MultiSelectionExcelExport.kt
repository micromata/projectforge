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

package org.projectforge.rest.multiselect

import de.micromata.merlin.excel.ExcelCell
import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.CellStyle
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.ui.ElementsRegistry


private val log = KotlinLogging.logger {}

/**
 * Exports the results of mass updates
 */
object MultiSelectionExcelExport {
  private class Context<T>(
    val multiSelectedPage: AbstractMultiSelectedPage<T>,
    val wrapStyle: CellStyle,
    val boldStyle: CellStyle,
    val amountStyle: CellStyle,
  )

  fun <T> export(
    massUpdateContext: MassUpdateContext<T>,
    multiSelectedPage: AbstractMultiSelectedPage<T>
  ): ByteArray {
    log.info("Exporting results of mass update as Excel file.")
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale!!).use { workbook ->
      val sheet = workbook.createOrGetSheet(translate("massUpdate.result.excel.title"))
      val boldFont = workbook.createOrGetFont("bold", bold = true)
      val boldStyle = workbook.createOrGetCellStyle("boldStyle")
      boldStyle.setFont(boldFont)
      val amountStyle = workbook.createOrGetCellStyle("amount")
      amountStyle.dataFormat = workbook.createDataFormat().getFormat("#,##0.00;[Red]-#,##0.00")
      val decimalStyle = workbook.createOrGetCellStyle("decimal")
      decimalStyle.dataFormat = workbook.createDataFormat().getFormat("0.0")
      val wrapStyle = workbook.createOrGetCellStyle("wrapText")
      wrapStyle.wrapText = true
      val context = Context(multiSelectedPage, wrapStyle, boldStyle, amountStyle)
      val firstRow = sheet.createRow() // First row
      val identifierHeadCols = multiSelectedPage.customizeExcelIdentifierHeadCells()
      for (i in 0 until identifierHeadCols.size) {
        firstRow.createCell().setCellStyle(boldStyle) // empty cells (identifier cells, default is "Element").
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
      val headRow = sheet.createRow()
      identifierHeadCols.forEach { head ->
        sheet.registerColumns(head)
      }
      headRow.fillHeadRow()
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
      sheet.setColumnWidth(headRow.createCell().setCellValue("Id").colNumber, 11 * 256) // Id column as last one
      headRow.fillHeadRow(boldStyle)
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
        row.createCell().setCellValue(massUpdateObject.getId())
      }
      return workbook.asByteArrayOutputStream.toByteArray()
    }
  }

  private fun <T> displayValue(
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
    if (!context.multiSelectedPage.handleCellStyle(cell, field, value, cellValue)) {
      context.multiSelectedPage.layoutContext?.let { lc ->
        ElementsRegistry.getElementInfo(lc, field)?.let { elementInfo ->
          when (elementInfo.propertyType) {
            PropertyType.CURRENCY -> cell.setCellStyle(context.amountStyle)
            else -> {}
          }
        }
      }
    }
  }
}
