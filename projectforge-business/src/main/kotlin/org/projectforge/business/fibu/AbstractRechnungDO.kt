/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.persistence.*
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.persistence.candh.CandHIgnore
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.xmlstream.XmlObjectReader
import java.math.BigDecimal
import java.time.LocalDate

@MappedSuperclass
abstract class AbstractRechnungDO : DefaultBaseDO(), IRechnung {
    @PropertyInfo(i18nKey = "fibu.rechnung.datum")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(nullable = false)
    open var datum: LocalDate? = null

    @get:PropertyInfo(i18nKey = "calendar.year")
    @get:Transient
    @get:GenericField
    @get:IndexingDependency(derivedFrom = [ObjectPath(PropertyValue(propertyName = "datum"))])
    open val year: Int
        get() = datum?.year ?: 0

    @PropertyInfo(i18nKey = "fibu.rechnung.betreff")
    @FullTextField
    @get:Column(length = 4000)
    open var betreff: String? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = 4000)
    open var bemerkung: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.besonderheiten")
    @FullTextField
    @get:Column(length = 4000)
    open var besonderheiten: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.faelligkeit")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column
    open var faelligkeit: LocalDate? = null

    /**
     * Wird nur zur Berechnung benutzt und kann für die Anzeige aufgerufen werden. Vorher sollte recalculate aufgerufen
     * werden.
     */
    @PropertyInfo(i18nKey = "fibu.rechnung.zahlungsZiel")
    @get:Transient
    open var zahlungsZielInTagen: Int? = null

    /**
     * Wird nur zur Berechnung benutzt und kann für die Anzeige aufgerufen werden. Vorher sollte recalculate aufgerufen
     * werden.
     */
    @get:Transient
    open var discountZahlungsZielInTagen: Int? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.bezahlDatum")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "bezahl_datum")
    open var bezahlDatum: LocalDate? = null

    /**
     * Bruttobetrag, der tatsächlich bezahlt wurde.
     */
    @PropertyInfo(i18nKey = "fibu.rechnung.zahlBetrag", type = PropertyType.CURRENCY)
    @get:Column(name = "zahl_betrag", scale = 2, precision = 12)
    override var zahlBetrag: BigDecimal? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.currency")
    @get:Column(length = 10)
    override var currency: String? = null

    /**
     * This Datev account number is used for the exports of invoices. For debitor invoices (RechnungDO): If not given then
     * the account number assigned to the ProjektDO if set or KundeDO is used instead (default).
     */
    @PropertyInfo(i18nKey = "fibu.konto")
    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
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
     * True, if invoice is issued and not canceled, deleted or only planned.
     */
    @get:Transient
    abstract val isValid: Boolean

    /**
     * The user interface status of an invoice. The [RechnungUIStatus] is stored as XML.
     */
    @NoHistory
    @get:Column(name = "ui_status_as_xml", length = 10000)
    open var uiStatusAsXml: String? = null

    @NoHistory
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

    /**
     * Must be set via [RechnungCalculator.calculate].
     */
    @get:Transient
    @CandHIgnore // Do not handle it by canh (it might not be initialized).
    lateinit var info: RechnungInfo

    internal val isInfoInitialized: Boolean
        @Transient
        get() = this::info.isInitialized

    /**
     * Ensures that [info] is initialized by calling [RechnungCalculator.ensureInfo] if needed.
     * Always returns a non-null [RechnungInfo] instance.
     */
    val ensuredInfo: RechnungInfo
        @Transient
        get() {
            if (!isInfoInitialized) {
                RechnungCalculator.calculate(this)
            }
            return info
        }

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
     * @param idx (0 - based)
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

    protected abstract fun addPositionWithoutCheck(position: AbstractRechnungsPositionDO)

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
}
