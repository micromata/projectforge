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
import jakarta.persistence.LockModeType
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.criteria.Root
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * A wrapper for EntityManager with some convenience methods. The EntityManager is created by the given entityManagerFactory.
 */
class PfPersistenceContext(
    entityManagerFactory: EntityManagerFactory,
) : AutoCloseable {
    val em: EntityManager = entityManagerFactory.createEntityManager()
/*
    private var threadLocalContextSet = false

    init {
        ThreadLocalPersistenceContext.get().let { existingContext ->
            if (existingContext != null) {
                log.debug { "ThreadLocalPersistenceContext already set: $existingContext" }
            } else {
                threadLocalContextSet = true
                ThreadLocalPersistenceContext.set(this)
            }
        }
        //    openEntityManagers.add(em)
        //    log.info { "Created EntityManager: $em (${openEntityManagers.size} opened entity managers)." }
    }
*/
    fun <T> selectById(
        entityClass: Class<T>,
        id: Any?,
        attached: Boolean = false,
        lockModeType: LockModeType? = null,
    ): T? {
        return EntityManagerUtil.selectById(em, entityClass, id = id, attached = attached, lockModeType = lockModeType)
    }

    /**
     * @param nullAllowed If false, an exception is thrown if no result is found.
     * @param errorMessage If not null, this message is used in the exception.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    @JvmOverloads
    fun <T> selectNamedSingleResult(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        attached: Boolean = false,
    ): T? {
        return selectSingleResult(
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            nullAllowed = nullAllowed,
            errorMessage = errorMessage,
            attached = attached,
            namedQuery = true,
        )
    }

    /**
     * @param nullAllowed If false, an exception is thrown if no result is found.
     * @param errorMessage If not null, this message is used in the exception.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    @JvmOverloads
    fun <T> selectSingleResult(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        attached: Boolean = false,
        namedQuery: Boolean = false,
    ): T? {
        return EntityManagerUtil.selectSingleResult(
            em,
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            nullAllowed = nullAllowed,
            errorMessage = errorMessage,
            attached = attached,
            namedQuery = namedQuery,
        )
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> queryNullable(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        lockModeType: LockModeType? = null,
    ): List<T?> {
        return EntityManagerUtil.queryNullable(
            em,
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            attached = attached,
            lockModeType = lockModeType,
        )
    }

    /**
     * No null result values are allowed.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> query(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        namedQuery: Boolean = false,
        maxResults: Int? = null,
        lockModeType: LockModeType? = null,
    ): List<T> {
        return EntityManagerUtil.query(
            em,
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            attached = attached,
            namedQuery = namedQuery,
            maxResults = maxResults,
            lockModeType = lockModeType,
        )
    }

    /**
     * Convenience call for query() with namedQuery = true.
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    fun <T> namedQuery(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        maxResults: Int? = null,
    ): List<T> {
        return query(
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            attached = attached,
            namedQuery = true,
            maxResults = maxResults,
        )
    }


    fun <T> createQuery(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        namedQuery: Boolean = false,
    ): TypedQuery<T> {
        return EntityManagerUtil.createQuery(
            em,
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            namedQuery = namedQuery,
        )
    }

    fun insert(
        dbObj: Any,
    ) {
        return EntityManagerUtil.insert(em, dbObj)
    }

    fun update(
        dbObj: Any,
    ) {
        return EntityManagerUtil.update(em, dbObj)
    }

    fun delete(
        dbObj: Any,
    ) {
        return EntityManagerUtil.delete(em, dbObj)
    }

    fun <T> delete(
        entityClass: Class<T>,
        id: Any,
    ) {
        return EntityManagerUtil.delete(em, entityClass, id = id)
    }

    fun <T> criteriaUpdate(
        entityClass: Class<T>,
        update: (cb: CriteriaBuilder, root: Root<T>, update: CriteriaUpdate<T>) -> Unit
    ) {
        return EntityManagerUtil.criteriaUpdate(em, entityClass, update)
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeUpdate(
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        namedQuery: Boolean = false,
    ): Int {
        return EntityManagerUtil.executeUpdate(em, sql, keyValues = keyValues, namedQuery = namedQuery)
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeNamedUpdate(
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): Int {
        return EntityManagerUtil.executeUpdate(em, sql, keyValues = keyValues, namedQuery = true)
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    fun executeNativeUpdate(
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): Int {
        return EntityManagerUtil.executeNativeQueryUpdate(em, sql, keyValues = keyValues)
    }

    /**
     * Calls em.flush().
     */
    fun flush() {
        em.flush()
    }

    override fun close() {
        if (em.isOpen) {
            em.close()
            //openEntityManagers.remove(em)
            //log.info { "Closed EntityManager: $em (${openEntityManagers.size} opened entity managers)." }
        }
/*        if (threadLocalContextSet) {
            ThreadLocalPersistenceContext.remove()
            threadLocalContextSet = false // Paranoia setting ;-)
        }*/
    }

    /**
     * Gets the next number for a new entity. The next number is the maximum number of the attribute + 1.
     * @param table The name of the table (e. g. RechnungDO).
     * @param attribute The name of the attribute (e. g. rechnungsnummer).
     * @param startNumber The number to start with if no entry is found.
     */
    fun getNextNumber(table: String, attribute: String, startNumber: Int = 0): Int {
        val maxNumber = selectSingleResult(
            "select max(t.$attribute) from $table t",
            Int::class.java,
        ) ?: run {
            log.info("First entry of $table")
            startNumber
        }
        return maxNumber + 1
    }

    //companion object {
    //    private val openEntityManagers = mutableSetOf<EntityManager>()
    //}
/*
    internal object ThreadLocalPersistenceContext {
        private val threadLocal = ThreadLocal<PfPersistenceContext?>()

        fun get(): PfPersistenceContext? {
            return threadLocal.get()
        }

        fun set(context: PfPersistenceContext) {
            threadLocal.set(context)
        }

        fun remove() {
            threadLocal.remove()
        }
    }*/
}
