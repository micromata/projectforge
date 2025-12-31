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

package org.projectforge.framework.persistence.jpa

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.LockModeType
import mu.KotlinLogging
import org.hibernate.Session
import org.projectforge.framework.persistence.api.HibernateUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for handling persistence contexts (EntityManagers).
 * It provides methods for running code in transactional or readonly context.
 * It also provides methods for executing queries and selecting entities.
 */
@Service
open class PfPersistenceService {
    // private val openedTransactions = mutableSetOf<EntityTransaction>()

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @PostConstruct
    private fun postConstruct() {
        instance = this
        HibernateUtils.internalInit(entityManagerFactory)
    }

    /**
     * Re-uses the current EntityManager (context) for the block or a new one, if no EntityManager (context) is set in ThreadLocal before.
     * If a transaction is already running inside the current thread (threadlocal is used), the block will be executed in the same transaction.
     * @see internalRunInNewTransaction
     */
    fun <T> runInTransaction(
        run: (context: PfPersistenceContext) -> T
    ): T {
        val context = PfPersistenceContextThreadLocal.getTransactional() // Transactional context.
        return if (context == null) {
            internalRunInNewTransaction(false, run = run)
        } else {
            context.run(run)
        }
    }

    /**
     * Creates a new PfPersistenceContext (EntityManager), also if any EntityManager is available in ThreadLocal.
     * After finishing the block, the transactional context will be closed as well as removed from ThreadLocal.
     * Any previous transactional context in ThreadLocal will be restored after finishing the block.
     */
    @JvmOverloads
    fun <T> runInNewTransaction(
        recordCallStats: Boolean = false,
        recorcCallStatsExtended: Boolean = false,
        run: (context: PfPersistenceContext) -> T
    ): T {
        return internalRunInNewTransaction(
            recordCallStats = recordCallStats,
            recordCallStatsExtended = recorcCallStatsExtended,
            run = run,
        )
    }

    /**
     * Uses the current EntityManager for the block or a new one, if no EntityManager is set in ThreadLocal before.
     * If no EntityManager is set in ThreadLocal before, a new EntityManager will be created and set in ThreadLocal.
     * @see internalRunReadOnly
     */
    fun <T> runReadOnly(
        block: (context: PfPersistenceContext) -> T
    ): T {
        val context = PfPersistenceContextThreadLocal.getTransactionalOrReadonly() // Readonly or transactional context.
        if (context != null) {
            return context.run(block)
        }
        return internalRunReadOnly(recordCallStats = false, recordCallStatsExtended = false, block)
    }

    /**
     * Creates a new PfPersistenceContext (EntityManager), also if any EntityManager is available in ThreadLocal.
     * After finishing the block, the EntityManager will be closed as well as removed from ThreadLocal.
     * Any previous EntityManager in ThreadLocal will be restored after finishing the block.
     */
    @JvmOverloads
    fun <T> runIsolatedReadOnly(
        recordCallStats: Boolean = false,
        recordCallStatsExtended: Boolean = false,
        block: (context: PfPersistenceContext) -> T
    ): T {
        return internalRunReadOnly(
            recordCallStats = recordCallStats,
            recordCallStatsExtended = recordCallStatsExtended, block
        )
    }

