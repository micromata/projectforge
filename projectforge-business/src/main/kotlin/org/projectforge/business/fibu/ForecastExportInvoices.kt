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

package org.projectforge.business.fibu

import de.micromata.merlin.excel.ExcelSheet
import mu.KotlinLogging
import org.projectforge.business.fibu.ForecastExportContext.ForecastCol
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Fills the invoices in the forecast excel workbook.
 *
 * @author Florian Blumenstein
 * @author Kai Reinhard
 */
@Service
internal class ForecastExportInvoices { // open needed by Wicket.
    @Autowired
    private lateinit var ordersCache: AuftragsCache

    @Autowired
    private lateinit var projektCache: ProjektCache

    @Autowired
    private lateinit var kundeCache: KundeCache

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    internal fun fillInvoices(ctx: ForecastExportContext) {
        val firstMonthCol = ctx.invoicesSheet.getColumnDef(ForecastExportContext.MonthCol.MONTH1.header)!!.columnNumber
        for (invoice in ctx.invoices) {
            if (invoice.status == RechnungStatus.GEPLANT || invoice.status == RechnungStatus.STORNIERT) {
                continue // Ignoriere stornierte oder geplante Rechnungen.
            }
            // Resolve the lazy kunde/projekt proxies from the caches once per invoice (avoids an n+1: reading
            // invoice.kundeAsString below would otherwise trigger one SELECT on T_FIBU_KUNDE per invoice). The
            // identifier read inside getXIfNotInitialized is free (KundeDO.nummer/ProjektDO.id are the @Id).
            invoice.kunde = kundeCache.getKundeIfNotInitialized(invoice.kunde)
            invoice.projekt = projektCache.getProjektIfNotInitialized(invoice.projekt)
            rechnungCache.getRechnungInfo(invoice.id)?.positions?.forEach { pos ->
                val orderPosInfo = rechnungCache.getOrderPositionInfoOfInvoicePos(pos.id)
                val orderPosId = orderPosInfo?.id
                val orderPositionFound = orderPosId != null && ctx.orderPositionMap.containsKey(orderPosId)
                if (!ctx.showAll && !orderPositionFound) {
                    return@forEach // Ignore invoices referring an order position or project which isn't part of the order list filtered by the user.
                }
                var order = if (orderPosId != null) {
                    ctx.orderMapByPositionId[orderPosId]
                } else {
                    null
                }
                if (orderPosId != null && order == null) {
                    val orderId = pos.auftragsId ?: ordersCache.getOrderPositionInfo(orderPosId)?.auftragId
                    order = ordersCache.getOrderInfo(orderId)
                    if (order == null) {
                        log.error("Shouldn't occur: can't determine order from order position: $orderPosId")
                        return@forEach // continue
                    }
                    ctx.orderMapByPositionId[orderPosId] = order
                }
                var monthIndex = getMonthIndex(ctx, PFDay.fromOrNow(invoice.datum))
                if (monthIndex !in -24..11) {
                    return@forEach // continue
                }
                if (monthIndex >= 0) {
                    insertIntoSheet(ctx, ctx.invoicesSheet, invoice, pos, order, orderPosId, firstMonthCol, monthIndex)
                    ctx.planningDate?.let { planningDate ->
                        if (invoice.datum!! < planningDate) {
                            // Planning date is given, so add the invoice to the planning sheet.
                            insertIntoSheet(
                                ctx,
                                ctx.planningInvoicesSheet,
                                invoice,
                                pos,
                                order,
                                orderPosId,
                                firstMonthCol,
                                monthIndex
                            )
                        }
                    }
                } else if (monthIndex >= -12) {
                    // The 12 months before startDate: shift into the same Month1..Month12 columns of the prev-year sheet.
                    monthIndex += 12
                    insertIntoSheet(
                        ctx,
                        ctx.invoicesPrevYearSheet,
                        invoice,
                        pos,
                        order,
                        orderPosId,
                        firstMonthCol,
                        monthIndex
                    )
                } else {
                    // The 12 months two years before startDate (-24..-13): shift into the prev-prev-year sheet.
                    monthIndex += 24
                    insertIntoSheet(
                        ctx,
                        ctx.invoicesPrevPrevYearSheet,
                        invoice,
                        pos,
                        order,
                        orderPosId,
                        firstMonthCol,
                        monthIndex
                    )
                }
            }
        }
    }

    /**
     * Writes the visible-column COUNTIF formula for every invoice row collected by [fillInvoices]/[insertIntoSheet].
     * Must be called after [ForecastExport.fillOrderPositions], because the formula references the forecast sheet's
     * visibleID range whose upper bound ([ForecastExportContext.forecastDataLastExcelRow]) is only known then. Bounding
     * the range to the actual last forecast row (instead of a fixed 100000) keeps the formula cheap to evaluate.
     */
    internal fun fillInvoiceVisibleColumn(ctx: ForecastExportContext) {
        val visibleProjectIdCol =
            ctx.forecastSheet.getColumnDef(ForecastCol.VISIBLE_PROJECT_ID.header)?.columnNumberAsLetters
        val lastRow = ctx.forecastDataLastExcelRow
        // The PROJECT_ID column letter is identical across all invoice sheets (same template layout), so resolve it once.
        val projectIdCol = ctx.invoicesSheet.getColumnDef(ForecastExportContext.InvoicesCol.PROJECT_ID.header)
            ?.columnNumberAsLetters
        ctx.invoiceVisibleCells.forEach { (sheet, rowNumber) ->
            val excelRowNumber = rowNumber + 1 // Excel row numbers start with 1.
            ExcelUtils.setCellFormula(
                sheet,
                rowNumber,
                ForecastExportContext.InvoicesCol.VISIBLE.header,
                "COUNTIF(Forecast_Data!$visibleProjectIdCol$11:$visibleProjectIdCol$lastRow, $projectIdCol$excelRowNumber) > 0"
            )
        }
    }

