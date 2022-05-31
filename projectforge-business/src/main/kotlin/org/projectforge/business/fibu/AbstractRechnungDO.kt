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

import de.micromata.genome.db.jpa.history.api.NoHistory
import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.annotations.Analyze
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.xmlstream.XmlObjectReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.persistence.*

@MappedSuperclass
@JpaXmlPersist(
  beforePersistListener = [AbstractRechnungXmlBeforePersistListener::class],
  persistAfter = [Kost2ArtDO::class]
)
abstract class AbstractRechnungDO : DefaultBaseDO(), IRechnung {
  @PropertyInfo(i18nKey = "fibu.rechnung.datum")
  @Field(analyze = Analyze.NO)
  @get:Column(nullable = false)
  open var datum: LocalDate? = null

  @PropertyInfo(i18nKey = "fibu.rechnung.betreff")
  @Field
  @get:Column(length = 4000)
  open var betreff: String? = null

  @PropertyInfo(i18nKey = "comment")
  @Field
  @get:Column(length = 4000)
  open var bemerkung: String? = null

  @PropertyInfo(i18nKey = "fibu.rechnung.besonderheiten")
  @Field
  @get:Column(length = 4000)
  open var besonderheiten: String? = null

  @PropertyInfo(i18nKey = "fibu.rechnung.faelligkeit")
  @Field(analyze = Analyze.NO)
  @get:Column
  open var faelligkeit: LocalDate? = null

  /**
   * Wird nur zur Berechnung benutzt und kann für die Anzeige aufgerufen werden. Vorher sollte recalculate aufgerufen
   * werden.
   */
  @PropertyInfo(i18nKey = "fibu.rechnung.zahlungsZiel")
  @Field(analyze = Analyze.NO)
  @get:Transient
  open var zahlungsZielInTagen: Int? = null

  /**
   * Wird nur zur Berechnung benutzt und kann für die Anzeige aufgerufen werden. Vorher sollte recalculate aufgerufen
   * werden.
   */
  @get:Transient
  open var discountZahlungsZielInTagen: Int? = null

  @PropertyInfo(i18nKey = "fibu.rechnung.bezahlDatum")
  @Field(analyze = Analyze.NO)
  @get:Column(name = "bezahl_datum")
  open var bezahlDatum: LocalDate? = null

  /**
   * Bruttobetrag, der tatsächlich bezahlt wurde.
   */
  @PropertyInfo(i18nKey = "fibu.rechnung.zahlBetrag", type = PropertyType.CURRENCY)
  @get:Column(name = "zahl_betrag", scale = 2, precision = 12)
  override var zahlBetrag: BigDecimal? = null

