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

import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Forcast excel export.
 *
 * @author Florian Blumenstein
 */
object ForecastUtils { // open needed by Wicket.

    @JvmStatic
    val auftragsPositionsStatusToShow = listOf(
        //AuftragsStatus.ABGELEHNT,
        //AuftragsStatus.ABGESCHLOSSEN,
        AuftragsStatus.BEAUFTRAGT,
        //AuftragsStatus.ERSETZT,
        //AuftragsStatus.ESKALATION,
        AuftragsStatus.GELEGT,
        AuftragsStatus.IN_ERSTELLUNG,
        AuftragsStatus.LOI,
        //AuftragsStatus.OPTIONAL,
        AuftragsStatus.POTENZIAL
    )

    @JvmStatic
    val auftragsStatusToShow = listOf(
        // AuftragsStatus.ABGELEHNT,
        AuftragsStatus.ABGESCHLOSSEN,
        AuftragsStatus.BEAUFTRAGT,
        // AuftragsStatus.ERSETZT,
        AuftragsStatus.ESKALATION,
        AuftragsStatus.GELEGT,
        AuftragsStatus.IN_ERSTELLUNG,
        AuftragsStatus.LOI,
        AuftragsStatus.POTENZIAL
    )

    @JvmStatic
    fun getPaymentSchedule(order: OrderInfo, pos: OrderPositionInfo): List<OrderInfo.PaymentScheduleInfo> {
        val schedules = order.paymentScheduleEntries ?: return emptyList()
        return schedules
            .filter { it.positionNumber != null && it.scheduleDate != null && it.amount != null }
            .filter { it.positionNumber?.toInt() == pos.number?.toInt() }
    }

    /**
     * Multiplies the probability with the net sum.
     */
    @JvmStatic
    fun computeProbabilityNetSum(order: OrderInfo, pos: OrderPositionInfo): BigDecimal {
        val netSum = pos.netSum ?: BigDecimal.ZERO
        val probability = getProbabilityOfAccurence(order, pos)
        return netSum.multiply(probability)
    }

    /**
     * Multiplies the probability with the amounts of all payment schedule amounts. If a payment schedule
     * is already fully invoiced (vollstaendigFakturiert), the amount is not multiplied with the probability.
     */
    @JvmStatic
    fun computeProbabilityPaymentSchedule(order: OrderInfo, pos: OrderPositionInfo): BigDecimal {
        var sum = BigDecimal.ZERO
        val probability = getProbabilityOfAccurence(order, pos)
        order.getPaymentScheduleEntriesOfPosition(pos)?.forEach { scheduleInfo ->
            val amount = scheduleInfo.amount ?: return@forEach
            sum += if (scheduleInfo.vollstaendigFakturiert) {
                amount
            } else {
                amount.multiply(probability)
            }
        }
        return sum
    }

    /**
     * See doc/misc/ForecastExportProbabilities.xlsx
     */
    @JvmStatic
    fun getProbabilityOfAccurence(order: OrderInfo, pos: OrderPositionInfo): BigDecimal {
        // See ForecastExportProbabilities.xlsx
        // Excel rows: Order 1-4
        if (order.status.isIn(AuftragsStatus.ABGELEHNT, AuftragsStatus.ERSETZT) == true
            || pos.status.isIn(AuftragsStatus.ABGELEHNT, AuftragsStatus.ERSETZT) == true
        ) {
            return BigDecimal.ZERO
        }
        // Excel rows: Order 5-6
        if (pos.status.isIn(AuftragsStatus.POTENZIAL, AuftragsStatus.OPTIONAL) == true) {
            return getGivenProbability(order, BigDecimal.ZERO)
        }
        // Excel rows: Order 7
        if (pos.status == AuftragsStatus.BEAUFTRAGT) {
            return BigDecimal.ONE
        }
        // Excel rows: Order 8
        if (order.status == AuftragsStatus.POTENZIAL) {
            return getGivenProbability(order, BigDecimal.ZERO)
        }
        // Excel rows: Order 9-10
        if (order.status.isIn(AuftragsStatus.ABGESCHLOSSEN, AuftragsStatus.BEAUFTRAGT) == true) {
            return BigDecimal.ONE
        }
        // Excel rows: Order 11-12
        if (order.status.isIn(
                AuftragsStatus.ESKALATION,
                AuftragsStatus.GELEGT,
                AuftragsStatus.IN_ERSTELLUNG
            ) == true
        ) {
            if (pos.status.isIn(
                    AuftragsStatus.ESKALATION,
                    AuftragsStatus.GELEGT,
                    AuftragsStatus.IN_ERSTELLUNG
                ) == true
            ) {
                // Excel rows: Order 11
                return getGivenProbability(order, POINT_FIVE)
            } else if (pos.status == AuftragsStatus.LOI) {
                // Excel rows: Order 12
                return getGivenProbability(order, POINT_NINE)
            }
        }
        // Excel rows: Order 13
        if (order.status == AuftragsStatus.LOI
            && pos.status.isIn(
                AuftragsStatus.ESKALATION,
                AuftragsStatus.GELEGT,
                AuftragsStatus.IN_ERSTELLUNG
            ) == true
        ) {
            return getGivenProbability(order, POINT_NINE)
        }
        // Excel rows: Order 14
        return getGivenProbability(order, BigDecimal.ZERO)
    }

