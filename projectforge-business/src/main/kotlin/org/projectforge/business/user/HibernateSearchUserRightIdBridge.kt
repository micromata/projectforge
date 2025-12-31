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

package org.projectforge.business.user

import org.hibernate.search.engine.backend.document.DocumentElement
import org.hibernate.search.engine.backend.document.IndexFieldReference
import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext

/**
 * UserRightId bridge for hibernate search uses the id string of UserRightId for search.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see UserRightId.getId
 */
class HibernateSearchUserRightIdBridge : TypeBridge<UserRightId> {
    private val idField: IndexFieldReference<String?>? = null

    override fun write(target: DocumentElement, bridgedElement: UserRightId, context: TypeBridgeWriteContext) {
        target.addValue(idField, bridgedElement.id)
    }
}