  /**
   * This Datev account number is used for the exports of invoices. For debitor invoices (RechnungDO): If not given then
   * the account number assigned to the ProjektDO if set or KundeDO is used instead (default).
   */
  @PropertyInfo(i18nKey = "fibu.konto")
  @IndexedEmbedded(depth = 1)
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "konto_id")
  open var konto: KontoDO? = null

  @PropertyInfo(i18nKey = "fibu.rechnung.discountPercent")
  @get:Column
  open var discountPercent: BigDecimal? = null

  @PropertyInfo(i18nKey = "fibu.rechnung.discountMaturity")
  @get:Column
  open var discountMaturity: LocalDate? = null

  @get:Transient
  abstract val abstractPositionen: List<AbstractRechnungsPositionDO>?

  /**
   * The user interface status of an invoice. The [RechnungUIStatus] is stored as XML.
   */
  @field:NoHistory
  @get:Column(name = "ui_status_as_xml", length = 10000)
  open var uiStatusAsXml: String? = null

  @field:NoHistory
  val uiStatus: RechnungUIStatus? = null
    @Transient
    get() {
      if (field == null && StringUtils.isEmpty(uiStatusAsXml)) {
        return RechnungUIStatus()
      } else if (field == null) {
        val reader = XmlObjectReader()
        reader.initialize(RechnungUIStatus::class.java)
        return reader.read(uiStatusAsXml) as RechnungUIStatus
      }
      return field
    }

  @get:PropertyInfo(i18nKey = "fibu.common.brutto", type = PropertyType.CURRENCY)
  val grossSum: BigDecimal
    @Transient
    get() = RechnungCalculator.calculateGrossSum(this)

  @get:PropertyInfo(i18nKey = "fibu.rechnung.faelligkeit", type = PropertyType.CURRENCY)
  val faelligkeitOrDiscountMaturity: LocalDate?
    @Transient
    get() {
      discountMaturity?.let {
        if (!isBezahlt && !it.isBefore(LocalDate.now())) {
          return discountMaturity
        }
      }
      return faelligkeit
    }

  /**
   * Gibt den Bruttobetrag zurueck bzw. den Betrag abzueglich Skonto, wenn die Skontofrist noch nicht
   * abgelaufen ist. Ist die Rechnung bereits bezahlt, wird der tatsaechlich bezahlte Betrag zurueckgegeben.
   */
  val grossSumWithDiscount: BigDecimal
    @Transient
    get() {
      zahlBetrag?.let { return it }
      discountPercent?.let { percent ->
        if (percent.compareTo(BigDecimal.ZERO) != 0) {
          discountMaturity?.let { expireDate ->
            if (expireDate >= LocalDate.now()) {
              return grossSum.multiply(
                (HUNDRED - percent).divide(HUNDRED, 2, RoundingMode.HALF_UP)
              ).setScale(2, RoundingMode.HALF_UP)
            }
          }
        }
      }
      return grossSum
    }

  @get:PropertyInfo(i18nKey = "fibu.common.netto", type = PropertyType.CURRENCY)
  override val netSum: BigDecimal
    @Transient
    get() = RechnungCalculator.calculateNetSum(this)

  override val vatAmountSum: BigDecimal
    @Transient
    get() = RechnungCalculator.calculateVatAmountSum(this)

  /**
   * (this.status == RechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null)
   */
  @get:Transient
  abstract val isBezahlt: Boolean

  val isUeberfaellig: Boolean
    @Transient
    get() {
      if (isBezahlt) {
        return false
      }
      val today = PFDateTime.now()
      return this.faelligkeit?.isBefore(today.localDate) ?: false
    }

  val kontoId: Int?
    @Transient
    get() = konto?.id

  /**
   * @return The total sum of all cost assignment net amounts of all positions.
   */
  val kostZuweisungenNetSum: BigDecimal
    @Transient
    get() = RechnungCalculator.kostZuweisungenNetSum(this)

  val kostZuweisungFehlbetrag: BigDecimal
    @Transient
    get() = kostZuweisungenNetSum.subtract(netSum)

  override fun recalculate() {
    val date = PFDateTime.fromOrNull(this.datum)
    // recalculate the transient fields
    if (date == null) {
      this.zahlungsZielInTagen = null
      this.discountZahlungsZielInTagen = null
      return
    }
    val dueDate = PFDateTime.fromOrNull(this.faelligkeit)
    this.zahlungsZielInTagen = if (dueDate == null) null else date.daysBetween(dueDate).toInt()
    val discount = this.discountMaturity
    this.discountZahlungsZielInTagen = if (discount == null) null else date.daysBetween(discount).toInt()
  }

  /**
   * @param idx
   * @return PositionDO with given index or null, if not exist.
   */
  fun getAbstractPosition(idx: Int): AbstractRechnungsPositionDO? {
    return abstractPositionen?.getOrNull(idx)
  }

  fun addPosition(position: AbstractRechnungsPositionDO) {
    ensureAndGetPositionen()
    // Get the highest used number + 1 or take 1 for the first position.
    val nextNumber = abstractPositionen!!.maxByOrNull { it.number }?.number?.plus(1)?.toShort() ?: 1
    position.number = nextNumber
    setAbstractRechnung(position)
    addPositionWithoutCheck(position)
  }

  abstract protected fun addPositionWithoutCheck(position: AbstractRechnungsPositionDO)

  abstract fun setAbstractRechnung(position: AbstractRechnungsPositionDO)

  abstract fun ensureAndGetPositionen(): MutableList<out AbstractRechnungsPositionDO>

  /**
   * Gibt es mindestens eine Kostzuweisung?
   */
  fun hasKostZuweisungen(): Boolean {
    return abstractPositionen?.any {
      !it.kostZuweisungen.isNullOrEmpty()
    } ?: false
  }

  companion object {
    private val HUNDRED = BigDecimal("100.00")
  }
}
