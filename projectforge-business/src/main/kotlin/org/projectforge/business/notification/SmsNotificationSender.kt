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

import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.messaging.SmsSender
import org.projectforge.messaging.SmsSender.HttpResponseCode
import org.projectforge.sms.SmsSenderConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * Sends the notification as SMS to the mobile phone of the recipient: the SMS text of the rule, otherwise the subject
 * and the link. Synchronously to get the outcome.
 */
@Service
class SmsNotificationSender : NotificationChannelSender {
    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    override val channel = NotificationChannel.SMS

    override fun deliver(notification: NotificationDO, recipient: PFUserDO, now: Date): String? {
        if (!smsSenderConfig.isSmsConfigured()) {
            return "SMS isn't configured."
        }
        val mobilePhone = recipient.mobilePhone
        if (!NumberHelper.matchesPhoneNumber(mobilePhone)) {
            return "The recipient has no mobile phone number."
        }
        val message = buildMessage(notification)
        if (message.isBlank()) {
            return "The SMS text is empty."
        }
        return when (val code = SmsSender(smsSenderConfig).send(NumberHelper.extractPhonenumber(mobilePhone), message)) {
            HttpResponseCode.SUCCESS -> null
            // Not retried: UNKNOWN_ERROR is also an unexpected answer or a timeout of a gateway that may have sent the
            // SMS, a retry would send it again. The other errors are permanent anyway.
            else -> "SMS not sent ($code)."
        }
    }

    /** The SMS text of the notification, otherwise its subject and link, cut to the max length of an SMS. */
    internal fun buildMessage(notification: NotificationDO): String {
        val message = notification.smsText?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(
                notification.title?.takeIf { it.isNotBlank() },
                notification.link?.takeIf { it.isNotBlank() }?.let { notificationLinks.absolute(it) },
            ).joinToString("\n")
        return truncate(message.trim(), smsSenderConfig.smsMaxMessageLength)
    }

    companion object {
        internal fun truncate(message: String, maxLength: Int): String {
            if (maxLength <= 0 || message.length <= maxLength) {
                return message
            }
            return "${message.take(maxLength - 1).trimEnd()}…"
        }
    }
}
