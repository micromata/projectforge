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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.feedback.SendFeedback
import org.projectforge.business.feedback.SendFeedbackData
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The "Send feedback" page ("Feedback senden"), successor of Wicket's `FeedbackPage` (`wa/feedback`).
 *
 * Like [SendTextMessageRest] it is a non-entity, standalone page: it exposes plain JSON so the next
 * frontend can render a small form (the read-only receiver and sender plus a description) and mail the
 * feedback through the reused business layer ([SendFeedback]). The Pacman button of the old Wicket form
 * is dropped.
 *
 * The receiver (the configured feedback address) and the sender (the logged-in user) are decided
 * server-side both when the form is loaded and when it is sent — the client only supplies the
 * description, so a manipulated request cannot redirect the mail elsewhere.
 *
 * CSRF is inherited via `RestAuthenticationUtils`/`RestCsrfProtection` for all `/rs` endpoints; the legacy
 * page was not 2FA-gated, so no 2FA registration is needed.
 */
@RestController
@RequestMapping("${Rest.URL}/feedback")
class FeedbackPageRest {
    @Autowired
    private lateinit var sendFeedback: SendFeedback

    /** Initial form data: the prefilled receiver and sender, both read-only on the client. */
    class InitialData(
        val receiver: String? = null,
        val sender: String? = null,
    )

    /** Body of [send]: only the description is the user's — receiver and sender are set server-side. */
    class SendRequest(
        val description: String? = null,
    )

    /** Result of [send]: `success` drives a success vs. error toast on the client, `message` is localized. */
    class SendResult(
        val success: Boolean,
        val message: String,
    )

    @AccessChecked("Any logged-in user (as in Wicket); receiver and sender are set server side")
    @GetMapping
    fun getInitialData(): InitialData {
        return InitialData(receiver = feedbackReceiver(), sender = sender())
    }

    @AccessChecked("Any logged-in user (as in Wicket); receiver and sender are set server side")
    @PostMapping("send")
    fun send(@RequestBody postData: SendRequest): SendResult {
        val sender = sender()
        val data = SendFeedbackData()
        data.receiver = feedbackReceiver()
        data.sender = sender
        data.subject = "Feedback from $sender"
        data.description = postData.description
        val result = try {
            // Synchronous: the outcome is reported to the user as a toast, so a failing SMTP connection
            // must surface as a failure here instead of the optimistic "handed over" of the async default.
            sendFeedback.send(data, async = false)
        } catch (ex: Throwable) {
            log.error(ex.message, ex)
            false
        }
        return if (result) {
            SendResult(true, translate("feedback.mailSendSuccessful"))
        } else {
            SendResult(false, translate("mail.error.exception"))
        }
    }

    private fun feedbackReceiver(): String? {
        return Configuration.instance.getStringValue(ConfigurationParam.FEEDBACK_E_MAIL)
    }

    private fun sender(): String? {
        return ThreadLocalUserContext.loggedInUser?.getFullname()
    }
}
