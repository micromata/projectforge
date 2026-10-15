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

package org.projectforge.business.feedback

import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Sends the feedback mail. Moved here from projectforge-wicket (with the removal of the Wicket feedback
 * page) so it lives in a module projectforge-rest can reach, next to [SendMail]. The only caller is
 * [org.projectforge.rest.FeedbackPageRest].
 */
@Service
class SendFeedback {
    @Autowired
    private lateinit var sendMail: SendMail

    /**
     * Sender is set automatically to the logged-in context user if not given.
     *
     * @param async If true (the default), the mail is handed to a background worker and the return value
     * only says it was accepted, not that it was actually delivered. Pass false where the outcome is
     * reported to the user (as [org.projectforge.rest.FeedbackPageRest] does with a success/error toast),
     * so a failing SMTP connection surfaces as a failure instead of a false success.
     */
    @JvmOverloads
    fun send(data: SendFeedbackData, async: Boolean = true): Boolean {
        if (data.sender == null) {
            data.sender = ThreadLocalUserContext.loggedInUser?.getFullname()
        }
        val params = mutableMapOf<String, Any?>("data" to data, "subject" to data.subject)
        val msg = Mail()
        msg.addTo(data.receiver)
        msg.setProjectForgeSubject(data.subject)
        val content = sendMail.renderGroovyTemplate(
            msg,
            "mail/feedback.txt",
            params,
            I18nHelper.getLocalizedMessage("administration.configuration.param.feedbackEMail.label"),
            ThreadLocalUserContext.loggedInUser,
        )
        msg.setContent(content)
        msg.contentType = Mail.CONTENTTYPE_TEXT
        return sendMail.send(msg, null, null, async)
    }
}
