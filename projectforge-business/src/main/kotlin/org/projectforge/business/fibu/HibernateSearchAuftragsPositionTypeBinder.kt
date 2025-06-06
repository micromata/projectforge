/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.hibernate.search.engine.backend.document.DocumentElement
import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext
import org.projectforge.business.common.BaseUserGroupRightsDO
import org.projectforge.business.teamcal.admin.model.HibernateSearchUsersGroupsBridge

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchAuftragsPositionTypeBinder : TypeBinder {
    override fun bind(context: TypeBindingContext) {
        context.dependencies().useRootOnly()

        context.indexSchemaElement()
            .field("position") { f -> f.asString() }
            .toReference()

        val bridge: TypeBridge<AuftragsPositionDO> = HibernateSearchAuftragsPositionBridge()
        context.bridge(AuftragsPositionDO::class.java, bridge)
    }
}
