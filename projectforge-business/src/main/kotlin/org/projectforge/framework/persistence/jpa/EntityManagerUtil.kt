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

import jakarta.persistence.*
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.criteria.Root
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.hibernate.NonUniqueResultException
import org.hibernate.Session
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.api.HibernateUtils

private val log = KotlinLogging.logger {}

/**
 * Only for internal usage. Please use [PfPersistenceService] and [PfPersistenceContext] instead.
 */
internal object EntityManagerUtil {
    // private val openedTransactions = mutableSetOf<EntityTransaction>()

    /**
     */
    fun <T> runInTransaction(
        entityManagerFactory: EntityManagerFactory,
        run: (context: PfPersistenceContext) -> T
    ): T {
        return PfPersistenceContext(entityManagerFactory, withTransaction = true).use { context ->
            runInTransaction(context) {
                run(context)
            }
        }
    }

    /**
     * Without transaction and marks session as readonly.
     */
    fun <T> runReadonly(
        entityManagerFactory: EntityManagerFactory,
        run: (context: PfPersistenceContext) -> T
    ): T {
        PfPersistenceContext(entityManagerFactory, withTransaction = false).use { context ->
            val em = context.em
            // log.info { "Running read only" }
            em.unwrap(Session::class.java).isDefaultReadOnly = true
            // No transaction in readonly mode.
            return run(context)
        }
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
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        attached: Boolean = false,
        namedQuery: Boolean = false,
    ): T? {
        return runReadonly(entityManagerFactory) { context ->
            selectSingleResult(
                context.em,
                sql = sql,
                resultClass = resultClass,
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
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        attached: Boolean = false,
        namedQuery: Boolean = false,
    ): T? {
        val result = try {
            createQuery(
                em,
                sql = sql,
                resultClass = resultClass,
                keyValues = keyValues,
                namedQuery = namedQuery
            ).singleResult
        } catch (ex: NoResultException) {
            if (!nullAllowed) {
                throw InternalErrorException("${ex.message}: $sql ${errorMessage ?: ""}")
            }
            return null
        } catch (ex: NonUniqueResultException) {
            throw InternalErrorException("${ex.message}: $sql ${errorMessage ?: ""}")
        }
        if (!attached && HibernateUtils.isEntity(resultClass) && em.contains(result)) {
            em.detach(result)
        }
        return result
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> queryNullable(
        entityManagerFactory: EntityManagerFactory,
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        lockModeType: LockModeType? = null,
    ): List<T?> {
        return runReadonly(entityManagerFactory) { context ->
            queryNullable(
                context.em,
                sql = sql,
                resultClass = resultClass,
                keyValues = keyValues,
                attached = attached,
                lockModeType = lockModeType,
            )
        }
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> queryNullable(
        em: EntityManager,
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        lockModeType: LockModeType? = null,
    ): List<T?> {
        val q = createQuery<T>(em, sql = sql, resultClass = resultClass, keyValues = keyValues)
        if (lockModeType != null) {
            q.lockMode = lockModeType
        }
        val ret = q.resultList
        if (!attached && HibernateUtils.isEntity(resultClass)) {
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
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        namedQuery: Boolean = false,
        maxResults: Int? = null,
        lockModeType: LockModeType? = null,
    ): List<T> {
        return runReadonly(entityManagerFactory) { context ->
            query(
                context.em,
                sql = sql,
                resultClass = resultClass,
                keyValues = keyValues,
                attached = attached,
                namedQuery = namedQuery,
                maxResults = maxResults,
                lockModeType = lockModeType,
            )
        }
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> query(
        em: EntityManager,
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        namedQuery: Boolean = false,
        maxResults: Int? = null,
        lockModeType: LockModeType? = null,
    ): List<T> {
        val q = createQuery(em, sql = sql, resultClass = resultClass, keyValues = keyValues, namedQuery = namedQuery)
        if (lockModeType != null) {
            q.lockMode = lockModeType
        }
        if (maxResults != null) {
            q.maxResults = maxResults
        }
        val ret = q.resultList
        if (!attached && HibernateUtils.isEntity(resultClass)) {
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
        sql: String,
        resultClass: Class<T>,
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
        return runInTransaction(entityManagerFactory) { context ->
            insert(context.em, dbObj)
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
        return runInTransaction(entityManagerFactory) { context ->
            update(context.em, dbObj)
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
        return runInTransaction(entityManagerFactory) { context ->
            delete(context.em, dbObj)
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
        return runInTransaction(entityManagerFactory) { context ->
            delete(context.em, entityClass, id)
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
        return runInTransaction(entityManagerFactory) { context ->
            criteriaUpdate(context.em, entityClass, update)
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
        return runInTransaction(entityManagerFactory) { context ->
            executeUpdate(context.em, sql, *keyValues, namedQuery = namedQuery)
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
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeNativeUpdate(
        entityManagerFactory: EntityManagerFactory,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): Int {
        return runInTransaction(entityManagerFactory) { context ->
            executeNativeUpdate(context.em, sql, *keyValues)
        }
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeNativeUpdate(
        em: EntityManager,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): Int {
        val query = em.createNativeQuery(sql)
        for ((key, value) in keyValues) {
            query.setParameter(key, value)
        }
        return query.executeUpdate()
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeNativeQuery(
        entityManagerFactory: EntityManagerFactory,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): List<*> {
        return runReadonly(entityManagerFactory) { context ->
            executeNativeQuery(context.em, sql, *keyValues)
        }
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeNativeQuery(
        em: EntityManager,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): List<*> {
        val query = em.createNativeQuery(sql)
        for ((key, value) in keyValues) {
            query.setParameter(key, value)
        }
        return query.resultList
    }

    fun <T> getReference(
        entityManagerFactory: EntityManagerFactory,
        entityClass: Class<T>,
        id: Any
    ): T {
        return runReadonly(entityManagerFactory) { context ->
            context.em.getReference(entityClass, id)
        }
    }

    /**
     * Encapsulates the run statement in a transaction begin and commit. Rollback on error.
     */
    private fun <T> runInTransaction(
        context: PfPersistenceContext,
        execute: (context: PfPersistenceContext) -> T,
    ): T {
        val em = context.em
        em.transaction.begin()
        // openedTransactions.add(em.transaction)
        //log.info { "Begin transaction ${em.transaction}... (${openedTransactions.size} open transactions)" }
        try {
            val ret = execute(context)
            em.transaction.commit()
            //openedTransactions.remove(em.transaction)
            //log.info { "Commit transaction ${em.transaction}..." }
            return ret
        } catch (ex: Exception) {
            em.transaction.rollback()
            //openedTransactions.remove(em.transaction)
            //log.info { "Rollback transaction ${em.transaction}..." }
            log.error(ex.message, ex)
            throw ex
        }
    }

    fun queryToString(query: TypedQuery<*>, errorMessage: String?): String {
        val queryString = query.unwrap(org.hibernate.query.Query::class.java)?.getQueryString()
        val sb = StringBuilder()
        sb.append("query='$queryString', params=[") //query.getQueryString())
        var first = true
        try {
            for (param in query.parameters) { // getParameterMetadata().getNamedParameterNames()
                if (!first)
                    sb.append(",")
                else
                    first = false
                sb.append("${param.name}=[${query.getParameterValue(param)}]")
            }
        } catch (ex: Exception) {
            // Do nothing: Session/EntityManager closed.
        }
        sb.append("]")
        if (StringUtils.isNotBlank(errorMessage))
            sb.append(", msg=[$errorMessage]")
        return sb.toString()
    }
}
