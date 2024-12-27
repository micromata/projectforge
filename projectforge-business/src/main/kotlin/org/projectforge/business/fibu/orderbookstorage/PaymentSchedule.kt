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

import org.projectforge.business.fibu.PaymentScheduleDO
import java.math.BigDecimal
import java.time.LocalDate

class PaymentSchedule {
    var number: Short = 0
    var orderPositionNumber: Short? = null
    var scheduleDate: LocalDate? = null
    var amount: BigDecimal? = null
    var comment: String? = null
    var reached: Boolean = false
    var vollstaendigFakturiert: Boolean = false

    companion object {
        fun from(schedule: PaymentScheduleDO): PaymentSchedule {
            return PaymentSchedule().apply {
                number = schedule.number
                orderPositionNumber = schedule.positionNumber
                scheduleDate = schedule.scheduleDate
                amount = schedule.amount
                comment = schedule.comment
                reached = schedule.reached
                vollstaendigFakturiert = schedule.vollstaendigFakturiert
            }
        }
    }
}
