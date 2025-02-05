/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.excel

import de.micromata.merlin.excel.*
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.projectforge.common.BeanHelper
import org.projectforge.common.ClassUtils
import org.projectforge.common.DateFormatType
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.internal.MutablePropertyReference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private val log = KotlinLogging.logger {}

/**
 * For excel export.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object ExcelUtils {
    /**
     * Sets the active sheet and deselects all other sheets.
     */
    @JvmStatic
    fun setActiveSheet(workbook: ExcelWorkbook, activeSheetIndex: Int) {
        for (i in 0..<workbook.numberOfSheets) {
            workbook.getSheet(i).poiSheet.setSelected(i == activeSheetIndex)
        }
        workbook.setActiveSheet(activeSheetIndex)
    }

    /**
     * Sets the head row of the sheet.
     * @param sheet the sheet.
     * @param rowNum the row number of the head row (0-based).
     */
    fun setHeadRow(sheet: ExcelSheet, rowNum: Int) {
        setHeadRow(sheet, sheet.getRow(rowNum));
    }

    fun setHeadRow(sheet: ExcelSheet, row: ExcelRow) {
        ClassUtils.setPrivateField(sheet, "_headRow", row);
    }

    /**
     * Sets the auto filter for the given row.
     * @param sheet the sheet.
     * @param rowNum the row number of the row to set the auto filter.
     * @param fromColIndex the column index to start the auto filter.
     * @param toColIndex the column index to end the auto filter. If null, the last column is used.
     */
    fun setAutoFilter(sheet: ExcelSheet, rowNum: Int, fromColIndex: Int = 0, toColIndex: Int? = null) {
        val row = sheet.getRow(rowNum)
        val lastCol = toColIndex ?: row.lastCellNum.toInt()
        val range = CellRangeAddress(rowNum, rowNum, fromColIndex, lastCol - 1)
        sheet.poiSheet.setAutoFilter(range)
    }

    fun clearCells(row: ExcelRow, fromColIndex: Int = 0, toColIndex: Int? = null) {
        val lastCol = toColIndex ?: row.lastCellNum.toInt()
        for (i in fromColIndex until lastCol) {
            row.getCell(i).setBlank()
        }
    }

    /**
     * Moves a row to another position.
     * @param sheet the sheet.
     * @param fromRowIndex the row index to move.
     * @param toRowIndex the target row index.
     * @param shiftRows if true, the rows below the moved row are shifted up.
     */
    fun moveRow(sheet: ExcelSheet, fromRowIndex: Int, toRowIndex: Int, shiftRows: Boolean = true) {
        val fromRow = sheet.getRow(fromRowIndex).row
        var toRow = sheet.getRow(toRowIndex).row
        val poiSheet = sheet.poiSheet
        // Remove the target row first
        poiSheet.removeRow(toRow)
        // Create a new row at the target index
        toRow = poiSheet.createRow(toRowIndex)
        // Copy all cells from the source row to the target row
        for (i in 0 until fromRow.lastCellNum) {
            val oldCell = fromRow.getCell(i) ?: continue // Skip null cells
            val newCell = toRow.createCell(i, oldCell.cellType)
            // Copy cell style
            newCell.cellStyle = oldCell.cellStyle
            // Copy cell value based on its type
            when (oldCell.cellType) {
                CellType.STRING -> newCell.setCellValue(oldCell.stringCellValue)
                CellType.NUMERIC -> newCell.setCellValue(oldCell.numericCellValue)
                CellType.BOOLEAN -> newCell.setCellValue(oldCell.booleanCellValue)
                CellType.FORMULA -> newCell.cellFormula = oldCell.cellFormula
                CellType.BLANK -> newCell.setBlank()
                else -> {} // No action for unsupported types
            }
        }
        // Remove the original row after copying
        poiSheet.removeRow(fromRow)
        if (shiftRows) {
            // Shift rows up to fill the gap
            sheet.shiftRows(fromRowIndex + 1, sheet.poiSheet.lastRowNum, -1)
        }
    }

    /**
     * Should be used for workbook creation for every export.
     * @return workbook configured with date formats of the logged-in user and number formats.
     */
    @JvmStatic
    fun prepareWorkbook(filename: String? = null): ExcelWorkbook {
        val workbook = ExcelWorkbook(ThreadLocalUserContext.locale)
        filename?.let { workbook.filename = it }
        workbook.configuration.let { cfg ->
            cfg.setDateFormats(
                DateFormats.getExcelFormatString(DateFormatType.DATE),
                Configuration.TimeStampPrecision.DAY
            )
            cfg.intFormat = "#,##0"
            cfg.floatFormat = "#,##0.#"
            return workbook
        }
    }

    fun addHeadRow(sheet: ExcelSheet, style: CellStyle? = null) {
        val headRow = sheet.createRow() // second row as head row.
        sheet.columnDefinitions.forEachIndexed { index, it ->
            val cell = headRow.getCell(index).setCellValue(it.columnHeadname)
            style?.let { cell.setCellStyle(it) }
        }
    }

    fun getCell(row: ExcelRow, property: KProperty<*>): ExcelCell? {
        return row.getCell(property.name)
    }

    /**
     * Registers an excel column by using the translated i18n-key of the given property as column head and the
     * property name as alias (for referring and [de.micromata.merlin.excel.ExcelRow.autoFillFromObject].
     * @param size approx no of characters
     */
    @JvmStatic
    @JvmOverloads
    fun registerColumn(
        sheet: ExcelSheet,
        clazz: Class<*>,
        property: String,
        size: Int? = null,
        logErrorIfPropertyInfoNotFound: Boolean = true,
    ): ExcelColumnDef {
        val i18nKey = PropUtils.getI18nKey(clazz, property, logErrorIfPropertyInfoNotFound) ?: property
        val colDef = sheet.registerColumn(translate(i18nKey), property)
        if (size != null) {
            colDef.withSize(size)
        } else {
            if (property == "id") {
                colDef.withSize(Size.ID)
            } else {
                when (BeanHelper.determinePropertyType(clazz, property)) {
                    null -> {
                        colDef.withSize(Size.STANDARD)
                    }

                    LocalDate::class.java -> {
                        colDef.withSize(Size.DATE)
                    }

                    Date::class.java, LocalDateTime::class.java -> {
                        colDef.withSize(Size.DATE_TIME)
                    }

                    else -> {
                        colDef.withSize(Size.STANDARD)
                    }
                }
            }
        }
        return colDef
    }

    @JvmOverloads
    fun registerColumn(
        sheet: ExcelSheet,
        property: KProperty<*>,
        size: Int? = null,
        logErrorIfPropertyInfoNotFound: Boolean = true,
    ): ExcelColumnDef {
        if (property is MutablePropertyReference) {
            val kClass = property.owner as? KClass<*>
            if (kClass != null) {
                return registerColumn(sheet, kClass.java, property.name, size, logErrorIfPropertyInfoNotFound)
            }
        }
        if (logErrorIfPropertyInfoNotFound) {
            log.error { "Can't get declaringClass of property '${property.name}'. Can't register column." }
        }
        return ExcelColumnDef(sheet, property.name)
    }

    @JvmStatic
    fun autoFill(row: ExcelRow, obj: Any?, vararg ignoreProperties: String) {
        autoFill(row, obj, process, *ignoreProperties)
    }

    @JvmStatic
    fun autoFill(
        row: ExcelRow,
        obj: Any?,
        process: (Any, Any, ExcelCell, ExcelColumnDef) -> Boolean,
        vararg ignoreProperties: String
    ) {
        row.autoFillFromObject(obj, process, *ignoreProperties)
    }

    val process: (Any, Any, ExcelCell, ExcelColumnDef) -> Boolean = { _, propertyValue, cell, _ ->
        if (propertyValue is I18nEnum) {
            cell.setCellValue(translate(propertyValue.i18nKey))
            true
        } else {
            false
        }
    }

    /**
     * Clones the font and returns the new font.
     * @param bold if null, the original font's bold is used.
     * @param heightInPoints if null, the original font's heightInPoints is used.
     * @param color if null, the original font's color is used.
     * @param fontName if null, the original font's fontName is used.
     * @param origFont if given, the properties of this font are copied first.
     * @return the new font.
     */
    @JvmStatic
    fun createFont(
        workbook: ExcelWorkbook,
        id: String,
        bold: Boolean? = null,
        heightInPoints: Short? = null,
        color: Short? = null,
        fontName: String = "Arial",
        origFont: Font? = null,
    ): Font {
        return workbook.createOrGetFont(
            id,
            bold = origFont?.bold ?: bold ?: false,
            heightInPoints = origFont?.fontHeightInPoints ?: heightInPoints,
            color = origFont?.color ?: color,
        ).also { font ->
            font.fontName = origFont?.fontName ?: fontName
        }
    }

    /**
     * Clones the cell style and returns the new cell style.
     * @param font if null, the original cell style's font is used.
     * @param alignment if null, the original cell style's alignment is used.
     * @param fillForegroundColor if null, the original cell style's fillForegroundColor is used.
     * @param fillPattern if null, the original cell style's fillPattern is used.
     * @param borderStyle if null, the original cell style's borderStyle is used.
     * @param origStyle if given, the properties of this cell style are copied first.
     * @return the new cell style.
     */
    @JvmStatic
    fun createCellStyle(
        workbook: ExcelWorkbook,
        name: String,
        font: Font? = null,
        alignment: HorizontalAlignment? = null,
        fillForegroundColor: IndexedColors? = null,
        fillPattern: FillPatternType? = null,
        borderStyle: BorderStyle? = null,
        origStyle: CellStyle? = null,
    ): CellStyle {
        val style = workbook.createOrGetCellStyle(name)
        origStyle?.let {
            style.cloneStyleFrom(it)
            style.dataFormat = it.dataFormat
        }
        if (font != null) {
            style.setFont(font)
        }
        if (alignment != null) {
            style.alignment = alignment
        }
        if (fillForegroundColor != null) {
            style.fillForegroundColor = fillForegroundColor.index
            if (fillPattern == null) {
                style.fillPattern = FillPatternType.SOLID_FOREGROUND
            }
        }
        if (fillPattern != null) {
            style.fillPattern = fillPattern
        }
        borderStyle?.let {
            style.borderTop = it
            style.borderBottom = it
            style.borderLeft = it
            style.borderRight = it
        }
        return style
    }

    object Size {
        const val DATE = 10
        const val DATE_TIME = 20
        const val DURATION = 10
        const val EMAIL = 30
        const val EXTRA_LONG = 80
        const val ID = 10
        const val KOSTENTRAEGER = 11
        const val PHONENUMBER = 20
        const val TASK_PATH = 60
        const val TIMESTAMP = 16
        const val USER = 20
        const val STANDARD = 30
        const val ZIPCODE = 7
    }
}
