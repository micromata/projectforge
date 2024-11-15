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

package org.projectforge.business.task

import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder


/**
 * TaskPathBridge for hibernate search to search in the parent task titles.
 * https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#_classbridge
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchTaskPathTypeBinder : TypeBinder {
    override fun bind(context: TypeBindingContext) {
        context.dependencies().useRootOnly()

        context.indexSchemaElement()
            .field("taskpath") { f -> f.asString() }
            .toReference()

        val bridge: TypeBridge<TaskDO> = HibernateSearchTaskPathBridge()
        context.bridge(TaskDO::class.java, bridge)
    }
}
