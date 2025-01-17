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

package org.projectforge.business.fibu

import mu.KotlinLogging
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.fibu.orderbooksnapshots.OrderbookSnapshotsService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.*

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
    private lateinit var rechnungCache: RechnungCache

    internal fun fillInvoices(ctx: ForecastExportContext) {
        val firstMonthCol = ctx.invoicesSheet.getColumnDef(ForecastExportContext.MonthCol.MONTH1.header)!!.columnNumber
        for (invoice in ctx.invoices) {
            if (invoice.status == RechnungStatus.GEPLANT || invoice.status == RechnungStatus.STORNIERT) {
                continue // Ignoriere stornierte oder geplante Rechnungen.
            }
            rechnungCache.getRechnungInfo(invoice.id)?.positions?.forEach { pos ->
                // for (pos in rechnungCache.getRechnungsPositionVOSetByRechnungId(invoice.id) ?: continue) {
                val orderPosId = pos.auftragsPositionId
                val orderPositionFound = orderPosId != null && ctx.orderPositionMap.containsKey(orderPosId)
                // val invoiceProjektId = invoice.projektId
                // val orderProjectId = orderPos?.auftrag?.projektId // may differ from invoiceProjektId
                // val projectFound = invoiceProjektId != null && ctx.projectIds.contains(invoiceProjektId) ||
                //     orderProjectId != null && ctx.projectIds.contains(orderProjectId)
                if (!ctx.showAll && !orderPositionFound) { // !projectFound && !orderPositionFound) {
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
                if (monthIndex !in -12..11) {
                    return@forEach // continue
                }
                val sheet = if (monthIndex < 0) ctx.invoicesPrevYearSheet else ctx.invoicesSheet
                if (monthIndex < 0) {
                    monthIndex += 12
                }
                val rowNumber = sheet.createRow().rowNum
                sheet.setIntValue(rowNumber, ForecastExportContext.InvoicesCol.INVOICE_NR.header, invoice.nummer)
                sheet.setStringValue(rowNumber, ForecastExportContext.InvoicesCol.POS_NR.header, "#${pos.number}")
                sheet.setDateValue(
                    rowNumber,
                    ForecastExportContext.InvoicesCol.DATE.header,
                    PFDay(invoice.datum!!).localDate,
                    ctx.excelDateFormat
                )
                val projekt = projektCache.getProjektIfNotInitialized(invoice.projekt)
                sheet.setStringValue(
                    rowNumber,
                    ForecastExportContext.InvoicesCol.CUSTOMER.header,
                    invoice.kundeAsString
                )
                sheet.setStringValue(rowNumber, ForecastExportContext.InvoicesCol.PROJECT.header, projekt?.name)
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
        }
    }

    private fun getMonthIndex(ctx: ForecastExportContext, date: PFDay): Int {
        val monthDate = date.year * 12 + date.monthValue
        val monthBaseDate = ctx.startDate.year * 12 + ctx.startDate.monthValue
        return monthDate - monthBaseDate // index from 0 to 11
    }

}
