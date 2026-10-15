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

package org.projectforge.rest.fibu

import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.excel.ExcelUtils
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.util.Locale

/**
 * Regression test for the employee Excel export number format (see [EmployeePagesRest.exportAsExcel]).
 *
 * Reproduces the export path (column-level [de.micromata.merlin.excel.ExcelSheet.setColumnStyle] with an explicit
 * "#,##0.##" format), serializes to bytes, re-reads the written .xlsx and asserts that whole numbers are stored
 * with the expected format so Excel renders "35" instead of merlin's default float format "#.#" ("35,").
 */
class EmployeeExcelNumberFormatTest {
    @Test
    fun `numeric columns must use General format, not merlin's default float format`() {
        val bytes = ExcelWorkbook.createEmptyWorkbook(Locale.GERMANY).use { workbook ->
            val sheet = workbook.createOrGetSheet("test")
            val numberStyle = workbook.createOrGetCellStyle("number")
            numberStyle.dataFormat = workbook.createDataFormat().getFormat("General")
            ExcelUtils.registerColumn(sheet, EmployeeDO::class.java, "weeklyWorkingHours", 12)
            sheet.setColumnStyle("weeklyWorkingHours", numberStyle)
            ExcelUtils.addHeadRow(sheet)
            // Whole number in row 1, fractional in row 2.
            sheet.createRow().getCell("weeklyWorkingHours")!!.setCellValue(BigDecimal("35"))
            sheet.createRow().getCell("weeklyWorkingHours")!!.setCellValue(BigDecimal("37.5"))
            workbook.asByteArrayOutputStream.toByteArray()
        }
        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { poi ->
            val sheet = poi.getSheetAt(0)
            val whole = sheet.getRow(1).getCell(0)
            val fraction = sheet.getRow(2).getCell(0)
            // Must NOT be merlin's default float format "#.#", which renders whole numbers as "35,".
            Assertions.assertEquals("General", whole.cellStyle.dataFormatString)
            Assertions.assertEquals(35.0, whole.numericCellValue)
            Assertions.assertEquals("General", fraction.cellStyle.dataFormatString)
            Assertions.assertEquals(37.5, fraction.numericCellValue)
        }
    }
}
