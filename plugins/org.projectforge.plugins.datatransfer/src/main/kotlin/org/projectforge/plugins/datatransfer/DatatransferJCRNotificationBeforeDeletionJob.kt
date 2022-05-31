/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserLocale
import org.projectforge.common.StringHelper
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.time.PFDay
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * This jobs runs nightly and will inform all observers about files to be deleted within the next days automatically by
 * ProjectForge.
 */
@Component
class DatatransferJCRNotificationBeforeDeletionJob {
  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var notificationMailService: NotificationMailService

  @Autowired
  private lateinit var pluginAdminService: PluginAdminService

  /**
   * Runs nightly at 4:30
   * second, minute, hour
   */
  @Scheduled(cron = "0 30 4 * * *")
  fun execute() {
    if (!pluginAdminService.activePlugins.any { it.id == DataTransferPlugin.ID }) {
      log.info("Plugin data transfer not activated. Don't need notification job.")
      return
    }
    if (PFDay.now().isHolidayOrWeekend()) {
      log.info("Don't send notifications on files being deleted on holidays and weekends.")
      return
    }
    // key is the user id of the observer and the value is the list of observed attachments (including data transfer
    // area which will being deleted by the system.
    val notificationInfoByObserver = mutableMapOf<Int, MutableList<NotificationMailService.AttachmentNotificationInfo>>()
    log.info("Data transfer notification job started.")
    val startTimeInMillis = System.currentTimeMillis()

    // First of all, try to check all attachments of active areas:
    dataTransferAreaDao.internalLoadAll().forEach { dbo ->
      dbo.id?.let { id ->
        val expiryDays = dbo.expiryDays ?: 30
        val notifyDaysBeforeDeletion = getNotificationDaysBeforeDeletion(expiryDays)
        val expiryMillis = expiryDays.toLong() * DataTransferJCRCleanUpJob.MILLIS_PER_DAY
        val attachments = attachmentsService.internalGetAttachments(
          dataTransferAreaPagesRest.jcrPath!!,
          id
        )
        val observers = StringHelper.splitToInts(dbo.observerIds, ",", true)
        attachments?.forEach { attachment ->
          val time = attachment.lastUpdate?.time ?: attachment.created?.time
          if (time != null && startTimeInMillis - time + notifyDaysBeforeDeletion > expiryMillis) {
            val expiresInMillis = time + expiryMillis - startTimeInMillis
            val date = Date(time + expiryMillis)
            observers.forEach { userId ->
              val user = UserGroupCache.getInstance().getUser(userId)
              val locale = UserLocale.determineUserLocale(user)
              var observerAttachments = notificationInfoByObserver[userId]
              if (observerAttachments == null) {
                observerAttachments = mutableListOf()
                notificationInfoByObserver[userId] = observerAttachments
              }
              observerAttachments.add(NotificationMailService.AttachmentNotificationInfo(attachment, dbo, date, expiresInMillis, locale))
            }
          }
        }
      }
    }
    notificationInfoByObserver.forEach { (userId, attachments) ->
      notificationMailService.sendNotificationMail(userId, attachments)
    }
    log.info(
      "JCR notification job finished after ${(System.currentTimeMillis() - startTimeInMillis) / 1000} seconds. Number of notification mails: ${notificationInfoByObserver.size}"
    )
  }

  private fun getNotificationDaysBeforeDeletion(expiryDays: Int): Long {
    val notificationDays = when {
      expiryDays <= 10 -> -1 // Don't notify.
      expiryDays <= 30 -> 7  // Notify 7 days before being deleted.
      expiryDays <= 90 -> 14 // Notify 14 days before being deleted.
      else -> 30             // Notify 30 days before being deleted.
    }
    return notificationDays * DataTransferJCRCleanUpJob.MILLIS_PER_DAY
  }
}
