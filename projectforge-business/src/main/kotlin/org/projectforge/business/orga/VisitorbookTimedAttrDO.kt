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

import javax.persistence.Column
import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorType
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO

@Entity
@Table(name = "t_orga_visitorbook_timedattr", uniqueConstraints = [UniqueConstraint(columnNames = ["parent", "propertyName"])])
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "withdata", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("0")
open class VisitorbookTimedAttrDO : JpaTabAttrBaseDO<VisitorbookTimedDO, Int> {

    constructor() : super() {}

    constructor(parent: VisitorbookTimedDO, propertyName: String, type: Char,
                value: String) : super(parent, propertyName, type, value) {
    }

    constructor(parent: VisitorbookTimedDO) : super(parent) {}

    /**
     * @see org.projectforge.framework.persistence.attr.entities.DeprAttrBaseDO.createData
     */
    override fun createData(data: String): VisitorbookTimedAttrDataDO {
        return VisitorbookTimedAttrDataDO(this, data)

    }

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent", referencedColumnName = "pk")
    override fun getParent(): VisitorbookTimedDO {
        return super.getParent()

    }

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getPk(): Int? {
        return pk
    }

}
