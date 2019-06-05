/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import java.math.BigDecimal
import java.sql.Date
import java.util.ArrayList

import javax.persistence.Column
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass
import javax.persistence.Transient

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.annotations.Analyze
import org.hibernate.search.annotations.DateBridge
import org.hibernate.search.annotations.EncodingType
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Index
import org.hibernate.search.annotations.IndexedEmbedded
import org.hibernate.search.annotations.Resolution
import org.hibernate.search.annotations.Store
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.time.DateHolder
import org.projectforge.framework.time.DayHolder
import org.projectforge.framework.xstream.XmlObjectReader

import de.micromata.genome.db.jpa.history.api.NoHistory
import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist

@MappedSuperclass
@JpaXmlPersist(beforePersistListener = [AbstractRechnungXmlBeforePersistListener::class], persistAfter = [Kost2ArtDO::class])
abstract class AbstractRechnungDO<T : AbstractRechnungsPositionDO> : DefaultBaseDO() {

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
    @Transient
    @get:Transient
    open var zahlungsZielInTagen: Int? = null

    /**
     * Wird nur zur Berechnung benutzt und kann für die Anzeige aufgerufen werden. Vorher sollte recalculate aufgerufen
     * werden.
     */
    @Field(analyze = Analyze.NO)
    @Transient
    @get:Transient
    open var discountZahlungsZielInTagen: Int? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.bezahlDatum")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "bezahl_datum")
    open var bezahlDatum: Date? = null

    /**
     * Bruttobetrag, den der Kunde bezahlt hat.
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
    var konto: KontoDO? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.discountPercent")
    @get:Column
    open var discountPercent: BigDecimal? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.discountMaturity")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column
    open var discountMaturity: Date? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.receiver")
    @Field
    @get:Column
    open var receiver: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.iban")
    @Field
    @get:Column(length = 50)
    open var iban: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.bic")
    @Field
    @get:Column(length = 11)
    open var bic: String? = null

    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @Transient
    @get:Transient
    open var positionen: MutableList<T>? = null

    /**
     * The user interface status of an invoice. The [RechnungUIStatus] is stored as XML.
     */
    @NoHistory
    @get:Column(name = "ui_status_as_xml", length = 10000)
    open var uiStatusAsXml: String? = null

    @NoHistory
    open val uiStatus: RechnungUIStatus? = null
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

    val grossSum: BigDecimal
        @Transient
        get() {
            var brutto = BigDecimal.ZERO
            if (this.positionen != null) {
                for (position in this.positionen!!) {
                    brutto = brutto.add(position.bruttoSum)
                }
            }
            return brutto
        }

    val netSum: BigDecimal
        @Transient
        get() {
            var netto = BigDecimal.ZERO
            if (this.positionen != null) {
                for (position in this.positionen!!) {
                    netto = netto.add(position.netSum)
                }
            }
            return netto
        }

    val vatAmountSum: BigDecimal
        @Transient
        get() {
            var vatAmount = BigDecimal.ZERO
            if (this.positionen != null) {
                for (position in this.positionen!!) {
                    vatAmount = vatAmount.add(position.vatAmount)
                }
            }
            return vatAmount
        }

    @get:Transient
    abstract val isBezahlt: Boolean

    val isUeberfaellig: Boolean
        @Transient
        get() {
            if (isBezahlt == true) {
                return false
            }
            val today = DayHolder()
            return this.faelligkeit == null || this.faelligkeit!!.before(today.date) == true
        }

    val kontoId: Int?
        @Transient
        get() = if (konto != null) konto!!.id else null

    /**
     * @return The total sum of all cost assignment net amounts of all positions.
     */
    val kostZuweisungenNetSum: BigDecimal
        @Transient
        get() {
            if (this.positionen == null) {
                return BigDecimal.ZERO
            }
            var netSum = BigDecimal.ZERO
            for (pos in this.positionen!!) {
                if (CollectionUtils.isNotEmpty(pos.kostZuweisungen)) {
                    for (zuweisung in pos.kostZuweisungen) {
                        if (zuweisung.netto != null) {
                            netSum = netSum.add(zuweisung.netto)
                        }
                    }
                }
            }
            return netSum
        }

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
    fun getPosition(idx: Int): T? {
        if (positionen == null) {
            return null
        }
        return if (idx >= positionen!!.size) { // Index out of bounds.
            null
        } else positionen!![idx]
    }

    fun addPosition(position: T): AbstractRechnungDO<T> {
        ensureAndGetPositionen()
        var number: Short = 1
        for (pos in positionen!!) {
            if (pos.getNumber() >= number) {
                number = pos.getNumber()
                number++
            }
        }
        position.setNumber(number)
        position.rechnung = this
        this.positionen!!.add(position)
        return this
    }

    fun ensureAndGetPositionen(): List<T> {
        run {
            if (this.positionen == null) {
                positionen = ArrayList()
            }
            return positionen as MutableList<T>
        }
    }

    fun hasKostZuweisungen(): Boolean {
        if (this.positionen == null) {
            return false
        }
        for (pos in this.positionen!!) {
            if (CollectionUtils.isNotEmpty(pos.kostZuweisungen)) {
                return true
            }
        }
        return false
    }
}
