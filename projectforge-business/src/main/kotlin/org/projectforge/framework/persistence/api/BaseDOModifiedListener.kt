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

package org.projectforge.framework.persistence.api

import org.projectforge.framework.access.OperationType

/**
 * BaseDOModifiedListener may be registered at BaseDao and will be called every time an object was changed (added, modified or deleted).
 * Useful for external caches.
 * This listener is also used by [BaseDao].
 * All calls are outside transaction.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
interface BaseDOModifiedListener<O : ExtendedBaseDO<Long>> {
    /**
     * This method will be called before access check of inserting and updating the object.
     * Called outside/before transaction.
     * Does nothing at default.
     * @param obj The object to insert or modify.
     */
    fun beforeInsertOrModify(obj: O, operationType: OperationType) {
    }

    /**
     * This method will be called after inserting, updating, deleting or marking the data object as deleted.
     * This method is for example needed for expiring the UserGroupCache after inserting or updating a user or group
     * data object.
     * Called outside/before transaction.
     * Does nothing at default.
     * @param obj The inserted or modified object.
     * @param operationType The operation type.
     */
    fun afterInsertOrModify(obj: O, operationType: OperationType) {
    }
}
