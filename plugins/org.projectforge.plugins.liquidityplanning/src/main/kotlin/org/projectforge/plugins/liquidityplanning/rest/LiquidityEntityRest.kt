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

package org.projectforge.plugins.liquidityplanning.rest

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDay
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.liquidityplanning.LiquidityEntriesStatistics
import org.projectforge.plugins.liquidityplanning.LiquidityEntryDO
import org.projectforge.plugins.liquidityplanning.LiquidityEntryDao
import org.projectforge.plugins.liquidityplanning.LiquidityForecastBuilder
import org.projectforge.plugins.liquidityplanning.LiquidityForecastCashFlow
import org.projectforge.plugins.liquidityplanning.LiquidityForecastSettings
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDOEntityRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The layout free list and edit page of the liquidity entries, modelled on the invoice pages
 * ([org.projectforge.rest.fibu.IncomingInvoiceEntityRest]). The list columns and the edit form come from
 * `liquidity.page.tsx` in projectforge-next; this class answers the list, its synthetic filters, its
 * statistics, the Excel export and the liquidity forecast that drives the forecast tab.
 *
 * The [LiquidityEntryDO] is a plain data object of five fields with no DTO in between, so the base is
 * [AbstractDOEntityRest] rather than the DTO variant the invoices use.
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/liquidity")
class LiquidityEntityRest :
    AbstractDOEntityRest<LiquidityEntryDO, LiquidityEntryDao>(
        LiquidityEntryDao::class.java,
        "plugins.liquidityplanning.entry.title",
    ) {

    @Autowired
    private lateinit var liquidityForecastBuilder: LiquidityForecastBuilder

    /**
     * The four filters the Wicket list (`LiquidityEntryListForm`) offers and no single property of
     * [LiquidityEntryDO] answers: the payment state, the amount type, the base date and the "next days"
     * window. All synthetic (see [preProcessMagicFilter]); the base date opens the window and is also read
     * by the statistics banner to tell whether the figures are of a past date.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement(
                PAYMENT_STATUS_FILTER,
                label = translate("fibu.rechnung.filter.paymentStatus"),
                multi = false,
                defaultFilter = true,
            ).also { element ->
                element.values = listOf(
                    UISelectValue(PAYMENT_STATUS_UNPAID, translate("fibu.rechnung.filter.unbezahlt")),
                    UISelectValue(PAYMENT_STATUS_PAID, translate("fibu.rechnung.status.bezahlt")),
                )
            },
        )
        elements.add(
            UIFilterListElement(
                AMOUNT_TYPE_FILTER,
                label = translate("plugins.liquidityplanning.entry.type"),
                multi = false,
                defaultFilter = true,
            ).also { element ->
                element.values = listOf(
                    UISelectValue(AMOUNT_TYPE_CREDIT, translate("plugins.liquidityplanning.common.credit")),
                    UISelectValue(AMOUNT_TYPE_DEBIT, translate("plugins.liquidityplanning.common.debit")),
                )
            },
        )
        elements.add(
            UIFilterElement(
                BASE_DATE_FILTER,
                UIFilterElement.FilterType.DATE,
                label = translate("plugins.liquidityplanning.forecast.baseDate"),
                defaultFilter = true,
            ),
        )
        elements.add(
            UIFilterListElement(
                NEXT_DAYS_FILTER,
                label = translate("plugins.liquidityplanning.forecast.nextDays"),
                multi = false,
                defaultFilter = true,
            ).also { element ->
                element.values = NEXT_DAYS_OPTIONS.map { days ->
                    UISelectValue(days.toString(), "$days")
                }
            },
        )
    }

    /**
     * Turns the synthetic filters into [CustomResultFilter]s, replicating `LiquidityEntryDao.select`. The
     * amount type filter is new: the legacy DAO declared it but never applied it (a known no-op), so this is
     * a behaviour fix.
     */
    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<LiquidityEntryDO>>? {
        val filters = mutableListOf<CustomResultFilter<LiquidityEntryDO>>()

        val paymentEntry = source.entries.find { it.field == PAYMENT_STATUS_FILTER }
        paymentEntry?.synthetic = true
        val paymentStatus = paymentEntry?.value?.values?.firstOrNull { it.isNotBlank() }
        if (paymentStatus != null) {
            filters.add(PaymentStatusFilter(paymentStatus))
        }

        val amountEntry = source.entries.find { it.field == AMOUNT_TYPE_FILTER }
        amountEntry?.synthetic = true
        val amountType = amountEntry?.value?.values?.firstOrNull { it.isNotBlank() }
        if (amountType != null) {
            filters.add(AmountTypeFilter(amountType))
        }

        val baseDateEntry = source.entries.find { it.field == BASE_DATE_FILTER }
        baseDateEntry?.synthetic = true
        val baseDate = parseBaseDate(baseDateEntry?.value?.value)

        val nextDaysEntry = source.entries.find { it.field == NEXT_DAYS_FILTER }
        nextDaysEntry?.synthetic = true
        val nextDays = nextDaysEntry?.value?.values?.firstOrNull { it.isNotBlank() }?.toIntOrNull() ?: 0
        if (nextDays > 0) {
            filters.add(NextDaysFilter(baseDate, nextDays))
        }

        return filters
    }

    /**
     * Adds the statistics of the whole (filtered) result, the ones the Wicket list shows above its table
     * (`LiquidityEntryListForm`). The liquidity list is not server-side paged, so the result set is the
     * whole result and its statistics are the whole result's.
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<LiquidityEntryDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val result = super.postProcessResultSet(resultSet, request, magicFilter)
        if (resultSet.offset == null) {
            val baseDate = parseBaseDate(magicFilter.entries.find { it.field == BASE_DATE_FILTER }?.value?.value)
            result.statistics = LiquidityStatistics(baseDao.buildStatistics(resultSet.resultSet), baseDate)
        }
        return result
    }

    /**
     * The sums the list shows above its table. Mirrors [LiquidityEntriesStatistics]; [pastBaseDate] lets the
     * banner turn red when the figures are of a base date in the past (the Wicket list's red statistics box).
     */
    class LiquidityStatistics(statistics: LiquidityEntriesStatistics, baseDate: LocalDate?) {
        val counter: Int = statistics.counter
        val counterPaid: Int = statistics.counterPaid
        val total: BigDecimal = statistics.total
        val paid: BigDecimal = statistics.paid
        val open: BigDecimal? = statistics.open
        val overdue: BigDecimal? = statistics.overdue
        val pastBaseDate: Boolean = baseDate?.isBefore(LocalDate.now()) == true
    }

    /**
     * The filtered list as the Excel file Wicket's "Excel export" produces. The rows come through the same
     * pipeline as the list itself ([getResultList]). An empty result answers 404 rather than a file.
     */
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting liquidity entries as Excel file.")
        val entries = getResultList(filter)
        if (entries.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        ExcelUtils.prepareWorkbook().use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("plugins.liquidityplanning.entry.title.heading"))
            val currencyStyle = workbook.createOrGetCellStyle("currency")
            currencyStyle.dataFormat = workbook.createDataFormat().getFormat(CURRENCY_FORMAT)
            ExcelUtils.registerColumn(sheet, LiquidityEntryDO::dateOfPayment)
            ExcelUtils.registerColumn(sheet, LiquidityEntryDO::amount, 14)
            ExcelUtils.registerColumn(sheet, LiquidityEntryDO::paid)
            ExcelUtils.registerColumn(sheet, LiquidityEntryDO::subject, 40)
            ExcelUtils.registerColumn(sheet, LiquidityEntryDO::comment, 40)
            ExcelUtils.addHeadRow(sheet)
            entries.forEach { entry ->
                val row = sheet.createRow()
                row.autoFillFromObject(entry)
                ExcelUtils.getCell(row, LiquidityEntryDO::amount)?.setCellStyle(currencyStyle)
            }
            sheet.setAutoFilter()
            val filename = "ProjectForge-${translate("plugins.liquidityplanning.entry.title.heading")}" +
                "_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
            return RestUtils.downloadFile(filename, workbook.asByteArrayOutputStream.toByteArray())
        }
    }

    /**
     * The per-day liquidity forecast that drives the forecast tab of `/next/liquidity`. Reuses
     * [LiquidityForecastBuilder] and [LiquidityForecastCashFlow] and the running-balance accumulation of the
     * Wicket charts (`LiquidityChartBuilder`), but without the paranoia-case series.
     *
     * Read only, so the select access of the category is what is checked here.
     */
    @PostMapping("forecast")
    fun getForecast(@RequestBody request: ForecastRequest): ForecastResult {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val baseDate = parseBaseDate(request.baseDate?.toString()) ?: LocalDate.now()
        val startAmount = request.startAmount ?: BigDecimal.ZERO
        val nextDays = (request.nextDays ?: LiquidityForecastSettings.DEFAULT_FORECAST_DAYS)
            .coerceIn(1, LiquidityForecastSettings.MAX_FORECAST_DAYS)

        // Remember the chosen parameters as a user preference, as the Wicket forecast page did, so the tab
        // reopens with the last-used values. The raw request values are stored (not the clamped/parsed ones)
        // so the user gets back exactly what they entered.
        saveSettings(request)

        val forecast = liquidityForecastBuilder.build(baseDate)
        val cashFlow = LiquidityForecastCashFlow(forecast, nextDays)
        var dueDateBalance = startAmount
        var expectedBalance = startAmount
        var current = PFDay.fromOrNow(forecast.baseDate)
        val points = ArrayList<ForecastDay>(nextDays)
        for (i in 0 until nextDays) {
            if (i > 0) {
                dueDateBalance = dueDateBalance
                    .add(cashFlow.debits[i - 1]).add(cashFlow.credits[i - 1])
                expectedBalance = expectedBalance
                    .add(cashFlow.debitsExpected[i - 1]).add(cashFlow.creditsExpected[i - 1])
            }
            points.add(
                ForecastDay(
                    date = current.localDate.toString(),
                    dueDateBalance = dueDateBalance,
                    expectedBalance = expectedBalance,
                    // Credits are negative amounts (money coming in), debits positive (money going out).
                    creditExpected = cashFlow.creditsExpected[i],
                    debitExpected = cashFlow.debitsExpected[i],
                ),
            )
            current = current.plusDays(1)
        }
        return ForecastResult(
            baseDate = baseDate.toString(),
            startAmount = startAmount,
            nextDays = nextDays,
            points = points,
        )
    }

    /**
     * The forecast parameters last used by the logged-in user, so the forecast tab can seed its controls
     * with them on open (the successor of the Wicket page's `LiquidityForecastSettings` user preference).
     * Returns the defaults if the user has never run a forecast.
     */
    @GetMapping("forecast/settings")
    fun getForecastSettings(): ForecastSettings {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        return userPrefService.ensureEntry(USER_PREF_AREA, USER_PREF_NAME, ForecastSettings())
    }

    /** Stores the chosen forecast parameters as the logged-in user's preference. */
    private fun saveSettings(request: ForecastRequest) {
        userPrefService.putEntry(
            USER_PREF_AREA,
            USER_PREF_NAME,
            ForecastSettings(
                startAmount = request.startAmount,
                baseDate = request.baseDate?.toString(),
                nextDays = request.nextDays,
            ),
        )
    }

    /** The parameters of the forecast, posted by the forecast tab. */
    class ForecastRequest(
        val startAmount: BigDecimal? = null,
        val baseDate: LocalDate? = null,
        val nextDays: Int? = null,
    )

    /**
     * The persisted forecast parameters. [baseDate] is kept as an ISO string (not a [LocalDate]) so a value
     * the user entered but that is not in the past — which the forecast itself ignores — still round-trips
     * back into the control unchanged.
     */
    class ForecastSettings(
        var startAmount: BigDecimal? = null,
        var baseDate: String? = null,
        var nextDays: Int? = null,
    )

    /** The forecast the tab renders: the echoed parameters and one [ForecastDay] per day. */
    class ForecastResult(
        val baseDate: String,
        val startAmount: BigDecimal,
        val nextDays: Int,
        val points: List<ForecastDay>,
    )

    /**
     * One day of the forecast. [dueDateBalance] is the running balance by actual due date (the black line of
     * the Wicket chart), [expectedBalance] the one by expected date of payment (the green/red difference
     * line); [creditExpected] (negative) and [debitExpected] (positive) are the day's expected cash flow, the
     * bars of the second Wicket chart.
     */
    class ForecastDay(
        val date: String,
        val dueDateBalance: BigDecimal,
        val expectedBalance: BigDecimal,
        val creditExpected: BigDecimal,
        val debitExpected: BigDecimal,
    )

    /** Keeps only paid or only unpaid entries — `LiquidityEntryListForm`'s payment-state radio. */
    private class PaymentStatusFilter(private val status: String) : CustomResultFilter<LiquidityEntryDO> {
        override fun match(list: MutableList<LiquidityEntryDO>, element: LiquidityEntryDO): Boolean {
            return when (status) {
                PAYMENT_STATUS_PAID -> element.paid
                PAYMENT_STATUS_UNPAID -> !element.paid
                else -> true
            }
        }
    }

    /**
     * Keeps only credits (amount < 0, money coming in) or only debits (amount > 0). The Wicket form offered
     * this radio but the DAO never applied it; implemented here for real.
     */
    private class AmountTypeFilter(private val type: String) : CustomResultFilter<LiquidityEntryDO> {
        override fun match(list: MutableList<LiquidityEntryDO>, element: LiquidityEntryDO): Boolean {
            val amount = element.amount ?: return false
            return when (type) {
                AMOUNT_TYPE_CREDIT -> amount.signum() < 0
                AMOUNT_TYPE_DEBIT -> amount.signum() > 0
                else -> true
            }
        }
    }

    /**
     * The "next days" window of `LiquidityEntryDao.select`: from the base date on, entries whose date of
     * payment lies within [nextDays]; before the base date only unpaid (overdue) entries are kept. An entry
     * without a date of payment is treated as of the base date (day 0), so it stays in the window.
     */
    private class NextDaysFilter(
        baseDate: LocalDate?,
        private val nextDays: Int,
    ) : CustomResultFilter<LiquidityEntryDO> {
        private val base = PFDay.fromOrNow(baseDate)

        override fun match(list: MutableList<LiquidityEntryDO>, element: LiquidityEntryDO): Boolean {
            val payment = element.dateOfPayment ?: return true
            if (payment.isBefore(base.localDate)) {
                // Past entries: keep only the unpaid (overdue) ones, drop what was already paid.
                return !element.paid
            }
            return base.daysBetween(payment) <= nextDays
        }
    }

    companion object {
        internal const val PAYMENT_STATUS_FILTER = "paymentStatus"
        private const val PAYMENT_STATUS_PAID = "paid"
        private const val PAYMENT_STATUS_UNPAID = "unpaid"

        internal const val AMOUNT_TYPE_FILTER = "amountType"
        private const val AMOUNT_TYPE_CREDIT = "credit"
        private const val AMOUNT_TYPE_DEBIT = "debit"

        internal const val BASE_DATE_FILTER = "baseDate"

        internal const val NEXT_DAYS_FILTER = "nextDays"
        private val NEXT_DAYS_OPTIONS = listOf(7, 10, 14, 30, 60, 90)

        private const val CURRENCY_FORMAT = "#,##0.00;[Red]-#,##0.00"

        /** The user-preference area and name under which the forecast tab's parameters are remembered. */
        private const val USER_PREF_AREA = "liquidityForecast"
        private const val USER_PREF_NAME = "settings"

        /** The base date is today unless a past date is given (`LiquidityFilter.baseDate`). */
        private fun parseBaseDate(value: String?): LocalDate? {
            val date = value?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            return date?.takeIf { it.isBefore(LocalDate.now()) }
        }
    }
}
