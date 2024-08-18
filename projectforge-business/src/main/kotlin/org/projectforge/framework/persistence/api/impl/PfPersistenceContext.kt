package org.projectforge.framework.persistence.api.impl

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.criteria.Root

class PfPersistenceContext(val entityManagerFactory: EntityManagerFactory, val em: EntityManager) {
    fun <T> runInTransaction(
        readonly: Boolean = false,
        run: (em: EntityManager) -> T
    ): T {
        return EntityManagerUtil.runInTransaction(entityManagerFactory, readonly = readonly, run)
    }

    fun <T> runInReadOnlyTransaction(block: (em: EntityManager) -> T): T {
        return runInTransaction(true, block)
    }

    fun <T> selectById(
        entityClass: Class<T>,
        id: Any?,
        detached: Boolean = true,
    ): T? {
        return EntityManagerUtil.selectById(em, entityClass, id = id, detached = detached)
    }

    /**
     * @param nullAllowed If false, an exception is thrown if no result is found.
     * @param errorMessage If not null, this message is used in the exception.
     * @param detached If true, the result is detached (default).
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
            em,
            resultClass,
            sql = sql,
            keyValues = *keyValues,
            nullAllowed = nullAllowed,
            errorMessage = errorMessage,
            detached = detached
        )
    }

    /**
     * @param detached If true, the result is detached if of type entity (default).
     */
    fun <T> queryNullable(
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        detached: Boolean = true,
    ): List<T?> {
        return EntityManagerUtil.queryNullable(
            em,
            resultClass,
            sql,
            *keyValues,
            detached = detached,
        )
    }

    /**
     * No null result values are allowed.
     * @param detached If true, the result is detached if of type entity (default).
     */
    fun <T> query(
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        detached: Boolean = true,
        namedQuery: Boolean = false,
        maxResults: Int? = null,
    ): List<T> {
        return EntityManagerUtil.query(
            em,
            resultClass,
            sql,
            *keyValues,
            detached = detached,
            namedQuery = namedQuery,
            maxResults = maxResults,
        )
    }

    fun <T> createQuery(
        resultClass: Class<T>,
        sql: String,
        vararg keyValues: Pair<String, Any?>,
        namedQuery: Boolean = false,
    ): TypedQuery<T> {
        return EntityManagerUtil.createQuery(em, resultClass, sql = sql, keyValues = keyValues, namedQuery = namedQuery)
    }

    fun insert(
        dbObj: Any,
    ) {
        return EntityManagerUtil.insert(em, dbObj)
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
    ): Int {
        return EntityManagerUtil.executeUpdate(em, sql, *keyValues)
    }
}
