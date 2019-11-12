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
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO
import de.micromata.genome.db.jpa.tabattr.entities.TimeableBaseDO
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.IdObject
import java.io.Serializable
import javax.persistence.*

/**
 *
 * Time pending attributes of an employee.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_fibu_employee_timed", uniqueConstraints = [UniqueConstraint(columnNames = ["employee_id", "group_name", "start_time"])], indexes = [Index(name = "idx_fibu_employee_timed_start_time", columnList = "start_time")])
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "pk")
class EmployeeTimedDO : TimeableBaseDO<EmployeeTimedDO, Int>(), TimeableAttrRow<Int>, IdObject<Int> {

    /**
     * @return Zugehöriger Mitarbeiter.
     */
    @PropertyInfo(i18nKey = "fibu.employee")
    @IndexedEmbedded(depth = 2)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "employee_id", nullable = false)
    var employee: EmployeeDO? = null

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

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "parent", targetEntity = EmployeeTimedAttrDO::class, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "propertyName")
    override fun getAttributes(): Map<String, JpaTabAttrBaseDO<EmployeeTimedDO, Int>> {
        return super.getAttributes()
    }

    @Transient
    override fun getAttrEntityClass(): Class<out JpaTabAttrBaseDO<EmployeeTimedDO, out Serializable>> {
        return EmployeeTimedAttrDO::class.java
    }

    @Transient
    override fun getAttrEntityWithDataClass(): Class<out JpaTabAttrBaseDO<EmployeeTimedDO, out Serializable>> {
        return EmployeeTimedAttrWithDataDO::class.java
    }

    @Transient
    override fun getAttrDataEntityClass(): Class<out JpaTabAttrDataBaseDO<out JpaTabAttrBaseDO<EmployeeTimedDO, Int>, Int>> {
        return EmployeeTimedAttrDataDO::class.java
    }

    override fun createAttrEntity(key: String, type: Char, value: String): JpaTabAttrBaseDO<EmployeeTimedDO, Int> {
        return EmployeeTimedAttrDO(this, key, type, value)
    }

    override fun createAttrEntityWithData(key: String, type: Char, value: String): JpaTabAttrBaseDO<EmployeeTimedDO, Int> {
        return EmployeeTimedAttrWithDataDO(this, key, type, value)
    }

}
