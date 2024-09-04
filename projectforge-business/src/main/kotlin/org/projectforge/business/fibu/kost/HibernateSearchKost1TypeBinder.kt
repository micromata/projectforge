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

package org.projectforge.business.fibu.kost

import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchKost1TypeBinder : TypeBinder {
    override fun bind(context: TypeBindingContext) {
        context.dependencies()
            // It doesn't matter if these fields are indexed by Hibernate Search:
            .use("nummernkreis")
            .use("bereich")
            .use("teilbereich")
            .use("endziffer")

        val bridge: TypeBridge<Kost1DO> = HibernateSearchKost1Bridge()
        context.bridge(Kost1DO::class.java, bridge)
    }
}
