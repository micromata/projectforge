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

package org.projectforge.framework.persistence.api.impl

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.LockModeType
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.criteria.Root
import mu.KotlinLogging
import org.hibernate.NonUniqueResultException
import org.hibernate.Session
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.utils.SQLHelper.queryToString

private val log = KotlinLogging.logger {}

internal object EntityManagerUtil {
    /**
     * @param readonly If true, no transaction is used.
     */
    fun <T> runInTransaction(
        entityManagerFactory: EntityManagerFactory,
        readonly: Boolean = false,
        run: (context: PfPersistenceContext) -> T
    ): T {
        return PfPersistenceContext(entityManagerFactory).use { context ->
            runInTransactionIfNotReadonly(context, readonly = readonly) {
                run(context)
            }
        }
    }

    fun <T> runReadonly(
        entityManagerFactory: EntityManagerFactory,
        block: (context: PfPersistenceContext) -> T
    ): T {
        return runInTransaction(entityManagerFactory, true, block)
    }

    fun <T> selectById(
        entityManagerFactory: EntityManagerFactory,
        entityClass: Class<T>,
        id: Any?,
        attached: Boolean = false,
        lockModeType: LockModeType? = null,
    ): T? {
        id ?: return null
        return runReadonly(entityManagerFactory) { context ->
            selectById(context.em, entityClass, id = id, attached = attached, lockModeType = lockModeType)
        }
    }

    fun <T> selectById(
        em: EntityManager,
        entityClass: Class<T>,
        id: Any?,
        attached: Boolean = false,
        lockModeType: LockModeType? = null,
    ): T? {
        id ?: return null
        val entity = if (lockModeType != null) {
            em.find(entityClass, id, lockModeType)
        } else {
            em.find(entityClass, id)
        }
        entity ?: return null
        if (!attached && em.contains(entity)) {
            em.detach(entity)
        }
        return entity
    }

    /**
     * @param nullAllowed If false, an exception is thrown if no result is found.
     * @param errorMessage If not null, this message is used in the exception.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> selectSingleResult(
        entityManagerFactory: EntityManagerFactory,
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        attached: Boolean = false,
        namedQuery: Boolean = false,
    ): T? {
        PfPersistenceContext(entityManagerFactory).use { context ->
            return selectSingleResult(
                context.em,
                resultClass,
                sql = sql,
                keyValues = keyValues,
                nullAllowed = nullAllowed,
                errorMessage = errorMessage,
                attached = attached,
                namedQuery = namedQuery,
            )
        }
    }

    /**
     * @param nullAllowed If false, an exception is thrown if no result is found.
     * @param errorMessage If not null, this message is used in the exception.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> selectSingleResult(
        em: EntityManager,
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        attached: Boolean = false,
        namedQuery: Boolean = false,
    ): T? {
        val query = createQuery(em, resultClass, sql, *keyValues, namedQuery = namedQuery)
        val result = try {
            // NoResultException – if there is no result
            // NonUniqueResultException
            if (nullAllowed) {
                val list = query.resultList
                if (list.isEmpty()) {
                    return null
                }
                if (list.size > 1) {
                    throw (NonUniqueResultException(list.size))
                }
                list[0]
            } else {
                query.singleResult
            }
        } catch (ex: Exception) {
            throw InternalErrorException(
                "${ex.message}: ${
                    queryToString(
                        query,
                        errorMessage
                    )
                }"
            )
        }
        if (!attached && em.contains(result)) {
            em.detach(result)
        }
        return result
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> queryNullable(
        entityManagerFactory: EntityManagerFactory,
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
    ): List<T?> {
        PfPersistenceContext(entityManagerFactory).use { context ->
            return queryNullable(
                context.em,
                resultClass,
                sql,
                *keyValues,
                attached = attached,
            )
        }
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> queryNullable(
        em: EntityManager,
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
    ): List<T?> {
        val q = createQuery<T>(em, resultClass, sql, *keyValues)
        val ret = q.resultList
        if (!attached) {
            ret.forEach { obj ->
                if (obj != null && em.contains(obj)) {
                    em.detach(obj)
                }
            }
        }
        return ret
    }

    /**
     * No null result values are allowed.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> query(
        entityManagerFactory: EntityManagerFactory,
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        namedQuery: Boolean = false,
        maxResults: Int? = null,
    ): List<T> {
        PfPersistenceContext(entityManagerFactory).use { context ->
            return query(
                context.em,
                resultClass = resultClass,
                sql = sql,
                keyValues = keyValues,
                attached = attached,
                namedQuery = namedQuery,
                maxResults = maxResults,
            )
        }
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> query(
        em: EntityManager,
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        namedQuery: Boolean = false,
        maxResults: Int? = null,
    ): List<T> {
        val q = createQuery(em, resultClass, sql, *keyValues, namedQuery = namedQuery)
        if (maxResults != null) {
            q.maxResults = maxResults
        }
        val ret = q.resultList
        if (!attached) {
            ret.forEach { obj ->
                if (obj != null && em.contains(obj)) {
                    em.detach(obj)
                }
            }
        }
        return ret
    }

    fun <T> createQuery(
        em: EntityManager,
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        namedQuery: Boolean = false,
    ): TypedQuery<T> {
        val query: TypedQuery<T> = if (namedQuery) {
            em.createNamedQuery(sql, resultClass)
        } else {
            em.createQuery(sql, resultClass)
        }
        for ((key, value) in keyValues) {
            query.setParameter(key, value)
        }
        return query
    }

    /**
     * em.persist() is used.
     */
    fun insert(
        entityManagerFactory: EntityManagerFactory,
        dbObj: Any,
    ) {
        PfPersistenceContext(entityManagerFactory).use { context ->
            runInTransactionIfNotReadonly(context) {
                insert(context.em, dbObj)
            }
        }
    }

