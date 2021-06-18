/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.user.service.UserService
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileInfo
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
open class NotificationMailService {
  @Autowired
  private lateinit var domainService: DomainService

  @Autowired
  private lateinit var sendMail: SendMail

  @Autowired
  private lateinit var userService: UserService

  fun sendMail(
    event: AttachmentsEventType,
    file: FileInfo,
    dataTransfer: DataTransferAreaDO,
    byUser: PFUserDO?,
    byExternalUser: String?
  ) {
    val recipients = mutableListOf<Int>()
    if (event == AttachmentsEventType.DELETE) {
      val createdByUserId = file.createdByUser?.toIntOrNull()
      if (createdByUserId != null && createdByUserId != ThreadLocalUserContext.getUserId()) {
        // File object created by another user was deleted, so notify createdBy user:
        recipients.add(createdByUserId)
      }
    }
    StringHelper.splitToInts(dataTransfer.observerIds, ",", false).forEach {
      recipients.add(it)
    }
    /*
    Don't notify admins anymore.
    StringHelper.splitToInts(dataTransfer.adminIds, ",", false).forEach {
      recipients.add(it)
    }*/
    if (recipients.isNullOrEmpty()) {
      // No observers, admins and deleted files of other createdByUsers.
      return
    }
    val link = domainService.getDomain(
      PagesResolver.getDynamicPageUrl(
        DataTransferPageRest::class.java,
        id = dataTransfer.id ?: 0
      )
    )
    recipients.distinct().forEach { id ->
      val recipient = userService.internalGetById(id)
      val mail = prepareMail(recipient, event, file.fileName ?: "???", dataTransfer, link, byUser, byExternalUser)
      mail?.let {
        sendMail.send(it)
      }
    }
  }

  internal fun prepareMail(
    recipient: PFUserDO,
    event: AttachmentsEventType,
    fileName: String,
    dataTransfer: DataTransferAreaDO,
    link: String,
    byUser: PFUserDO?,
    byExternalUser: String?
  ): Mail? {
    if (recipient.id == byUser?.id) {
      // Don't send user his own events.
      return null
    }
    val titleKey: String
    val messageKey: String
    val byUserString: String
    if (byUser != null || byExternalUser == DataTransferJCRCleanUpJob.SYSTEM_USER) {
      titleKey = "plugins.datatransfer.mail.subject.title"
      messageKey = "plugins.datatransfer.mail.subject.msg"
      byUserString = byUser?.getFullname() ?: DataTransferJCRCleanUpJob.SYSTEM_USER
    } else {
      titleKey = "plugins.datatransfer.mail.subject.external.title"
      messageKey = "plugins.datatransfer.mail.subject.external.msg"
      byUserString = byExternalUser ?: "???"
    }
    val title = myTranslate(
      recipient,
      titleKey,
      dataTransfer.displayName,
      translate("plugins.datatransfer.mail.action.$event")
    )
    val message = myTranslate(
      recipient,
      messageKey,
      fileName,
      dataTransfer.displayName,
      byUserString,
      translate("plugins.datatransfer.mail.action.$event")
    )
    // EventInfo is given to mail renderer.
    val eventInfo = EventInfo(link = link, user = byUserString, message = message)

    val mail = Mail()
    mail.subject = message // Subject equals to message
    mail.contentType = Mail.CONTENTTYPE_HTML
    mail.setTo(recipient.email, recipient.getFullname())
    if (mail.to.isEmpty()) {
      log.error { "Recipient without mail address, no mail will be sent to '${recipient.getFullname()}: $dataTransfer" }
      return null
    }
    val data = mutableMapOf<String, Any>("eventInfo" to eventInfo)
    mail.content =
      sendMail.renderGroovyTemplate(mail, "mail/dataTransferMail.html", data, title, recipient)
    return mail
  }

  internal class EventInfo(val link: String, val user: String?, val message: String)

  companion object {
    private var _defaultLocale: Locale? = null
    private val defaultLocale: Locale
      get() {
        if (_defaultLocale == null) {
          _defaultLocale = ConfigurationServiceAccessor.get().defaultLocale ?: Locale.getDefault()
        }
        return _defaultLocale!!
      }

    private fun myTranslate(recipient: PFUserDO?, i18nKey: String, vararg params: Any): String {
      val locale = recipient?.locale ?: defaultLocale
      return I18nHelper.getLocalizedMessage(locale, i18nKey, *params)
    }

    private fun myTranslate(recipient: PFUserDO?, value: Boolean?): String {
      return myTranslate(recipient, if (value == true) "yes" else "no")
    }
  }
}
