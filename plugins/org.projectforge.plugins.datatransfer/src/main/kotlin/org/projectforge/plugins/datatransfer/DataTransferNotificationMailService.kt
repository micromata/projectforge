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

package org.projectforge.plugins.datatransfer

import mu.KotlinLogging
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.user.UserLocale
import org.projectforge.business.user.service.UserService
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.projectforge.plugins.datatransfer.rest.DataTransferPageRest
import org.projectforge.rest.core.PagesResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard
 */
@Service
open class DataTransferNotificationMailService {
  @Autowired
  private lateinit var domainService: DomainService

  @Autowired
  private lateinit var sendMail: SendMail

  @Autowired
  private lateinit var userService: UserService

  internal lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Suppress("unused")
  class AttachmentNotificationInfo(
    val attachment: Attachment,
    val dataTransferArea: DataTransferAreaDO,
    val expiryDate: Date, // Used by e-mail template
    val expiresInMillis: Long,
    locale: Locale,
  ) {
    var link: String? = null // link to dataTransferArea.
    val expiresInTimeLeft = DataTransferUtils.expiryTimeLeft(attachment, dataTransferArea.expiryDays, locale)
  }

  fun sendMails(
    area: DataTransferAreaDO,
    auditEntries: List<DataTransferAuditDO>,
    downloadAuditEntries: List<DataTransferAuditDO>,
  ): Int {
    // First detect all recipients by checking all audit entries:
    val recipients = mutableSetOf<Long>()
    val observerIds = StringHelper.splitToLongs(area.observerIds, ",")
    auditEntries.forEach { audit ->
      registerRecipients(audit, observerIds, recipients)
    }
    /*
    Don't notify admins anymore.
    StringHelper.splitToInts(dataTransfer.adminIds, ",", false).forEach {
      recipients.add(it)
    }*/
    if (recipients.isEmpty()) {
      // No observers, admins and deleted files of other createdByUsers.
      return 0
    }
    val link = domainService.getDomain(
      PagesResolver.getDynamicPageUrl(
        DataTransferPageRest::class.java,
        id = area.id ?: 0
      )
    )
    var counter = 0
    recipients.distinct().forEach { id ->
      val recipient = userService.internalGetById(id)
      val mail = prepareMail(recipient, area, link, auditEntries, downloadAuditEntries)
      mail?.let {
        try {
          if (sendMail.send(it)) {
            ++counter
          }
        } catch (ex: Exception) {
          log.error(ex.message, ex)
        }
      }
    }
    return counter
  }

  /**
   * Sends an email with files being deleted to an observer.
   */
  fun sendNotificationMail(
    userId: Long,
    notificationInfoList: List<DataTransferNotificationMailService.AttachmentNotificationInfo>?
  ) {
    if (notificationInfoList.isNullOrEmpty()) {
      return
    }
    val recipient = userService.internalGetById(userId)
    if (recipient == null) {
      log.error { "Can't determine observer by id: $userId" }
      return
    }
    if (!recipient.hasSystemAccess()) {
      // Observer has now system access (anymore). Don't send any notification.
      return
    }
    prepareMail(recipient, notificationInfoList)?.let {
      sendMail.send(it)
    }
  }

