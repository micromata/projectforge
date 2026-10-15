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

import de.micromata.merlin.excel.ExcelCell
import de.micromata.merlin.excel.ExcelRow
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.poi.ss.usermodel.CellStyle
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDay
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The Excel file of the liquidity forecast (forecast tab of `/next/liquidity`). Four sheets: the per-day
 * cash flow with running balances (Excel formulas, so amounts may be edited afterwards), all entries the
 * forecast is based on (liquidity entries and open invoices with their expected dates of payment) and the
 * debitor and creditor invoices taken into account.
 */
object LiquidityForecastExcelExport {
    private const val CURRENCY_FORMAT = "#,##0.00;[Red]-#,##0.00"
    private const val AMOUNT_SIZE = 14

    /** Index of the cash-flow sheet's row holding the start amount (below the group and column headers). */
    internal const val CASH_FLOW_START_ROW = 2

    fun export(forecast: LiquidityForecast, cashFlow: LiquidityForecastCashFlow, startAmount: BigDecimal): ByteArray {
        ExcelUtils.prepareWorkbook().use { workbook ->
            return write(workbook, forecast, cashFlow, startAmount)
        }
    }

    /** Fills the given [workbook] (prepared by [export], a bare one in tests) and serializes it. */
    internal fun write(
        workbook: ExcelWorkbook,
        forecast: LiquidityForecast,
        cashFlow: LiquidityForecastCashFlow,
        startAmount: BigDecimal,
    ): ByteArray {
        val currencyStyle = workbook.createOrGetCellStyle("currency")
        currencyStyle.dataFormat = workbook.createDataFormat().getFormat(CURRENCY_FORMAT)
        addCashFlowSheet(workbook, forecast, cashFlow, startAmount, currencyStyle)
        addEntriesSheet(workbook, forecast, currencyStyle)
        addDebitorInvoicesSheet(workbook, forecast.invoices.orEmpty(), currencyStyle)
        addCreditorInvoicesSheet(workbook, forecast.creditorInvoices.orEmpty(), currencyStyle)
        // The balances are formulas without cached values: let Excel/LibreOffice compute them on open.
        workbook.pOIWorkbook.setForceFormulaRecalculation(true)
        return workbook.asByteArrayOutputStream.toByteArray()
    }

