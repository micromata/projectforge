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

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.LockModeType
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.criteria.Root
import mu.KotlinLogging
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.api.impl.EntityManagerUtil
import org.projectforge.framework.persistence.api.impl.PfPersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
open class PfPersistenceService {
    companion object {
        @JvmStatic
        lateinit var instance: PfPersistenceService
            private set
    }

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @PostConstruct
    private fun postConstruct() {
        instance = this
        HibernateUtils.internalInit(entityManagerFactory)
    }

    /**
     * @param readonly If true, no transaction is used.
     */
    open fun <T> runInTransaction(
        readonly: Boolean = false, run: (context: PfPersistenceContext) -> T
    ): T {
        return EntityManagerUtil.runInTransaction(entityManagerFactory, readonly, run)
    }

    open fun <T> runReadOnly(
        block: (context: PfPersistenceContext) -> T
    ): T {
        return runInTransaction(true, block)
    }

    /**
     * @see EntityManagerUtil.selectById
     */
    @JvmOverloads
    open fun <T> selectById(
        entityClass: Class<T>, id: Any?, attached: Boolean = false
    ): T? {
        return EntityManagerUtil.selectById(entityManagerFactory, entityClass, id, attached = attached)
    }

    /**
     * @see EntityManagerUtil.selectSingleResult
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
            entityManagerFactory,
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
     * Convenience call for selectSingleResult() with namedQuery = true.
     * @see EntityManagerUtil.selectSingleResult
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
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    @JvmOverloads
    open fun <T> queryNullable(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
    ): List<T?> {
        return EntityManagerUtil.queryNullable(
            entityManagerFactory,
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            attached = attached,
        )
    }

    /**
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    @JvmOverloads
    open fun <T> query(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        namedQuery: Boolean = false,
        maxResults: Int? = null,
        lockModeType: LockModeType? = null,
    ): List<T> {
        return EntityManagerUtil.query(
            entityManagerFactory,
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
    @JvmOverloads
    open fun <T> namedQuery(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        maxResults: Int? = null,
        lockModeType: LockModeType? = null,
    ): List<T> {
        return query(
            sql = sql,
            resultClass = resultClass,
            keyValues = keyValues,
            attached = attached,
            namedQuery = true,
            maxResults = maxResults,
            lockModeType = lockModeType,
        )
    }

    open fun insert(dbObj: Any) {
        EntityManagerUtil.insert(entityManagerFactory, dbObj)
    }

    /**
     * Calls Query(sql, params).executeUpdate()
     */
    open fun executeUpdate(
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        namedQuery: Boolean = false,
    ): Int {
        return EntityManagerUtil.executeUpdate(entityManagerFactory, sql, *keyValues, namedQuery = namedQuery)
    }

    /**
     * Convenience call for executeUpdate() with namedQuery = true.
     */
    open fun executeNamedUpdate(
        sql: String,
        vararg keyValues: Pair<String, Any?>,
    ): Int {
        return executeUpdate(sql, *keyValues, namedQuery = true)
    }

    open fun delete(dbObj: Any) {
        EntityManagerUtil.delete(entityManagerFactory, dbObj)
    }

    open fun <T> delete(entityClass: Class<T>, id: Any) {
        EntityManagerUtil.delete(entityManagerFactory, entityClass, id)
    }

    open fun <T> criteriaUpdate(
        entityClass: Class<T>, update: (cb: CriteriaBuilder, root: Root<T>, criteriaUpdate: CriteriaUpdate<T>) -> Unit
    ) {
        EntityManagerUtil.criteriaUpdate(entityManagerFactory, entityClass, update)
    }

    open fun <T> getReference(
        entityClass: Class<T>, id: Any
    ): T {
        return EntityManagerUtil.getReference(entityManagerFactory, entityClass, id)
    }

    /**
     * Gets the next number for a new entity. The next number is the maximum number of the attribute + 1.
     * @param table The name of the table (e. g. RechnungDO).
     * @param attribute The name of the attribute (e. g. rechnungsnummer).
     * @param startNumber The number to start with if no entry is found.
     */
    open fun getNextNumber(table: String, attribute: String, startNumber: Int = 0): Int {
        val maxNumber = selectSingleResult(
            "select max(t.$attribute) from $table t",
            Int::class.java,
        ) ?: run {
            log.info("First entry of $table")
            startNumber
        }
        return maxNumber + 1

    }
}
