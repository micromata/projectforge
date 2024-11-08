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

import org.projectforge.common.extensions.abbreviate
import java.io.Serializable
import java.math.BigDecimal

/**
 * Cached information about an order position.
 */
class OrderPositionInfo(position: AuftragsPositionDO, order: OrderInfo) : Serializable {
    val id = position.id
    val number = position.number
    val auftrag = order
    val auftragId = order.id
    val auftragNummer = order.nummer
    val titel = position.titel
    var invoicedSum = BigDecimal.ZERO
    val status = position.status
    val paymentType = position.paymentType
    val art = position.art
    val personDays = position.personDays
    val nettoSumme = position.nettoSumme
    val vollstaendigFakturiert = position.vollstaendigFakturiert
    val periodOfPerformanceType = position.periodOfPerformanceType
    val periodOfPerformanceBegin = position.periodOfPerformanceBegin
    val periodOfPerformanceEnd = position.periodOfPerformanceEnd
    val taskId = position.taskId
    val bemerkung = position.bemerkung.abbreviate(30)
    val toBeInvoiced: Boolean

    init {
        if (position.deleted) {
            throw IllegalArgumentException("Position is deleted: $position")
        }
        toBeInvoiced = if (status != null && status.isIn(AuftragsPositionsStatus.ABGELEHNT, AuftragsPositionsStatus.ERSETZT)) {
                false
            } else if (order?.auftragsStatus != AuftragsStatus.ABGESCHLOSSEN && status != AuftragsPositionsStatus.ABGESCHLOSSEN) {
                false
            } else vollstaendigFakturiert != true
    }
}
