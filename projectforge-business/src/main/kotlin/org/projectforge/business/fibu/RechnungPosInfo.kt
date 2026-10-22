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

package org.projectforge.business.fibu

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.collections4.MapUtils.getNumber
import org.jetbrains.kotlin.ir.types.IdSignatureValues.result
import java.io.Serializable
import java.math.BigDecimal

class RechnungPosInfo(@JsonIgnore val rechnungInfo: RechnungInfo?, position: AbstractRechnungsPositionDO) : Serializable, Comparable<RechnungPosInfo> {
    var id = position.id
    var number = position.number
    var text = position.text
    var deleted = position.deleted
    var menge = position.menge
    var einzelNetto = position.einzelNetto
    var vat = position.vat
    var netSum = BigDecimal.ZERO
    var grossSum = BigDecimal.ZERO
    var vatAmount = BigDecimal.ZERO
    var kostZuweisungNetSum = BigDecimal.ZERO
    var kostZuweisungGrossSum = BigDecimal.ZERO
    var kostZuweisungNetFehlbetrag = BigDecimal.ZERO
    var auftragsPositionId: Long? = null // auftragsPosition.id
    var auftragsId: Long? = null // auftrag.id
    var auftragsPositionNummer: Short? = null // auftragsPosition.number

    var kostZuweisungen: List<KostZuweisungInfo>? = null

    /**
     * False if the invoice of this position is planned (GEPLANT), cancelled (STORNIERT) or a cancellation, same
     * as [RechnungDO.isValid]. Such an invoice stays linked to its order positions, but its amounts must not
     * count as invoiced: a cancelled invoice and its cancellation count 0 together.
     */
    @get:JsonIgnore
    val isInvoiced: Boolean
        get() = rechnungInfo?.status?.isIn(RechnungStatus.GEPLANT, RechnungStatus.STORNIERT) != true &&
                rechnungInfo?.typ != RechnungTyp.CANCELLATION

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RechnungPosInfo) return false

        if (number != other.number) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        var result = number.toInt()
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }

    /**
     * By invoice number (a cancellation right after the invoice it cancels, see [RechnungInfo.sortNummer]), then
     * position number. The invoice id makes it a total order: the cache keeps these in TreeSets, where two
     * positions comparing equal (e.g. of two cancellations, which have no number of their own) would be merged.
     */
    override fun compareTo(other: RechnungPosInfo): Int {
        return comparator.compare(this, other)
    }

    companion object {
        private val comparator = compareBy<RechnungPosInfo> { it.rechnungInfo?.sortNummer ?: -1 }
            .thenBy { it.rechnungInfo?.typ == RechnungTyp.CANCELLATION }
            .thenBy { it.rechnungInfo?.id ?: -1 }
            .thenBy { it.number }
    }
}
