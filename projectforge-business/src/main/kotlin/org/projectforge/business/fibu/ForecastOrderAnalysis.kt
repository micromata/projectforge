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

import jakarta.annotation.PostConstruct
import org.projectforge.business.fibu.orderbooksnapshots.OrderbookSnapshotsService
import org.projectforge.common.extensions.formatCurrency
import org.projectforge.common.extensions.formatForUser
import org.projectforge.common.extensions.formatFractionAsPercent
import org.projectforge.common.html.*
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
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

    /**
     * @param distributeUnusedBudget If given, this value is used instead of the configured default
     * ([ForecastOrderPosInfo.defaultDistributeUnusedBudget], see
     * projectforge.fibu.forecast.distributeUnusedBudget).
     */
    @JvmOverloads
    fun exportOrderAnalysis(
        orderId: Long?,
        snapshotDate: LocalDate? = null,
        distributeUnusedBudget: Boolean? = null,
    ): List<ForecastOrderPosInfo>? {
        val orderInfo = loadOrder(orderId = orderId, snapshotDate = snapshotDate)
        return exportOrderAnalysis(orderInfo, distributeUnusedBudget)
    }

    private fun exportOrderAnalysis(
        orderInfo: OrderInfo?,
        distributeUnusedBudget: Boolean? = null,
    ): List<ForecastOrderPosInfo>? {
        orderInfo ?: return null
        val snapshotDate = orderInfo.snapshotDate
        val result = orderInfo.infoPositions?.map { posInfo ->
            ForecastOrderPosInfo(orderInfo, posInfo).also {
                // Distribution must not re-forecast months already covered by actual invoices:
                it.lastInvoiceMonth = latestInvoiceMonth(posInfo, snapshotDate)
                distributeUnusedBudget?.let { value -> it.distributeUnusedBudget = value }
                it.calculate()
            }
        }?.sortedBy { it.orderPosNumber }
        result?.forEach { fcPosInfo ->
            val posInfo = fcPosInfo.orderPosInfo
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
                ?: return noAnalysis(translateMsg("$I18N_PREFIX.error.orderNotFound", orderId))
        return htmlExport(orderInfo)
    }

    fun htmlExport(orderInfo: OrderInfo, ): String {
        val orderId = orderInfo.id
        // Both cases of projectforge.fibu.forecast.distributeUnusedBudget are calculated, because they may differ
        // significantly for time&material positions (even distribution vs. run rate extrapolation). The configured
        // default comes first, so that only the other variant drops out if both produce the same forecast:
        val defaultFirst = ForecastOrderPosInfo.defaultDistributeUnusedBudget
        val variants = listOf(defaultFirst, !defaultFirst).mapNotNull { distributeUnusedBudget ->
            exportOrderAnalysis(orderInfo, distributeUnusedBudget)?.let { Variant(distributeUnusedBudget, it) }
        }.distinctBy { variant -> variant.list.joinToString("||") { signature(it) } }
        if (variants.isEmpty()) {
            return noAnalysis(translateMsg("$I18N_PREFIX.error.noPositions", orderInfo.nummer))
        }
        val showVariants = variants.size > 1
        // The list of the configured default is used for all values which don't depend on distributeUnusedBudget:
        val list = variants.first().list
        val allMonths = variants.flatMap { variant -> variant.list.flatMap { it.months } }
        val firstMonth = allMonths.minByOrNull { it.date }?.date
        val lastMonth = allMonths.maxByOrNull { it.date }?.date
        if (firstMonth == null || lastMonth == null) {
            return noAnalysis(translateMsg("$I18N_PREFIX.error.noPositions", orderInfo.nummer))
        }
        val title = translateMsg("$I18N_PREFIX.title", orderInfo.nummer)
        val html = HtmlDocument(title)
        html.add(Html.H1(title))
        html.add(Html.H2().also {
            orderInfo.snapshotDate?.let { snapshotDate ->
                it.add(
                    Html.Span(
                        "${translate("$I18N_PREFIX.snapshotDate")}: $snapshotDate, ",
                        style = "color: red; font-weight: bold;"
                    )
                )
            }
            it.add(Html.Span("${translate("created")}: ${PFDateTime.now().format()}"))
        })
        html.add(Html.Alert(Html.Alert.Type.INFO).also { div ->
            div.add(Html.Text(translate("$I18N_PREFIX.legend.forecast") + " "))
            div.add(Html.Span(translate("$I18N_PREFIX.legend.blue"), style = "color: blue; font-weight: bold;"))
            div.add(Html.Text(" " + translate("$I18N_PREFIX.legend.invoiced") + " "))
            div.add(Html.Span(translate("$I18N_PREFIX.legend.green"), style = "color: green; font-weight: bold;"))
        })
        if (showVariants) {
            html.add(Html.Alert(Html.Alert.Type.INFO).also { div ->
                div.add(Html.P(translateMsg("$I18N_PREFIX.variants.info", DISTRIBUTE_UNUSED_BUDGET)).add(CssClass.BOLD))
                div.add(HtmlList(HtmlList.Type.UNORDERED).also { ul ->
                    ul.addItem().also { item ->
                        item.add(variantLabel(true), bold = true)
                            .add(": " + translate("$I18N_PREFIX.variants.true"))
                    }
                    ul.addItem().also { item ->
                        item.add(variantLabel(false), bold = true)
                            .add(
                                ": " + translateMsg(
                                    "$I18N_PREFIX.variants.false",
                                    ForecastOrderPosInfo.RUN_RATE_MIN_ELAPSED_MONTHS,
                                )
                            )
                    }
                })
                div.add(
                    Html.P(
                        translateMsg(
                            "$I18N_PREFIX.variants.default",
                            variantLabel(ForecastOrderPosInfo.defaultDistributeUnusedBudget),
                        )
                    )
                )
            })
        }
        variants.forEach { variant ->
            if (variant.lostBudgetWarning) {
                html.add(Html.Alert(Html.Alert.Type.DANGER).also { div ->
                    val msg = translateMsg(
                        "$I18N_PREFIX.lostBudget.warning",
                        variant.lostBudget.formatCurrency(true),
                    )
                    div.add(Html.Text(if (showVariants) "${variantLabel(variant.distributeUnusedBudget)}: $msg" else msg))
                })
            }
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
            variants.forEach { variant ->
                addRow(table, label(translate("$I18N_PREFIX.lostBudget"), variant, showVariants), variant.lostBudget)
            }
        })
        //
        // Forecast for all positions
        //
        html.add(Html.H2(translate("$I18N_PREFIX.allPositions")))
        html.add(
            Html.Alert(Html.Alert.Type.INFO).also { div ->
                div.add(Html.P(translate("$I18N_PREFIX.distribution")).add(CssClass.BOLD))
                div.add(HtmlList(HtmlList.Type.ORDERED).also { list ->
                    list.addItem(translate("$I18N_PREFIX.distribution.paymentSchedule"))
                        .addItem().also { item ->
                            item.add(translate("fibu.auftrag.position.paymenttype.festpreispaket"), bold = true)
                                .add(": " + translate("$I18N_PREFIX.distribution.fixedPrice"))
                        }
                        .addItem().also { item ->
                            item.add(translate("fibu.auftrag.position.paymenttype.time_and_materials"), bold = true)
                                .add(", ")
                                .add(translate("fibu.auftrag.position.paymenttype.pauschale"), bold = true)
                                .add(": " + translate("$I18N_PREFIX.distribution.monthly"))
                        }
                })
            })
        variants.forEach { variant ->
            if (showVariants) {
                html.add(Html.H3(variantHeader(variant)))
            }
            html.add(HtmlTable().also { table ->
                val headRow = table.addHeadRow()
                headRow.addTH(translate("label.position.short"))
                val rows = mutableListOf<HtmlTable.TR>()
                val totals = mutableListOf<BigDecimal>()
                variant.list.forEach { fcPosInfo ->
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
                    variant.list.forEachIndexed { index, fcPosInfo ->
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
                    tr.addTH(translate("sum"))
                    totals.forEach {
                        tr.addTD(it.formatCurrency()).also {
                            it.add(CssClass.ALIGN_RIGHT, CssClass.BOLD)
                        }
                    }
                }
            })
        }
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
            // The variant counterparts of this position (same position, calculated with distributeUnusedBudget=true/false):
            val posVariants = variants.mapNotNull { variant ->
                variant.list.find { it.orderPosNumber == fcPosInfo.orderPosNumber }
                    ?.let { PosVariant(variant.distributeUnusedBudget, it) }
            }
            posVariants.forEach { posVariant ->
                if (posVariant.fcPosInfo.lostBudgetWarning) {
                    html.add(Html.Alert(Html.Alert.Type.DANGER).also { div ->
                        val msg = translateMsg(
                            "$I18N_PREFIX.lostBudget.warning.position",
                            posVariant.fcPosInfo.lostBudget.formatCurrency(true),
                        )
                        div.add(
                            Html.Text(
                                if (showVariants) {
                                    "${variantLabel(posVariant.distributeUnusedBudget)}: $msg"
                                } else {
                                    msg
                                }
                            )
                        )
                    })
                }
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
                posVariants.forEach { posVariant ->
                    addRow(
                        table,
                        label(translate("$I18N_PREFIX.lostBudget"), posVariant.distributeUnusedBudget, showVariants),
                        posVariant.fcPosInfo.lostBudget,
                    )
                    addRow(
                        table,
                        label(translate("fibu.common.difference"), posVariant.distributeUnusedBudget, showVariants),
                        posVariant.fcPosInfo.difference,
                    )
                }
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
                if (showVariants) {
                    headRow.addTH(translate("$I18N_PREFIX.variants.column"))
                }
                val monthDates = posVariants.flatMap { it.fcPosInfo.months }.map { it.date }.distinct().sorted()
                monthDates.forEach { headRow.addTH(ForecastExport.formatMonthHeader(it)) }
                posVariants.forEach { posVariant ->
                    val row = table.addRow()
                    if (showVariants) {
                        row.addTH(variantLabel(posVariant.distributeUnusedBudget), CssClass.FIXED_WIDTH_NO_WRAP)
                    }
                    monthDates.forEach { date ->
                        val month = posVariant.fcPosInfo.months.find { it.date == date }
                        if (month != null) {
                            addForecastValue(row, month)
                        } else {
                            row.addTD() // Empty cell
                        }
                    }
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

    /**
     * One complete analysis of the order, calculated with a fixed value of
     * [ForecastOrderPosInfo.distributeUnusedBudget].
     */
    private class Variant(val distributeUnusedBudget: Boolean, val list: List<ForecastOrderPosInfo>) {
        val lostBudget: BigDecimal get() = list.sumOf { it.lostBudget }
        val lostBudgetWarning: Boolean get() = list.any { it.lostBudgetWarning }
    }

    private class PosVariant(val distributeUnusedBudget: Boolean, val fcPosInfo: ForecastOrderPosInfo)

    private fun variantLabel(distributeUnusedBudget: Boolean): String {
        return translate("$I18N_PREFIX.variants.$distributeUnusedBudget.label")
    }

    private fun variantHeader(variant: Variant): String {
        val label = variantLabel(variant.distributeUnusedBudget)
        return if (variant.distributeUnusedBudget == ForecastOrderPosInfo.defaultDistributeUnusedBudget) {
            "$label (${translate("$I18N_PREFIX.variants.configuredDefault")})"
        } else {
            label
        }
    }

    /**
     * @return The label, suffixed by the variant, if both [ForecastOrderPosInfo.distributeUnusedBudget] variants are
     * shown.
     */
    private fun label(label: String, variant: Variant, showVariants: Boolean): String {
        return label(label, variant.distributeUnusedBudget, showVariants)
    }

    private fun label(label: String, distributeUnusedBudget: Boolean, showVariants: Boolean): String {
        return if (showVariants) "$label (${variantLabel(distributeUnusedBudget)})" else label
    }

    /**
     * Signature of one position's forecast, used to detect whether both [distributeUnusedBudget] variants result in
     * the same analysis. In that case only one variant is shown.
     */
    private fun signature(fcPosInfo: ForecastOrderPosInfo): String {
        val months = fcPosInfo.months.joinToString(",") { "${it.date}=${it.toBeInvoicedSum}/${it.lostBudget}" }
        return "${fcPosInfo.orderPosNumber}:${fcPosInfo.difference}:$months"
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

    /**
     * @return The month (begin of month) of the latest actual invoice for the given position (respecting the
     * snapshot date), or null if nothing was invoiced yet.
     */
    private fun latestInvoiceMonth(posInfo: OrderPositionInfo, snapshotDate: LocalDate?): PFDay? {
        val latest = filterInvoices(posInfo, snapshotDate)?.mapNotNull { it.rechnungInfo?.date }?.maxOrNull()
        return latest?.let { PFDay.from(it).beginOfMonth }
    }

    companion object {
        private lateinit var instance: ForecastOrderAnalysis

        /** i18n prefix of all labels and texts of this analysis page. */
        private const val I18N_PREFIX = "fibu.auftrag.forecast.analysis"

        /**
         * Name of the configuration parameter, used as-is (not translated) in the explanation of the two variants,
         * so the user finds it in application.properties and in Forecast.kts.
         */
        private const val DISTRIBUTE_UNUSED_BUDGET = "projectforge.fibu.forecast.distributeUnusedBudget"

        fun createAnalysisAsHtml(
            orderId: Long? = null,
            orderNumber: Int? = null,
            snapshotDate: LocalDate? = null,
        ): String {
            return instance.htmlExport(orderId = orderId, orderNumber = orderNumber, snapshotDate)
        }
    }
}
