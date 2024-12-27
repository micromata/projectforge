/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.persistence.Transient
import mu.KotlinLogging
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.extensions.abbreviate
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Information about an order with additional calculated values.
 * @see OrderInfo.calculateAll
 */
class OrderInfo() : Serializable {
    class PaymentScheduleInfo(schedule: PaymentScheduleDO) : Serializable {
        val id = schedule.id
        val number = schedule.number
        val positionNumber = schedule.positionNumber
        val amount = schedule.amount
        val reached = schedule.reached
        val scheduleDate = schedule.scheduleDate

        /**
         * Not deleted and amount is given > 0,00.
         */
        val valid = !schedule.deleted && amount != null && amount > BigDecimal.ZERO
        val vollstaendigFakturiert = schedule.vollstaendigFakturiert
        val comment = schedule.comment.abbreviate(30)
        val toBeInvoiced: Boolean
            get() = !vollstaendigFakturiert && valid && reached
    }

    var id: Long? = null
    var deleted: Boolean = false
    var nummer: Int? = null
    var titel: String? = null
    var status = AuftragsStatus.POTENZIAL
    var angebotsDatum: LocalDate? = null
    var created: Date? = null
    var erfassungsDatum: LocalDate? = null
    var entscheidungsDatum: LocalDate? = null
    lateinit var kundeAsString: String
    lateinit var projektAsString: String
    var probabilityOfOccurrence: Int? = null
    var periodOfPerformanceBegin: LocalDate? = null
    var periodOfPerformanceEnd: LocalDate? = null
    var contactPerson: PFUserDO? = null
    var bemerkung: String? = null
    var paymentScheduleEntries: Collection<PaymentScheduleInfo>? = null

    /**
     * The positions (not deleted) of the order with additional information.
     */
    val infoPositions: Collection<OrderPositionInfo>?
        get() = AuftragsCache.instance.getOrderPositionInfosByAuftragId(id)

    fun updateFields(order: AuftragDO, paymentSchedules: Collection<PaymentScheduleDO>? = null) {
        id = order.id
        deleted = order.deleted
        nummer = order.nummer
        titel = order.titel
        order.status.let {
            if (it != null) {
                status = it
            } else {
                log.error { "Order without status: $order shouldn't occur. Assuming POTENZIAL." }
            }
        }
        angebotsDatum = order.angebotsDatum
        created = order.created
        erfassungsDatum = order.erfassungsDatum
        entscheidungsDatum = order.entscheidungsDatum
        kundeAsString = order.kundeAsString
        projektAsString = order.projektAsString
        probabilityOfOccurrence = order.probabilityOfOccurrence
        periodOfPerformanceBegin = order.periodOfPerformanceBegin
        periodOfPerformanceEnd = order.periodOfPerformanceEnd
        contactPerson = order.contactPerson
        bemerkung = order.bemerkung.abbreviate(30)
        paymentSchedules?.let { this.paymentScheduleEntries = it.map { PaymentScheduleInfo(it) } }
    }

    /**
     * isVollstaendigFakturiert must be calculated first.
     * @return FAKTURIERT if isVollstaendigFakturiert == true, otherwise AuftragsStatus as String.
     */
    val statusAsString: String?
        @Transient
        get() {
            return if (isVollstaendigFakturiert == true) {
                I18nHelper.getLocalizedMessage("fibu.auftrag.status.fakturiert")
            } else {
                I18nHelper.getLocalizedMessage(status.i18nKey)
            }
        }

    /**
     * The sum of all net sums of the positions of the order without positions which are rejected or replaced
     * ([AuftragsStatus.ABGELEHNT], [AuftragsStatus.ERSETZT]).
     */
    var netSum = BigDecimal.ZERO

    /**
     * The sum of all net sums of the positions (only commissioned positions) of this order. This value is 0 for lost orders.
     */
    var commissionedNetSum = BigDecimal.ZERO

    /**
     * For not lost orders the sum of all akquise sums of the positions of this order.
     */
    var akquiseSum = BigDecimal.ZERO


    @get:PropertyInfo(i18nKey = "fibu.fakturiert")
    var invoicedSum = BigDecimal.ZERO

    /**
     * Gets the sum of reached payment schedules amounts and finished positions (abgeschlossen) but not yet invoiced.
     * It's a little bit tricky because a payment schedule can be assigned to a position and a position can have a to-be-invoiced amount.
     * A payment schedule may also be unassigned to a position.
     * So, the to-be-invoiced amount might be faulty.
     * @see calculateToBeInvoicedSum
     */
    var toBeInvoicedSum = BigDecimal.ZERO

    var isVollstaendigFakturiert: Boolean = false

    /**
     * @return The sum of person days of all positions.
     */
    var personDays = BigDecimal.ZERO

    /**
     * True for finished orders or order positions not marked as invoiced or reached payment milestones.
     */
    var toBeInvoiced: Boolean = false

    var notYetInvoicedSum = BigDecimal.ZERO

    var positionAbgeschlossenUndNichtVollstaendigFakturiert: Boolean = false

    var paymentSchedulesReached: Boolean = false

    fun getInfoPosition(id: Long?): OrderPositionInfo? {
        id ?: return null
        return infoPositions?.find { it.id == id }
    }

    /** Use this method to calculate all fields of this order info without using the cached order info. */
    fun calculateAll(order: AuftragDO) {
        updateFields(order, order.paymentSchedules) // Update order info fields from given order.
        val posInfos = order.positionen?.map { OrderPositionInfo(it, order.info) }
        calculateAll(order, posInfos, order.paymentSchedules)
    }

