/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.excel.PropertyMapping
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
            //AuftragsPositionsStatus.ABGELEHNT,
            //AuftragsPositionsStatus.ABGESCHLOSSEN,
            AuftragsPositionsStatus.BEAUFTRAGT,
            //AuftragsPositionsStatus.ERSETZT,
            //AuftragsPositionsStatus.ESKALATION,
            AuftragsPositionsStatus.GELEGT,
            AuftragsPositionsStatus.IN_ERSTELLUNG,
            AuftragsPositionsStatus.LOI,
            //AuftragsPositionsStatus.OPTIONAL,
            AuftragsPositionsStatus.POTENZIAL)

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
            AuftragsStatus.POTENZIAL)

    @JvmStatic
    fun getPaymentSchedule(order: AuftragDO, pos: AuftragsPositionDO): List<PaymentScheduleDO> {
        val schedules = order.paymentSchedules ?: return emptyList()
        return schedules
                .filter { it.positionNumber != null && it.scheduleDate != null && it.amount != null }
                .filter { it.positionNumber!!.toInt() == pos.number.toInt() }
    }

    /**
     * Multiplies the probability with the net sum.
     */
    @JvmStatic
    fun computeProbabilityNetSum(order: AuftragDO, pos: AuftragsPositionDO): BigDecimal {
        val netSum = if (pos.nettoSumme != null) pos.nettoSumme!! else BigDecimal.ZERO
        val probability = getProbabilityOfAccurence(order, pos)
        return netSum.multiply(probability)
    }

    /**
     * See doc/misc/ForecastExportProbabilities.xlsx
     */
    @JvmStatic
    fun getProbabilityOfAccurence(order: AuftragDO, pos: AuftragsPositionDO): BigDecimal {
        // See ForecastExportProbabilities.xlsx
        // Excel rows: Order 1-4
        if (order.auftragsStatus?.isIn(AuftragsStatus.ABGELEHNT, AuftragsStatus.ERSETZT) == true
                || pos.status?.isIn(AuftragsPositionsStatus.ABGELEHNT, AuftragsPositionsStatus.ERSETZT) == true) {
            return BigDecimal.ZERO
        }
        // Excel rows: Order 5-6
        if (pos.status?.isIn(AuftragsPositionsStatus.POTENZIAL, AuftragsPositionsStatus.OPTIONAL) == true) {
            return getGivenProbability(order, BigDecimal.ZERO)
        }
        // Excel rows: Order 7
        if (pos.status == AuftragsPositionsStatus.BEAUFTRAGT) {
            return BigDecimal.ONE
        }
        // Excel rows: Order 8
        if (order.auftragsStatus == AuftragsStatus.POTENZIAL) {
            return getGivenProbability(order, BigDecimal.ZERO)
        }
        // Excel rows: Order 9-10
        if (order.auftragsStatus?.isIn(AuftragsStatus.ABGESCHLOSSEN, AuftragsStatus.BEAUFTRAGT) == true) {
            return BigDecimal.ONE
        }
        // Excel rows: Order 11-12
        if (order.auftragsStatus?.isIn(AuftragsStatus.ESKALATION, AuftragsStatus.GELEGT, AuftragsStatus.IN_ERSTELLUNG) == true) {
            if (pos.status?.isIn(AuftragsPositionsStatus.ESKALATION, AuftragsPositionsStatus.GELEGT, AuftragsPositionsStatus.IN_ERSTELLUNG) == true) {
                // Excel rows: Order 11
                return getGivenProbability(order, POINT_FIVE)
            } else if (pos.status == AuftragsPositionsStatus.LOI) {
                // Excel rows: Order 12
                return getGivenProbability(order, POINT_NINE)
            }
        }
        // Excel rows: Order 13
        if (order.auftragsStatus == AuftragsStatus.LOI
                && pos.status?.isIn(AuftragsPositionsStatus.ESKALATION, AuftragsPositionsStatus.GELEGT, AuftragsPositionsStatus.IN_ERSTELLUNG) == true) {
            return getGivenProbability(order, POINT_NINE)
        }
        // Excel rows: Order 14
        return getGivenProbability(order, BigDecimal.ZERO)
    }

    @JvmStatic
    fun getGivenProbability(order: AuftragDO, defaultValue: BigDecimal): BigDecimal {
        val propability = order.probabilityOfOccurrence ?: return defaultValue
        return BigDecimal(propability).divide(NumberHelper.HUNDRED, 2, RoundingMode.HALF_UP)
    }

    @JvmStatic
    fun getStartLeistungszeitraum(order: AuftragDO, pos: AuftragsPositionDO): PFDay {
        return getLeistungszeitraumDate(pos, order.periodOfPerformanceBegin, pos.periodOfPerformanceBegin)
    }

    @JvmStatic
    fun getEndLeistungszeitraum(order: AuftragDO, pos: AuftragsPositionDO): PFDay {
        return getLeistungszeitraumDate(pos, order.periodOfPerformanceEnd, pos.periodOfPerformanceEnd)
    }

    private fun getLeistungszeitraumDate(pos: AuftragsPositionDO, orderDate: LocalDate?, posDate: LocalDate?): PFDay {
        var result = PFDay.now()
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) {
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

    @JvmStatic
    fun getMonthCountForOrderPosition(order: AuftragDO?, pos: AuftragsPositionDO): BigDecimal? {
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) {
            if (pos.periodOfPerformanceEnd != null && pos.periodOfPerformanceBegin != null) {
                return getMonthCount(pos.periodOfPerformanceBegin!!, pos.periodOfPerformanceEnd!!)
            }
        } else {
            if (order!!.periodOfPerformanceEnd != null && order.periodOfPerformanceBegin != null) {
                return getMonthCount(order.periodOfPerformanceBegin!!, order.periodOfPerformanceEnd!!)
            }
        }
        return null
    }

    @JvmStatic
    fun getMonthCount(start: LocalDate, end: LocalDate): BigDecimal {
        val startDate = PFDay.from(start) // not null
        val endDate = PFDay.from(end) // not null
        val diffYear = endDate.year - startDate.year
        val diffMonth = diffYear * 12 + endDate.monthValue - startDate.monthValue + 1
        return BigDecimal.valueOf(diffMonth.toLong())
    }

    @JvmStatic
    fun addCurrency(mapping: PropertyMapping, col: Enum<*>, value: BigDecimal?) {
        if (NumberHelper.isNotZero(value)) {
            mapping.add(col, value)
        } else {
            mapping.add(col, "")
        }
    }

    @JvmStatic
    fun getInvoices(invoicePositions: Set<RechnungsPositionVO>?): String {
        return invoicePositions?.joinToString(", ") { it.rechnungNummer?.toString() ?: "" } ?: ""
    }

    @JvmStatic
    fun ensureErfassungsDatum(order: AuftragDO): LocalDate? {
        if (order.erfassungsDatum != null)
            return order.erfassungsDatum
        if (order.created != null)
            return PFDay.from(order.created).localDate
        if (order.angebotsDatum != null)
            return order.angebotsDatum
        return PFDay.now().localDate
    }

    private val POINT_FIVE = BigDecimal(".5")
    private val POINT_NINE = BigDecimal(".9")
}
