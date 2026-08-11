/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.PaymentScheduleDO
import java.math.BigDecimal
import java.time.LocalDate

/**
 * One payment milestone of an order, as the edit form of `/next/order` sends and receives it.
 *
 * Same contract as [AuftragsPosition]: [number] identifies the row for the collection handler (see
 * [PaymentScheduleDO.equals]), and a deleted schedule stays in the list with `deleted = true` instead
 * of being dropped, since `AuftragDO.paymentSchedules` has no `@SoftDeleteCollection` either.
 */
class PaymentSchedule(
    /** Row number inside the order, 1-based and stable. */
    var number: Short = 0,
    /**
     * The [AuftragsPosition.number] this milestone belongs to — a position number, not an id. A
     * milestone may be unassigned (null), it then counts towards the order as a whole.
     */
    var positionNumber: Short? = null,
    var scheduleDate: LocalDate? = null,
    var amount: BigDecimal? = null,
    var comment: String? = null,
    var reached: Boolean = false,
    /**
     * Only writable with `FIBU_AUSGANGSRECHNUNGEN = READWRITE`, see
     * [Auftrag.vollstaendigFakturiertWriteAccess].
     */
    var vollstaendigFakturiert: Boolean = false,
) : BaseDTO<PaymentScheduleDO>() {

    /**
     * Also assigns the back reference [PaymentScheduleDO.auftrag] (the column is `nullable = false`, and
     * the collection handler matches by (number, auftrag.id)). Called by [Auftrag.copyTo].
     */
    fun copyTo(dest: PaymentScheduleDO, order: AuftragDO) {
        copyTo(dest)
        dest.auftrag = order
    }
}
