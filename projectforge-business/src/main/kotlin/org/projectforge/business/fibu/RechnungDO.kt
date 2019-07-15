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

import com.fasterxml.jackson.annotation.JsonBackReference
import de.micromata.genome.db.jpa.history.api.WithHistory
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.annotations.*
import org.hibernate.search.bridge.builtin.IntegerBridge
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import java.math.BigDecimal
import java.sql.Date
import java.util.*
import javax.persistence.*

/**
 * Geplante und gestellte Rechnungen.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
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
class RechnungDO : AbstractRechnungDO<RechnungsPositionDO>(), Comparable<RechnungDO> {

    @PropertyInfo(i18nKey = "fibu.rechnung.nummer")
    @Field(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @get:Column(nullable = true)
    var nummer: Int? = null

    /**
     * Rechnungsempfänger. Dieser Kunde kann vom Kunden, der mit dem Projekt verbunden ist abweichen.
     */
    @PropertyInfo(i18nKey = "fibu.kunde")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kunde_id", nullable = true)
    var kunde: KundeDO? = null

    /**
     * Freitextfeld, falls Kunde nicht aus Liste gewählt werden kann bzw. für Rückwärtskompatibilität mit alten Kunden.
     */
    @PropertyInfo(i18nKey = "fibu.kunde")
    @Field
    @get:Column(name = "kunde_text")
    var kundeText: String? = null

    @PropertyInfo(i18nKey = "fibu.projekt")
    @IndexedEmbedded(depth = 2)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "projekt_id", nullable = true)
    var projekt: ProjektDO? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.status")
    @Field(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    var status: RechnungStatus? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.typ")
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 40)
    var typ: RechnungTyp? = null

    @PropertyInfo(i18nKey = "fibu.customerref1")
    @Field
    @get:Column(name = "customerref1")
    var customerref1: String? = null

    @PropertyInfo(i18nKey = "fibu.attachment")
    @Field
    @get:Column(name = "attachment")
    var attachment: String? = null

    @PropertyInfo(i18nKey = "fibu.customer.address")
    @Field
    @get:Column(name = "customeraddress")
    var customerAddress: String? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.from")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_begin")
    var periodOfPerformanceBegin: Date? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.to")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_end")
    var periodOfPerformanceEnd: Date? = null

    val kundeId: Int?
        @Transient
        get() = if (this.kunde == null) {
            null
        } else kunde!!.id

    val projektId: Int?
        @Transient
        get() = if (this.projekt == null) {
            null
        } else projekt!!.id

    /**
     * (this.status == RechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null)
     */
    override val isBezahlt: Boolean
        @Transient
        get() = if (this.netSum.compareTo(BigDecimal.ZERO) == 0) {
            true
        } else this.status == RechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null

    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @JsonBackReference
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "rechnung", targetEntity = RechnungsPositionDO::class)
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    override var positionen: MutableList<RechnungsPositionDO>? = null

    val auftragsPositionVOs: Set<AuftragsPositionVO>?
        @Transient
        get() {
            if (this.positionen == null) {
                return null
            }
            var set: MutableSet<AuftragsPositionVO>? = null
            for (pos in this.positionen!!) {
                if (pos.auftragsPosition == null) {
                    continue
                } else if (set == null) {
                    set = TreeSet()
                }
                set.add(AuftragsPositionVO(pos.auftragsPosition))
            }
            return set
        }

    /**
     * @see KundeFormatter.formatKundeAsString
     */
    val kundeAsString: String
        @Transient
        get() = KundeFormatter.formatKundeAsString(this.kunde, this.kundeText)

    override fun setRechnung(position: RechnungsPositionDO) {
        position.rechnung = this
    }

    override fun compareTo(other: RechnungDO): Int {
        if (this.datum != null && other.datum != null) {
            val r = other.datum!!.compareTo(this.datum!!)
            if (r != 0) {
                return r
            }

        }
        if (this.nummer == null) {
            return if (other.nummer == null) 0 else 1
        }
        return if (other.nummer == null) {
            -1
        } else this.nummer!!.compareTo(other.nummer!!)
    }
}
