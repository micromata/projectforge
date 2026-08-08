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

package org.projectforge.framework.persistence.database

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import org.apache.commons.lang3.ClassUtils
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.massindexing.MassIndexer
import org.hibernate.search.mapper.orm.session.SearchSession
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor
import org.projectforge.framework.persistence.api.ReindexSettings
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.DayHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Creates index creation script and re-indexes data-base.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
// Check open connections in PostgreSQL:
// SELECT backend_start, query_start, state_change, wait_event_type, state, query  FROM pg_stat_activity where state <> 'idle';
@Service
open class DatabaseDao {
    private var currentReindexRun: Date? = null

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @JvmOverloads
    fun <T> rebuildDatabaseSearchIndices(clazz: Class<T>, settings: ReindexSettings = ReindexSettings()): String {
        if (currentReindexRun != null) {
            val otherJobStarted =
                DateTimeFormatter.instance().getFormattedDateTime(currentReindexRun, Locale.ENGLISH, DateHelper.UTC)
            return ("Another re-index job is already running. The job was started at: $otherJobStarted (UTC)")
        }
        val sb = StringBuilder()
        reindex(clazz, settings, sb)
        return sb.toString()
    }

    fun <T> reindex(clazz: Class<T>, settings: ReindexSettings, sb: StringBuilder) {
        if (currentReindexRun != null) {
            sb.append(" (cancelled due to another running index-job)")
            return
        }
        synchronized(this) {
            try {
                currentReindexRun = Date()
                sb.append(ClassUtils.getShortClassName(clazz))
                reindexObjects(clazz, settings)
                sb.append(", ")
            } finally {
                currentReindexRun = null
            }
        }
    }

    /**
     * Blocking re-index run, used by the classic clients (Wicket admin page, cron job, synchronous REST calls).
     * For a cancellable run inside a coroutine use [reindexSuspending].
     */
    private fun <T> reindexObjects(clazz: Class<T>, settings: ReindexSettings) {
        entityManagerFactory.createEntityManager().use { em ->
            try {
                createMassIndexer(em, clazz, settings, IndexProgressMonitor(clazz))
                    .startAndWait() // Blockiert, bis die Indizierung abgeschlossen ist
            } catch (ex: InterruptedException) {
                log.error(ex.message, ex)
            }
        }
    }

    /**
     * Re-indexes the given class without blocking the calling thread and, unlike [rebuildDatabaseSearchIndices],
     * abortable: [MassIndexer.startAndWait] can't be interrupted by a coroutine, while cancelling the future of
     * [MassIndexer.start] really stops the indexing (see CancellableExecutionCompletableFuture of Hibernate Search).
     *
     * Serializing concurrent runs is up to the caller (jobs do it via their queue strategy).
     */
    suspend fun <T> reindexSuspending(
        clazz: Class<T>,
        settings: ReindexSettings,
        monitor: MassIndexingMonitor = IndexProgressMonitor(clazz),
    ) {
        // The EntityManager has to stay open until the indexer is done, so await() happens inside use { }.
        entityManagerFactory.createEntityManager().use { em ->
            createMassIndexer(em, clazz, settings, monitor).start().await()
        }
    }

    private fun <T> createMassIndexer(
        em: EntityManager,
        clazz: Class<T>,
        settings: ReindexSettings,
        monitor: MassIndexingMonitor,
    ): MassIndexer {
        // totalEntries are given by Hibernate search to MassIndexingMonitor.
        val searchSession: SearchSession = Search.session(em)
        val indexer = searchSession.massIndexer(clazz)
            .threadsToLoadObjects(4) // Anzahl der Threads zum Laden von Entitäten
            .batchSizeToLoadObjects(25) // Batch-Größe
            .idFetchSize(150) // Größe des ID-Fetch
            .monitor(monitor) // Fortschrittsmonitor hinzufügen
        val fromDate = settings.fromDate
        val strategy = ReindexerRegistry.get(clazz)
        val modifiedAtProperty = strategy.modifiedAtProperty
        if (fromDate != null && modifiedAtProperty != null) {
            // Only the recently modified entries: the rest of the index has to survive, so no purge. Without this
            // (purgeAllOnStart defaults to true) a partial run would wipe every document not touched by it.
            indexer.purgeAllOnStart(false)
            val condition = StringBuilder("$modifiedAtProperty >= :fromDate")
            // The change history holds the rows of all entities, so a run started for a list page has to say whose
            // (otherwise re-indexing the book list would also re-index yesterday's history of every other entity).
            // Several names, because history rows are written per entity instance: the order list needs the history
            // of its positions and payment schedules, too. Never empty, see ReindexSettings.
            val entityNames = settings.entityNames?.takeIf { strategy.entityNameProperty != null }
            if (entityNames != null) {
                condition.append(" and ${strategy.entityNameProperty} in :entityNames")
            }
            log.info { "${clazz.simpleName}: Re-indexing only where $condition, fromDate=$fromDate, entityNames=$entityNames" }
            val step = indexer.type(clazz).reindexOnly(condition.toString()).param("fromDate", fromDate)
            // Hibernate ORM's setParameter detects the collection of an in-parameter and binds it as a list.
            entityNames?.let { step.param("entityNames", it) }
        } else if (fromDate != null) {
            log.info { "${clazz.simpleName}: No property of last modification known, so all entries are re-indexed." }
        }
        return indexer
    }

    companion object {
        /**
         * Since yesterday. [ReindexSettings.getLastNEntries] is set for the classic clients displaying it, but has no
         * effect on the indexing itself: reindexOnly of Hibernate Search takes a where condition without order or
         * limit, and limitIndexedObjectsTo would cap arbitrary entries, not the newest ones.
         *
         * @param entityNames The entities the run was started for (see BaseDao.historyEntityNames), restricting the
         *          change history to their rows. Null or empty for a system wide run, whose history isn't restricted.
         */
        @JvmStatic
        @JvmOverloads
        fun createReindexSettings(onlyNewest: Boolean, entityNames: Collection<String>? = null): ReindexSettings {
            return if (onlyNewest) {
                val day = DayHolder()
                day.add(Calendar.DAY_OF_MONTH, -1) // Since yesterday:
                ReindexSettings(day.utilDate, 1000, entityNames) // Maximum 1,000 newest entries.
            } else {
                ReindexSettings()
            }
        }
    }
}
