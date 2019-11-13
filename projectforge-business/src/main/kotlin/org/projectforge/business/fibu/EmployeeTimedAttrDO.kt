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
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO
import javax.persistence.*

/**
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 */
@Entity
@Table(name = "t_fibu_employee_timedattr")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "withdata", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("0")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "pk")
open class EmployeeTimedAttrDO : JpaTabAttrBaseDO<EmployeeTimedDO, Int> {
    constructor() : super()

    constructor(parent: EmployeeTimedDO, propertyName: String, type: Char,
                value: String) : super(parent, propertyName, type, value)

    constructor(parent: EmployeeTimedDO) : super(parent)

    override fun createData(data: String): EmployeeTimedAttrDataDO {
        return EmployeeTimedAttrDataDO(this, data)

    }

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent", referencedColumnName = "pk")
    override fun getParent(): EmployeeTimedDO {
        return super.getParent()

    }

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getPk(): Int? {
        return pk
    }

}
