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

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.criteria.Root
import org.projectforge.framework.persistence.api.impl.EntityManagerUtil
import org.springframework.stereotype.Service

@Service
open class PfPersistenceService {
    private lateinit var entityManagerFactory: EntityManagerFactory

    open fun <T> runInTransaction(
        readonly: Boolean = false,
        run: (em: EntityManager) -> T
    ): T {
        return EntityManagerUtil.runInTransaction(entityManagerFactory, readonly, run)
    }

    open fun <T> runInReadOnlyTransaction(
        block: (em: EntityManager) -> T
    ): T {
        return runInTransaction(true, block)
    }

    /**
     * @see EntityManagerUtil.selectById
     */
    open fun <T> selectById(
        entityClass: Class<T>,
        id: Any?,
        detached: Boolean = true
    ): T? {
        return EntityManagerUtil.selectById(entityManagerFactory, entityClass, id, detached)
    }

    /**
     * @see EntityManagerUtil.selectSingleResult
     */
    fun <T> selectSingleResult(
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        detached: Boolean = true,
    ): T? {
        return EntityManagerUtil.selectSingleResult(
            entityManagerFactory,
            resultClass,
            sql,
            *keyValues,
            nullAllowed = nullAllowed,
            errorMessage = errorMessage,
            detached = detached,
        )
    }

    /**
     * @param detached If true, the result is detached if of type entity (default).
     */
    open fun <T> queryNullable(
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        detached: Boolean = true,
    ): List<T?> {
        return EntityManagerUtil.queryNullable(entityManagerFactory, resultClass, sql, *keyValues, detached = detached)
    }

    /**
     * @param detached If true, the result is detached if of type entity (default).
     */
    open fun <T> query(
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        detached: Boolean = true,
    ): List<T> {
        return EntityManagerUtil.query(entityManagerFactory, resultClass, sql, *keyValues, detached = detached)
    }

    open fun insert(dbObj: Any) {
        EntityManagerUtil.insert(entityManagerFactory, dbObj)
    }

    open fun delete(dbObj: Any) {
        EntityManagerUtil.delete(entityManagerFactory, dbObj)
    }

    open fun <T> delete(entityClass: Class<T>, id: Any) {
        EntityManagerUtil.delete(entityManagerFactory, entityClass, id)
    }

    open fun <T> criteriaUpdate(
        entityClass: Class<T>,
        update: (cb: CriteriaBuilder, root: Root<T>, criteriaUpdate: CriteriaUpdate<T>) -> Unit
    ) {
        EntityManagerUtil.criteriaUpdate(entityManagerFactory, entityClass, update)
    }
}