    /**
     * Creates a new PfPersistenceContext (EntityManager), also if any EntityManager is available in ThreadLocal.
     * Any previous transactional context in ThreadLocal will be restored after finishing the block.
     */
    private fun <T> internalRunInNewTransaction(
        recordCallStats: Boolean,
        recordCallStatsExtended: Boolean = false,
        run: (context: PfPersistenceContext) -> T
    ): T {
        val saved = PfPersistenceContextThreadLocal.getTransactional()
        try {
            PfPersistenceContext(
                entityManagerFactory,
                type = PfPersistenceContext.ContextType.TRANSACTION,
            ).use { context ->
                if (recordCallStats || recordCallStatsExtended) {
                    context.recordCallsStats(recordCallStatsExtended)
                }
                PfPersistenceContextThreadLocal.setTransactional(context)
                PfPersistenceContextThreadLocal.getStatsState().transactionCreated()
                log.debug { "Begin transactional context=${context.contextId} in ThreadLocal... (saved context=${saved?.contextId})" }
                val em = context.em
                em.transaction.begin()
                // openedTransactions.add(em.transaction)
                //log.info { "Begin transaction ${em.transaction}... (${openedTransactions.size} open transactions)" }
                try {
                    val ret = run(context)
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
        } finally {
            val removed = PfPersistenceContextThreadLocal.removeTransactional()
            log.debug { "Remove transactional context=${removed?.contextId} from ThreadLocal... (restored context=${saved?.contextId})" }
            saved?.let { PfPersistenceContextThreadLocal.setTransactional(it) } // Restore previous context, if any.
            PfPersistenceContextThreadLocal.getStatsState().transactionClosed()
        }
    }

    /**
     * Creates a new PfPersistenceContext (EntityManager), also if any EntityManager is available in ThreadLocal.
     * Any previous readonly context in ThreadLocal will be restored after finishing the block.
     */
    private fun <T> internalRunReadOnly(
        recordCallStats: Boolean,
        recordCallStatsExtended: Boolean = false,
        block: (context: PfPersistenceContext) -> T
    ): T {
        val saved = PfPersistenceContextThreadLocal.getReadonly()
        try {
            PfPersistenceContext(
                entityManagerFactory,
                type = PfPersistenceContext.ContextType.READONLY
            ).use { context ->
                if (recordCallStats || recordCallStatsExtended) {
                    context.recordCallsStats(recordCallStatsExtended)
                }
                PfPersistenceContextThreadLocal.setReadonly(context)
                PfPersistenceContextThreadLocal.getStatsState().readonlyCreated()
                log.debug { "New readonly context=${context.contextId} in ThreadLocal... (saved context=${saved?.contextId})" }
                val em = context.em
                // log.info { "Running read only" }
                em.unwrap(Session::class.java).isDefaultReadOnly = true
                // No transaction in readonly mode.
                return block(context)
            }
        } finally {
            val removed = PfPersistenceContextThreadLocal.removeReadonly()
            log.debug { "Remove readonly context=${removed?.contextId} from ThreadLocal... (restored context=${saved?.contextId})" }
            saved?.let { PfPersistenceContextThreadLocal.setReadonly(it) } // Restore previous context, if any.
            PfPersistenceContextThreadLocal.getStatsState().readonlyClosed()
        }
    }


    /**
     * Encapsulated in [runReadOnly].
     * @see PfPersistenceContext.find
     */
    @JvmOverloads
    fun <T> find(
        entityClass: Class<T>, id: Any?, attached: Boolean = false, entityGraphName: String? = null
    ): T? {
        return runReadOnly { context ->
            context.find(entityClass, id, attached, entityGraphName = entityGraphName)
        }
    }

    /**
     * Encapsulated in [runReadOnly].
     * @see PfPersistenceContext.selectAll
     */
    @JvmOverloads
    fun <T> selectAll(
        entityClass: Class<T>,
        attached: Boolean = false,
        lockModeType: LockModeType? = null,
        entityGraphName: String? = null,
    ): List<T> {
        return runReadOnly { context ->
            context.selectAll(
                entityClass,
                attached = attached,
                lockModeType = lockModeType,
                entityGraphName = entityGraphName,
            )
        }
    }

    /**
     * Encapsulated in [runReadOnly].
     * @see PfPersistenceContext.selectSingleResult
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
        entityGraphName: String? = null,
    ): T? {
        return runReadOnly { context ->
            context.selectSingleResult(
                sql = sql,
                resultClass = resultClass,
                keyValues = keyValues,
                nullAllowed = nullAllowed,
                errorMessage = errorMessage,
                attached = attached,
                namedQuery = namedQuery,
                entityGraphName = entityGraphName,
            )
        }
    }

    /**
     * Convenience call for [selectSingleResult] with namedQuery = true. Encapsulated in [runReadOnly].
     */
    @JvmOverloads
    fun <T> selectNamedSingleResult(
        namedQuery: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        nullAllowed: Boolean = true,
        errorMessage: String? = null,
        attached: Boolean = false,
        entityGraphName: String? = null,
    ): T? {
        return selectSingleResult(
            sql = namedQuery,
            resultClass = resultClass,
            keyValues = keyValues,
            nullAllowed = nullAllowed,
            errorMessage = errorMessage,
            attached = attached,
            namedQuery = true,
            entityGraphName = entityGraphName,
        )
    }

    /**
     * Encapsulated in [runReadOnly].
     * @see PfPersistenceContext.executeQuery
     * @param attached If true, the result will not be detached if of type entity (default is false, meaning detached).
     */
    @JvmOverloads
    fun <T> executeQuery(
        sql: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        namedQuery: Boolean = false,
        maxResults: Int? = null,
        lockModeType: LockModeType? = null,
        entityGraphName: String? = null,
    ): List<T> {
        return runReadOnly { context ->
            context.executeQuery(
                sql = sql,
                resultClass = resultClass,
                keyValues = keyValues,
                attached = attached,
                namedQuery = namedQuery,
                maxResults = maxResults,
                lockModeType = lockModeType,
                entityGraphName = entityGraphName,
            )
        }
    }

    /**
     * Convenience call for [executeQuery] with namedQuery = true. Encapsulated in [runReadOnly].
     * @see executeQuery
     */
    @JvmOverloads
    fun <T> executeNamedQuery(
        namedQuery: String,
        resultClass: Class<T>,
        vararg keyValues: Pair<String, Any?>,
        attached: Boolean = false,
        maxResults: Int? = null,
        lockModeType: LockModeType? = null,
        entityGraphName: String? = null,
    ): List<T> {
        return executeQuery(
            sql = namedQuery,
            resultClass = resultClass,
            keyValues = keyValues,
            attached = attached,
            namedQuery = true,
            maxResults = maxResults,
            lockModeType = lockModeType,
            entityGraphName = entityGraphName,
        )
    }

    /**
     * Encapsulated in [runReadOnly].
     * @see PfPersistenceContext.getReference
     */
    fun <T> getReference(
        entityClass: Class<T>, id: Any
    ): T {
        return runReadOnly { context ->
            context.getReference(entityClass, id)
        }
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

    /**
     * For testing purposes only.
     * @return The statistics state (a copy for later comparison).
     * @see PersistenceConnectionStats
     */
    fun getCopyOfCurrentStateForTesting(): PersistenceConnectionStats {
        return PfPersistenceContextThreadLocal.getStatsState().getCopyOfCurrentState()
    }

    /**
     * For testing purposes only.
     */
    fun getActivitiesForTesting(oldState: PersistenceConnectionStats): PersistenceConnectionStats {
        return PfPersistenceContextThreadLocal.getStatsState().getActivities(oldState)
    }

    companion object {
        @JvmStatic
        lateinit var instance: PfPersistenceService
            private set
    }
}
