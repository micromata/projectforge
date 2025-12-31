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

package org.projectforge.business.fibu

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonManagedReference
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.history.PersistenceBehavior

/**
 * Repräsentiert eine Position innerhalb einer Eingangsrechnung.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "t_fibu_eingangsrechnung_position",
    uniqueConstraints = [UniqueConstraint(columnNames = ["eingangsrechnung_fk", "number"])],
    indexes = [Index(
        name = "idx_fk_t_fibu_eingangsrechnung_position_eingangsrechnung_fk",
        columnList = "eingangsrechnung_fk"
    )]
)
open class EingangsrechnungsPositionDO : AbstractRechnungsPositionDO() {

    @JsonBackReference
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "eingangsrechnung_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var eingangsrechnung: EingangsrechnungDO? = null

    override val rechnungId: Long?
        @Transient
        get() = eingangsrechnung?.id

    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(
        mappedBy = "eingangsrechnungsPosition",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
        orphanRemoval = false,
        fetch = FetchType.LAZY,
    )
    @get:OrderColumn(name = "index")
    @JsonManagedReference
    override var kostZuweisungen: MutableList<KostZuweisungDO>? = null

    override fun checkKostZuweisungId(zuweisung: KostZuweisungDO): Boolean {
        return zuweisung.eingangsrechnungsPosition?.id == this.id
    }

    /**
     * Clones this including cost assignments and order position (without id's).
     *
     * @return
     */
    fun newClone(): EingangsrechnungsPositionDO {
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

    fun newInstance(): EingangsrechnungsPositionDO {
        return EingangsrechnungsPositionDO()
    }
}
