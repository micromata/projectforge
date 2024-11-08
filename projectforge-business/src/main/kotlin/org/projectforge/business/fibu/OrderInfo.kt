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
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * Information about an order with additional calculated values.
 * @param order The order to get the information from.
 * @see OrderInfo.calculateAll
 */
class OrderInfo() : Serializable {
    class PaymentScheduleInfo(schedule: PaymentScheduleDO) : Serializable {
        val id = schedule.id
        val positionNumber = schedule.positionNumber
        val amount = schedule.amount
        val reached = schedule.reached
        val scheduleDate = schedule.scheduleDate
        val valid = schedule.valid
        val vollstaendigFakturiert = schedule.vollstaendigFakturiert
        val comment = schedule.comment.abbreviate(30)
    }

    var id: Long? = null
    var nummer: Int? = null
    var titel: String? = null
    var auftragsStatus: AuftragsStatus? = null
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
    var paymentSchedules: Collection<PaymentScheduleInfo>? = null

    /**
     * The positions (not deleted) of the order with additional information.
     */
    val infoPositions: Collection<OrderPositionInfo>?
        get() = AuftragsCache.instance.getOrderPositionInfosByAuftragId(id)

    fun updateFields(order: AuftragDO, paymentSchedules: Collection<PaymentScheduleDO>? = null) {
        id = order.id
        nummer = order.nummer
        titel = order.titel
        auftragsStatus = order.auftragsStatus
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
        paymentSchedules?.let { this.paymentSchedules = it.map { PaymentScheduleInfo(it) } }
    }

    /**
     * isVollstaendigFakturiert must be calculated first.
     * @return FAKTURIERT if isVollstaendigFakturiert == true, otherwise AuftragsStatus as String.
     */
    val auftragsStatusAsString: String?
        @Transient
        get() {
            if (isVollstaendigFakturiert == true) {
                return I18nHelper.getLocalizedMessage("fibu.auftrag.status.fakturiert")
            }
            auftragsStatus.let {
                return if (it != null) I18nHelper.getLocalizedMessage(it.i18nKey) else null
            }
        }

    var netSum = BigDecimal.ZERO

    var orderedNetSum = BigDecimal.ZERO

    var akquiseSum = BigDecimal.ZERO


    @get:PropertyInfo(i18nKey = "fibu.fakturiert")
    var invoicedSum = BigDecimal.ZERO

    /**
     * Gets the sum of reached payment schedules amounts and finished positions (abgeschlossen) but not yet invoiced.
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

    /**
     * For list of orders you should select positions and payment schedules in one query to avoid N+1 problem.
     * @param positionInfos The positions of the order. If not given, the positions of the order will be lazy loaded (if order is attached).
     * @param paymentSchedules The payment schedules of the order. If not given, the schedules of the order will be lazy loaded (if order is attached).
     */
    fun calculateAll(
        order: AuftragDO,
        positionInfos: Collection<OrderPositionInfo>?,
        paymentSchedules: Collection<PaymentScheduleDO>?,
    ) {
        updateFields(order, paymentSchedules)
        netSum = calculateNetSum(positionInfos)
        orderedNetSum = calculateOrderedNetSum(order, positionInfos)
        analyzeInvoices(positionInfos) // Calculates invoicedSum and positionAbgeschlossenUndNichtVollstaendigFakturiert.
        toBeInvoicedSum = calculateToBeInvoicedSum(positionInfos, paymentSchedules)
        notYetInvoicedSum = orderedNetSum - invoicedSum
        isVollstaendigFakturiert = calculateIsVollstaendigFakturiert(order, positionInfos, paymentSchedules)
        personDays = calculatePersonDays(positionInfos)
        paymentSchedulesReached =
            paymentSchedules?.any { !it.deleted && it.reached && !it.vollstaendigFakturiert } ?: false
        if (paymentSchedulesReached) {
            log.debug("Payment schedules reached for order: ${order.id}")
            toBeInvoiced = true
        } else {
            if (order.auftragsStatus == AuftragsStatus.ABGESCHLOSSEN || positionInfos?.any { it.status == AuftragsPositionsStatus.ABGESCHLOSSEN } == true) {
                toBeInvoiced = (positionInfos?.any { it.toBeInvoiced } == true)
                if (toBeInvoiced) {
                    log.debug("Finished order and/or positions and to be invoiced: ${order.id}")
                }
            }
        }
        order.auftragsStatus.let { status ->
            if (status == null ||
                status.isIn(AuftragsStatus.POTENZIAL, AuftragsStatus.IN_ERSTELLUNG, AuftragsStatus.GELEGT)
            ) {
                akquiseSum = netSum
            }
        }
    }

