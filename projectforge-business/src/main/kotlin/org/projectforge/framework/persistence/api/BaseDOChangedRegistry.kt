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

package org.projectforge.framework.persistence.api

import mu.KotlinLogging
import org.projectforge.framework.access.OperationType

private val log = KotlinLogging.logger {}

class BaseDOChangedRegistry<O : ExtendedBaseDO<Long>> : BaseDOChangedListener<O> {
    private val objectChangedListeners = mutableListOf<BaseDOChangedListener<O>>()

    /**
     * Registers a [BaseDOChangedListener].
     * @param objectChangedListener The listener to register.
     * @see BaseDOChangedListener
     */
    fun register(objectChangedListener: BaseDOChangedListener<O>) {
        log.info(javaClass.simpleName + ": Registering " + objectChangedListener.javaClass.name)
        objectChangedListeners.add(objectChangedListener)
    }

    /**
     * Calls for all registered [BaseDOChangedListener.afterLoad].
     * @see BaseDOChangedListener.afterLoad
     */
    override fun afterLoad(obj: O) {
        objectChangedListeners.forEach { it.afterLoad(obj) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.onInsertOrModify].
     * @see BaseDOChangedListener.onInsertOrModify
     */
    override fun onInsertOrModify(obj: O, operationType: OperationType) {
        objectChangedListeners.forEach { it.onInsertOrModify(obj, operationType) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.beforeInsertOrModify].
     * @see BaseDOChangedListener.beforeInsertOrModify
     */
    override fun beforeInsertOrModify(obj: O, operationType: OperationType) {
        objectChangedListeners.forEach { it.beforeInsertOrModify(obj, operationType) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.afterInsertOrModify].
     * @see BaseDOChangedListener.afterInsertOrModify
     */
    override fun afterInsertOrModify(changedObject: O, operationType: OperationType) {
        objectChangedListeners.forEach { it.afterInsertOrModify(changedObject, operationType) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.onInsert].
     * @see BaseDOChangedListener.onInsert
     */
    override fun onInsert(obj: O) {
        objectChangedListeners.forEach { it.onInsert(obj) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.afterInsert].
     * @see BaseDOChangedListener.afterInsert
     */
    override fun afterInsert(obj: O) {
        objectChangedListeners.forEach { it.afterInsert(obj) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.afterUpdate].
     * @see BaseDOChangedListener.afterUpdate
     */
    override fun afterUpdate(obj: O, dbObj: O?, isModified: Boolean) {
        objectChangedListeners.forEach { it.afterUpdate(obj, dbObj, isModified) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.onUpdate].
     * @see BaseDOChangedListener.onUpdate
     */
    override fun onUpdate(obj: O, dbObj: O) {
        objectChangedListeners.forEach { it.onUpdate(obj, dbObj) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.onDelete].
     * @see BaseDOChangedListener.onDelete
     */
    override fun onDelete(obj: O) {
        objectChangedListeners.forEach { it.onDelete(obj) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.afterDelete].
     * @see BaseDOChangedListener.afterDelete
     */
    override fun afterDelete(obj: O) {
        objectChangedListeners.forEach { it.afterDelete(obj) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.onUndelete].
     * @see BaseDOChangedListener.onUndelete
     */
    override fun onUndelete(obj: O) {
        objectChangedListeners.forEach { it.onUndelete(obj) }
    }

    /**
     * Calls for all registered [BaseDOChangedListener.afterUndelete].
     * @see BaseDOChangedListener.afterUndelete
     */
    override fun afterUndelete(obj: O) {
        objectChangedListeners.forEach { it.afterUndelete(obj) }
    }
}
