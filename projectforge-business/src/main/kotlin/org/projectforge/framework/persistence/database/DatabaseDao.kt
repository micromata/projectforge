/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.genome.jpa.StdRecord
import org.apache.commons.lang3.ClassUtils
import org.hibernate.ScrollMode
import org.hibernate.Session
import org.hibernate.query.Query
import org.hibernate.search.FullTextSession
import org.hibernate.search.Search
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.ReindexSettings
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO
import org.projectforge.framework.persistence.jpa.PfEmgr
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.framework.persistence.utils.PFTransactionTemplate.runInTrans
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.DayHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.FlushModeType
import javax.persistence.TypedQuery

/**
 * Creates index creation script and re-indexes data-base.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
class DatabaseDao {
    private var currentReindexRun: Date? = null
    @Autowired
    private val emgrFactory: PfEmgrFactory? = null

    fun <T> rebuildDatabaseSearchIndices(clazz: Class<T>, settings: ReindexSettings): String {
        if (currentReindexRun != null) {
            val otherJobStarted = DateTimeFormatter.instance().getFormattedDateTime(currentReindexRun, Locale.ENGLISH, DateHelper.UTC)
            return ("Another re-index job is already running. The job was started at: $otherJobStarted (UTC)")
        }
        val buf = StringBuffer()
        reindex(clazz, settings, buf)
        return buf.toString()
    }

    fun <T> reindex(clazz: Class<T>, settings: ReindexSettings, buf: StringBuffer) {
        if (currentReindexRun != null) {
            buf.append(" (cancelled due to another running index-job)")
            return
        }
        synchronized(this) {
            try {
                currentReindexRun = Date()
                buf.append(ClassUtils.getShortClassName(clazz))
                reindex(clazz, settings)
                buf.append(", ")
            } finally {
                currentReindexRun = null
            }
        }
    }

    /**
     * @param clazz
     */
    private fun <T> reindex(clazz: Class<T>, settings: ReindexSettings) {
        if (settings.lastNEntries != null || settings.fromDate != null) { // OK, only partly re-index required:
            reindexObjects(clazz, settings)
            return
        }
        // OK, full re-index required:
        if (isIn(clazz, TimesheetDO::class.java, PfHistoryMasterDO::class.java)) { // MassIndexer throws LazyInitializationException for some classes, so use it only for the important classes (with most entries):
            reindexMassIndexer(clazz)
            return
        }
        reindexObjects(clazz, null)
    }

    private fun isIn(clazz: Class<*>, vararg classes: Class<*>): Boolean {
        for (cls in classes) {
            if (clazz == cls) {
                return true
            }
        }
        return false
    }

    private fun <T> reindexObjects(clazz: Class<T>, settings: ReindexSettings?) {
        runInTrans(emgrFactory!!) { em: EntityManager ->
            val session = em.delegate as Session
            val number = getRowCount(em, clazz, settings) // Get number of objects to re-index (select count(*) from).
            val scrollMode = number > MIN_REINDEX_ENTRIES_4_USE_SCROLL_MODE
            log.info("Reindexing [${clazz.simpleName}]: Starting reindexing of $number entries with scrollMode=$scrollMode...")
            val batchSize = 1000 // NumberUtils.createInteger(System.getProperty("hibernate.search.worker.batch_size")
            var fullTextSession: FullTextSession? = null
            try {
                fullTextSession = Search.getFullTextSession(session)
                fullTextSession.flushMode = FlushModeType.COMMIT
                // HibernateCompatUtils.setFlushMode(fullTextSession, FlushMode.MANUAL);
                // HibernateCompatUtils.setCacheMode(fullTextSession, CacheMode.IGNORE);
                var index: Long = 0
                val monitor = IndexProgressMonitor("Reindexing [" + clazz.simpleName + "]: ", number)
                if (scrollMode) { // Scroll-able results will avoid loading too many objects in memory
                    val query = createCriteria(em, clazz, settings)
                    val hquery = query.unwrap(Query::class.java)
                    val results = hquery.setFetchSize(1000).setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY)
                    while (results.next()) {
                        val obj = results[0]
                        if (obj is ExtendedBaseDO<*>) {
                            obj.recalculate()
                        }
                        fullTextSession.index(obj)
                        monitor.documentsAdded(1)
                        if (index++ % batchSize == 0L) {
                            em.clear()
                            fullTextSession.flush() // clear every batchSize since the queue is processed
                            fullTextSession.clear()
                        }
                    }
                } else {
                    val query = createCriteria(em, clazz, settings)
                    val list = query.resultList
                    for (obj in list) {
                        if (obj is ExtendedBaseDO<*>) {
                            (obj as ExtendedBaseDO<*>).recalculate()
                        }
                        fullTextSession.index<Any>(obj)
                        if (index++ % batchSize == 0L) {
                            fullTextSession.flush() // clear every batchSize since the queue is processed
                            fullTextSession.clear()
                        }
                    }
                }
                log.info("Reindexing [${clazz.simpleName}]: optimizing of " + number + " objects...")
                val searchFactory = fullTextSession.searchFactory
                searchFactory.optimize(clazz)
                log.info("Reindexing [${clazz.simpleName}]: reindexing done.")
                return@runInTrans index
            } finally {
                if (fullTextSession != null && fullTextSession.isOpen) {
                    fullTextSession.close()
                }
            }
        }
    }

    private fun <T> reindexMassIndexer(clazz: Class<T>): Long {
        return emgrFactory!!.runInTrans { emgr: PfEmgr ->
            val em = emgr.entityManager
            val session = em.delegate as Session
            val number = getRowCount(em, clazz, null) // Get number of objects to re-index (select count(*) from).
            log.info("Reindexing [${clazz.simpleName}]: Starting (mass) re-indexing of $number entries...")
            var fullTextSession: FullTextSession? = null
            try {
                fullTextSession = Search.getFullTextSession(session)
                try {
                    fullTextSession.createIndexer(clazz) //
                            .batchSizeToLoadObjects(5) //
                            .optimizeOnFinish(true)
                            .idFetchSize(1000) //.cacheMode(CacheMode.NORMAL) //
                            .threadsToLoadObjects(5) //
                            .progressMonitor(IndexProgressMonitor("Reindexing [${clazz.simpleName}]: ", number)) //.threadsForIndexWriter(1) //
                            .startAndWait()
                } catch (ex: InterruptedException) {
                    log.error("Exception encountered while reindexing: ${ex.message}", ex)
                }
                log.info("Reindexing [${clazz.simpleName}]: reindexing of $number objects done.")
                return@runInTrans number
            } finally {
                if (fullTextSession != null && fullTextSession.isOpen) {
                    fullTextSession.close()
                }
            }
        }
    }

    private fun <T> getRowCount(entityManager: EntityManager, clazz: Class<T>, settings: ReindexSettings?): Long {
        return createQuery(entityManager, clazz, java.lang.Long::class.java, settings).singleResult as Long
    }

    private fun <T> createCriteria(entityManager: EntityManager, clazz: Class<T>, settings: ReindexSettings?): TypedQuery<T> {
        return createQuery(entityManager, clazz, clazz, settings)
    }

    private fun <T> createQuery(entityManager: EntityManager, clazz: Class<*>, resultClazz: Class<T>, settings: ReindexSettings?): TypedQuery<T> {
        val rowCountOnly = resultClazz == java.lang.Long::class.java
        val select = if (rowCountOnly) "select count(*) from ${clazz.simpleName}" else "from  ${clazz.simpleName}"
        if (settings?.fromDate != null) {
            val modAtProp = when {
                AbstractBaseDO::class.java.isAssignableFrom(clazz) -> "lastUpdate"
                StdRecord::class.java.isAssignableFrom(clazz) -> "modifiedAt"
                else -> null
            }
            if (modAtProp != null) {
                val query = entityManager.createQuery("$select where $modAtProp > :modifiedAt", resultClazz)
                query.setParameter("modifiedAt", settings.fromDate)
                return query
            }
            log.error("Modified since '${settings.fromDate}' not supported for entities of type '${clazz.simpleName}'. Database column to use is unknown. Selecting all entities for indexing")
        } else if (settings?.lastNEntries != null) {
            val orderByProp =
                    if (PfHistoryMasterDO::class.java.isAssignableFrom(clazz)) "pk"
                    else "id"
            val query = entityManager.createQuery("$select order by $orderByProp desc", resultClazz)
            query.maxResults = settings.lastNEntries
            return query
        }
        return entityManager.createQuery(select, resultClazz)
    }

    companion object {
        private const val MIN_REINDEX_ENTRIES_4_USE_SCROLL_MODE = 2000
        private val log = LoggerFactory.getLogger(DatabaseDao::class.java)
        /**
         * Since yesterday and 1,000 newest entries at maximimum.
         */
        @JvmStatic
        fun createReindexSettings(onlyNewest: Boolean): ReindexSettings {
            return if (onlyNewest) {
                val day = DayHolder()
                day.add(Calendar.DAY_OF_MONTH, -1) // Since yesterday:
                ReindexSettings(day.date, 1000) // Maximum 1,000 newest entries.
            } else {
                ReindexSettings()
            }
        }
    }
}
