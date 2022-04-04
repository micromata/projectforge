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

package org.projectforge.business.fibu

import org.projectforge.business.utils.CurrencyFormatter
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDay.Companion.fromOrNow
import org.projectforge.framework.utils.NumberHelper.add
import org.projectforge.statistics.IntAggregatedValues
import java.io.Serializable
import java.math.BigDecimal

open class AbstractRechnungsStatistik<T : AbstractRechnungDO?> : Serializable {
  var brutto: BigDecimal
    protected set
  var netto: BigDecimal
    protected set
  var gezahlt: BigDecimal
    protected set
  var offen: BigDecimal
    protected set
  var ueberfaellig: BigDecimal
    protected set

  /**
   * Fehlbeträge, die der Kunde weniger überwiesen hat und die akzeptiert wurden, d. h. die Rechnung gilt als bezahlt.
   */
  var skonto: BigDecimal
    protected set
  protected var zahlungsZielSum: Long = 0
  protected var tatsaechlichesZahlungsZiel = IntAggregatedValues()
  var counterBezahlt: Int
    protected set
  var counter: Int
    protected set

  init {
    skonto = BigDecimal.ZERO
    ueberfaellig = skonto
    offen = ueberfaellig
    gezahlt = offen
    netto = gezahlt
    brutto = netto
    counterBezahlt = 0
    counter = counterBezahlt
  }

  fun add(rechnung: T) {
    val netto = rechnung!!.netSum
    val brutto = rechnung.grossSum
    val gezahlt = rechnung.zahlBetrag
    this.netto = add(this.netto, netto)
    this.brutto = add(this.brutto, brutto)
    if (gezahlt != null) {
      this.gezahlt = add(this.gezahlt, gezahlt)
      if (gezahlt.compareTo(brutto) < 0) {
        skonto = add(skonto, brutto.subtract(gezahlt))
      }
    } else {
      offen = add(offen, brutto)
      if (rechnung.isUeberfaellig) {
        ueberfaellig = add(ueberfaellig, brutto)
      }
    }
    val datum = fromOrNow(rechnung.datum)
    val faelligDatum = fromOrNow(rechnung.faelligkeit)
    zahlungsZielSum += datum.daysBetween(faelligDatum)
    if (rechnung.bezahlDatum != null) {
      val bezahlDatum = fromOrNow(rechnung.bezahlDatum)
      tatsaechlichesZahlungsZiel.add(datum.daysBetween(bezahlDatum).toInt(), brutto.toInt())
      counterBezahlt++
    }
    counter++
  }

  val zahlungszielAverage: Int
    get() = if (counter == 0) {
      0
    } else (zahlungsZielSum / counter).toInt()
  val asMarkdown: String
    get() {
      val sb = StringBuilder()
      append(sb, "fibu.common.brutto", CurrencyFormatter.format(brutto))
      append(sb, "fibu.common.netto", CurrencyFormatter.format(netto))
      append(sb, "fibu.rechnung.offen", CurrencyFormatter.format(offen), "blue")
      append(sb, "fibu.rechnung.filter.ueberfaellig", CurrencyFormatter.format(ueberfaellig),"red")
      append(sb, "fibu.rechnung.skonto", CurrencyFormatter.format(skonto))
      append(sb,"fibu.rechnung.zahlungsZiel", "$zahlungszielAverage")
      append(sb, "fibu.rechnung.zahlungsZiel.actual", "Ø$tatsaechlichesZahlungzielAverage")
      return sb.toString()
    }

  private fun append(sb: StringBuilder, i18nKey: String, value: String) {
    sb.append(translate(i18nKey)).append(": ").append(value).append(" | ")
  }

  private fun append(sb: StringBuilder, i18nKey: String, value: String, color: String) {
    sb.append("<span style=\"color:$color;\">")
    append(sb, i18nKey, value)
    sb.append("</span>")
  }

  val tatsaechlichesZahlungzielAverage: Int
    get() = tatsaechlichesZahlungsZiel.weightedAverage

  companion object {
    private const val serialVersionUID = 3695426728243488756L
  }
}
