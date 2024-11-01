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

package org.projectforge.framework.persistence.search

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManagerFactory
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity
import org.projectforge.common.StringHelper
import org.projectforge.framework.configuration.Configuration.Companion.instance
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.api.ReindexSettings
import org.projectforge.framework.persistence.database.DatabaseDao
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class HibernateSearchReindexer {
    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var databaseDao: DatabaseDao

    private var currentReindexRun: Date? = null

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    private lateinit var indexedEntities: Collection<SearchIndexedEntity<*>>

    @PostConstruct
    private fun postConstruct() {
        indexedEntities = Search.mapping(entityManagerFactory).allIndexedEntities()
    }

    fun execute() {
        log.info("Re-index job started.")
        /*if (databaseDao == null) {
            log.error("Job not configured, aborting.")
            return
        }*/
        val result = rebuildDatabaseSearchIndices()
        if (result.contains("*")) {
            log.error(ERROR_MSG)
            val recipients = instance
                .getStringValue(ConfigurationParam.SYSTEM_ADMIN_E_MAIL)
            if (StringUtils.isNotBlank(recipients)) {
                log.info("Try to inform administrator about re-indexing error.")
                val msg = Mail()
                msg.addTo(recipients)
                msg.setProjectForgeSubject("Error while re-indexing ProjectForge data-base.")
                msg.content = """
                    $ERROR_MSG
                    
                    Result:
                    $result
                    """.trimIndent()
                msg.contentType = Mail.CONTENTTYPE_TEXT
                sendMail.send(msg, null, null)
            }
        }
        log.info("Re-index job finished successfully.")
    }

    fun rebuildDatabaseSearchIndices(settings: ReindexSettings, vararg classes: Class<*>): String {
        if (currentReindexRun != null) {
            val sb = StringBuilder()
            if (classes != null && classes.size > 0) {
                var first = true
                for (cls in classes) {
                    first = StringHelper.append(sb, first, cls.name, ", ")
                }
            }
            val date = DateTimeFormatter.instance().getFormattedDateTime(
                currentReindexRun, Locale.ENGLISH,
                DateHelper.UTC
            )
            log.info(
                ("Re-indexing of '" + sb.toString()
                        + "' cancelled due to another already running re-index job started at " + date + " (UTC):")
            )
            return "Another re-index job is already running. The job was started at: $date"
        }
        synchronized(this) {
            try {
                currentReindexRun = Date()
                val sb = StringBuilder()
                if (classes != null && classes.size > 0) {
                    for (cls in classes) {
                        reindex(cls, settings, sb)
                    }
                } else {
                    // Re-index of all ProjectForge entities:
                    indexedEntities.forEach { entity ->
                        reindex(entity.javaClass, settings, sb)
                    }
                }
                return sb.toString()
            } finally {
                currentReindexRun = null
            }
        }
    }

    private fun reindex(clazz: Class<*>, settings: ReindexSettings, sb: StringBuilder) {
        try {
            // Try to check, if class is available (entity of ProjectForge's core or of active plugin).
            persistenceService.selectSingleResult(
                "select t from " + clazz.name + " t where t.id = :id",
                clazz,
                "id" to -1,
                nullAllowed = false,
            )
        } catch (ex: Exception) {
            if (HistoryEntryDO::class.java != clazz) {
                log.info("Class '$clazz' not available (OK for non-active plugins and HistoryEntryDO).")
            }
            return
        }
        // PF-378: Performance of run of full re-indexing the data-base is very slow for large data-bases
        // Single transactions needed, otherwise the full run will be very slow for large data-bases.
        try {
            databaseDao.reindex(clazz, settings, sb)
        } catch (ex: Exception) {
            sb.append(" (an error occured, see log file for further information.), ")
            log.error("While rebuilding data-base-search-index for '" + clazz.name + "': " + ex.message, ex)
        }
    }

    fun rebuildDatabaseSearchIndices(): String {
        return rebuildDatabaseSearchIndices(ReindexSettings())
    }

    companion object {
        private const val ERROR_MSG =
            ("Error while re-indexing data base: found lock files while re-indexing data-base. "
                    + "Try to run re-index manually in the web administration menu and if occured again, "
                    + "shutdown ProjectForge, delete lock file(s) in hibernate-search sub directory and restart.")
    }
}
