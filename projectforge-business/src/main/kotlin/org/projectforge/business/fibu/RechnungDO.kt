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

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import de.micromata.genome.db.jpa.history.api.WithHistory
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.annotations.*
import org.hibernate.search.bridge.builtin.IntegerBridge
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import java.math.BigDecimal
import java.sql.Date
import javax.persistence.*

/**
 * Geplante und gestellte Rechnungen.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
//@Cacheable
//@Cache(region = "invoices", usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "t_fibu_rechnung",
        uniqueConstraints = [UniqueConstraint(columnNames = ["nummer", "tenant_id"])],
        indexes = [
            javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_konto_id", columnList = "konto_id"),
            javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_kunde_id", columnList = "kunde_id"),
            javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_projekt_id", columnList = "projekt_id"),
            javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_tenant_id", columnList = "tenant_id")
        ])
@WithHistory(noHistoryProperties = ["lastUpdate", "created"],
        nestedEntities = [RechnungsPositionDO::class])
@NamedQueries(
        NamedQuery(name = RechnungDO.SELECT_MIN_MAX_DATE, query = "select min(datum), max(datum) from RechnungDO"),
        NamedQuery(name = RechnungDO.FIND_OTHER_BY_NUMMER, query = "from RechnungDO where nummer=:nummer and id!=:id"))
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class RechnungDO : AbstractRechnungDO(), Comparable<RechnungDO> {

    @PropertyInfo(i18nKey = "fibu.rechnung.nummer")
    @Field(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @get:Column(nullable = true)
    open var nummer: Int? = null

    /**
     * Rechnungsempfänger. Dieser Kunde kann vom Kunden, der mit dem Projekt verbunden ist abweichen.
     */
    @PropertyInfo(i18nKey = "fibu.kunde")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kunde_id", nullable = true)
    open var kunde: KundeDO? = null

    /**
     * Freitextfeld, falls Kunde nicht aus Liste gewählt werden kann bzw. für Rückwärtskompatibilität mit alten Kunden.
     */
    @PropertyInfo(i18nKey = "fibu.kunde")
    @Field
    @get:Column(name = "kunde_text")
    open var kundeText: String? = null

    @PropertyInfo(i18nKey = "fibu.projekt")
    @IndexedEmbedded(depth = 2)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projekt_id", nullable = true)
    open var projekt: ProjektDO? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.status")
    @Field(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var status: RechnungStatus? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.typ")
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 40)
    open var typ: RechnungTyp? = null

    @PropertyInfo(i18nKey = "fibu.customerref1")
    @Field
    @get:Column(name = "customerref1")
    open var customerref1: String? = null

    @PropertyInfo(i18nKey = "fibu.attachment")
    @Field
    @get:Column(name = "attachment")
    open var attachment: String? = null

    @PropertyInfo(i18nKey = "fibu.customer.address")
    @Field
    @get:Column(name = "customeraddress")
    open var customerAddress: String? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.from")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_begin")
    open var periodOfPerformanceBegin: Date? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.to")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_end")
    open var periodOfPerformanceEnd: Date? = null

    /**
     * (this.status == RechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null)
     */
    override val isBezahlt: Boolean
        @Transient
        get() = if (this.netSum.compareTo(BigDecimal.ZERO) == 0) {
            true
        } else this.status == RechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null


    val kundeId: Int?
        @Transient
        get() = if (this.kunde == null) {
            null
        } else kunde!!.nummer

    val projektId: Int?
        @Transient
        get() = if (this.projekt == null) {
            null
        } else projekt!!.id


    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @IndexedEmbedded(depth = 3)
    @get:OneToMany(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, mappedBy = "rechnung", targetEntity = RechnungsPositionDO::class)
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    open var positionen: MutableList<RechnungsPositionDO>? = null

    override val abstractPositionen: List<AbstractRechnungsPositionDO>?
        @Transient
        get() = positionen

    override fun ensureAndGetPositionen(): MutableList<out AbstractRechnungsPositionDO> {
        if (this.positionen == null) {
            positionen = mutableListOf()
        }
        return positionen!!
    }

    override fun addPositionWithoutCheck(position: AbstractRechnungsPositionDO) {
        position as RechnungsPositionDO
        this.positionen!!.add(position)
        position.rechnung = this
    }

    override fun setRechnung(position: AbstractRechnungsPositionDO) {
        position as RechnungsPositionDO
        position.rechnung = this
    }

    val auftragsPositionVOs: Set<AuftragsPositionVO>?
        @Transient
        get() {
            val result = mutableSetOf<AuftragsPositionVO>()
            this.positionen?.forEach {
                val auftragsPosition = it.auftragsPosition
                if (auftragsPosition != null)
                    result.add(AuftragsPositionVO(auftragsPosition))
            }
            return result
        }

    /**
     * @see KundeFormatter.formatKundeAsString
     */
    val kundeAsString: String
        @Transient
        get() = KundeFormatter.formatKundeAsString(this.kunde, this.kundeText)

    fun setRechnung(position: RechnungsPositionDO) {
        position.rechnung = this
    }

    override fun compareTo(other: RechnungDO): Int {
        val cmp = compareValues(this.datum, other.datum)
        if (cmp != 0) return cmp
        return compareValues(this.nummer, other.nummer)
    }

    companion object {
        internal const val SELECT_MIN_MAX_DATE = "RechnungDO_SelectMinMaxDate"
        internal const val FIND_OTHER_BY_NUMMER = "RechnungDO_FindOtherByNummer"
    }
}
