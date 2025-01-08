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

private val log = KotlinLogging.logger {}

/**
 * This class calculates the cash flows of an order item.
 * The actual invoices are used for the past and the uninvoiced and open expenses are used for the forecast.
 * These are either distributed according to the payment plan of the higher-level order or evenly distributed
 * over the performance period.
 */
class ForecastOrderPosInfo(
    @JsonIgnore
    val orderInfo: OrderInfo,
    val orderPosInfo: OrderPositionInfo,
    baseDate: PFDay = PFDay.now()
) {
    class MonthEntry(
        /** First day of month. */
        val date: PFDay
    ) {
        var toBeInvoicedSum: BigDecimal = BigDecimal.ZERO

        /** Mark this month as error. */
        var error: Boolean = false
    }

    class PaymentEntries(val scheduleDate: PFDay, val amount: BigDecimal)

    var baseMonth = baseDate.beginOfMonth
        private set
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
    lateinit var probabilityNetSum: BigDecimal
        private set
    lateinit var probabilityNetSumWithoutPaymentSchedule: BigDecimal
        private set
    val invoicedSum = orderPosInfo.invoicedSum
    lateinit var toBeInvoicedSum: BigDecimal
        private set
    private var futureInvoicesAmountRest = BigDecimal.ZERO
    var difference = BigDecimal.ZERO
        private set
    lateinit var paymentSchedules: List<OrderInfo.PaymentScheduleInfo>

    val months = mutableListOf<MonthEntry>()
    val paymentEntries = mutableListOf<PaymentEntries>()

    /**
     * @return true, if the given period is part of the performance period (does an overlap exist?).
     */
    fun match(startDate: PFDay, endDate: PFDay): Boolean {
        return periodOfPerformanceBegin.isBefore(endDate) && periodOfPerformanceEnd.isAfter(startDate)
    }

    fun calculate() {
        probability = ForecastUtils.getProbabilityOfAccurence(orderInfo, orderPosInfo)
        probabilityNetSum = ForecastUtils.computeProbabilityNetSum(orderInfo, orderPosInfo)
        toBeInvoicedSum = if (probabilityNetSum > invoicedSum) probabilityNetSum - invoicedSum else BigDecimal.ZERO
        paymentSchedules = ForecastUtils.getPaymentSchedule(orderInfo, orderPosInfo)
        createMonths()
        var distributionStartDay = periodOfPerformanceBegin
        var sumPaymentSchedule = BigDecimal.ZERO
        // handle payment schedule
        if (paymentSchedules.isNotEmpty()) {
            var sum = BigDecimal.ZERO
            distributionStartDay = PFDay.fromOrNow(paymentSchedules[0].scheduleDate)
            for (schedule in paymentSchedules) {
                if (schedule.vollstaendigFakturiert) { // Ignore payments already invoiced.
                    schedule.amount?.let { sum += it }
                    continue
                }
                val amount = schedule.amount!!.multiply(probability)
                sum += amount
                schedule.scheduleDate?.let { scheduleDate ->
                    if (distributionStartDay.isBefore(scheduleDate)) {
                        distributionStartDay = PFDay.from(scheduleDate)
                    }
                }
                paymentEntries.add(PaymentEntries(distributionStartDay, amount))
            }
            fillByPaymentSchedule()
            sumPaymentSchedule = sum
        }
        // compute diff, return if diff is empty
        probabilityNetSumWithoutPaymentSchedule = probabilityNetSum - sumPaymentSchedule
        if (probabilityNetSumWithoutPaymentSchedule.compareTo(BigDecimal.ZERO) != 0) {
            // handle diff
            when (orderPosInfo.paymentType) {
                AuftragsPositionsPaymentType.FESTPREISPAKET -> { // fill rest at end of project time
                    val month = months.last()
                    val value = if (probabilityNetSumWithoutPaymentSchedule > toBeInvoicedSum) {
                        toBeInvoicedSum
                    } else {
                        probabilityNetSumWithoutPaymentSchedule
                    }
                    month.toBeInvoicedSum = value
                }

                else -> {
                    fillMonthColumnsDistributed(distributionStartDay)
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

    private fun fillByPaymentSchedule() { // payment values
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

    /**
     * @param distributionStartDay The day from which the distribution should start. It is the begin of the
     *                             performance period or of last payment schedule date.
     */
    private fun fillMonthColumnsDistributed(
        distributionStartDay: PFDay,
    ) {
        val firstMonth = distributionStartDay.beginOfMonth
        val lastMonth = periodOfPerformanceEnd
        if (lastMonth < firstMonth) { // should not happen
            return
        }
        val monthCount = firstMonth.monthsBetween(lastMonth) + 1 // Jan-Jan -> 1, Jan-Feb -> 2, ...
        val partlyNettoSum = probabilityNetSumWithoutPaymentSchedule.divide(
            BigDecimal.valueOf(monthCount),
            RoundingMode.HALF_UP
        )
        futureInvoicesAmountRest = toBeInvoicedSum
        months.forEachIndexed { index, monthEntry ->
            val month = monthEntry.date
            if (month >= firstMonth) { // Start distribution
                if (month >= baseMonth) {
                    // Distribute payments only in future (after base month).
                    val value =
                        if (index == months.size - 1 && (partlyNettoSum > futureInvoicesAmountRest && partlyNettoSum > BigDecimal.ZERO)) {
                            // If month is the last month of performance period, the total rest of sum is to be invoiced.
                            futureInvoicesAmountRest
                        } else {
                            partlyNettoSum
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

    val orderPosString: String
        get() = "$orderNumber.$orderPosNumber"

    private fun createMonths() {
        var month = periodOfPerformanceBegin.beginOfMonth
        do {
            log.debug { "Adding month $month" }
            months.add(MonthEntry(month))
            month = month.plusMonths(1)
        } while (month <= periodOfPerformanceEnd)
        months.add(MonthEntry(month)) // Add one month after end of performance period (last invoice month).
    }

    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }
}
