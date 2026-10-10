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

package org.projectforge.plugins.datatransfer

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.Constants
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.framework.time.PFDateTime
import org.projectforge.plugins.core.PluginAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * This job is running hourly and will send notifications to observers if any action was audited in their observed
 * data transfer areas.
 * Outdated audit entries (older than 30 days will be deleted).
 */
@Component
class DatatransferAuditJob {
    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    @Autowired
    private lateinit var dataTransferAuditDao: DataTransferAuditDao

    @Autowired
    private lateinit var dataTransferNotificationMailService: DataTransferNotificationMailService

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    @Autowired
    private lateinit var schedulerJobRunner: SchedulerJobRunner

    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(SCHEDULER_JOB, ::execute) {
            if (pluginAdminService.activePlugins.any { it.id == DataTransferPlugin.ID }) null else "Plugin data transfer not activated."
        }
    }

    // Every 5 minutes, starting 5 minutes after starting.
    @Scheduled(fixedDelay = DELAY_MILLIS, initialDelay = DELAY_MILLIS)
    fun execute() {
        schedulerJobRunner.runAsync(SCHEDULER_JOB) {
            var sentMailCounter = 0
            val areas = dataTransferAreaDao.selectAll(checkAccess = false)
            // Fetch all queued and download audit entries with two bulk queries instead of two queries per area
            // (avoids flooding the database with a select per area on every run).
            val queuedEntriesByAreaId = dataTransferAuditDao.internalGetQueuedEntriesGroupedByAreaId()
            val downloadEntriesByAreaId =
                dataTransferAuditDao.internalGetDownloadEntriesGroupedByAreaId(queuedEntriesByAreaId.keys)
            areas.forEach { area ->
                val areaId = area.id ?: return@forEach
                val auditEntries = queuedEntriesByAreaId[areaId]
                if (!auditEntries.isNullOrEmpty()) {
                    val downloadAuditEntries = downloadEntriesByAreaId[areaId] ?: emptyList()
                    dataTransferNotificationMailService.sendMails(area, auditEntries, downloadAuditEntries)
                    ++sentMailCounter
                    dataTransferAuditDao.removeFromQueue(auditEntries)
                }
            }
            dataTransferAuditDao.deleteOldEntries(
                PFDateTime.now().minusDays(30)
            ) // If you change this, you should change:
            // i18n: plugins.datatransfer.audit.events, plugins.datatransfer.audit.downloadEvents
            if (sentMailCounter > 0) {
                log.info { "DataTransfer audit job finished. Number of sent mails: $sentMailCounter." }
            }
        }
    }

    companion object {
        private const val DELAY_MILLIS = 5 * Constants.MILLIS_PER_MINUTE

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "datatransfer.audit", SchedulerJobArea.FILES, DatatransferAuditJob::class.java, "execute",
            SchedulerSchedule.FixedDelay(DELAY_MILLIS, DELAY_MILLIS),
        )
    }
}