    /**
     * Analyzes the invoices of the positions and calculates the invoiced sum and the flag positionAbgeschlossenUndNichtVollstaendigFakturiert.
     */
    private fun analyzeInvoices(positions: Collection<OrderPositionInfo>?) {
        invoicedSum = BigDecimal.ZERO
        positions?.forEach { pos ->
            RechnungCache.instance.getRechnungsPosInfosByAuftragsPositionId(pos.id, false)?.let { set ->
                invoicedSum += RechnungDao.getNettoSumme(set)
                infoPositions?.find { it.id == pos.id }?.invoicedSum = invoicedSum
            }
            if (pos.toBeInvoiced) {
                positionAbgeschlossenUndNichtVollstaendigFakturiert = true
            }
        }
    }

    private companion object {
        fun calculateNetSum(positions: Collection<OrderPositionInfo>?): BigDecimal {
            positions ?: return BigDecimal.ZERO
            return positions
                .filter {
                    it.nettoSumme != null &&
                            it.status !in listOf(AuftragsPositionsStatus.ABGELEHNT, AuftragsPositionsStatus.ERSETZT)
                }
                .sumOf { it.nettoSumme ?: BigDecimal.ZERO }
        }

        /**
         * Adds all net sums of the positions (only ordered positions) and return the total sum. The order itself
         * must be of state LOI, commissioned (beauftragt) or escalation.
         * @param order The order to get the net sum from.
         * @param positions The positions of the order.
         * @return The total net sum of the order.
         */
        fun calculateOrderedNetSum(order: AuftragDO, positions: Collection<OrderPositionInfo>?): BigDecimal {
            if (order.auftragsStatus?.isNotIn(
                    AuftragsStatus.BEAUFTRAGT,
                    AuftragsStatus.ESKALATION,
                    AuftragsStatus.ABGESCHLOSSEN
                ) == true
            ) {
                return BigDecimal.ZERO
            }
            return positions?.filter {
                it.status?.isIn(
                    AuftragsPositionsStatus.ABGESCHLOSSEN,
                    AuftragsPositionsStatus.BEAUFTRAGT,
                    AuftragsPositionsStatus.ESKALATION,
                ) == true
            }?.sumOf { it.nettoSumme ?: BigDecimal.ZERO } ?: BigDecimal.ZERO
        }

        fun calculateToBeInvoicedSum(
            positions: Collection<OrderPositionInfo>?,
            paymentSchedules: Collection<PaymentScheduleDO>?
        ): BigDecimal {
            var sum = BigDecimal.ZERO
            val posWithPaymentReached = mutableSetOf<Short?>()
            paymentSchedules?.filter { it.toBeInvoiced }?.forEach { schedule ->
                posWithPaymentReached.add(schedule.positionNumber)
                schedule.amount?.let { amount -> sum = sum.add(amount) }
            }
            positions?.filter { it.toBeInvoiced }?.forEach { pos ->
                if (!posWithPaymentReached.contains(pos.number)) {
                    // Amount wasn't already added from payment schedule:
                    pos.nettoSumme?.let { nettoSumme -> sum = sum.add(nettoSumme) }
                }
            }
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
            paymentSchedules: Collection<PaymentScheduleDO>?
        ): Boolean {
            if (order.auftragsStatus != AuftragsStatus.ABGESCHLOSSEN) {
                // Only finished orders can be fully invoiced.
                return false
            }
            positions?.forEach { pos ->
                if (pos.vollstaendigFakturiert != true &&
                    (pos.status?.isIn(AuftragsPositionsStatus.ABGELEHNT, AuftragsPositionsStatus.ERSETZT) != true)
                ) {
                    // Only finished positions which are not rejected or replaced can be fully invoiced.
                    return false
                }
            }
            paymentSchedules?.filter { it.valid && !it.vollstaendigFakturiert }?.forEach { paymentSchedule ->
                positions?.find { it.number == paymentSchedule.number }?.let { pos ->
                    if (pos.status?.isIn(
                            AuftragsPositionsStatus.ABGESCHLOSSEN,
                            AuftragsPositionsStatus.BEAUFTRAGT,
                            AuftragsPositionsStatus.ESKALATION,
                        ) == true && pos.vollstaendigFakturiert != true
                    ) {
                        // Only finished positions which are not fully invoiced can be fully invoiced.
                        return false
                    }
                }
            }
            return true
        }

        fun calculatePersonDays(positions: Collection<OrderPositionInfo>?): BigDecimal {
            var result = BigDecimal.ZERO
            positions?.filter { it.personDays != null }?.forEach { pos ->
                if (pos.status != AuftragsPositionsStatus.ABGELEHNT && pos.status != AuftragsPositionsStatus.ERSETZT) {
                    result += pos.personDays!!
                }
            }
            return result
        }
    }
}
