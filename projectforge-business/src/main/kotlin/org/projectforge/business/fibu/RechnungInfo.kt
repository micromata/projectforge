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

import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Information about an invoice for [RechnungDO] and [EingangsrechnungDO] (debitors and creditors).
 */
class RechnungInfo(invoice: AbstractRechnungDO) : Serializable {
    val id = invoice.id ?: -1
    val deleted = invoice.deleted
    val nummer = if (invoice is RechnungDO) invoice.nummer else null
    var positions: List<RechnungPosInfo>? = null
        internal set

    val date = invoice.datum

    @PropertyInfo(i18nKey = "fibu.common.netto", type = PropertyType.CURRENCY)
    var netSum = BigDecimal.ZERO

    @PropertyInfo(i18nKey = "fibu.common.brutto", type = PropertyType.CURRENCY)
    var grossSum = BigDecimal.ZERO

    var vatAmount: BigDecimal = BigDecimal.ZERO

    /**
     * Returns the gross amount or the amount less the discount if the discount period has not yet expired.
     * If the invoice has already been paid, the amount actually paid will be returned.
     */
    @PropertyInfo(i18nKey = "fibu.common.brutto", type = PropertyType.CURRENCY)
    var grossSumWithDiscount = BigDecimal.ZERO

    val bezahlDatum = invoice.bezahlDatum

    val zahlBetrag = invoice.zahlBetrag

    val faelligkeit = invoice.faelligkeit

    val discountMaturity = invoice.discountMaturity

    /**
     * The status of the invoice. Only available for [RechnungDO].
     */
    var status: RechnungStatus? = if (invoice is RechnungDO) invoice.status else null

    /**
     * @return The total sum of all cost assignment net amounts of all positions.
     */
    var kostZuweisungenNetSum = BigDecimal.ZERO

    var kostZuweisungenFehlbetrag = BigDecimal.ZERO

    var isBezahlt = false

    var isUeberfaellig = false

    @get:PropertyInfo(i18nKey = "fibu.rechnung.faelligkeit", type = PropertyType.CURRENCY)
    var faelligkeitOrDiscountMaturity: LocalDate? = null

    /**
     * Gets sorted list of Kost1DO of all positions.
     */
    val sortedKost1: List<KostZuweisungInfo.Kost>?
        get() = positions
            ?.flatMap { it.kostZuweisungen.orEmpty() }
            ?.map { it.kost1 }
            ?.toSet()
            ?.sortedBy { it.formattedNumber }

    /**
     * Gets sorted list of Kost2DO of all positions.
     */
    val sortedKost2: List<KostZuweisungInfo.Kost>?
        get() = positions
            ?.flatMap { it.kostZuweisungen.orEmpty() }
            ?.map { it.kost2 }
            ?.distinct()
            ?.sortedBy { it.formattedNumber }

    val sortedKost2AsString: String
        get() = sortedKost2?.joinToString(", ") { it.formattedNumber } ?: ""

    companion object {
        fun numbersAsString(sortedKost: List<KostZuweisungInfo.Kost>?): String {
            return sortedKost?.joinToString(", ") { it.formattedNumber } ?: ""
        }

        fun detailsAsString(sortedKost: List<KostZuweisungInfo.Kost>?): String {
            return sortedKost?.joinToString(separator = " | ") { it.displayName ?: "???" } ?: ""
        }
    }
}
