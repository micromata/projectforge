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

import org.projectforge.business.fibu.OrderInfo
import org.projectforge.business.fibu.PaymentScheduleDO
import java.math.BigDecimal
import java.time.LocalDate

class PaymentSchedule {
    var id: Long? = null
    var number: Short = 0
    var orderPositionNumber: Short? = null
    var scheduleDate: LocalDate? = null
    var amount: BigDecimal? = null
    var comment: String? = null
    var reached: Boolean = false
    var vollstaendigFakturiert: Boolean = false

    companion object {
        fun from(scheduleInfo: OrderInfo.PaymentScheduleInfo): PaymentSchedule {
            return PaymentSchedule().apply {
                id = scheduleInfo.id
                number = scheduleInfo.number
                orderPositionNumber = scheduleInfo.positionNumber
                scheduleDate = scheduleInfo.scheduleDate
                amount = scheduleInfo.amount
                comment = scheduleInfo.comment
                reached = scheduleInfo.reached
                vollstaendigFakturiert = scheduleInfo.vollstaendigFakturiert
            }
        }
    }
}
