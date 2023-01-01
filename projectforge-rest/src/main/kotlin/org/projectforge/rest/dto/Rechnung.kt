/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.i18n.translate
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
  var ueberfaellig: Boolean? = null,
  var zahlungsZielInTagen: Int? = null,
  var discountZahlungsZielInTagen: Int? = null,
  var bezahlDatum: LocalDate? = null,
  override var zahlBetrag: BigDecimal? = null,
  var konto: Konto? = null,
  var discountPercent: BigDecimal? = null,
  var discountMaturity: LocalDate? = null
) : BaseDTO<RechnungDO>(), IRechnung {
  override var positionen: MutableList<RechnungsPosition>? = null

  override var netSum: BigDecimal = BigDecimal.ZERO

  override var vatAmountSum: BigDecimal = BigDecimal.ZERO

  var grossSum: BigDecimal = BigDecimal.ZERO

  var grossSumWithDiscount: BigDecimal = BigDecimal.ZERO

  var statusAsString: String? = null

  var kost1List: String? = null

  var kost1Info: String? = null

  var kost2List: String? = null

  var kost2Info: String? = null

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
    this.netSum = RechnungCalculator.calculateNetSum(src)
    this.vatAmountSum = RechnungCalculator.calculateVatAmountSum(src)
    this.grossSum = RechnungCalculator.calculateGrossSum(src)
    this.grossSumWithDiscount = src.grossSumWithDiscount
    ueberfaellig = src.isUeberfaellig
    src.status?.let {
      statusAsString = translate(it.i18nKey)
    }
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
