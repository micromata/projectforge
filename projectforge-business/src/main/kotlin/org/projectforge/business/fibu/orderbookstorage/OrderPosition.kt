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

import org.projectforge.business.fibu.*
import java.math.BigDecimal
import java.time.LocalDate

internal class OrderPosition {
    var id: Long? = null
    var number: Short = 0
    var taskId: Long? = null
    var art: AuftragsPositionsArt? = null
    var paymentType: AuftragsPositionsPaymentType? = null
    var status: AuftragsStatus? = null
    var titel: String? = null
    var netSum: BigDecimal? = null
    var personDays: BigDecimal? = null
    var vollstaendigFakturiert: Boolean? = false
    var periodOfPerformanceType: PeriodOfPerformanceType? = PeriodOfPerformanceType.SEEABOVE
    var periodOfPerformanceBegin: LocalDate? = null
    var periodOfPerformanceEnd: LocalDate? = null
    var modeOfPaymentType: ModeOfPaymentType? = null

    companion object {
        fun from(pos: OrderPositionInfo): OrderPosition {
            return OrderPosition().apply {
                id = pos.id
                number = pos.number
                taskId = pos.taskId
                art = pos.art
                paymentType = pos.paymentType
                status = pos.status
                titel = Order.abbreviate(pos.titel)
                netSum = pos.netSum
                personDays = pos.personDays
                vollstaendigFakturiert = pos.vollstaendigFakturiert
                periodOfPerformanceType = pos.periodOfPerformanceType
                periodOfPerformanceBegin = pos.periodOfPerformanceBegin
                periodOfPerformanceEnd = pos.periodOfPerformanceEnd
                modeOfPaymentType = pos.modeOfPaymentType
            }
        }
    }
}
