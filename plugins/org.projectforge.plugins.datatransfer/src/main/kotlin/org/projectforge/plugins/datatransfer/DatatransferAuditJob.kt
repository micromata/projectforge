/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.Constants
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

  // Every 5 minutes, starting 5 minutes after starting.
  @Scheduled(fixedDelay = 5 * Constants.MILLIS_PER_MINUTE, initialDelay = 5 * Constants.MILLIS_PER_MINUTE)
  fun execute() {
    if (!pluginAdminService.activePlugins.any { it.id == DataTransferPlugin.ID }) {
      log.info("Plugin data transfer not activated. Don't need to send any notification.")
      return
    }
    log.info("Data transfer audit job started.")
    val startTimeInMillis = System.currentTimeMillis()

    var sentMailCounter = 0
    val areas = dataTransferAreaDao.internalLoadAll()
    areas.forEach { area ->
      val auditEntries = dataTransferAuditDao.internalGetQueuedEntriesByAreaId(area.id)
      val downloadAuditEntries = dataTransferAuditDao.internalGetDownloadEntriesByAreaId(area.id)
      if (!auditEntries.isNullOrEmpty()) {
        dataTransferNotificationMailService.sendMails(area, auditEntries, downloadAuditEntries)
        ++sentMailCounter
        dataTransferAuditDao.removeFromQueue(auditEntries)
      }
    }
    dataTransferAuditDao.deleteOldEntries(PFDateTime.now().minusDays(30)) // If you change this, you should change:
    // i18n: plugins.datatransfer.audit.events, plugins.datatransfer.audit.downloadEvents
    log.info("DataTransfer audit job finished after ${(System.currentTimeMillis() - startTimeInMillis) / 1000} seconds. Number of sent mails: $sentMailCounter.")
  }
}
