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

import com.fasterxml.jackson.annotation.JsonManagedReference
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.history.PersistenceBehavior
import java.time.LocalDate

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
@Table(
    name = "t_fibu_rechnung",
    indexes = [
        jakarta.persistence.Index(name = "idx_fk_t_fibu_rechnung_konto_id", columnList = "konto_id"),
        jakarta.persistence.Index(name = "idx_fk_t_fibu_rechnung_kunde_id", columnList = "kunde_id"),
        jakarta.persistence.Index(name = "idx_fk_t_fibu_rechnung_projekt_id", columnList = "projekt_id")]
)
/*@WithHistory(
  noHistoryProperties = ["lastUpdate", "created"],
  nestedEntities = [RechnungsPositionDO::class]
)*/
@NamedQueries(
    NamedQuery(name = RechnungDO.SELECT_MIN_MAX_DATE, query = "select min(datum), max(datum) from RechnungDO"),
    NamedQuery(name = RechnungDO.FIND_OTHER_BY_NUMMER, query = "from RechnungDO where nummer=:nummer and id!=:id")
)
open class RechnungDO : AbstractRechnungDO(), Comparable<RechnungDO> {

    @PropertyInfo(i18nKey = "fibu.rechnung.nummer")
    @GenericField // was: @FullTextField(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @get:Column(nullable = true)
    open var nummer: Int? = null

    /**
     * Rechnungsempfänger. Dieser Kunde kann vom Kunden, der mit dem Projekt verbunden ist abweichen.
     */
    @PropertyInfo(i18nKey = "fibu.kunde")
    @IndexedEmbedded(includeDepth = 1)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kunde_id", nullable = true)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var kunde: KundeDO? = null

    /**
     * Freitextfeld, falls Kunde nicht aus Liste gewählt werden kann bzw. für Rückwärtskompatibilität mit alten Kunden.
     */
    @PropertyInfo(i18nKey = "fibu.kunde.text")
    @FullTextField
    @get:Column(name = "kunde_text")
    open var kundeText: String? = null

    @PropertyInfo(i18nKey = "fibu.projekt")
    @IndexedEmbedded(includeDepth = 2)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projekt_id", nullable = true)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var projekt: ProjektDO? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.status")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var status: RechnungStatus? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.typ")
    @FullTextField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 40)
    open var typ: RechnungTyp? = null

    @PropertyInfo(i18nKey = "fibu.customerref1")
    @FullTextField
    @get:Column(name = "customerref1")
    open var customerref1: String? = null

    @PropertyInfo(i18nKey = "fibu.attachment")
    @FullTextField
    @get:Column(name = "attachment")
    open var attachment: String? = null

    @PropertyInfo(i18nKey = "fibu.customer.address")
    @FullTextField
    @get:Column(name = "customeraddress")
    open var customerAddress: String? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.from")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "period_of_performance_begin")
    open var periodOfPerformanceBegin: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.to")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "period_of_performance_end")
    open var periodOfPerformanceEnd: LocalDate? = null

    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    @JsonManagedReference
    @IndexedEmbedded(includeDepth = 3)
    @get:OneToMany(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
        orphanRemoval = false,
        fetch = FetchType.LAZY,
        mappedBy = "rechnung",
        targetEntity = RechnungsPositionDO::class,
    )
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    override var positionen: MutableList<RechnungsPositionDO>? = null

    override val abstractPositionen: List<AbstractRechnungsPositionDO>?
        @Transient
        get() = positionen

    /**
     *  @return true if the invoice is valid: isn't deleted and status is not GEPLANT or STORNIERT
     */
    override val isValid: Boolean
        @Transient
        get() = !deleted && status?.isIn(RechnungStatus.GEPLANT, RechnungStatus.STORNIERT) == false

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

    override fun setAbstractRechnung(position: AbstractRechnungsPositionDO) {
        position as RechnungsPositionDO
        position.rechnung = this
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
