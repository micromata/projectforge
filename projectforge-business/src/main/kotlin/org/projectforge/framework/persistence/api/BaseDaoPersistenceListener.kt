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

package org.projectforge.framework.persistence.api

import org.projectforge.framework.access.OperationType

/**
 * BaseDOPostChangedListener may be registered at BaseDao and will be called every time an object was changed (added, modified or deleted).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
interface BaseDaoPersistenceListener<O : ExtendedBaseDO<Long>> : BaseDOModifiedListener<O> {
    /**
     * This method will be called before inserting, updating, deleting or marking the data object as deleted.
     * Callsed after access check.
     * Called inside transaction.
     * Does nothing at default.
     * @param obj The object to insert or modify.
     */
    fun onInsertOrModify(obj: O, operationType: OperationType) {
    }

    /**
     * This method will be called after loading an object from the database.
     * Called outside transaction for lists (if called by afterLoad(list), inside transaction otherwise.
     * Does nothing at default.
     * @param obj The loaded object.
     */
    fun afterLoad(obj: O) {
    }

    /**
     * This method will be called after loading an object from the database.
     * Called outside transaction.
     * Does nothing at default.
     * @param list The loaded object list.
     * @return The given list itself. Override this method and return another list for avoiding lazy fetching (for example, see AuftragDao).
     */
    fun afterLoad(list: List<O>): List<O> {
        list.forEach { afterLoad(it) }
        return list
    }


    /**
     * This method will be called after inserting.
     * Called inside transaction.
     * Does nothing at default.
     * @param obj The inserted object
     */
    fun afterInsert(obj: O) {
    }

    /**
     * This method will be called before inserting, after access check.
     * Called inside transaction.
     * Does nothing at default.
     * @param obj The object to insert.
     */
    fun onInsert(obj: O) {
    }

    /**
     * This method will be called before updating the data object.
     * Will also called if in update method no modification was detected.
     * Callsed after access check.
     * Called inside transaction.
     * Does nothing at default.
     *
     * @param obj   The object to change.
     * @param dbObj The current database version of this object.
     */
    fun onUpdate(obj: O, dbObj: O) {
    }

    /**
     * This method will be called after updating. Does nothing at default.
     * Called inside transaction.
     * Does nothing at default.
     * @param obj        The modified object
     * @param dbObj      The object from database before modification. Only given, if [BaseDao.supportAfterUpdate] is true.
     * @param isModified is true if the object was changed, false if the object wasn't modified.
     */
    fun afterUpdate(obj: O, dbObj: O?, isModified: Boolean) {
    }

    /**
     * This method will be called before deleting.
     * Callsed after access check.
     * Called inside transaction.
     * Does nothing at default.
     *
     * @param obj The object to delete.
     */
    fun onDelete(obj: O) {
    }

    /**
     * This method will be called after deleting as well as after object is marked as deleted.
     * Called inside transaction.
     * Does nothing at default.
     * @param obj The deleted object.
     */
    fun afterDelete(obj: O) {
    }

    /**
     * This method will be called before undeleting.
     * Called inside transaction.
     * Does nothing at default.
     * @param obj The object to undelete.
     */
    fun onUndelete(obj: O) {
    }

    /**
     * This method will be called after undeleting.
     * Called inside transaction.
     * Does nothing at default.
     * @param obj The undeleted object.
     */
    fun afterUndelete(obj: O) {
    }
}