    private fun insertIntoSheet(
        ctx: ForecastExportContext, sheet: ExcelSheet, invoice: RechnungDO, pos: RechnungPosInfo,
        order: OrderInfo?, orderPosId: Long?, firstMonthCol: Int, monthIndex: Int,
    ) {
        invoice.projekt?.id?.let {
            ctx.invoicedProjectIds.add(it)
        }
        order?.projektId?.let {
            ctx.invoicedProjectIds.add(it)
        }
        val rowNumber = sheet.createRow().rowNum
        sheet.setIntValue(rowNumber, ForecastExportContext.InvoicesCol.INVOICE_NR.header, invoice.nummer)
        // Fall back to the project of the order: the filter selection of the forecast sheet is propagated by project
        // id only, so every invoice row needs one. Invoices without any project get PROJECT_ID_NONE (see the
        // 'without project' pseudo order row in the forecast sheet).
        val projectId = invoice.projekt?.id ?: order?.projektId
        if (projectId == null) {
            ctx.hasInvoicesWithoutProject = true
        }
        ExcelUtils.setLongValue(
            sheet,
            rowNumber,
            ForecastExportContext.InvoicesCol.PROJECT_ID.header,
            projectId ?: ForecastExportContext.PROJECT_ID_NONE
        )
        sheet.setStringValue(rowNumber, ForecastExportContext.InvoicesCol.POS_NR.header, "#${pos.number}")
        // The visible-column COUNTIF references the forecast sheet's visibleID range, which only exists after
        // fillOrderPositions has written its rows. Remember this cell and write the formula later in
        // fillInvoiceVisibleColumn, so the range can be bounded to the actual last forecast row.
        ctx.invoiceVisibleCells.add(sheet to rowNumber)
        sheet.setDateValue(
            rowNumber,
            ForecastExportContext.InvoicesCol.DATE.header,
            PFDay(invoice.datum!!).localDate,
            ctx.excelDateFormat
        )
        // kunde and projekt were already resolved from the caches in fillInvoices, so these reads don't lazy-load.
        sheet.setStringValue(
            rowNumber,
            ForecastExportContext.InvoicesCol.CUSTOMER.header,
            invoice.kundeAsString
        )
        sheet.setStringValue(rowNumber, ForecastExportContext.InvoicesCol.PROJECT.header, invoice.projekt?.name)
        sheet.setStringValue(rowNumber, ForecastExportContext.InvoicesCol.SUBJECT.header, invoice.betreff)
        sheet.setStringValue(rowNumber, ForecastExportContext.InvoicesCol.POS_TEXT.header, pos.text)
        invoice.bezahlDatum?.let {
            sheet.setDateValue(
                rowNumber,
                ForecastExportContext.InvoicesCol.DATE_OF_PAYMENT.header,
                PFDay(it).localDate,
                ctx.excelDateFormat
            )
        }
        val leistungsZeitraumColDef =
            sheet.getColumnDef(ForecastExportContext.InvoicesCol.LEISTUNGSZEITRAUM.header)
        invoice.periodOfPerformanceBegin?.let {
            sheet.setDateValue(rowNumber, leistungsZeitraumColDef, PFDay(it).localDate, ctx.excelDateFormat)
        }
        invoice.periodOfPerformanceEnd?.let {
            sheet.setDateValue(
                rowNumber,
                leistungsZeitraumColDef!!.columnNumber + 1,
                PFDay(it).localDate,
                ctx.excelDateFormat
            )
        }
        if (order != null && orderPosId != null) {
            sheet.setStringValue(
                rowNumber,
                ForecastExportContext.InvoicesCol.ORDER.header,
                "${order.nummer}.${pos.auftragsPositionNummer}"
            )
        }
        sheet.setBigDecimalValue(
            rowNumber,
            ForecastExportContext.InvoicesCol.NETSUM.header,
            pos.netSum
        ).cellStyle =
            ctx.currencyCellStyle
        sheet.setBigDecimalValue(rowNumber, firstMonthCol + monthIndex, pos.netSum).cellStyle =
            ctx.currencyCellStyle

    }

    private fun getMonthIndex(ctx: ForecastExportContext, date: PFDay): Int {
        val monthDate = date.year * 12 + date.monthValue
        val monthBaseDate = ctx.startDate.year * 12 + ctx.startDate.monthValue
        return monthDate - monthBaseDate // index from 0 to 11
    }

}
