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

import org.projectforge.business.fibu.*
import org.projectforge.framework.utils.NumberFormatter
import java.math.BigDecimal
import java.time.LocalDate

class Rechnung(var nummer: Int? = null,
               var kunde: Kunde? = null,
               var kundeText: String? = null,
               var projekt: Projekt? = null,
               var status: RechnungStatus? = null,
               var typ: RechnungTyp? = null,
               var customerref1: String? = null,
               var attachment: String? = null,
               var customerAddress: String? = null,
               var periodOfPerformanceBegin: LocalDate? = null,
               var periodOfPerformanceEnd: LocalDate? = null,
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
) : BaseDTO<RechnungDO>(), IRechnung {
    override var positionen: MutableList<RechnungsPosition>? = null

    override val netSum: BigDecimal
        get() = RechnungCalculator.calculateNetSum(this)

    override val vatAmountSum: BigDecimal
        get() = RechnungCalculator.calculateVatAmountSum(this)

    val grossSum: BigDecimal
        get() = RechnungCalculator.calculateGrossSum(this)

    var orders: Int? = null
        get() = positionen?.size

    var formattedNetSum: String? = null

    var formattedVatAmountSum: String? = null

    var formattedGrossSum: String? = null


    val isBezahlt: Boolean
        get() = if (this.netSum.compareTo(BigDecimal.ZERO) == 0) {
            true
        } else this.bezahlDatum != null && this.zahlBetrag != null


    override fun copyFrom(src: RechnungDO) {
        super.copyFrom(src)
        val list = positionen ?: mutableListOf()
        src.positionen?.forEach {
            val pos = RechnungsPosition()
            pos.copyFrom(it)
            list.add(pos)
        }
        positionen = list
        orders = positionen!!.size
        formattedNetSum = NumberFormatter.formatCurrency(netSum)
        formattedGrossSum = NumberFormatter.formatCurrency(grossSum)
        formattedVatAmountSum = NumberFormatter.formatCurrency(vatAmountSum)
    }

    override fun copyTo(dest: RechnungDO) {
        super.copyTo(dest)
        val list = dest.positionen ?: mutableListOf()
        positionen?.forEach {
            val pos = RechnungsPositionDO()
            it.copyTo(pos)
            list.add(pos)
        }
        dest.positionen = list
    }
}
