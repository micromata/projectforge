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

import java.io.Serializable
import java.math.BigDecimal

class RechnungInfo(val invoice: AbstractRechnungDO) : Serializable {
    class PositionInfo(position: IRechnungsPosition) : IRechnungsPosition, Serializable {
        override var id = position.id
        override var deleted: Boolean = position.deleted
        override var menge: BigDecimal? = position.menge
        override var einzelNetto: BigDecimal? = position.einzelNetto
        override var netSum: BigDecimal = position.netSum
        override var vat: BigDecimal? = position.vat
        val grossSumm = RechnungCalculator.calculateGrossSum(position)
    }

    var positions: List<PositionInfo>? = null
        private set

    var netSum = BigDecimal.ZERO

    var grossSum = BigDecimal.ZERO

    var grossSumWithDiscount = BigDecimal.ZERO

    val zahlBetrag = invoice.zahlBetrag

    fun getPosition(id: Long?): PositionInfo? {
        id ?: return null
        return positions?.find { it.id == id }
    }

    init {
        positions = invoice.positionen?.filter { !it.deleted }?.map { PositionInfo(it) }
        val undeleted = positions?.filter { !it.deleted }
        undeleted?.let {
            netSum = it.sumOf { it.netSum }
            netSum = it.sumOf { it.grossSumm }
        }
        grossSumWithDiscount = RechnungCalculator.calculateGrossSumWithDiscount(invoice, grossSum)
    }

    private companion object {
        fun calculateNetSum(positions: Collection<IRechnungsPosition>?): BigDecimal {
            positions ?: return BigDecimal.ZERO
            return positions
                .filter { !it.deleted }
                .sumOf { it.netSum }
        }
    }
}
