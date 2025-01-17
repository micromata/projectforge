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
import org.projectforge.common.extensions.abbreviate
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Cached information about an order position.
 * @param position The order position (not given e.g. for test cases if [OrderPositionInfo] is deserialized from json.
 */
class OrderPositionInfo(position: AuftragsPositionDO? = null, order: OrderInfo? = null) : Serializable {
    var id = position?.id
    var deleted = position?.deleted ?: false// Deleted positions shouldn't occur.
    var number = position?.number

    @JsonIgnore
    var auftrag = order
    var auftragId = order?.id
    var auftragNummer = order?.nummer
    var titel = position?.titel
    var status = position?.status ?: AuftragsStatus.POTENZIAL //  default value shouldn't occur!
    var paymentType = position?.paymentType
    var art = position?.art
    var personDays = position?.personDays
    var modeOfPaymentType = position?.modeOfPaymentType

    /** netSum of the order position in database. */
    var dbNetSum = position?.nettoSumme ?: BigDecimal.ZERO

    /**
     * For finished positions. True if the position is fully invoiced and the status is [AuftragsStatus.ABGESCHLOSSEN].
     */
    var vollstaendigFakturiert = position?.vollstaendigFakturiert == true && status == AuftragsStatus.ABGESCHLOSSEN
    var periodOfPerformanceType = position?.periodOfPerformanceType
    var periodOfPerformanceBegin = position?.periodOfPerformanceBegin
    var periodOfPerformanceEnd = position?.periodOfPerformanceEnd
    var taskId = position?.task?.id
    var bemerkung = position?.bemerkung.abbreviate(30)

    /**
     * True if the position should be invoiced.
     * For lost positions [AuftragsOrderState.LOST] false.
     * For closed orders [AuftragsStatus.ABGESCHLOSSEN] and closed positions [AuftragsPositionsStatus.ABGESCHLOSSEN] true if not fully invoiced.
     * Otherwise, false.
     * @see recalculateAll
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

    /**
     * If true, the order position was loaded from a snapshot. Therefore, no recalculation should be done.
     */
    var snapshotVersion: Boolean = false

    init {
        /*if (position.status == null) {
            log.info { "Position without status: $position shouldn't occur. Assuming POTENZIAL." }
        }*/
        if (position?.deleted == true) {
            log.debug { "Position is deleted: $position" }
            // Nothing to calculate.
        } else if (snapshotVersion) {
            log.debug { "Position is loaded from snapshot: $position" }
            // Nothing to calculate.
        } else if (order != null) {
            recalculateAll(order)
        }
    }

    /**
     * The fields are independent of the order status. The OrderInfo parent object has to consider the order status.
     * @param order The parent order. If the order is closed, the ordered position are considered as to be invoiced. This position will be marked
     * as to-be-invoiced if there is a payment schedule for this position marked as reached.
     */
    fun recalculateAll(order: OrderInfo, snapshotDate: LocalDate? = null) {
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
        akquiseSum = if (status.orderState == AuftragsOrderState.POTENTIAL) dbNetSum else BigDecimal.ZERO
        recalculateInvoicedSum(snapshotDate)
    }

    fun recalculateInvoicedSum(snapshotDate: LocalDate? = null) {
        invoicedSum = BigDecimal.ZERO
        RechnungCache.instance.getRechnungsPosInfosByAuftragsPositionId(id)?.let { set ->
            val useSet = if (snapshotDate != null) {
                // If a snapshot date is given, only consider invoices before this date.
                set.filter { (it.rechnungInfo?.date ?: LocalDate.MAX) <= snapshotDate }
            } else {
                set
            }
            invoicedSum += RechnungDao.getNettoSumme(useSet)
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
        updateFieldsIfDeleted()
    }

    private fun updateFieldsIfDeleted() {
        if (deleted) {
            // Leave the values for invoicedSum untouched.
            netSum = BigDecimal.ZERO
            commissionedNetSum = BigDecimal.ZERO
            toBeInvoiced = false
            toBeInvoicedSum = BigDecimal.ZERO
            notYetInvoiced = BigDecimal.ZERO
            akquiseSum = BigDecimal.ZERO
        }
    }
}
