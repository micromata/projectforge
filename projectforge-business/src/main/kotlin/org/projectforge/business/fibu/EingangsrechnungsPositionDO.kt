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
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.hibernate.search.annotations.Indexed
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import javax.persistence.*

/**
 * Repräsentiert eine Position innerhalb einer Eingangsrechnung.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_fibu_eingangsrechnung_position",
        uniqueConstraints = [UniqueConstraint(columnNames = ["eingangsrechnung_fk", "number"])],
        indexes = [Index(name = "idx_fk_t_fibu_eingangsrechnung_position_eingangsrechnung_fk", columnList = "eingangsrechnung_fk"), Index(name = "idx_fk_t_fibu_eingangsrechnung_position_tenant_id", columnList = "tenant_id")])
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class EingangsrechnungsPositionDO : AbstractRechnungsPositionDO() {
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "eingangsrechnung_fk", nullable = false)
    open var eingangsrechnung: EingangsrechnungDO? = null

    override val rechnungId: Int?
        @Transient
        get() = eingangsrechnung?.id

    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @get:JoinColumn(name = "eingangsrechnungs_pos_fk")
    @get:OrderColumn(name = "index")
    @JsonBackReference
    override var kostZuweisungen: MutableList<KostZuweisungDO>? = null

    override fun checkKostZuweisungId(zuweisung: KostZuweisungDO): Boolean {
        return zuweisung.eingangsrechnungsPositionId == this.id
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
