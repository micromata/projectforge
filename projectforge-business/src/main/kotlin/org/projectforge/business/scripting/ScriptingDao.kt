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

package org.projectforge.business.scripting

import org.projectforge.business.PfCaches
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
import java.io.Serializable

open class ScriptingDao<O : ExtendedBaseDO<Long>>
    (baseDao: BaseDao<O>) {
    protected val caches by lazy { PfCaches.instance }

    private val __baseDao = baseDao

    /**
     * For backward compatibility in scripts. Use [select] instead.
     * @see [select]
     */
    fun getList(filter: BaseSearchFilter): List<O> {
        return select(filter)
    }

    open fun select(filter: BaseSearchFilter): List<O> {
        val res = __baseDao.select(filter)
        res.forEach { caches.initializeBaseDO(it) }
        return res
    }

    val list: List<O>
        /**
         * Show whole list of objects with select access (without deleted entries).
         *
         * @return
         */
        get() = select(QueryFilter())

    /**
     * For backward compatibility in scripts. Use [select] instead.
     * @see [select]
     */
    fun getList(filter: QueryFilter): List<O> {
        return select(filter)
    }

    open fun select(filter: QueryFilter): List<O> {
        val res = __baseDao.select(filter)
        res.forEach { caches.initializeBaseDO(it) }
        return res
    }


    /**
     * @see BaseDao.find
     * @throws AccessException
     */
    @Throws(AccessException::class)
    open fun getById(id: Serializable?): O? {
        return __baseDao.find(id)
    }

    val dOClass: Class<O>
        get() = __baseDao.getEntityClass()
}