  internal fun prepareMail(
    recipient: PFUserDO,
    dataTransfer: DataTransferAreaDO,
    link: String,
    auditEntries: List<DataTransferAuditDO>,
    downloadAuditEntries: List<DataTransferAuditDO>,
  ): Mail? {
    val locale = UserLocale.determineUserLocale(recipient)
    val foreignAuditEntries = auditEntries.filter { it.byUser?.id != recipient.id }
    if (foreignAuditEntries.isEmpty()) {
      // Don't send user his own events.
      return null
    }
    if (!dataTransferAreaDao.hasSelectAccess(dataTransfer, recipient)) {
      // Recipient has no access, so skip mail.
      return null
    }
    foreignAuditEntries.forEach { it.createdByUserAsString(locale) }
    downloadAuditEntries.forEach { it.createdByUserAsString(locale) }
    val title =
      I18nHelper.getLocalizedMessage(recipient, "plugins.datatransfer.mail.observe.subject", dataTransfer.displayName)
    val message =
      I18nHelper.getLocalizedMessage(recipient, "plugins.datatransfer.mail.observe.message", dataTransfer.displayName)
    val mail = Mail()
    mail.subject = title // Subject equals to message
    mail.contentType = Mail.CONTENTTYPE_HTML
    mail.setTo(recipient.email, recipient.getFullname())
    if (mail.to.isEmpty()) {
      log.error { "Recipient without mail address, no mail will be sent to '${recipient.getFullname()}: $dataTransfer" }
      return null
    }
    val data = mutableMapOf<String, Any?>(
      "link" to link,
      "message" to message,
      "auditEntries" to foreignAuditEntries,
      "externalAccessEnabled" to dataTransfer.externalAccessEnabled,
      "downloadAuditEntries" to downloadAuditEntries,
    )
    mail.content =
      sendMail.renderGroovyTemplate(mail, "mail/dataTransferMail.html", data, title, recipient)
    return mail
  }

  internal fun prepareMail(
    recipient: PFUserDO,
    notificationInfoList: List<DataTransferNotificationMailService.AttachmentNotificationInfo>
  ): Mail? {
    val locale = UserLocale.determineUserLocale(recipient)
    notificationInfoList.forEach { info ->
      if (info.link == null) {
        info.link = domainService.getDomain(
          PagesResolver.getDynamicPageUrl(
            DataTransferPageRest::class.java,
            id = info.dataTransferArea.id ?: 0
          )
        )
      }
      info.attachment.addExpiryInfo(
        DataTransferUtils.expiryTimeLeft(
          info.attachment,
          info.dataTransferArea.expiryDays,
          locale,
        )
      )
    }
    val sortedList = notificationInfoList.filter {
      dataTransferAreaDao.hasSelectAccess(it.dataTransferArea, recipient)
    }.sortedBy { it.expiresInMillis }

    if (sortedList.isEmpty()) {
      // OK, all entries with no user access (does it really occur?)
      return null
    }
    val mail = Mail()
    mail.subject =
      I18nHelper.getLocalizedMessage(recipient, "plugins.datatransfer.mail.notificationBeforeDeletion.subject")
    mail.contentType = Mail.CONTENTTYPE_HTML
    mail.setTo(recipient.email, recipient.getFullname())
    if (mail.to.isEmpty()) {
      log.error { "Recipient without mail address, no mail will be sent to '${recipient.getFullname()} about files being deleted." }
      return null
    }
    val data = mutableMapOf<String, Any?>(
      "attachments" to sortedList,
      "subject" to mail.subject,
      "locale" to locale,
    )
    mail.content =
      sendMail.renderGroovyTemplate(mail, "mail/dataTransferFilesBeingDeletedMail.html", data, mail.subject, recipient)
    return mail
  }

  companion object {
    internal fun registerRecipients(audit: DataTransferAuditDO, observerIds: LongArray, recipients: MutableSet<Long>) {
      audit.eventType?.let { eventType ->
        if (eventType == AttachmentsEventType.DELETE || eventType == AttachmentsEventType.MODIFICATION) {
          val uploadByUserId = audit.uploadByUser?.id
          if (uploadByUserId != null && uploadByUserId != audit.byUser?.id) {
            // File object created by another user was deleted or modified, so notify uploadBy user:
            recipients.add(uploadByUserId)
          }
        }
        if (eventType.isIn(AttachmentsEventType.MODIFICATION, AttachmentsEventType.UPLOAD, AttachmentsEventType.DELETE)) {
          val byUserId = audit.byUser?.id
          // Not a download event, so inform the oberservers about (UPLOAD and MODIFICATION), if not done by themselves.
          observerIds.forEach { observerId ->
            if (observerId != byUserId) {
              recipients.add(observerId)
            }
          }
        }
      }
    }
  }
}
