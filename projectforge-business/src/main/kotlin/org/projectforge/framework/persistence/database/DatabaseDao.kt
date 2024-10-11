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

package org.projectforge.framework.persistence.database

import jakarta.persistence.EntityManagerFactory
import mu.KotlinLogging
import org.apache.commons.lang3.ClassUtils
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.session.SearchSession
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor
import org.projectforge.framework.persistence.api.ReindexSettings
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.DayHolder
import org.projectforge.framework.utils.NumberFormatter
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
                reindex(clazz, settings)
                sb.append(", ")
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
        entityManagerFactory.createEntityManager().use { em ->
            // totalEntries are given by Hibernate search to MassIndexingMonitor.
            // val totalEntries = em.createQuery("SELECT COUNT(u) FROM ${clazz.simpleName} u", Long::class.java).singleResult
            val searchSession: SearchSession = Search.session(em)
            try {
                // Starte den MassIndexer für eine bestimmte Entität (z.B. EmployeeDO)
                searchSession.massIndexer(clazz)
                    .threadsToLoadObjects(4) // Anzahl der Threads zum Laden von Entitäten
                    .batchSizeToLoadObjects(25) // Batch-Größe
                    .idFetchSize(150) // Größe des ID-Fetch
                    .monitor(IndexProgressMonitor(clazz)) // Fortschrittsmonitor hinzufügen
                    .startAndWait() // Blockiert, bis die Indizierung abgeschlossen ist
            } catch (ex: InterruptedException) {
                log.error(ex.message, ex)
            }
        }
    }

    companion object {
        /**
         * Since yesterday and 1,000 newest entries at maximimum.
         */
        @JvmStatic
        fun createReindexSettings(onlyNewest: Boolean): ReindexSettings {
            return if (onlyNewest) {
                val day = DayHolder()
                day.add(Calendar.DAY_OF_MONTH, -1) // Since yesterday:
                ReindexSettings(day.utilDate, 1000) // Maximum 1,000 newest entries.
            } else {
                ReindexSettings()
            }
        }
    }
}
