/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import org.hibernate.search.annotations.*
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.time.DateHolder
import org.projectforge.framework.time.DayHolder
import org.projectforge.framework.xstream.XmlObjectReader
import java.math.BigDecimal
import java.sql.Date
import javax.persistence.*

@MappedSuperclass
@JpaXmlPersist(beforePersistListener = [AbstractRechnungXmlBeforePersistListener::class], persistAfter = [Kost2ArtDO::class])
abstract class AbstractRechnungDO : DefaultBaseDO() {
    @PropertyInfo(i18nKey = "fibu.rechnung.datum")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(nullable = false)
    open var datum: Date? = null

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
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column
    open var faelligkeit: Date? = null

    /**
     * Wird nur zur Berechnung benutzt und kann für die Anzeige aufgerufen werden. Vorher sollte recalculate aufgerufen
     * werden.
     */
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
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "bezahl_datum")
    open var bezahlDatum: Date? = null

    /**
     * Bruttobetrag, der tatsächlich bezahlt wurde.
     */
    @PropertyInfo(i18nKey = "fibu.rechnung.zahlBetrag", type = PropertyType.CURRENCY)
    @get:Column(name = "zahl_betrag", scale = 2, precision = 12)
    open var zahlBetrag: BigDecimal? = null

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
    open var discountMaturity: Date? = null

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

    @get:PropertyInfo(i18nKey = "fibu.common.brutto")
    val grossSum: BigDecimal
        @Transient
        get() = RechnungCalculator.calculateGrossSum(this)

    @get:PropertyInfo(i18nKey = "fibu.common.netto")
    val netSum: BigDecimal
        @Transient
        get() = RechnungCalculator.calculateNetSum(this)

    val vatAmountSum: BigDecimal
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
            val today = DayHolder()
            return this.faelligkeit?.before(today.date) ?: false
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
        // recalculate the transient fields
        if (this.datum == null) {
            this.zahlungsZielInTagen = null
            this.discountZahlungsZielInTagen = null
            return
        }

        val date = DateHolder(this.datum)
        this.zahlungsZielInTagen = if (this.faelligkeit == null) null else date.daysBetween(this.faelligkeit)
        this.discountZahlungsZielInTagen = if (this.discountMaturity == null) null else date.daysBetween(this.discountMaturity)
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
        val nextNumber = abstractPositionen!!.maxBy { it.number }?.number?.plus(1)?.toShort() ?: 1
        position.number = nextNumber
        setRechnung(position)
        addPositionWithoutCheck(position)
    }

    abstract protected fun addPositionWithoutCheck(position: AbstractRechnungsPositionDO)

    abstract fun setRechnung(position: AbstractRechnungsPositionDO)

    abstract fun ensureAndGetPositionen(): MutableList<out AbstractRechnungsPositionDO>

    /**
     * Gibt es mindestens eine Kostzuweisung?
     */
    fun hasKostZuweisungen(): Boolean {
        return abstractPositionen?.any {
            !it.kostZuweisungen.isNullOrEmpty()
        } ?: false
    }
}
