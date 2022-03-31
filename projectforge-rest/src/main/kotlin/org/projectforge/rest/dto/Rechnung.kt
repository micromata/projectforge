/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

class Rechnung(
  var nummer: Int? = null,
  var customer: Customer? = null,
  var kundeText: String? = null,
  var project: Project? = null,
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

  var formattedNetSum: String? = null

  var formattedVatAmountSum: String? = null

  var formattedGrossSum: String? = null


  val isBezahlt: Boolean
    get() = if (this.netSum.compareTo(BigDecimal.ZERO) == 0) {
      true
    } else this.bezahlDatum != null && this.zahlBetrag != null

  override fun copyFrom(src: RechnungDO) {
    super.copyFrom(src)
    src.projekt?.let { p ->
      project = Project()
      project?.copyFromMinimal(p)
    }
    src.kunde?.let { c ->
      customer = Customer()
      customer?.copyFromMinimal(c)
    }
    formattedNetSum = NumberFormatter.formatCurrency(src.netSum, true)
    formattedGrossSum = NumberFormatter.formatCurrency(src.grossSum, true)
    formattedVatAmountSum = NumberFormatter.formatCurrency(src.vatAmountSum, true)
  }

  fun copyPositionenFrom(src: RechnungDO) {
    val list = positionen ?: mutableListOf()
    src.positionen?.forEach {
      list.add(RechnungsPosition(it))
    }
    src.projekt?.let {
      project = Project()
      project?.copyFromMinimal(it)
    }
    kundeText = src.kundeAsString
    src.konto?.let {
      konto = Konto()
      konto?.copyFromMinimal(it)
    }
    positionen = list
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