    /**
     * One row per day: credits, debits and balance by expected date of payment (B–D) and by due date (E–G).
     * The row below the headers holds the start amount; each day's balance is the previous balance plus the
     * previous day's cash flow (as in the forecast charts), and a closing row sums up the whole period.
     */
    private fun addCashFlowSheet(
        workbook: ExcelWorkbook,
        forecast: LiquidityForecast,
        cashFlow: LiquidityForecastCashFlow,
        startAmount: BigDecimal,
        currencyStyle: CellStyle,
    ) {
        val sheet = workbook.createOrGetSheet(translate("plugins.liquidityplanning.forecast.cashflow"))
        sheet.enableMultipleColumns = true // Credit, debit and balance appear twice.
        sheet.registerColumn(translate("date"), "date").withSize(ExcelUtils.Size.DATE + 6)
        listOf("Expected", "").forEach { suffix ->
            sheet.registerColumn(translate("plugins.liquidityplanning.common.credit"), "credits$suffix")
                .withSize(AMOUNT_SIZE)
            sheet.registerColumn(translate("plugins.liquidityplanning.common.debit"), "debits$suffix")
                .withSize(AMOUNT_SIZE)
            sheet.registerColumn(translate("plugins.liquidityplanning.forecast.balance"), "balance$suffix")
                .withSize(AMOUNT_SIZE)
        }
        val boldStyle = workbook.createOrGetCellStyle(ExcelUtils.BOLD_STYLE)
        sheet.createRow() // Group header row.
        ExcelUtils.addHeadRow(sheet)
        sheet.setMergedRegion(0, 0, 1, 3, translate("plugins.liquidityplanning.entry.expectedDateOfPayment"))
            .setCellStyle(boldStyle)
        sheet.setMergedRegion(0, 0, 4, 6, translate("plugins.liquidityplanning.forecast.dueDate"))
            .setCellStyle(boldStyle)
        sheet.createFreezePane(1, CASH_FLOW_START_ROW)

        // Excel row numbers are 1-based: the start row's number is CASH_FLOW_START_ROW + 1.
        val firstRowNumber = CASH_FLOW_START_ROW + 1
        sheet.createRow().let { row ->
            row.getCell(0).setCellValue(translate("plugins.liquidityplanning.forecast.startAmount"))
            row.getCell(3).setCellValue(startAmount).setCellStyle(currencyStyle)
        }
        setFormula(sheet, CASH_FLOW_START_ROW, 6, "D$firstRowNumber", currencyStyle)

        var day = PFDay.fromOrNow(forecast.baseDate)
        val nextDays = cashFlow.credits.size
        for (i in 0 until nextDays) {
            val row = sheet.createRow()
            val rowIndex = CASH_FLOW_START_ROW + 1 + i
            val prev = rowIndex // 1-based number of the previous row.
            row.getCell(0).setCellValue(day.localDate)
            setAmount(row.getCell(1), cashFlow.creditsExpected[i], currencyStyle)
            setAmount(row.getCell(2), cashFlow.debitsExpected[i], currencyStyle)
            setFormula(sheet, rowIndex, 3, "D$prev+SUM(B$prev:C$prev)", currencyStyle)
            setAmount(row.getCell(4), cashFlow.credits[i], currencyStyle)
            setAmount(row.getCell(5), cashFlow.debits[i], currencyStyle)
            setFormula(sheet, rowIndex, 6, "G$prev+SUM(E$prev:F$prev)", currencyStyle)
            day = day.plusDays(1)
        }
        val sumRowIndex = CASH_FLOW_START_ROW + 1 + nextDays
        val last = sumRowIndex // 1-based number of the last day's row.
        sheet.createRow().getCell(0).setCellValue(translate("sum")).setCellStyle(boldStyle)
        listOf("B", "C", "E", "F").forEach { col ->
            setFormula(sheet, sumRowIndex, col[0] - 'A', "SUM($col$firstRowNumber:$col$last)", currencyStyle)
        }
        setFormula(sheet, sumRowIndex, 3, "D$firstRowNumber+SUM(B$firstRowNumber:C$last)", currencyStyle)
        setFormula(sheet, sumRowIndex, 6, "G$firstRowNumber+SUM(E$firstRowNumber:F$last)", currencyStyle)
    }

    /** All forecast entries: the liquidity entries merged with the open debitor and creditor invoices. */
    private fun addEntriesSheet(workbook: ExcelWorkbook, forecast: LiquidityForecast, currencyStyle: CellStyle) {
        val sheet = workbook.createOrGetSheet(translate("filter.all"))
        val clazz = LiquidityEntry::class.java
        ExcelUtils.registerColumn(sheet, clazz, "dateOfPayment", ExcelUtils.Size.DATE + 6)
        ExcelUtils.registerColumn(sheet, clazz, "expectedDateOfPayment", ExcelUtils.Size.DATE + 6)
        ExcelUtils.registerColumn(sheet, clazz, "amount", AMOUNT_SIZE)
        ExcelUtils.registerColumn(sheet, clazz, "paid", 8)
        ExcelUtils.registerColumn(sheet, clazz, "subject", 60)
        ExcelUtils.registerColumn(sheet, clazz, "type", 20)
        ExcelUtils.registerColumn(sheet, clazz, "comment", 40)
        ExcelUtils.addHeadRow(sheet)
        forecast.getEntries().forEach { entry ->
            val row = sheet.createRow()
            setValue(row, "dateOfPayment", entry.dateOfPayment)
            setValue(row, "expectedDateOfPayment", entry.expectedDateOfPayment)
            setValue(row, "amount", entry.amount)?.setCellStyle(currencyStyle)
            setValue(row, "paid", entry.isPaid)
            setValue(row, "subject", entry.subject)
            setValue(row, "type", entry.type?.let { translate(it.i18nKey) })
            setValue(row, "comment", entry.comment)
        }
        sheet.setAutoFilter()
    }

