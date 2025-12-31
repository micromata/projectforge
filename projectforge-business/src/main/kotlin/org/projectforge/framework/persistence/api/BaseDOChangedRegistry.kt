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

import mu.KotlinLogging
import org.projectforge.framework.access.OperationType

private val log = KotlinLogging.logger {}

class BaseDOChangedRegistry<O : ExtendedBaseDO<Long>>(val baseDao: BaseDao<O>) : BaseDaoPersistenceListener<O> {
    private val objectChangedListeners = mutableListOf<BaseDOModifiedListener<O>>()

    /**
     * Registers a [BaseDOModifiedListener].
     * @param objectChangedListener The listener to register.
     * @see BaseDOModifiedListener
     */
    fun register(objectChangedListener: BaseDOModifiedListener<O>) {
        log.info(javaClass.simpleName + ": Registering " + objectChangedListener.javaClass.name)
        objectChangedListeners.add(objectChangedListener)
    }

    /**
     * Calls [BaseDaoPersistenceListener.afterLoad] for baseDao.
     * @see BaseDaoPersistenceListener.afterLoad
     */
    override fun afterLoad(obj: O) {
        baseDao.afterLoad(obj)
    }

    /**
     * Calls [BaseDaoPersistenceListener.afterLoad] for baseDao.
     * @see BaseDaoPersistenceListener.afterLoad
     */
    override fun afterLoad(list: List<O>): List<O> {
        return baseDao.afterLoad(list)
    }

    /**
     * Calls [BaseDaoPersistenceListener.beforeInsertOrModify] for baseDao.
     * Calls for all registered [BaseDOModifiedListener.beforeInsertOrModify].
     * @see BaseDOModifiedListener.beforeInsertOrModify
     */
    override fun beforeInsertOrModify(obj: O, operationType: OperationType) {
        baseDao.beforeInsertOrModify(obj, operationType)
        objectChangedListeners.forEach { it.beforeInsertOrModify(obj, operationType) }
    }

    /**
     * Calls [BaseDaoPersistenceListener.afterInsertOrModify] for baseDao.
     * Calls for all registered [BaseDOModifiedListener.afterInsertOrModify].
     * @see BaseDOModifiedListener.afterInsertOrModify
     */
    override fun afterInsertOrModify(obj: O, operationType: OperationType) {
        baseDao.afterInsertOrModify(obj, operationType)
        objectChangedListeners.forEach { it.afterInsertOrModify(obj, operationType) }
    }

    /**
     * Calls [BaseDaoPersistenceListener.onInsertOrModify] for baseDao.
     * @see BaseDaoPersistenceListener.onInsertOrModify
     */
    override fun onInsertOrModify(obj: O, operationType: OperationType) {
        baseDao.onInsertOrModify(obj, operationType)
    }

    /**
     * Calls [BaseDaoPersistenceListener.onInsert] for baseDao.
     * @see BaseDaoPersistenceListener.onInsert
     */
    override fun onInsert(obj: O) {
        baseDao.onInsert(obj)
    }

    /**
     * Calls [BaseDaoPersistenceListener.afterInsert] for baseDao.
     * @see BaseDaoPersistenceListener.afterInsert
     */
    override fun afterInsert(obj: O) {
        baseDao.afterInsert(obj)
    }

    /**
     * Calls [BaseDaoPersistenceListener.afterUpdate] for baseDao.
     * @see BaseDaoPersistenceListener.afterUpdate
     */
    override fun afterUpdate(obj: O, dbObj: O?, isModified: Boolean) {
        baseDao.afterUpdate(obj, dbObj, isModified)
    }

    /**
     * Calls [BaseDaoPersistenceListener.onUpdate] for baseDao.
     * @see BaseDaoPersistenceListener.onUpdate
     */
    override fun onUpdate(obj: O, dbObj: O) {
        baseDao.onUpdate(obj, dbObj)
    }

    /**
     * Calls [BaseDaoPersistenceListener.onDelete] for baseDao.
     * @see BaseDaoPersistenceListener.onDelete
     */
    override fun onDelete(obj: O) {
        baseDao.onDelete(obj)
    }

    /**
     * Calls [BaseDaoPersistenceListener.afterDelete] for baseDao.
     * @see BaseDaoPersistenceListener.afterDelete
     */
    override fun afterDelete(obj: O) {
        baseDao.afterDelete(obj)
    }

    /**
     * Calls [BaseDaoPersistenceListener.onUndelete] for baseDao.
     * @see BaseDaoPersistenceListener.onUndelete
     */
    override fun onUndelete(obj: O) {
        baseDao.onUndelete(obj)
    }

    /**
     * Calls [BaseDaoPersistenceListener.afterUndelete] for baseDao.
     * @see BaseDaoPersistenceListener.afterUndelete
     */
    override fun afterUndelete(obj: O) {
        baseDao.afterUndelete(obj)
    }
}