    /**
     * em.persist() is used.
     */
    fun insert(
        em: EntityManager,
        dbObj: Any,
    ) {
        em.persist(dbObj)
    }

    /**
     * em.merge() is used.
     */
    fun update(
        entityManagerFactory: EntityManagerFactory,
        dbObj: Any,
    ) {
        PfPersistenceContext(entityManagerFactory).use { context ->
            runInTransactionIfNotReadonly(context) {
                update(context.em, dbObj)
            }
        }
    }

    /**
     * em.merge() is used.
     */
    fun update(
        em: EntityManager,
        dbObj: Any,
    ) {
        em.merge(dbObj)
    }

    fun delete(
        entityManagerFactory: EntityManagerFactory,
        dbObj: Any,
    ) {
        PfPersistenceContext(entityManagerFactory).use { context ->
            runInTransactionIfNotReadonly(context) {
                delete(context.em, dbObj)
            }
        }
    }

    fun delete(
        em: EntityManager,
        dbObj: Any,
    ) {
        em.remove(dbObj)
    }

    fun <T> delete(
        entityManagerFactory: EntityManagerFactory,
        entityClass: Class<T>,
        id: Any,
    ) {
        PfPersistenceContext(entityManagerFactory).use { context ->
            runInTransactionIfNotReadonly(context) {
                delete(context.em, entityClass, id)
            }
        }
    }

    fun <T> delete(
        em: EntityManager,
        entityClass: Class<T>,
        id: Any,
    ) {
        selectById(em, entityClass, id, attached = true)?.let { dbObj ->
            em.remove(dbObj)
        }
    }

    fun <T> criteriaUpdate(
        entityManagerFactory: EntityManagerFactory,
        entityClass: Class<T>,
        update: (cb: CriteriaBuilder, root: Root<T>, update: CriteriaUpdate<T>) -> Unit
    ) {
        PfPersistenceContext(entityManagerFactory).use { context ->
            runInTransactionIfNotReadonly(context) {
                criteriaUpdate(context.em, entityClass, update)
            }
        }
    }

    fun <T> criteriaUpdate(
        em: EntityManager,
        entityClass: Class<T>,
        update: (cb: CriteriaBuilder, root: Root<T>, update: CriteriaUpdate<T>) -> Unit
    ) {
        val cb = em.criteriaBuilder
        val criteriaUpdate = cb.createCriteriaUpdate(entityClass)
        // define root-Instanz
        val root = criteriaUpdate.from(entityClass)
        update(cb, root, criteriaUpdate)
        em.createQuery(criteriaUpdate).executeUpdate()
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeUpdate(
        entityManagerFactory: EntityManagerFactory,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        namedQuery: Boolean = false,
    ): Int {
        return PfPersistenceContext(entityManagerFactory).use { context ->
            runInTransactionIfNotReadonly(context) {
                executeUpdate(context.em, sql, *keyValues, namedQuery = namedQuery)
            }
        }
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeUpdate(
        em: EntityManager,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        namedQuery: Boolean = false,
    ): Int {
        val query = if (namedQuery) {
            em.createNamedQuery(sql)
        } else {
            em.createQuery(sql)
        }
        for ((key, value) in keyValues) {
            query.setParameter(key, value)
        }
        return query.executeUpdate()
    }

    /**
     * Encapsulates the run statement in a transaction begin and commit. Rollback on error.
     * @param readonly If true, the session will set as readonly and only the run statement will be executed.
     */
    private fun <T> runInTransactionIfNotReadonly(
        context: PfPersistenceContext,
        readonly: Boolean = false,
        execute: (context: PfPersistenceContext) -> T,
    ): T {
        val em = context.em
        if (readonly) {
            em.unwrap(Session::class.java).isDefaultReadOnly = true
            // No transaction in readonly mode.
            return execute(context)
        } else {
            em.transaction.begin()
            try {
                val ret = execute(context)
                em.transaction.commit()
                return ret
            } catch (ex: Exception) {
                em.transaction.rollback()
                log.error(ex.message, ex)
                throw ex
            }
        }
    }
}
