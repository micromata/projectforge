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

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.time.PFDay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * This class calculates the cash flows of an order item.
 * The actual invoices are used for the past and the uninvoiced and open expenses are used for the forecast.
 * These are either distributed according to the payment plan of the higher-level order or evenly distributed
 * over the performance period.
 * @param orderInfo The order.
 * @param orderPosInfo The order position.
 * @param baseDate The base date for the forecast.
 */
class ForecastOrderPosInfo(
    @JsonIgnore
    val orderInfo: OrderInfo,
    val orderPosInfo: OrderPositionInfo,
) {
    class MonthEntry(
        /** First day of month. */
        val date: PFDay
    ) {
        var toBeInvoicedSum: BigDecimal = BigDecimal.ZERO

        /**
         * If already invoiced, the sum of all invoices for this month.
         */
        var invoicedSum: BigDecimal = BigDecimal.ZERO

        var lostBudget: BigDecimal = BigDecimal.ZERO

        /** Mark this month as error. */
        var lostBudgetWarning: Boolean = false

        var lostBudgetPercent = 0
    }

    class PaymentEntryInfo(val scheduleDate: LocalDate, val amount: BigDecimal)

    /**
     * Snapshot date of the order or beginning of the current month.
     */
    val baseMonth = PFDay.fromOrNow(orderInfo.snapshotDate).beginOfMonth
    var orderNumber = orderInfo.nummer
        private set
    var orderPosNumber = orderPosInfo.number
        private set
    var periodOfPerformanceBegin = ForecastUtils.getStartLeistungszeitraum(orderInfo, orderPosInfo)
        private set
    var periodOfPerformanceEnd = ForecastUtils.getEndLeistungszeitraum(orderInfo, orderPosInfo)
        private set
    lateinit var probability: BigDecimal
        private set
    lateinit var weightedNetSum: BigDecimal
        private set
    lateinit var weightedNetSumWithoutPaymentSchedule: BigDecimal
        private set
    val invoicedSum = orderPosInfo.invoicedSum
    lateinit var toBeInvoicedSum: BigDecimal
        private set
    private var futureInvoicesAmountRest = BigDecimal.ZERO
    var difference = BigDecimal.ZERO
        private set
    lateinit var paymentSchedules: List<OrderInfo.PaymentScheduleInfo>
    private var distributionStartDay = periodOfPerformanceBegin

    /**
     * The month of the latest actual invoice for this order position (already filtered by snapshot/base date by the
     * caller), or null if nothing was invoiced yet. Must be set before [calculate] is called.
     *
     * This is used by [distributeMonthlyValues] to decide where the forecast distribution starts: months that are
     * already covered by an actual invoice must not be forecast again. For retroactive invoicing (FOLLOWING_MONTH)
     * this is what distinguishes "the current month's revenue is already invoiced" (start next month) from
     * "the current month is not invoiced yet" (include the current month in the distribution).
     */
    var lastInvoiceMonth: PFDay? = null

    /**
     * If true (default), unused/undistributed budget of the order position is booked as future revenue in the last
     * month of the performance period (may overestimate upcoming revenue). If false, this budget is not part of the
     * forecast and is shown as a negative difference sum (more conservative). Must be set before [calculate] is called.
     *
     * This is passed through from the forecast export (e.g. as a script parameter of Forecast.kts), so the behavior
     * can be chosen per export run. If not set, it falls back to the globally configured
     * [defaultDistributeUnusedBudget] (application.properties).
     */
    var distributeUnusedBudget: Boolean = defaultDistributeUnusedBudget

    val months = mutableListOf<MonthEntry>()
    val paymentEntries = mutableListOf<PaymentEntryInfo>()

    val lostBudget: BigDecimal
        get() = months.sumOf { it.lostBudget }

    val lostBudgetWarning: Boolean
        get() = months.any { it.lostBudgetWarning }

    /**
     * @return true, if the given period is part of the performance period (does an overlap exist?).
     */
    fun match(startDate: PFDay, endDate: PFDay): Boolean {
        return periodOfPerformanceBegin.isBefore(endDate) && periodOfPerformanceEnd.isAfter(startDate)
    }

    fun calculate() {
        probability = ForecastUtils.getProbabilityOfAccurence(orderInfo, orderPosInfo)
        weightedNetSum = ForecastUtils.computeProbabilityNetSum(orderInfo, orderPosInfo)
        toBeInvoicedSum = if (weightedNetSum > invoicedSum) weightedNetSum - invoicedSum else BigDecimal.ZERO
        paymentSchedules = ForecastUtils.getPaymentSchedule(orderInfo, orderPosInfo)
        createMonths()
        val sumPaymentSchedule = ForecastUtils.computeProbabilityPaymentSchedule(orderInfo, orderPosInfo)
        // handle payment schedule
        handlePaymentSchedules()
        // compute diff, return if diff is empty
        weightedNetSumWithoutPaymentSchedule = weightedNetSum - sumPaymentSchedule
        if (weightedNetSumWithoutPaymentSchedule.compareTo(BigDecimal.ZERO) != 0) {
            // handle diff
            when (orderPosInfo.paymentType) {
                AuftragsPositionsPaymentType.FESTPREISPAKET -> { // fill rest at end of project time
                    val month = months.last()
                    val value = if (weightedNetSumWithoutPaymentSchedule > toBeInvoicedSum) {
                        toBeInvoicedSum
                    } else {
                        weightedNetSumWithoutPaymentSchedule
                    }
                    if (value.abs() > BigDecimal.ONE) { // Ignore rounding errors.
                        month.toBeInvoicedSum += value
                    }
                }

                else -> {
                    distributeMonthlyValues(distributionStartDay)
                }
            }
        }
    }

    private fun setMonthValue(day: PFDay, value: BigDecimal) {
        val month = months.find { it.date == day.beginOfMonth }
        if (month == null) {
            log.error { "Oups, can't find month $day of order position $orderPosString: $this" }
        } else {
            month.toBeInvoicedSum = value
        }
    }

    private fun handlePaymentSchedules() { // payment values
        if (paymentSchedules.isEmpty()) {
            // Nothing to do.
            return
        }
        val firstScheduledDate = paymentSchedules.minOf { it.scheduleDate ?: LocalDate.MAX }
        distributionStartDay = PFDay.fromOrNull(firstScheduledDate) ?: distributionStartDay
        for (schedule in paymentSchedules) {
            val amount = schedule.amount
            val scheduleDate = schedule.scheduleDate
            if (scheduleDate == null || amount == null || schedule.vollstaendigFakturiert) { // Ignore payments already invoiced.
                continue
            }
            if (distributionStartDay.isBefore(scheduleDate)) {
                distributionStartDay = PFDay.from(scheduleDate)
            }
            // For info only (e.g. in Excel export):
            paymentEntries.add(PaymentEntryInfo(scheduleDate, amount.multiply(probability)))
        }
        months.forEach { current ->
            val currentMonth = current.date
            if (isPartOfForecast(currentMonth)) {
                var sum = BigDecimal.ZERO
                for (schedule in paymentSchedules) {
                    if (schedule.vollstaendigFakturiert) {
                        continue
                    }
                    val date = PFDay.fromOrNull(schedule.scheduleDate)
                    if (date != null && date.year == currentMonth.year && date.month == currentMonth.month) {
                        // Payment date matches current month: so add it.
                        sum += schedule.amount!!.multiply(probability).setScale(2, RoundingMode.HALF_UP)
                    }
                }
                if (sum != BigDecimal.ZERO) {
                    current.toBeInvoicedSum = sum
                }
            }
        }
    }

    fun getRemainingForecastSumAfter(date: PFDay): BigDecimal {
        var sum = BigDecimal.ZERO
        months.forEach {
            if (it.date > date) {
                sum += it.toBeInvoicedSum
            }
        }
        return sum
    }

    /**
     * @param distributionStartDay The day from which the distribution should start. It is the begin of the
     *                             performance period or of last payment schedule date.
     */
    private fun distributeMonthlyValues(
        distributionStartDay: PFDay,
    ) {
        val firstMonth =
            if (ForecastUtils.getForecastType(orderInfo, orderPosInfo) == AuftragForecastType.CURRENT_MONTH) {
                // Start distribution in the current month:
                distributionStartDay.beginOfMonth
            } else {
                // Start distribution in the following month:
                distributionStartDay.beginOfMonth.plusMonths(1)
            }
        val lastMonth = periodOfPerformanceEnd
        if (lastMonth < firstMonth) { // should not happen
            return
        }
        // Never forecast a month that is already covered by an actual invoice: distribution starts after the latest
        // invoiced month. For retroactive invoicing (FOLLOWING_MONTH) this is the key: the current month is only
        // "already handled" once its invoice has actually been written. If it hasn't been written yet, the current
        // month must remain part of the forecast (otherwise its revenue would be dropped and the remaining budget
        // spread over too few months). See orders 6809 / 6863.
        val firstNotInvoicedMonth = lastInvoiceMonth?.beginOfMonth?.plusMonths(1)
        // Distribution must not start before the current month (baseMonth) nor before firstMonth (forecast-type
        // dependent begin), and must skip any month already invoiced.
        val effectiveStart = maxOf(firstMonth, baseMonth, firstNotInvoicedMonth ?: baseMonth)
        val remainingMonthCount = months.count { it.date >= effectiveStart }.toLong()
        val partlyNetSum = if (remainingMonthCount > 0) {
            toBeInvoicedSum.divide(BigDecimal.valueOf(remainingMonthCount), RoundingMode.HALF_UP)
        } else {
            toBeInvoicedSum
        }
        // Determine the per-month forecast rate and whether the last month absorbs the remaining budget:
        // - PAUSCHALE: a fixed monthly rate over the whole performance period. Under-invoicing does NOT raise the
        //   future rate; the not-reached budget shows up as a negative difference (with warning). Independent of the
        //   distributeUnusedBudget switch.
        // - TIME_AND_MATERIALS with distributeUnusedBudget = false: extrapolate the historical call-off run rate
        //   (invoiced so far / elapsed performance months). Not-called-off budget is NOT assumed as future revenue
        //   but shown as a negative difference (with warning). This avoids forecasting the whole unused budget into
        //   the last month(s) of a call-off order. See order 120k call-off example.
        // - TIME_AND_MATERIALS with distributeUnusedBudget = true (default): even distribution of the remaining
        //   budget over the remaining months, the last month absorbing the rest (previous behavior).
        val paymentType = orderPosInfo.paymentType
        val useRunRate = paymentType == AuftragsPositionsPaymentType.PAUSCHALE ||
                (paymentType == AuftragsPositionsPaymentType.TIME_AND_MATERIALS && !distributeUnusedBudget)
        val monthlyRate: BigDecimal = when {
            paymentType == AuftragsPositionsPaymentType.PAUSCHALE -> {
                // Fixed pauschale rate = weighted net sum / total performance months.
                val totalMonths = ForecastUtils.getMonthCountForOrderPosition(orderInfo, orderPosInfo)
                if (totalMonths != null && totalMonths > BigDecimal.ZERO) {
                    weightedNetSum.divide(totalMonths, 2, RoundingMode.HALF_UP)
                } else {
                    partlyNetSum
                }
            }

            paymentType == AuftragsPositionsPaymentType.TIME_AND_MATERIALS && !distributeUnusedBudget -> {
                // Historical run rate = invoiced so far / elapsed performance months (before distribution start).
                val elapsedMonths = periodOfPerformanceBegin.beginOfMonth.monthsBetween(effectiveStart)
                if (elapsedMonths > 0 && invoicedSum > BigDecimal.ZERO) {
                    invoicedSum.divide(BigDecimal.valueOf(elapsedMonths), 2, RoundingMode.HALF_UP)
                } else {
                    // No invoicing history yet: fall back to even distribution of the remaining budget.
                    partlyNetSum
                }
            }

            else -> partlyNetSum
        }
        futureInvoicesAmountRest = toBeInvoicedSum
        var lastAssignedMonth: MonthEntry? = null
        months.forEachIndexed { index, monthEntry ->
            val month = monthEntry.date
            if (month >= firstMonth) { // Start distribution not before firstMonth.
                if (month >= effectiveStart) {
                    // Distribute payments only in future.
                    var value = if (useRunRate) monthlyRate else partlyNetSum
                    if (useRunRate) {
                        // Never forecast more than remains to be invoiced (cap to budget, no over-estimation).
                        value = minOf(value, futureInvoicesAmountRest)
                    } else if (index == months.size - 1) {
                        // Even distribution: the last month of the performance period absorbs the total rest.
                        if (distributeUnusedBudget) {
                            // Unused budget is added to the last month (may overestimate):
                            value = futureInvoicesAmountRest
                        } else {
                            value = minOf(partlyNetSum, futureInvoicesAmountRest)
                        }
                        if (futureInvoicesAmountRest > partlyNetSum) {
                            setLostBudget(monthEntry, futureInvoicesAmountRest - partlyNetSum)
                        }
                    }
                    if (value.abs() > BigDecimal.ONE) { // values < 0 are possible for Abrufaufträge (Sarah fragen, 4273)
                        setMonthValue(month, value)
                        lastAssignedMonth = monthEntry
                    }
                    futureInvoicesAmountRest -= value // Don't forecast more than to be invoiced.
                }
            }
        }
        // For run-rate distribution, budget that won't be reached at the current rate is a shortfall: report it as a
        // negative difference and flag a warning on the last forecast month (under-run of a call-off / pauschale).
        if (useRunRate && futureInvoicesAmountRest > BigDecimal.ONE) {
            (lastAssignedMonth ?: months.lastOrNull { it.date >= effectiveStart })?.let { monthEntry ->
                setLostBudget(monthEntry, futureInvoicesAmountRest)
            }
        }
        // Calculate the difference between to be invoiced sum and forecasted sums:
        if (futureInvoicesAmountRest.abs() <= BigDecimal.ONE) { // Only differences greater than 1 Euro
            futureInvoicesAmountRest = BigDecimal.ZERO
        }
        difference = futureInvoicesAmountRest.negate()
    }

    /**
     * Marks the given month with lost/under-run budget and raises a warning if it exceeds
     * [PERCENTAGE_OF_LOST_BUDGET_WARNING] percent of the weighted net sum.
     */
    private fun setLostBudget(monthEntry: MonthEntry, lostBudget: BigDecimal) {
        monthEntry.lostBudget = lostBudget
        monthEntry.lostBudgetPercent = if (weightedNetSum > BigDecimal.ZERO) {
            (lostBudget * BigDecimal(100)).divide(weightedNetSum, RoundingMode.HALF_UP).toInt()
        } else {
            0
        }
        if (monthEntry.lostBudgetPercent >= PERCENTAGE_OF_LOST_BUDGET_WARNING) {
            monthEntry.lostBudgetWarning = true
        }
    }

    /**
     * @return true, if the given date is used in forecast, means it not before the base month.
     */
    private fun isPartOfForecast(date: PFDay): Boolean {
        return !date.isBefore(baseMonth)
    }

    /**
     * @return "orderNumber.orderPosNumber", e.g. 123.1
     */
    val orderPosString: String
        get() = "$orderNumber.$orderPosNumber"

    private fun createMonths() {
        var month = periodOfPerformanceBegin.beginOfMonth
        var monthUntil = periodOfPerformanceEnd.beginOfMonth
        if (ForecastUtils.getForecastType(orderInfo, orderPosInfo) != AuftragForecastType.CURRENT_MONTH) {
            // Add one more month, because the forecast revenue is in the following month, so one month after end of performance period.
            monthUntil = monthUntil.plusMonths(1)
        }
        val lastScheduleDate = PFDay.fromOrNull(paymentSchedules.maxOfOrNull { it.scheduleDate ?: LocalDate.MIN })
        if (lastScheduleDate != null && lastScheduleDate > monthUntil) {
            monthUntil = lastScheduleDate
        }
        var paranoidCounter = 120 // Max 10 years as paranoia counter for avoiding endless loops.
        do {
            log.debug { "Adding month $month" }
            months.add(MonthEntry(month))
            month = month.plusMonths(1)
        } while (month <= monthUntil && paranoidCounter-- > 0)
    }

    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }

    companion object {
        const val PERCENTAGE_OF_LOST_BUDGET_WARNING = 10

        /**
         * Global default for [distributeUnusedBudget], configurable via application.properties
         * (`projectforge.fibu.forecast.distributeUnusedBudget`), set by [ForecastExport] on startup.
         * If true, unused budget will be added to the last distributed month.
         * If false, this budget will be added to the difference sum.
         *
         * The forecast Excel export (Forecast.kts) may override this per run via a script parameter; single-order
         * analyses ([ForecastOrderAnalysis]) use this configured default.
         */
        var defaultDistributeUnusedBudget = true
    }
}
