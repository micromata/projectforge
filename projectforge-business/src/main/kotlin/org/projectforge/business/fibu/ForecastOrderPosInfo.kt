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
        val firstMonth = distributionStartDay.beginOfMonth
        val lastMonth = periodOfPerformanceEnd
        if (lastMonth < firstMonth) { // should not happen
            return
        }
        val monthCount = firstMonth.monthsBetween(lastMonth) + 1 // Jan-Jan -> 1, Jan-Feb -> 2, ...
        val partlyNetSum = weightedNetSumWithoutPaymentSchedule.divide(
            BigDecimal.valueOf(monthCount),
            RoundingMode.HALF_UP
        )
        futureInvoicesAmountRest = toBeInvoicedSum
        months.forEachIndexed { index, monthEntry ->
            val month = monthEntry.date
            if (month > firstMonth) { // Start distribution one month after firstMonth (invoice one month later)
                if (month >= baseMonth) {
                    // Distribute payments only in future (after base month).
                    var value = partlyNetSum
                    if (index == months.size - 1) {
                        // If month is the last month of performance period, the total rest of sum is to be invoiced.
                        if (DISTRIBUTE_UNUSED_BUDGET) {
                            // Version 1 (unused budget will be added to last month (overestimation)):
                            value = futureInvoicesAmountRest
                        } else {
                            // Version 2 (unused budget isn't part of forecast and will be shown as negative difference sum (more realistic scenario?):
                            value = minOf(partlyNetSum, futureInvoicesAmountRest)
                        }
                        if (futureInvoicesAmountRest > partlyNetSum) {
                            monthEntry.lostBudget = futureInvoicesAmountRest - partlyNetSum
                            monthEntry.lostBudgetPercent =
                                if (weightedNetSum > BigDecimal.ZERO) {
                                    (monthEntry.lostBudget * BigDecimal(100)).divide(
                                        weightedNetSum,
                                        RoundingMode.HALF_UP
                                    ).toInt()
                                } else {
                                    0
                                }
                            if (monthEntry.lostBudgetPercent >= PERCENTAGE_OF_LOST_BUDGET_WARNING) {
                                monthEntry.lostBudgetWarning = true
                            }
                        }
                    }
                    if (value.abs() > BigDecimal.ONE) { // values < 0 are possible for Abrufaufträge (Sarah fragen, 4273)
                        setMonthValue(month, value)
                    }
                    futureInvoicesAmountRest -= value // Don't forecast more than to be invoiced.
                }
            }
        }
        // Calculate the difference between to be invoiced sum and forecasted sums:
        if (futureInvoicesAmountRest.abs() <= BigDecimal.ONE) { // Only differences greater than 1 Euro
            futureInvoicesAmountRest = BigDecimal.ZERO
        }
        difference = futureInvoicesAmountRest.negate()
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
        var monthUntil =
            periodOfPerformanceEnd.beginOfMonth.plusMonths(1) // Add one month after end of performance period.
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
         * If true, unused budget will be added to the last distributed month.
         * If false, this budget will be added to the difference sum.
         */
        internal val DISTRIBUTE_UNUSED_BUDGET = true
    }
}
