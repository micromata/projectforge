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

import jakarta.annotation.PostConstruct
import org.projectforge.business.fibu.orderbooksnapshots.OrderbookSnapshotsService
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
import java.time.LocalDate

@Service
class ForecastOrderAnalysis {
    @Autowired
    private lateinit var auftragsRechnungCache: AuftragsRechnungCache

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var orderbookSnapshotsService: OrderbookSnapshotsService

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    @JvmOverloads
    fun exportOrderAnalysis(orderId: Long?, snapshotDate: LocalDate? = null): List<ForecastOrderPosInfo>? {
        val orderInfo = loadOrder(orderId = orderId, snapshotDate = snapshotDate)
        return exportOrderAnalysis(orderInfo)
    }

    private fun exportOrderAnalysis(orderInfo: OrderInfo?): List<ForecastOrderPosInfo>? {
        orderInfo ?: return null
        val result = orderInfo.infoPositions?.map { posInfo ->
            ForecastOrderPosInfo(orderInfo, posInfo).also {
                it.calculate()
            }
        }?.sortedBy { it.orderPosNumber }
        result?.forEach { fcPosInfo ->
            val posInfo = fcPosInfo.orderPosInfo
            val snapshotDate = orderInfo.snapshotDate
            // Add all invoices:
            filterInvoices(posInfo, snapshotDate)?.forEach { invoicePosInfo ->
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

    private fun loadOrder(orderId: Long? = null, orderNumber: Int? = null, snapshotDate: LocalDate?): OrderInfo? {
        val closestSnapshotDate = snapshotDate?.let {
            orderbookSnapshotsService.findClosestSnapshotDate(it)
        }
        val id = orderId ?: auftragsCache.findOrderInfoByNumber(orderNumber)?.id
        val order = if (closestSnapshotDate == null) {
            auftragDao.find(id)
        } else {
            orderbookSnapshotsService.readSnapshot(closestSnapshotDate)?.find { it.id == id }
        }
        return order?.info
    }

    @JvmOverloads
    fun htmlExportAsByteArray(
        orderId: Long? = null,
        orderNumber: Int? = null,
        snapshotDate: LocalDate? = null,
    ): ByteArray {
        return htmlExport(orderId = orderId, orderNumber = orderNumber, snapshotDate = snapshotDate).toByteArray()
    }

    fun htmlExport(
        orderId: Long? = null,
        orderNumber: Int? = null,
        snapshotDate: LocalDate? = null,
        checkAccess: Boolean = true,
    ): String {
        if (checkAccess) {
            auftragDao.find(orderId) // Throws AccessException if not allowed.
        }
        val orderInfo =
            loadOrder(orderId = orderId, orderNumber = orderNumber, snapshotDate)
                ?: return noAnalysis("Order with id $orderId or positions not found.")
        return htmlExport(orderInfo)
    }

    fun htmlExport(orderInfo: OrderInfo, ): String {
        val orderId = orderInfo.id
        val list = exportOrderAnalysis(orderInfo)
            ?: return noAnalysis("No order positions found for order #${orderInfo.nummer}.")
        val firstMonth = list.flatMap { it.months }.minByOrNull { it.date }?.date
        val lastMonth = list.flatMap { it.months }.maxByOrNull { it.date }?.date
        if (firstMonth == null || lastMonth == null) {
            return noAnalysis("No order positions found for order with id ${orderId}.")
        }
        val title = "Forecast Order Analysis for order #${orderInfo.nummer}"
        val html = HtmlDocument(title)
        html.add(Html.H1(title))
        html.add(Html.H2().also {
            orderInfo.snapshotDate?.let { snapshotDate ->
                it.add(Html.Span("Snapshot date: $snapshotDate, ", style = "color: red; font-weight: bold;"))
            }
            it.add(Html.Span("created: ${PFDateTime.now().format()}"))
        })
        html.add(Html.Alert(Html.Alert.Type.INFO).also { div ->
            div.add(Html.Text("Forecast values are shown in "))
            div.add(Html.Span("blue,", style = "color: blue; font-weight: bold;"))
            div.add(Html.Text(" invoiced amounts in "))
            div.add(Html.Span("green.", style = "color: green; font-weight: bold;"))
        })
        val lostBudget = list.sumOf { it.lostBudget }
        val lostBudgetWarning = list.any { it.lostBudgetWarning }
        if (lostBudgetWarning) {
            html.add(Html.Alert(Html.Alert.Type.DANGER).also { div ->
                div.add(Html.Text("There is a lost-budget warning of ${lostBudget.formatCurrency(true)} (see positions below)."))
            })
        }
        //
        // Order information:
        //
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
            addRow(table, translate("fibu.probabilityOfOccurrence"), "${orderInfo.probabilityOfOccurrence} %")
            addRow(
                table,
                translate("fibu.auftrag.forecastType"),
                "${translate(ForecastUtils.getForecastType(orderInfo).i18nKey)}: ${translate("fibu.auftrag.forecastType.info")}"
            )
            addRow(table, translate("fibu.auftrag.nettoSumme"), orderInfo.netSum.formatCurrency(true))
            addRow(
                table,
                translate("fibu.auftrag.nettoSumme.weighted"),
                list.sumOf { it.weightedNetSum }.formatCurrency(true),
            )
            addRow(table, translate("fibu.invoiced"), orderInfo.invoicedSum, suppressZero = false)
            addRow(table, translate("fibu.notYetInvoiced"), orderInfo.notYetInvoicedSum)
            addRow(table, "Lost buget", lostBudget)
        })
        //
        // Forecast for all positions
        //
        html.add(Html.H2("${translate("fibu.auftrag.forecast")} all positions"))
        html.add(
            Html.Alert(Html.Alert.Type.INFO).also { div ->
                div.add(Html.P("Distribution of Forecast (Monatsverteilung)").add(CssClass.BOLD))
                div.add(HtmlList(HtmlList.Type.ORDERED).also { list ->
                    list.addItem("The payment plan is used first, if available.")
                        .addItem().also { item ->
                            item.add("Fixed price", bold = true)
                                .add(" projects are scheduled at the end of the performance period.")
                        }
                        .addItem().also { item ->
                            item.add("Time and materials", bold = true)
                                .add(" and ")
                                .add("Flat-rate", bold = true)
                                .add(" projects are distributed monthly during the performance period.")
                        }
                })
            })
        html.add(HtmlTable().also { table ->
            val headRow = table.addHeadRow()
            headRow.addTH(translate("label.position.short"))
            val rows = mutableListOf<HtmlTable.TR>()
            val totals = mutableListOf<BigDecimal>()
            list.forEach { fcPosInfo ->
                rows.add(table.addRow().also {
                    it.addTD().also { td ->
                        td.add(Html.A("#pos${fcPosInfo.orderPosNumber}", "#${fcPosInfo.orderPosNumber}"))
                    }
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
                    } else {
                        rows[index].addTD() // Empty cell
                    }
                }
                totals.add(total)
                currentMonth = currentMonth.plusMonths(1)
            } while (currentMonth <= lastMonth && paranoiaCounter-- > 0)
            table.addRow().also { tr ->
                tr.addTH("Sum")
                totals.forEach {
                    tr.addTD(it.formatCurrency()).also {
                        it.add(CssClass.ALIGN_RIGHT, CssClass.BOLD)
                    }
                }
            }
        })
        //
        // Forecast for each position
        //
        list.forEach { fcPosInfo ->
            val posInfo = fcPosInfo.orderPosInfo
            html.add(
                Html.H2(
                    "${translate("fibu.auftrag.position")} #${posInfo.number}",
                    id = "pos${posInfo.number}"
                )
            )
            if (fcPosInfo.lostBudgetWarning) {
                html.add(Html.Alert(Html.Alert.Type.DANGER).also { div ->
                    div.add(
                        Html.Text(
                            "There is a lost-budget warning of ${
                                fcPosInfo.lostBudget.formatCurrency(
                                    true
                                )
                            }"
                        )
                    )
                })
            }
            // Position information:
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
                addRow(
                    table,
                    translate("fibu.auftrag.forecastType"),
                    "${
                        translate(
                            ForecastUtils.getForecastType(
                                orderInfo,
                                posInfo
                            ).i18nKey
                        )
                    }: ${translate("fibu.auftrag.forecastType.info")}"
                )
                addRow(table, translate("fibu.auftrag.nettoSumme"), posInfo.netSum.formatCurrency(true))
                addRow(
                    table,
                    translate("fibu.auftrag.nettoSumme.weighted"),
                    fcPosInfo.weightedNetSum.formatCurrency(true)
                )
                addRow(table, translate("fibu.invoiced"), posInfo.invoicedSum.formatCurrency(true))
                addRow(table, translate("fibu.notYetInvoiced"), posInfo.notYetInvoiced)
                addRow(table, translate("projectmanagement.personDays"), posInfo.personDays.formatForUser())
                addRow(table, translate("fibu.notYetInvoiced"), posInfo.notYetInvoiced)
                addRow(table, "lost budget", fcPosInfo.lostBudget)
                fcPosInfo.difference
                fcPosInfo.getRemainingForecastSumAfter(PFDay.now())
                addRow(
                    table,
                    translate("fibu.periodOfPerformance"),
                    "${posInfo.periodOfPerformanceBegin.formatForUser()} - ${posInfo.periodOfPerformanceEnd.formatForUser()}"
                )
            })
            // Invoices:
            html.add(Html.H3(translate("fibu.rechnung.rechnungen")))
            html.add(HtmlTable().also { table ->
                table.addHeadRow().also { tr ->
                    tr.addTH(translate("fibu.rechnung.nummer"))
                    tr.addTH(translate("fibu.rechnung.datum"))
                    tr.addTH(translate("fibu.common.netto"))
                    tr.addTH(translate("fibu.rechnung.status.bezahlt"))
                    tr.addTH(translate("fibu.rechnung.text"), CssClass.EXPAND)
                }
                filterInvoices(posInfo, orderInfo.snapshotDate)?.forEach { invoicePosInfo ->
                    val invoiceInfo = invoicePosInfo.rechnungInfo
                    table.addRow().also { row ->
                        row.addTD("${invoiceInfo?.nummer}#${invoicePosInfo.number}")
                        row.addTD(invoiceInfo?.date.formatForUser())
                        row.addTD(invoicePosInfo.netSum.formatCurrency(), CssClass.ALIGN_RIGHT)
                        row.addTD(translate(invoiceInfo?.isBezahlt))
                        row.addTD(invoicePosInfo.text, CssClass.EXPAND)
                    }
                }
            })
            // Payment schedule:
            html.add(Html.H3(translate("fibu.auftrag.paymentschedule")))
            html.add(HtmlTable().also { table ->
                table.addHeadRow().also { tr ->
                    tr.addTH(translate("fibu.rechnung.datum.short"))
                    tr.addTH(translate("fibu.common.betrag"))
                    tr.addTH(translate("fibu.common.reached"))
                    tr.addTH(translate("comment"), CssClass.EXPAND)
                }
                orderInfo.paymentScheduleEntries?.filter { it.positionNumber == posInfo.number }
                    ?.forEach { entry ->
                        table.addRow().also { row ->
                            row.addTD(entry.scheduleDate.formatForUser())
                            row.addTD(entry.amount.formatCurrency(), CssClass.ALIGN_RIGHT)
                            row.addTD(translate(entry.reached))
                            row.addTD(entry.comment, CssClass.EXPAND)
                        }
                    }
            })
            // Forecast for position:
            html.add(Html.H3("${translate("fibu.auftrag.forecast")} #${posInfo.number}")) // Forecast current position
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

    private fun addRow(
        table: HtmlTable,
        label: String,
        value: BigDecimal?,
        suppressZero: Boolean = true
    ) {
        if (suppressZero && (value == null || value.abs() < BigDecimal.ONE)) {
            return
        }
        table.addRow().also { row ->
            row.addTH(label, CssClass.FIXED_WIDTH_NO_WRAP)
            row.addTD(value.formatCurrency(true))
        }
    }

    private fun noAnalysis(msg: String): String {
        return HtmlDocument(msg).add(Html.Alert(Html.Alert.Type.DANGER, msg)).toString()
    }

    private fun addForecastValue(
        row: HtmlTable.TR,
        month: ForecastOrderPosInfo.MonthEntry
    ): BigDecimal {
        val cssClass = if (month.lostBudgetWarning) CssClass.ERROR else CssClass.ALIGN_RIGHT
        val amount = maxOf(month.toBeInvoicedSum, month.invoicedSum)
        val style =
            if (amount == month.toBeInvoicedSum && amount.abs() >= BigDecimal.ONE) "color: blue;" else "color: green;"
        row.addTD(amount.formatCurrency(), cssClass).also { td -> td.attr("style", style) }
        return amount
    }

    private fun filterInvoices(
        posInfo: OrderPositionInfo,
        snapshotDate: LocalDate?
    ): Collection<RechnungPosInfo>? {
        val invoicePositions = auftragsRechnungCache.getRechnungsPosInfosByAuftragsPositionId(posInfo.id)
        return if (snapshotDate == null) {
            invoicePositions
        } else {
            invoicePositions?.filter { (it.rechnungInfo?.date ?: LocalDate.MAX) <= snapshotDate }
        }
    }

    companion object {
        private lateinit var instance: ForecastOrderAnalysis

        fun createAnalysisAsHtml(
            orderId: Long? = null,
            orderNumber: Int? = null,
            snapshotDate: LocalDate? = null,
        ): String {
            return instance.htmlExport(orderId = orderId, orderNumber = orderNumber, snapshotDate)
        }
    }
}