    private fun addDebitorInvoicesSheet(
        workbook: ExcelWorkbook,
        invoices: Collection<RechnungDO>,
        currencyStyle: CellStyle,
    ) {
        val sheet = workbook.createOrGetSheet(translate("fibu.rechnungen"))
        registerInvoiceDateColumns(sheet, RechnungDO::class.java)
        sheet.registerColumn(translate("fibu.rechnung.bruttoBetrag"), "gross").withSize(AMOUNT_SIZE)
        ExcelUtils.registerColumn(sheet, RechnungDO::class.java, "nummer", 8)
        sheet.registerColumn(translate("fibu.common.debitor"), "debitor").withSize(60)
        ExcelUtils.registerColumn(sheet, RechnungDO::class.java, "betreff", 100)
        ExcelUtils.addHeadRow(sheet)
        invoices.forEach { invoice ->
            val row = sheet.createRow()
            setValue(row, "datum", invoice.datum)
            setValue(row, "faelligkeit", invoice.faelligkeit)
            setValue(row, "gross", invoice.info.grossSum)?.setCellStyle(currencyStyle)
            setValue(row, "nummer", invoice.nummer)
            setValue(row, "debitor", invoice.kundeAsString)
            setValue(row, "betreff", invoice.betreff)
        }
        sheet.setAutoFilter()
    }

    private fun addCreditorInvoicesSheet(
        workbook: ExcelWorkbook,
        invoices: Collection<EingangsrechnungDO>,
        currencyStyle: CellStyle,
    ) {
        val sheet = workbook.createOrGetSheet(translate("fibu.eingangsrechnungen"))
        registerInvoiceDateColumns(sheet, EingangsrechnungDO::class.java)
        sheet.registerColumn(translate("fibu.rechnung.bruttoBetrag"), "gross").withSize(AMOUNT_SIZE)
        sheet.registerColumn(translate("fibu.common.creditor"), "kreditor").withSize(60)
        ExcelUtils.registerColumn(sheet, EingangsrechnungDO::class.java, "betreff", 100)
        ExcelUtils.addHeadRow(sheet)
        invoices.forEach { invoice ->
            val row = sheet.createRow()
            setValue(row, "datum", invoice.datum)
            setValue(row, "faelligkeit", invoice.faelligkeit)
            setValue(row, "gross", invoice.info.grossSum)?.setCellStyle(currencyStyle)
            setValue(row, "kreditor", invoice.kreditor)
            setValue(row, "betreff", invoice.betreff)
        }
        sheet.setAutoFilter()
    }

    private fun registerInvoiceDateColumns(sheet: ExcelSheet, clazz: Class<*>) {
        ExcelUtils.registerColumn(sheet, clazz, "datum", ExcelUtils.Size.DATE + 6)
        ExcelUtils.registerColumn(sheet, clazz, "faelligkeit", ExcelUtils.Size.DATE + 6)
    }

    /**
     * Sets [value] typed (dates as dates, amounts as numbers) and leaves the cell blank if it is null — the
     * Java getters of [LiquidityEntry] are platform types, which would bind the non-null overloads.
     */
    private fun setValue(row: ExcelRow, column: String, value: Any?): ExcelCell? {
        val cell = row.getCell(column) ?: return null
        when (value) {
            null -> {}
            is LocalDate -> cell.setCellValue(value)
            is BigDecimal -> cell.setCellValue(value)
            is Int -> cell.setCellValue(value)
            is Boolean -> cell.setCellValue(value)
            else -> cell.setCellValue(value.toString())
        }
        return cell
    }

    /** Leaves zero amounts blank (as the Wicket export did), so the days with cash flow stand out. */
    private fun setAmount(cell: ExcelCell, amount: BigDecimal, currencyStyle: CellStyle) {
        if (amount.signum() != 0) {
            cell.setCellValue(amount).setCellStyle(currencyStyle)
        }
    }

    private fun setFormula(sheet: ExcelSheet, rowIndex: Int, col: Int, formula: String, currencyStyle: CellStyle) {
        ExcelUtils.setCellFormula(sheet, rowIndex, col, formula).cellStyle = currencyStyle
    }
}
