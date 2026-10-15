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

package org.projectforge.plugins.liquidityplanning

import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

/**
 * Checks the layout and the formulas of the forecast Excel file: four sheets, one cash-flow row per day
 * framed by the start-amount and the sum row, and balances that add up once Excel evaluates them.
 */
class LiquidityForecastExcelExportTest {
    @Test
    fun `cash flow sheet has one row per day and its balances add up to start amount plus cash flow`() {
        val today = LocalDate.now()
        val nextDays = 30
        val forecast = LiquidityForecast()
        forecast.set(
            listOf(
                entry("-500.00", today.plusDays(3)), // Credit: money coming in.
                entry("200.00", today.plusDays(10)), // Debit: money going out.
                entry("1000.00", today.plusDays(100)), // Beyond the horizon: listed, but not in the cash flow.
            ),
        )
        forecast.setInvoices(emptyList())
        forecast.setCreditorInvoices(emptyList())
        forecast.build()
        val cashFlow = LiquidityForecastCashFlow(forecast, nextDays)

        // A bare workbook: ExcelUtils.prepareWorkbook needs the user's date format from the Spring configuration.
        val bytes = ExcelWorkbook.createEmptyWorkbook(Locale.GERMANY).use { workbook ->
            LiquidityForecastExcelExport.write(workbook, forecast, cashFlow, BigDecimal("1000"))
        }

        XSSFWorkbook(ByteArrayInputStream(bytes)).use { workbook ->
            assertEquals(4, workbook.numberOfSheets, "cash flow, all, debitor and creditor invoices")
            val sheet = workbook.getSheetAt(0)
            val startRow = LiquidityForecastExcelExport.CASH_FLOW_START_ROW
            // Group header, column header, start amount, one row per day, sum row.
            assertEquals(startRow + nextDays + 1, sheet.lastRowNum)
            assertEquals(1000.0, sheet.getRow(startRow).getCell(3).numericCellValue)
            val firstDay = sheet.getRow(startRow + 1).getCell(3)
            assertEquals(CellType.FORMULA, firstDay.cellType)
            assertEquals("D3+SUM(B3:C3)", firstDay.cellFormula)

            val evaluator = workbook.creationHelper.createFormulaEvaluator()
            val sumRow = sheet.getRow(sheet.lastRowNum)
            // Expected (D) and due-date (G) balance: 1000 - 500 + 200; the entry beyond the horizon is left out.
            assertEquals(700.0, evaluator.evaluate(sumRow.getCell(3)).numberValue, 0.001)
            assertEquals(700.0, evaluator.evaluate(sumRow.getCell(6)).numberValue, 0.001)
            assertEquals(-500.0, evaluator.evaluate(sumRow.getCell(1)).numberValue, 0.001)
            assertEquals(200.0, evaluator.evaluate(sumRow.getCell(2)).numberValue, 0.001)
            // The balance on day 4 contains the credit of day 3 (balance = previous balance + previous cash flow).
            assertEquals(500.0, evaluator.evaluate(sheet.getRow(startRow + 1 + 4).getCell(3)).numberValue, 0.001)

            val allSheet = workbook.getSheetAt(1)
            assertEquals(forecast.getEntries().size, allSheet.lastRowNum, "head row plus one row per entry")
            // Dates are written as Excel dates, not as text; the empty expected date stays blank.
            assertTrue(DateUtil.isCellDateFormatted(allSheet.getRow(1).getCell(0)))
            assertEquals("", allSheet.getRow(1).getCell(1).toString())
            assertTrue(DateUtil.isCellDateFormatted(sheet.getRow(startRow + 1).getCell(0)))
        }
    }

    private fun entry(amount: String, dateOfPayment: LocalDate): LiquidityEntryDO {
        val entry = LiquidityEntryDO()
        entry.amount = BigDecimal(amount)
        entry.dateOfPayment = dateOfPayment
        entry.subject = "entry $amount"
        return entry
    }
}
