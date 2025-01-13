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

import org.projectforge.common.extensions.formatCurrency
import org.projectforge.common.extensions.formatForUser
import org.projectforge.common.extensions.formatFractionAsPercent
import org.projectforge.common.html.*
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ForecastOrderAnalysis {
    @Autowired
    private lateinit var ordersCache: AuftragsCache

    @Autowired
    private lateinit var auftragsRechnungCache: AuftragsRechnungCache

    fun exportOrderAnalysis(orderId: Long?): List<ForecastOrderPosInfo>? {
        val orderInfo = ordersCache.getOrderInfo(orderId)
        val result = orderInfo?.infoPositions?.map { posInfo ->
            ForecastOrderPosInfo(orderInfo, posInfo).also {
                it.calculate()
            }
        }?.sortedBy { it.orderPosNumber }
        result?.forEach { fcPosInfo ->
            val posInfo = fcPosInfo.orderPosInfo
            // Add all invoices:
            auftragsRechnungCache.getRechnungsPosInfosByAuftragsPositionId(posInfo.id)?.forEach { invoicePosInfo ->
                val invoiceInfo = invoicePosInfo.rechnungInfo
                val date = invoiceInfo?.date
                val netSum = invoicePosInfo.netSum
                fcPosInfo.months.find { it.date.year == date?.year && it.date.month == date.month }?.let {
                    it.invoicedSum += netSum
                }
            }
        }
        return result
    }

    fun htmlExportAsByteArray(orderId: Long?): ByteArray {
        return htmlExport(orderId).toByteArray()
    }

    fun htmlExport(orderId: Long?): String {
        val orderInfo = ordersCache.getOrderInfo(orderId) ?: return noAnalysis("Order with id $orderId not found.")
        val list = exportOrderAnalysis(orderId)
            ?: return noAnalysis("No order positions found for order #${orderInfo.nummer}.")
        val firstMonth = list.flatMap { it.months }.minByOrNull { it.date }?.date
        val lastMonth = list.flatMap { it.months }.maxByOrNull { it.date }?.date
        if (firstMonth == null || lastMonth == null) {
            return noAnalysis("No order positions found for order with id ${orderId}.")
        }
        val title = "Forecast Order Analysis for order #${orderInfo.nummer}, ${PFDateTime.now().format()}"
        val html = HtmlDocument(title)
        html.add(H1(title))
        html.add(Alert(Alert.Type.INFO).also { div ->
            div.add(Span("Forecast values are shown in "))
            div.add(Span("red,", style = "color: red; font-weight: bold;"))
            div.add(Span(" invoiced amounts in "))
            div.add(Span("black.", style = "color: black; font-weight: bold;"))
        })
        html.add(HtmlTable().also { table ->
            addRow(table, translate("fibu.auftrag.nummer"), orderInfo.nummer.toString())
            addRow(table, translate("fibu.auftrag.angebot.datum"), orderInfo.angebotsDatum.formatForUser())
            addRow(table, translate("fibu.auftrag.title"), orderInfo.titel)
            addRow(table, translate("fibu.kunde"), orderInfo.kundeAsString)
            addRow(table, translate("fibu.projekt"), orderInfo.projektAsString)
            addRow(table, translate("comment"), orderInfo.bemerkung)
            addRow(table, translate("status"), orderInfo.statusAsString)
            addRow(
                table,
                translate("fibu.periodOfPerformance"),
                "${orderInfo.periodOfPerformanceBegin.formatForUser()} - ${orderInfo.periodOfPerformanceEnd.formatForUser()}"
            )
            addRow(table, translate("fibu.auftrag.nettoSumme"), orderInfo.netSum.formatCurrency(true))
            addRow(table, translate("fibu.invoiced"), orderInfo.invoicedSum, suppressZero = false)
            addRow(table, translate("fibu.notYetInvoiced"), orderInfo.notYetInvoicedSum)
        })

        html.add(H2("${translate("fibu.auftrag.forecast")} all positions")) // Forecast for all positions
        html.add(HtmlTable().also { table ->
            val headRow = table.addHeadRow()
            headRow.addTH(translate("label.position.short"))
            val rows = mutableListOf<HtmlTable.TR>()
            val totals = mutableListOf<BigDecimal>()
            list.forEach { fcPosInfo ->
                rows.add(table.addRow().also {
                    it.addTD("#${fcPosInfo.orderPosNumber}")
                })
            }
            var currentMonth: PFDay = firstMonth
            var paranoiaCounter = 120
            do {
                headRow.addTH(ForecastExport.formatMonthHeader(currentMonth))
                var total = BigDecimal.ZERO
                list.forEachIndexed { index, fcPosInfo ->
                    val month = fcPosInfo.months.find { it.date == currentMonth }
                    if (month != null) {
                        val amount = addForecastValue(rows[index], month)
                        total += amount
                    }
                }
                totals.add(total)
                currentMonth = currentMonth.plusMonths(1)
            } while (currentMonth <= lastMonth && paranoiaCounter-- > 0)
            table.addRow().also { tr ->
                tr.addTH("Sum")
                totals.forEach {
                    tr.addTD(it.formatCurrency()).also {
                        it.addClasses(CssClass.ALIGN_RIGHT, CssClass.BOLD)
                    }
                }
            }
        })

        list.forEach { fcPosInfo ->
            val posInfo = fcPosInfo.orderPosInfo
            html.add(H2("${translate("fibu.auftrag.position")} #${posInfo.number}"))
            html.add(HtmlTable().also { table ->
                addRow(table, translate("title"), posInfo.titel)
                addRow(table, translate("comment"), posInfo.bemerkung)
                addRow(table, translate("status"), translate(posInfo.status))
                addRow(
                    table,
                    translate("fibu.probabilityOfOccurrence"),
                    fcPosInfo.probability.formatFractionAsPercent(true)
                )
                addRow(table, translate("fibu.auftrag.position.art"), translate(posInfo.art))
                addRow(table, translate("fibu.auftrag.position.paymenttype"), translate(posInfo.paymentType))
                addRow(table, translate("fibu.auftrag.nettoSumme"), posInfo.netSum.formatCurrency(true))
                addRow(table, translate("fibu.invoiced"), posInfo.invoicedSum.formatCurrency(true))
                addRow(table, translate("fibu.notYetInvoiced"), posInfo.notYetInvoiced)
                addRow(table, translate("projectmanagement.personDays"), posInfo.personDays.formatForUser())
                addRow(table, translate("fibu.notYetInvoiced"), posInfo.notYetInvoiced)
                fcPosInfo.difference
                fcPosInfo.getRemainingForecastSumAfter(PFDay.now())
                addRow(
                    table,
                    translate("fibu.periodOfPerformance"),
                    "${posInfo.periodOfPerformanceBegin.formatForUser()} - ${posInfo.periodOfPerformanceEnd.formatForUser()}"
                )
            })
            html.add(H3(translate("fibu.rechnung.rechnungen")))
            html.add(HtmlTable().also { table ->
                table.addHeadRow().also { tr ->
                    tr.addTH(translate("fibu.rechnung.nummer"))
                    tr.addTH(translate("fibu.rechnung.datum"))
                    tr.addTH(translate("fibu.common.netto"))
                    tr.addTH(translate("fibu.rechnung.status.bezahlt"))
                    tr.addTH(translate("fibu.rechnung.text"))
                }
                auftragsRechnungCache.getRechnungsPosInfosByAuftragsPositionId(posInfo.id)?.forEach { invoicePosInfo ->
                    val invoiceInfo = invoicePosInfo.rechnungInfo
                    table.addRow().also { row ->
                        row.addTD("${invoiceInfo?.nummer}#${invoicePosInfo.number}")
                        row.addTD(invoiceInfo?.date.formatForUser())
                        row.addTD(invoicePosInfo.netSum.formatCurrency(true))
                        row.addTD(translate(invoiceInfo?.isBezahlt))
                        row.addTD(invoicePosInfo.text)
                    }
                }
            })
            html.add(H3(translate("fibu.auftrag.paymentschedule")))
            html.add(HtmlTable().also { table ->
                table.addHeadRow().also { tr ->
                    tr.addTH(translate("fibu.rechnung.datum.short"))
                    tr.addTH(translate("fibu.common.betrag"))
                    tr.addTH(translate("fibu.common.reached"))
                    tr.addTH(translate("comment"))
                }
                orderInfo.paymentScheduleEntries?.filter { it.positionNumber == posInfo.number }?.forEach { entry ->
                    table.addRow().also { row ->
                        row.addTD(entry.scheduleDate.formatForUser())
                        row.addTD(entry.amount.formatCurrency(true))
                        row.addTD(translate(entry.reached))
                        row.addTD(entry.comment)
                    }
                }
            })
            html.add(H3("${translate("fibu.auftrag.forecast")} #${posInfo.number}")) // Forecast current position
            html.add(HtmlTable().also { table ->
                val headRow = table.addHeadRow()
                val row = table.addRow()
                fcPosInfo.months.forEach { month ->
                    headRow.addTH(ForecastExport.formatMonthHeader(month.date))
                    addForecastValue(row, month)
                }
            })
        }
        return html.toString()
    }

    private fun addRow(table: HtmlTable, label: String, value: String?) {
        table.addRow().also { row ->
            row.addTH(label, CssClass.FIXED_WIDTH_NO_WRAP)
            row.addTD(value)
        }
    }

    private fun addRow(table: HtmlTable, label: String, value: BigDecimal?, suppressZero: Boolean = true) {
        if (suppressZero && (value == null || value.abs() < BigDecimal.ONE)) {
            return
        }
        table.addRow().also { row ->
            row.addTH(label, CssClass.FIXED_WIDTH_NO_WRAP)
            row.addTD(value.formatCurrency(true))
        }
    }

    private fun noAnalysis(msg: String): String {
        return HtmlDocument(msg).add(Alert(Alert.Type.DANGER, msg)).toString()
    }

    private fun addForecastValue(row: HtmlTable.TR, month: ForecastOrderPosInfo.MonthEntry): BigDecimal {
        val cssClass = if (month.error) CssClass.ERROR else CssClass.ALIGN_RIGHT
        val amount = maxOf(month.toBeInvoicedSum, month.invoicedSum)
        val style = if (amount == month.toBeInvoicedSum && amount.abs() >= BigDecimal.ONE) "color: red;" else null
        row.addTD(amount.formatCurrency(), cssClass).also { td ->
            if (style != null) {
                td.attr("style", style)
            }
        }
        return amount
    }
}
