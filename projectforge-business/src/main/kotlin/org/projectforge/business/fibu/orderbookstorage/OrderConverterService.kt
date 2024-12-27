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

package org.projectforge.business.fibu.orderbookstorage

import mu.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsCache
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.PaymentScheduleDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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

    fun convertFromOrder(col: Collection<Order?>?): List<AuftragDO>? {
        col ?: return null
        return col.filterNotNull().map { from(it) }
    }

    fun from(auftrag: AuftragDO): Order {
        var info = auftragsCache.getOrderInfo(auftrag.id)
        if (info == null) {
            log.warn { "No order info found for order with id ${auftrag.id}, trying to refresh AuftragsCache..." }
            auftragsCache.setExpired(auftrag)
            info = auftragsCache.getOrderInfo(auftrag.id)
            if (info == null) {
                log.error { "No order info found for order with id ${auftrag.id}, also after AuftragsCache.refresh!!! No details calculated." }
            }
        }
        if (info != null) {
            auftrag.info = info
        }
        return Order.from(auftrag)
    }

    fun from(order: Order): AuftragDO {
        return AuftragDO().apply {
            nummer = order.nummer
            referenz = order.referenz
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
        }
    }

    private fun from(pos: OrderPosition): AuftragsPositionDO {
        return AuftragsPositionDO().apply {
            number = pos.number
            task = caches.getTask(pos.taskId)
            art = pos.art
            paymentType = pos.paymentType
            status = pos.status
            titel = pos.titel
            nettoSumme = pos.nettoSumme
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