    /**
     * For list of orders you should select positions and payment schedules in one query to avoid N+1 problem.
     * @param positionInfos The positions of the order. If not given, the positions of the order will be lazy loaded (if order is attached).
     * @param paymentScheduleEntries The payment schedules of the order. If not given, the schedules of the order will be lazy loaded (if order is attached).
     */
    fun calculateAll(
        order: AuftragDO,
        positionInfos: Collection<OrderPositionInfo>?,
        paymentScheduleDOEntries: Collection<PaymentScheduleDO>?,
    ) {
        updateFields(order, paymentScheduleDOEntries)
        positionInfos?.forEach { it.recalculate(this) }
        netSum = positionInfos?.sumOf { it.netSum } ?: BigDecimal.ZERO
        commissionedNetSum = if (status.orderState != AuftragsOrderState.LOST) {
            positionInfos?.sumOf { it.commissionedNetSum } ?: BigDecimal.ZERO
        } else {
            BigDecimal.ZERO
        }
        akquiseSum = if (status.orderState != AuftragsOrderState.LOST) {
            positionInfos?.sumOf { it.akquiseSum } ?: BigDecimal.ZERO
        } else {
            BigDecimal.ZERO
        }
        invoicedSum = positionInfos?.sumOf { it.invoicedSum } ?: BigDecimal.ZERO
        positionAbgeschlossenUndNichtVollstaendigFakturiert = positionInfos?.any { it.toBeInvoiced } == true
        toBeInvoicedSum = calculateToBeInvoicedSum(positionInfos, paymentScheduleEntries)
        notYetInvoicedSum = positionInfos?.sumOf { it.notYetInvoiced } ?: BigDecimal.ZERO
        if (notYetInvoicedSum < BigDecimal.ZERO) {
            notYetInvoicedSum = BigDecimal.ZERO
        }
        isVollstaendigFakturiert = calculateIsVollstaendigFakturiert(order, positionInfos, paymentScheduleEntries)
        personDays = calculatePersonDays(positionInfos)
        paymentSchedulesReached = paymentScheduleEntries?.any { it.toBeInvoiced } ?: false
        if (paymentSchedulesReached) {
            log.debug("Payment schedules reached for order: ${order.id}")
            toBeInvoiced = true
        } else {
            if (order.status == AuftragsStatus.ABGESCHLOSSEN || positionInfos?.any { it.status == AuftragsStatus.ABGESCHLOSSEN } == true) {
                toBeInvoiced = (positionInfos?.any { it.toBeInvoiced } == true)
                if (toBeInvoiced) {
                    log.debug("Finished order and/or positions and to be invoiced: ${order.id}")
                }
            }
        }
        if (deleted) {
            toBeInvoiced = false
            netSum = BigDecimal.ZERO
            commissionedNetSum = BigDecimal.ZERO
            akquiseSum = BigDecimal.ZERO
            toBeInvoicedSum = BigDecimal.ZERO
            positionAbgeschlossenUndNichtVollstaendigFakturiert = false
            notYetInvoicedSum = BigDecimal.ZERO
            paymentSchedulesReached = false
        }
    }

    private companion object {

        /**
         * Sums all to be invoiced amounts of the positions and payment schedules.
         * It's a little bit tricky because a payment schedule can be assigned to a position and a position can have a to-be-invoiced amount.
         * A payment schedule may also be unassigned to a position.
         * The to-be-invoiced amount of a position will only be added, if not already reached by a payment schedule assigned to this position.
         */
        fun calculateToBeInvoicedSum(
            positions: Collection<OrderPositionInfo>?,
            paymentSchedules: Collection<PaymentScheduleInfo>?
        ): BigDecimal {
            var sum = BigDecimal.ZERO
            val posWithPaymentReached = mutableSetOf<Short?>()
            paymentSchedules?.filter { it.toBeInvoiced }?.forEach { schedule ->
                schedule.amount?.let { amount ->
                    posWithPaymentReached.add(schedule.positionNumber)
                    sum += amount
                }
            }
            sum += positions?.filter { !posWithPaymentReached.contains(it.number) }?.sumOf { it.toBeInvoicedSum }
                ?: BigDecimal.ZERO
            return sum
        }

        /**
         * Checks if the order is fully invoiced. An order is fully invoiced if all positions are fully invoiced and all
         * payment schedules are fully invoiced.
         * @param order The order to check.
         * @param positions The positions of the order.
         * @param paymentSchedules The payment schedules of the order.
         */
        fun calculateIsVollstaendigFakturiert(
            order: AuftragDO,
            positions: Collection<OrderPositionInfo>?,
            paymentSchedules: Collection<PaymentScheduleInfo>?
        ): Boolean {
            if (order.status != AuftragsStatus.ABGESCHLOSSEN) {
                // Only finished orders can be fully invoiced.
                return false
            }
            if (positions?.any { !it.vollstaendigFakturiert && !it.deleted && it.status.orderState != AuftragsOrderState.LOST } == true) {
                // Only fully invoiced positions can be fully invoiced.
                return false
            }
            if (paymentSchedules?.any { it.valid && !it.vollstaendigFakturiert } == true) {
                // Only fully invoiced payment schedules can be fully invoiced.
                return false
            }
            return true
        }

        fun calculatePersonDays(positions: Collection<OrderPositionInfo>?): BigDecimal {
            var result = BigDecimal.ZERO
            positions?.filter { it.personDays != null }?.forEach { pos ->
                if (pos.status != AuftragsStatus.ABGELEHNT && pos.status != AuftragsStatus.ERSETZT) {
                    result += pos.personDays!!
                }
            }
            return result
        }
    }
}
