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

package org.projectforge.business.orga

import java.io.Serializable

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MapKey
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.UniqueConstraint

import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.IdObject

import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO
import de.micromata.genome.db.jpa.tabattr.entities.TimeableBaseDO

@Entity
@Indexed
@Table(name = "t_orga_visitorbook_timed", uniqueConstraints = [UniqueConstraint(columnNames = ["visitor_id", "group_name", "start_time"])], indexes = [Index(name = "idx_orga_visitorbook_timed_start_time", columnList = "start_time")])
class VisitorbookTimedDO : TimeableBaseDO<VisitorbookTimedDO, Int>(), TimeableAttrRow<Int>, IdObject<Int> {

    /**
     * @return Zugehöriger Mitarbeiter.
     */
    @PropertyInfo(i18nKey = "orga.visitorbook")
    @IndexedEmbedded(depth = 2)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "visitor_id", nullable = false)
    var visitor: VisitorbookDO? = null

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getPk(): Int? {
        return pk
    }

    @Transient
    override fun getId(): Int? {
        return pk
    }

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "parent", targetEntity = VisitorbookTimedAttrDO::class, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "propertyName")
    override fun getAttributes(): Map<String, JpaTabAttrBaseDO<VisitorbookTimedDO, Int>> {
        return super.getAttributes()
    }

    @Transient
    override fun getAttrEntityClass(): Class<out JpaTabAttrBaseDO<VisitorbookTimedDO, out Serializable>> {
        return VisitorbookTimedAttrDO::class.java
    }

    @Transient
    override fun getAttrEntityWithDataClass(): Class<out JpaTabAttrBaseDO<VisitorbookTimedDO, out Serializable>> {
        return VisitorbookTimedAttrWithDataDO::class.java
    }

    @Transient
    override fun getAttrDataEntityClass(): Class<out JpaTabAttrDataBaseDO<out JpaTabAttrBaseDO<VisitorbookTimedDO, Int>, Int>> {
        return VisitorbookTimedAttrDataDO::class.java
    }

    override fun createAttrEntity(key: String, type: Char, value: String): JpaTabAttrBaseDO<VisitorbookTimedDO, Int> {
        return VisitorbookTimedAttrDO(this, key, type, value)
    }

    override fun createAttrEntityWithData(key: String, type: Char, value: String): JpaTabAttrBaseDO<VisitorbookTimedDO, Int> {
        return VisitorbookTimedAttrWithDataDO(this, key, type, value)
    }

}
