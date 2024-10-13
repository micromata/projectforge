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

package org.projectforge.framework.persistence.jpa

/**
 * ThreadLocal for [PfPersistenceContext].
 * It is used to store context of current transaction or readonly operation.
 * If any method is using [PfPersistenceContext] and it is not passed as parameter, it will be get automatically by
 * [PfPersistenceService] for re-using contexts (readonly as well as transactional ones).
 * For readonly operations, any existing context (readonly or transactional) will be used.
 */
internal object PfPersistenceContextThreadLocal {
    private val threadLocalReadonly = ThreadLocal<PfPersistenceContext?>()
    private val threadLocalTransactional = ThreadLocal<PfPersistenceContext?>()

    /**
     * Gets context of ThreadLocal with transaction, if exists, or readonly context, if exists. Null, if no context exist.
     */
    fun get(): PfPersistenceContext? {
        return getTransactional() ?: getReadonly()
    }

    /**
     * Gets readonly context of ThreadLocal, if exists. Null, if no context exist.
     */
    fun getReadonly(): PfPersistenceContext? {
        return threadLocalReadonly.get()
    }

    /**
     * Gets transactional context of ThreadLoca, if exists. Null, if no context exist.
     */
    fun getTransactional(): PfPersistenceContext? {
        return threadLocalTransactional.get()
    }

    /**
     * Sets readonly context of ThreadLocal.
     */
    fun setReadonly(context: PfPersistenceContext) {
        require(context.type == PfPersistenceContext.ContextType.READONLY) { "Context must be of type READONLY." }
        threadLocalReadonly.set(context)
    }

    /**
     * Sets transactional context of ThreadLocal.
     */
    fun setTransactional(context: PfPersistenceContext) {
        require(context.type == PfPersistenceContext.ContextType.TRANSACTION) { "Context must be of type TRANSACTION." }
        threadLocalTransactional.set(context)
    }

    /**
     * Removes readonly context of ThreadLocal, if exists.
     */
    fun removeReadonly(): PfPersistenceContext? {
        val ret = threadLocalReadonly.get()
        threadLocalReadonly.remove()
        return ret
    }

    /**
     * Removes transactional context of ThreadLocal, if exists.
     */
    fun removeTransactional(): PfPersistenceContext? {
        val ret = threadLocalTransactional.get()
        threadLocalTransactional.remove()
        return ret
    }
}
