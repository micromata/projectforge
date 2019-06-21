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
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO

@Entity
@Table(name = "t_orga_visitorbook_timedattrdata")
class VisitorbookTimedAttrDataDO : JpaTabAttrDataBaseDO<VisitorbookTimedAttrDO, Int> {

    constructor() : super() {}

    constructor(parent: VisitorbookTimedAttrDO, value: String) : super(parent, value) {}

    constructor(parent: VisitorbookTimedAttrDO) : super(parent) {}

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getPk(): Int? {
        return pk
    }

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_id", referencedColumnName = "pk")
    override fun getParent(): VisitorbookTimedAttrDO {
        return super.getParent()
    }

}
