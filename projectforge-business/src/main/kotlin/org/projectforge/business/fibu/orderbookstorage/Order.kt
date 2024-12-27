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

import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.OrderInfo
import org.projectforge.business.fibu.OrderPositionInfo
import java.math.BigDecimal
import java.time.LocalDate

/**
 * For storing serializing and deserializing orders.
 */
internal class Order {
    var nummer: Int? = null
    var referenz: String? = null
    var positionen: Collection<OrderPosition>? = null
    var status: AuftragsStatus? = null
    var kundeId: Long? = null
    var kundeText: String? = null
    var projektId: Long? = null
    var titel: String? = null
    var paymentSchedules: Collection<PaymentSchedule>? = null
    var periodOfPerformanceBegin: LocalDate? = null
    var periodOfPerformanceEnd: LocalDate? = null
    var probabilityOfOccurrence: Int? = null

    /**
     * [OrderInfo.netSum]
     */
    var netSum = BigDecimal.ZERO

    /**
     * [OrderInfo.commissionedNetSum]
     */
    var commissionedNetSum = BigDecimal.ZERO

    /**
     * [OrderInfo.akquiseSum]
     */
    var akquiseSum = BigDecimal.ZERO


    /**
     * [OrderInfo.invoicedSum]
     */
    var invoicedSum = BigDecimal.ZERO

    /**
     * [OrderInfo.toBeInvoicedSum]
     */
    var toBeInvoicedSum = BigDecimal.ZERO

    /**
     * [OrderInfo.isVollstaendigFakturiert]
     */
    var isVollstaendigFakturiert: Boolean = false

    /**
     * [OrderInfo.personDays]
     */
    var personDays = BigDecimal.ZERO

    /**
     * [OrderInfo.toBeInvoiced]
     */
    var toBeInvoiced: Boolean = false

    /**
     * [OrderInfo.notYetInvoicedSum]
     */
    var notYetInvoicedSum = BigDecimal.ZERO

    /**
     * [OrderInfo.positionAbgeschlossenUndNichtVollstaendigFakturiert]
     */
    var positionAbgeschlossenUndNichtVollstaendigFakturiert: Boolean = false

    /**
     * [OrderInfo.paymentSchedulesReached]
     */
    var paymentSchedulesReached: Boolean = false

    companion object {
        /**
         * [AuftragDO.info] must be calculated before calling this method.
         */
        fun from(order: AuftragDO): Order {
            return Order().apply {
                nummer = order.nummer
                referenz = order.referenz
                positionen = order.info.infoPositions?.map { OrderPosition.from(it) }
                status = order.status
                kundeId = order.kunde?.id
                kundeText = order.kundeText
                projektId = order.projekt?.id
                titel = order.titel
                paymentSchedules = order.info.paymentScheduleEntries?.map { PaymentSchedule.from(it) }
                periodOfPerformanceBegin = order.periodOfPerformanceBegin
                periodOfPerformanceEnd = order.periodOfPerformanceEnd
                probabilityOfOccurrence = order.probabilityOfOccurrence

                val info = order.info
                netSum = info.netSum
                commissionedNetSum = info.commissionedNetSum
                akquiseSum = info.akquiseSum
                invoicedSum = info.invoicedSum
                toBeInvoicedSum = info.toBeInvoicedSum
                isVollstaendigFakturiert = info.isVollstaendigFakturiert
                personDays = info.personDays
                toBeInvoiced = info.toBeInvoiced
                notYetInvoicedSum = info.notYetInvoicedSum
                positionAbgeschlossenUndNichtVollstaendigFakturiert =
                    info.positionAbgeschlossenUndNichtVollstaendigFakturiert
                paymentSchedulesReached = info.paymentSchedulesReached
            }
        }
    }
}
