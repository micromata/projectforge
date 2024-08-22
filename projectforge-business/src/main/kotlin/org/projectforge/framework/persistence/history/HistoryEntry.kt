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

package org.projectforge.framework.persistence.history

import org.projectforge.framework.persistence.api.IdObject
import java.io.Serializable
import java.util.*

/**
 * An change for an Entity.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
interface HistoryEntry<I : Serializable> : IdObject<I> {
    /**
     * Gets the modified at.
     *
     * @return the modified at
     */
    val modifiedAt: Date?

    /**
     * Gets the modified by.
     *
     * @return the modified by
     */
    val modifiedBy: String?

    val userName: String?
        /**
         * alias to getModifiedBy.
         *
         * @return the user which modified the entry
         */
        get() = modifiedBy

    /**
     * Gets the diff entries.
     *
     * @return the diff entries
     */
    val diffEntries: List<DiffEntry>?

    /**
     * Gets the entity op type.
     *
     * @return the entity op type
     */
    val entityOpType: EntityOpType?

    /**
     * Gets the entity name.
     *
     * @return the entity name
     */
    val entityName: String?

    /**
     * Gets the entity id.
     *
     * @return the entity id
     */
    val entityId: Long?
}