    @JvmStatic
    fun getGivenProbability(order: OrderInfo, defaultValue: BigDecimal): BigDecimal {
        val propability = order.probabilityOfOccurrence ?: return defaultValue
        return BigDecimal(propability).divide(NumberHelper.HUNDRED, 2, RoundingMode.HALF_UP)
    }

    @JvmStatic
    fun getStartLeistungszeitraum(order: OrderInfo, pos: OrderPositionInfo): PFDay {
        return getLeistungszeitraumDate(pos, order.periodOfPerformanceBegin, pos.periodOfPerformanceBegin)
    }

    @JvmStatic
    fun getEndLeistungszeitraum(order: OrderInfo, pos: OrderPositionInfo): PFDay {
        return getLeistungszeitraumDate(pos, order.periodOfPerformanceEnd, pos.periodOfPerformanceEnd)
    }

    internal fun getLeistungszeitraumDate(
        periodOfPerformanceType: PeriodOfPerformanceType?,
        orderDate: LocalDate?,
        posDate: LocalDate?
    ): PFDay {
        var result = PFDay.now()
        if (PeriodOfPerformanceType.OWN == periodOfPerformanceType) {
            if (posDate != null) {
                result = PFDay.from(posDate) // not null
            }
        } else {
            if (orderDate != null) {
                result = PFDay.from(orderDate) // not null
            }
        }
        return result
    }

    private fun getLeistungszeitraumDate(
        pos: OrderPositionInfo,
        orderDate: LocalDate?,
        posDate: LocalDate?
    ): PFDay {
        return getLeistungszeitraumDate(pos.periodOfPerformanceType, orderDate, posDate)
    }

    @JvmStatic
    fun getMonthCountForOrderPosition(order: OrderInfo?, pos: OrderPositionInfo): BigDecimal? {
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) {
            if (pos.periodOfPerformanceEnd != null && pos.periodOfPerformanceBegin != null) {
                return getMonthCount(pos.periodOfPerformanceBegin, pos.periodOfPerformanceEnd)
            }
        } else {
            val periodOfPerformanceBegin = order?.periodOfPerformanceBegin
            val periodOfPerformanceEnd = order?.periodOfPerformanceEnd
            if (periodOfPerformanceEnd != null && periodOfPerformanceBegin != null) {
                return getMonthCount(periodOfPerformanceBegin, periodOfPerformanceEnd)
            }
        }
        return null
    }

    @JvmStatic
    fun getMonthCount(start: LocalDate?, end: LocalDate?): BigDecimal {
        start ?: return BigDecimal.ZERO
        end ?: return BigDecimal.ZERO
        val startDate = PFDay.from(start) // not null
        val endDate = PFDay.from(end) // not null
        val diffYear = endDate.year - startDate.year
        val diffMonth = diffYear * 12 + endDate.monthValue - startDate.monthValue + 1
        return BigDecimal.valueOf(diffMonth.toLong())
    }

    @JvmStatic
    fun getInvoices(invoicePositions: Collection<RechnungPosInfo>?): String {
        return invoicePositions?.sortedByDescending { it.number }
            ?.joinToString(", ") { it.rechnungInfo?.nummer?.toString() ?: "" } ?: ""
    }

    @JvmStatic
    fun ensureErfassungsDatum(order: OrderInfo): LocalDate {
        order.erfassungsDatum?.let {
            return it
        }
        order.created?.let { created ->
            return PFDay.from(created).localDate
        }
        order.angebotsDatum?.let {
            return it
        }
        return PFDay.now().localDate
    }

    private val POINT_FIVE = BigDecimal(".5")
    private val POINT_NINE = BigDecimal(".9")
}
