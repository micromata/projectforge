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

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.*
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import java.math.BigDecimal
import java.time.LocalDate

class Eingangsrechnung(
  var receiver: String? = null,
  var iban: String? = null,
  var ibanFormatted: String? = null,
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
  var faelligkeitOrDiscountMaturity: LocalDate? = null,
  var ueberfaellig: Boolean? = null,
  var zahlungsZielInTagen: Int? = null,
  var discountZahlungsZielInTagen: Int? = null,
  var bezahlDatum: LocalDate? = null,
  override var zahlBetrag: BigDecimal? = null,
  // `var`, not `val`: a form has to be able to send it back, and a read only property is silently dropped
  // by Jackson on the way in (see Rechnung.currency).
  override var currency: String? = null,
  var konto: Konto? = null,
  var discountPercent: BigDecimal? = null,
  var discountMaturity: LocalDate? = null
) : BaseDTO<EingangsrechnungDO>(), IRechnung {
  override var positionen: MutableList<EingangsrechnungsPosition>? = null

  var netSum: BigDecimal = BigDecimal.ZERO

  var vatAmountSum: BigDecimal = BigDecimal.ZERO

  var grossSum: BigDecimal = BigDecimal.ZERO

  var grossSumWithDiscount: BigDecimal = BigDecimal.ZERO

  var paymentTypeAsString: String? = null

  var kost1List: String? = null

  var kost1Info: String? = null

  var kost2List: String? = null

  var kost2Info: String? = null

  /**
   * The net sum of all cost assignments of all positions, and how much of [netSum] is not assigned to a
   * cost unit yet. Read-only, and a hint only: `EingangsrechnungDao` performs no validation of the cost
   * assignment sums, so an invoice with a difference saves fine. See [Rechnung].
   */
  var kostZuweisungenNetSum: BigDecimal? = null
  var kostZuweisungenFehlbetrag: BigDecimal? = null

  /**
   * Access flags, so the hand built next form knows what to offer. The DAO stays the authority in every
   * case — these only decide what is shown. See [Rechnung].
   */
  var writeAccess: Boolean = false
  var deleteAccess: Boolean = false

  /**
   * Whether cost accounting is configured at all (`Configuration.isCostConfigured`). The form hides the
   * cost assignments of a position when it is false. See [Rechnung].
   */
  var costConfigured: Boolean = false

  val isBezahlt: Boolean
    get() = if (this.netSum.compareTo(BigDecimal.ZERO) == 0) {
      true
    } else this.bezahlDatum != null && this.zahlBetrag != null

  override fun copyFrom(src: EingangsrechnungDO) {
    super.copyFrom(src)
    src.paymentType?.let {
      paymentTypeAsString = translate(it.i18nKey)
    }
    // ensuredInfo, not info: the latter is a lateinit that throws for an invoice nobody calculated yet,
    // which is every invoice the recalculate endpoint and newBaseDTO build.
    val info = src.ensuredInfo
    ueberfaellig = info.isUeberfaellig
    ibanFormatted = src.ibanFormatted
    this.faelligkeitOrDiscountMaturity = info.faelligkeitOrDiscountMaturity
    this.netSum = info.netSum
    this.vatAmountSum = info.vatAmount
    this.grossSum = info.grossSum
    this.grossSumWithDiscount = info.grossSumWithDiscount
    this.kostZuweisungenNetSum = info.kostZuweisungenNetSum
    this.kostZuweisungenFehlbetrag = info.kostZuweisungenFehlbetrag
  }

  /**
   * The lean row of the hand built next list: the columns of `creditor-invoice.page.tsx` and nothing else,
   * so `JsonInclude.Include.NON_NULL` keeps the rest off the wire (see [BaseDTO.copyFrom4ListRow]).
   */
  override fun copyFrom4ListRow(src: EingangsrechnungDO) {
    id = src.id
    deleted = src.deleted
    // Two columns every next list offers, hidden until the user switches them on.
    copyAuditFieldsFrom(src)
    kreditor = src.kreditor
    referenz = src.referenz
    betreff = src.betreff
    bemerkung = src.bemerkung
    datum = src.datum
    bezahlDatum = src.bezahlDatum
    currency = src.currency
    iban = src.iban
    ibanFormatted = src.ibanFormatted
    // The account of the invoice itself ("11400 - ..."), the name only — displayName is a computed getter
    // of KontoDO, so it is not in the constructor.
    konto = PfCaches.instance.getKontoIfNotInitialized(src.konto)?.let { account ->
      Konto().also { it.displayName = account.displayName }
    }
    src.paymentType?.let { paymentTypeAsString = translate(it.i18nKey) }
    val info = src.ensuredInfo
    faelligkeitOrDiscountMaturity = info.faelligkeitOrDiscountMaturity
    netSum = info.netSum
    grossSumWithDiscount = info.grossSumWithDiscount
    // Row colours rather than columns: overdue reads red, unpaid blue (see creditor-invoice.page.tsx).
    ueberfaellig = info.isUeberfaellig
    val kost1Sorted = info.sortedKost1
    kost1List = RechnungInfo.numbersAsString(kost1Sorted)
    kost1Info = RechnungInfo.detailsAsString(kost1Sorted)
    val kost2Sorted = info.sortedKost2
    kost2List = RechnungInfo.numbersAsString(kost2Sorted)
    kost2Info = RechnungInfo.detailsAsString(kost2Sorted)
  }

  /**
   * [copyFrom] plus the positions with their cost assignments, for the edit page: it has to show every
   * row, and to send them all back on save. See [Rechnung.copyFromWithCollections].
   */
  fun copyFromWithCollections(src: EingangsrechnungDO) {
    // First, so the RechnungInfo and with it every position's RechnungPosInfo exists.
    copyFrom(src)
    positionen = src.positionen?.map { position ->
      EingangsrechnungsPosition().also { it.copyFrom(position) }
    }?.toMutableList()
    val positionInfos = src.ensuredInfo.positions
    positionen?.forEach { position ->
      positionInfos?.find { it.number == position.number }?.let { position.assignSums(it) }
    }
  }

  /**
   * Rebuilds [EingangsrechnungDO.positionen] instead of appending to it: the destination is a fresh
   * [EingangsrechnungDO] per request, and appending would duplicate every row of an invoice that already
   * carries positions. Each position keeps its `id`, `number` and `deleted` flag and gets the back
   * reference to [dest]. See [Rechnung.copyTo].
   */
  override fun copyTo(dest: EingangsrechnungDO) {
    super.copyTo(dest)
    dest.positionen = positionen?.map { dto ->
      EingangsrechnungsPositionDO().also { dto.copyTo(it, dest) }
    }?.toMutableList()
  }

  companion object {
    /**
     * The calculated sums of an invoice, computed from its own (possibly unsaved) positions without
     * touching the caches. The one code path the recalculate endpoint of `IncomingInvoiceEntityRest` and
     * a fresh DTO share. See [Rechnung.calculateInvoiceInfo].
     */
    fun calculateInvoiceInfo(src: EingangsrechnungDO): RechnungInfo {
      return RechnungCalculator.calculate(src, useCaches = false)
    }
  }
}
