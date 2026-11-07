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


package org.projectforge.business.notification

import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/** Sends the notification as mail (template `mail/notificationMail.html`), synchronously to get the outcome. */
@Service
class MailNotificationSender : NotificationChannelSender {
    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    override val channel = NotificationChannel.MAIL

    override fun deliver(notification: NotificationDO, recipient: PFUserDO, now: Date): String? {
        if (!sendMail.isConfigured) {
            return "Mailing isn't configured."
        }
        val mail = buildMail(notification, recipient) ?: return "The recipient has no mail address."
        if (!sendMail.send(mail, null, null, false)) {
            return "Mail not sent."
        }
        return null
    }

    internal fun buildMail(notification: NotificationDO, recipient: PFUserDO): Mail? {
        val mail = Mail()
        mail.setTo(recipient)
        if (mail.to.isEmpty()) {
            return null
        }
        val subject = notification.title ?: ""
        mail.subject = subject
        mail.contentType = Mail.CONTENTTYPE_HTML
        val body = notification.body ?: ""
        val data = mutableMapOf<String, Any?>("body" to body)
        // Rule handlers may embed their link in the body already (chat notifications don't).
        val linkUrl = notification.link?.takeIf { it.isNotBlank() }?.let { notificationLinks.absolute(it) }
        if (linkUrl != null && !body.contains(linkUrl)) {
            data["linkUrl"] = linkUrl
            data["linkText"] = I18nHelper.getLocalizedMessage(recipient, "notification.open")
        }
        mail.content = sendMail.renderGroovyTemplate(mail, TEMPLATE, data, subject, recipient)
        return mail
    }

    companion object {
        const val TEMPLATE = "mail/notificationMail.html"
    }
}
