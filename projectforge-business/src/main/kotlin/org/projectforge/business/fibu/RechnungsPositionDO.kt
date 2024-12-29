/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonManagedReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.history.PersistenceBehavior
import java.time.LocalDate

/**
 * Repräsentiert eine Position innerhalb eine Rechnung.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "t_fibu_rechnung_position",
    uniqueConstraints = [UniqueConstraint(columnNames = ["rechnung_fk", "number"])],
    indexes = [jakarta.persistence.Index(
        name = "idx_fk_t_fibu_rechnung_position_auftrags_position_fk",
        columnList = "auftrags_position_fk"
    ), jakarta.persistence.Index(name = "idx_fk_t_fibu_rechnung_position_rechnung_fk", columnList = "rechnung_fk")]
)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class RechnungsPositionDO : AbstractRechnungsPositionDO() {
    @JsonBackReference
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "rechnung_fk", nullable = false)
    @get:AssociationInverseSide(inversePath = ObjectPath(PropertyValue(propertyName = "rechnung")))
    @get:IndexingDependency(derivedFrom = [ObjectPath(PropertyValue(propertyName = "positionen"))])
    open var rechnung: RechnungDO? = null

    override val rechnungId: Long?
        @Transient
        get() = rechnung?.id

    @PropertyInfo(i18nKey = "fibu.auftrag.position")
    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "auftrags_position_fk")
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    open var auftragsPosition: AuftragsPositionDO? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.type")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "period_of_performance_type", length = 10)
    open var periodOfPerformanceType: PeriodOfPerformanceType? = PeriodOfPerformanceType.SEEABOVE

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.from")
    @get:Column(name = "period_of_performance_begin")
    open var periodOfPerformanceBegin: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.to")
    @get:Column(name = "period_of_performance_end")
    open var periodOfPerformanceEnd: LocalDate? = null

    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
        orphanRemoval = false,
        fetch = FetchType.LAZY,
    )
    @get:JoinColumn(name = "rechnungs_pos_fk")
    @get:OrderColumn(name = "index")
    @JsonManagedReference
    override var kostZuweisungen: MutableList<KostZuweisungDO>? = null

    override fun checkKostZuweisungId(zuweisung: KostZuweisungDO): Boolean {
        return zuweisung.rechnungsPosition?.id == this.id
    }

    /**
     * Clones this including cost assignments and order position (without id's).
     *
     * @return
     */
    fun newClone(): RechnungsPositionDO {
        val rechnungsPosition = newInstance()
        rechnungsPosition.copyValuesFrom(this, "id", "kostZuweisungen")
        if (this.kostZuweisungen != null) {
            for (origKostZuweisung in this.kostZuweisungen!!) {
                val kostZuweisung = origKostZuweisung.newClone()
                rechnungsPosition.addKostZuweisung(kostZuweisung)
            }
        }
        return rechnungsPosition
    }

    fun newInstance(): RechnungsPositionDO {
        return RechnungsPositionDO()
    }
}
