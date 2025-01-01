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
import org.projectforge.common.extensions.abbreviate
import java.io.Serializable
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

/**
 * Cached information about an order position.
 */
class OrderPositionInfo(position: AuftragsPositionDO, order: OrderInfo) : Serializable {
    val id = position.id
    val deleted = position.deleted // Deleted positions shouldn't occur.
    val number = position.number
    val auftrag = order
    val auftragId = order.id
    val auftragNummer = order.nummer
    val titel = position.titel
    val status = position.status ?: AuftragsStatus.POTENZIAL //  default value shouldn't occur!
    val paymentType = position.paymentType
    val art = position.art
    val personDays = position.personDays
    val modeOfPaymentType = position.modeOfPaymentType

    /** netSum of the order position in database. */
    val dbNetSum = position.nettoSumme ?: BigDecimal.ZERO

    /**
     * For finished postio
     */
    val vollstaendigFakturiert = position.vollstaendigFakturiert == true && status == AuftragsStatus.ABGESCHLOSSEN
    val periodOfPerformanceType = position.periodOfPerformanceType
    val periodOfPerformanceBegin = position.periodOfPerformanceBegin
    val periodOfPerformanceEnd = position.periodOfPerformanceEnd
    val taskId = position.task?.id
    val bemerkung = position.bemerkung.abbreviate(30)

    /**
     * True if the position should be invoiced.
     * For lost positions [AuftragsOrderState.LOST] false.
     * For closed orders [AuftragsStatus.ABGESCHLOSSEN] and closed positions [AuftragsPositionsStatus.ABGESCHLOSSEN] true if not fully invoiced.
     * Otherwise, false.
     * @see recalculate
     */
    var toBeInvoiced: Boolean = false

    /**
     * The net sum for commissioned positions ([AuftragsOrderState.COMMISSIONED]), otherwise, 0.
     * @see calculate
     */
    var commissionedNetSum = BigDecimal.ZERO

    /**
     * The net sum of the position as stored in database if the position isn't rejected or replaced.
     * For lost positions [AuftragsOrderState.LOST] 0.
     */
    var netSum = BigDecimal.ZERO

    /**
     * Sum of not yet ordered positions [AuftragsOrderState.POTENTIAL].
     */
    var akquiseSum = BigDecimal.ZERO

    var invoicedSum = BigDecimal.ZERO

    /**
     * Sum of the position which should be invoiced. This might be ignored if there are reached payment schedules for
     * this position. This is determined by the parent [OrderInfo].
     */
    var toBeInvoicedSum = BigDecimal.ZERO

    /**
     * Net sum of ordered positions minus invoiced sum.
     */
    var notYetInvoiced = BigDecimal.ZERO

    init {
        /*if (position.status == null) {
            log.info { "Position without status: $position shouldn't occur. Assuming POTENZIAL." }
        }*/
        if (position.deleted) {
            log.debug {"Position is deleted: $position"}
            // Nothing to calculate.
        } else {
            recalculate(order)
        }
    }

    /**
     * The fields are independent of the order status. The OrderInfo parent object has to consider the order status.
     * @param order The parent order. If the order is closed, the ordered position are considered as to be invoiced. This position will be marked
     * as to-be-invoiced if there is a payment schedule for this position marked as reached.
     */
    fun recalculate(order: OrderInfo) {
        netSum = if (status.orderState != AuftragsOrderState.LOST) dbNetSum else BigDecimal.ZERO
        commissionedNetSum = if (status.orderState == AuftragsOrderState.COMMISSIONED) netSum else BigDecimal.ZERO
        toBeInvoiced = if (status.orderState == AuftragsOrderState.LOST) {
            false
        } else if (status == AuftragsStatus.ABGESCHLOSSEN ||
            (order.status == AuftragsStatus.ABGESCHLOSSEN && status.orderState == AuftragsOrderState.COMMISSIONED) ||
            // Now, check payment schedules
            order.paymentScheduleEntries?.any { it.positionNumber == number && it.toBeInvoiced } == true
        ) {
            !vollstaendigFakturiert
        } else false
        invoicedSum = BigDecimal.ZERO
        RechnungCache.instance.getRechnungsPosInfosByAuftragsPositionId(id)?.let { set ->
            invoicedSum += RechnungDao.getNettoSumme(set)
        }
        toBeInvoicedSum = if (toBeInvoiced) netSum - invoicedSum else BigDecimal.ZERO
        notYetInvoiced = if (status.orderState == AuftragsOrderState.COMMISSIONED && !vollstaendigFakturiert) {
            netSum - invoicedSum
        } else {
            BigDecimal.ZERO
        }
        if (notYetInvoiced < BigDecimal.ZERO) {
            notYetInvoiced = BigDecimal.ZERO
        }
        akquiseSum = if (status.orderState == AuftragsOrderState.POTENTIAL) dbNetSum else BigDecimal.ZERO
        if (deleted) {
            // Leave the values for invoicedSum untouched.
            netSum = BigDecimal.ZERO
            commissionedNetSum = BigDecimal.ZERO
            toBeInvoiced = false
            toBeInvoicedSum = BigDecimal.ZERO
            notYetInvoiced = BigDecimal.ZERO
            akquiseSum = BigDecimal.ZERO
            return
        }

    }
}
