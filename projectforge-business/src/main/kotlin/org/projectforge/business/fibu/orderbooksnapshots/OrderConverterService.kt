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

package org.projectforge.business.fibu.orderbooksnapshots

import mu.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Service
internal class OrderConverterService {
    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var caches: PfCaches

    fun convertFromAuftragDO(col: Collection<AuftragDO?>?): List<Order>? {
        col ?: return null
        return col.filterNotNull().map { from(it) }
    }

    fun convertFromOrder(col: Collection<Order?>?, snapshotDate: LocalDate): List<AuftragDO>? {
        col ?: return null
        return col.filterNotNull().map { from(it, snapshotDate) }
    }

    fun from(auftrag: AuftragDO): Order {
        val info = auftragsCache.getOrderInfo(auftrag.id)
        if (info == null) {
            log.warn { "No order info found for order with id ${auftrag.id}, trying to recalculate it..." }
            auftrag.info.calculateAll(auftrag) // Expensive due to lazy loadings...
        } else {
            auftrag.info = info
        }
        return Order.from(auftrag)
    }

    fun from(order: Order, snapshotDate: LocalDate): AuftragDO {
        return AuftragDO().apply {
            id = order.id
            lastUpdate = order.lastUpdate
            nummer = order.nummer
            angebotsDatum = order.angebotsDatum
            positionen = order.positionen?.map { from(it) }?.toMutableList()
            status = order.status
            kunde = caches.getKunde(order.kundeId)
            kundeText = order.kundeText
            projekt = caches.getProjekt(order.projektId)
            titel = order.titel
            paymentSchedules = order.paymentSchedules?.map { from(it) }?.toMutableList()
            periodOfPerformanceBegin = order.periodOfPerformanceBegin
            periodOfPerformanceEnd = order.periodOfPerformanceEnd
            probabilityOfOccurrence = order.probabilityOfOccurrence
            forecastType = order.forecastType
            // Write the fields also to the info object.
            info.snapshotDate = snapshotDate
            info.nummer = order.nummer
            info.angebotsDatum = order.angebotsDatum
            info.netSum = order.netSum
            info.commissionedNetSum = order.commissionedNetSum
            info.akquiseSum = order.akquiseSum
            info.invoicedSum = order.invoicedSum
            info.toBeInvoicedSum = order.toBeInvoicedSum
            info.isVollstaendigFakturiert = order.isVollstaendigFakturiert
            info.personDays = order.personDays
            info.toBeInvoiced = order.toBeInvoiced
            info.notYetInvoicedSum = order.notYetInvoicedSum
            info.positionAbgeschlossenUndNichtVollstaendigFakturiert =
                order.positionAbgeschlossenUndNichtVollstaendigFakturiert
            info.paymentSchedulesReached = order.paymentSchedulesReached
            info.periodOfPerformanceBegin = order.periodOfPerformanceBegin
            info.periodOfPerformanceEnd = order.periodOfPerformanceEnd
            info.probabilityOfOccurrence = order.probabilityOfOccurrence
            info.forecastType = order.forecastType
            info.infoPositions = positionen?.map { OrderPositionInfo(it, info).also { it.snapshotVersion = true } }
            info.infoPositions?.forEach { infoPos ->
                order.positionen?.find { it.number == infoPos.number }?.let { pos ->
                    infoPos.netSum = pos.netSum ?: BigDecimal.ZERO
                    infoPos.invoicedSum = pos.invoicedSum ?: BigDecimal.ZERO
                    infoPos.akquiseSum = pos.akquiseSum ?: BigDecimal.ZERO
                    infoPos.commissionedNetSum = pos.commissionedNetSum ?: BigDecimal.ZERO
                    infoPos.notYetInvoiced = pos.notYetInvoiced ?: BigDecimal.ZERO
                    infoPos.toBeInvoicedSum = pos.toBeInvoicedSum ?: BigDecimal.ZERO
                    infoPos.recalculateInvoicedSum(snapshotDate)
                }
            }
            info.calculateInvoicedSum(info.infoPositions)
            info.kundeAsString = kundeAsString
            info.projektAsString = projekt?.name
            info.updatePaymentScheduleEntries(paymentSchedules)
        }
    }

    private fun from(pos: OrderPosition): AuftragsPositionDO {
        return AuftragsPositionDO().apply {
            id = pos.id
            number = pos.number
            task = caches.getTask(pos.taskId)
            art = pos.art
            paymentType = pos.paymentType
            forecastType = pos.forecastType
            status = pos.status
            titel = pos.titel
            nettoSumme = pos.netSum
            personDays = pos.personDays
            vollstaendigFakturiert = pos.vollstaendigFakturiert
            periodOfPerformanceType = pos.periodOfPerformanceType
            periodOfPerformanceBegin = pos.periodOfPerformanceBegin
            periodOfPerformanceEnd = pos.periodOfPerformanceEnd
            modeOfPaymentType = pos.modeOfPaymentType
        }
    }

    private fun from(schedule: PaymentSchedule): PaymentScheduleDO {
        return PaymentScheduleDO().apply {
            id = schedule.id
            number = schedule.number
            positionNumber = schedule.orderPositionNumber
            scheduleDate = schedule.scheduleDate
            amount = schedule.amount
            comment = schedule.comment
            reached = schedule.reached
            vollstaendigFakturiert = schedule.vollstaendigFakturiert
        }
    }
}
