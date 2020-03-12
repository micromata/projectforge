/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.IRechnung
import org.projectforge.business.fibu.PaymentType
import org.projectforge.business.fibu.RechnungCalculator
import java.math.BigDecimal
import java.time.LocalDate

class Eingangsrechnung(var receiver: String? = null,
                       var iban: String? = null,
                       var bic: String? = null,
                       var referenz: String? = null,
                       var kreditor: String? = null,
                       var paymentType: PaymentType? = null,
                       var customernr: String? = null,
                       var datum: LocalDate? = null,
                       var betreff: String? = null,
                       var bemerkung: String? = null,
                       var besonderheiten: String? = null,
                       var faelligkeit: LocalDate? = null,
                       var zahlungsZielInTagen: Int? = null,
                       var discountZahlungsZielInTagen: Int? = null,
                       var bezahlDatum: LocalDate? = null,
                       override var zahlBetrag: BigDecimal? = null,
                       var konto: Konto? = null,
                       var discountPercent: BigDecimal? = null,
                       var discountMaturity: LocalDate? = null
) : BaseDTO<EingangsrechnungDO>(), IRechnung {
    override var positionen: List<EingangsrechnungsPosition>? = null

    override val netSum: BigDecimal
        get() = RechnungCalculator.calculateNetSum(this)

    override val vatAmountSum: BigDecimal
        get() = RechnungCalculator.calculateVatAmountSum(this)


    val isBezahlt: Boolean
        get() = if (this.netSum.compareTo(BigDecimal.ZERO) == 0) {
            true
        } else this.bezahlDatum != null && this.zahlBetrag != null


    override fun copyFrom(src: EingangsrechnungDO) {
        super.copyFrom(src)
        // TBD...
        // Positionen, Konto
    }

    override fun copyTo(dest: EingangsrechnungDO) {
        super.copyTo(dest)
        // TBD...
        // Positionen, Konto
    }
}
