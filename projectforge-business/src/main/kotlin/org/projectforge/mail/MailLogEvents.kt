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

package org.projectforge.mail

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogNotify

object MailLogEvents {
    @JvmField
    val NOT_CONFIGURED = LogEvent(
        code = "mail.notConfigured",
        category = LogCategory.CONFIG,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "A mail should be sent, but sending of mails isn't configured. The mail is dropped.",
        action = "Configure the mail server (mail.session.pfmailsession.* in projectforge.properties) if this installation should send mails.",
    )

    @JvmField
    val SEND_FAILED = LogEvent(
        code = "mail.sendFailed",
        category = LogCategory.EXTERNAL,
        explanation = "A mail couldn't be created or delivered to the mail server. The user got an error message.",
        action = "Check whether the mail server is reachable and accepts the configured credentials and sender address (mail.session.pfmailsession.*).",
    )

    @JvmField
    val MISSING_RECIPIENT = LogEvent(
        code = "mail.missingRecipient",
        category = LogCategory.BUG,
        explanation = "A mail without any recipient should be sent: the code calling SendMail didn't check it.",
        action = "Find the caller in the stack trace or by the message and check its recipients.",
    )
}
