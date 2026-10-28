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

package org.projectforge.rest.poll

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.PfCaches
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.user.UserLocale
import org.projectforge.business.utils.HtmlHelper
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.HtmlSanitizer
import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * The mails of a poll. Each recipient gets a mail of their own, in their language: the invitation, the reminder of
 * the deadline, the end (the results only to the users with full access) and the note that somebody answered for
 * them. Sent from the owner's address, so a reply reaches the owner.
 */
@Service
class PollMailService {
    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var pollDao: PollDao

    /**
     * Invites the attendees (and informs the users with full access). [subject] and [text] replace the standard
     * subject and introduction if given; they are used as typed (no placeholders), the link and the deadline follow.
     */
    fun sendInvitation(poll: PollDO, subject: String?, text: String?) {
        val recipients = pollDao.getAttendeeIds(poll) + pollDao.getFullAccessUserIds(poll)
        recipients.forEach { userId ->
            send(poll, userId) { locale ->
                val intro = if (text.isNullOrBlank()) {
                    translateMsg(locale, "poll.mail.invitation.content", html(poll.title), html(ownerName(poll)))
                } else {
                    "<p>${html(text)}</p>"
                }
                Content(
                    subject?.takeIf { it.isNotBlank() } ?: translateMsg(locale, "poll.mail.invitation.subject", poll.title),
                    intro + translateMsg(locale, "poll.mail.link", link(poll), formatDeadline(poll, locale))
                )
            }
        }
    }

    /** Reminds the given attendees, who haven't answered yet, of the deadline in [days] days. */
    fun sendReminder(poll: PollDO, userIds: Collection<Long>, days: Long) {
        userIds.forEach { userId ->
            send(poll, userId) { locale ->
                Content(
                    translateMsg(locale, "poll.mail.reminder.subject", poll.title, days),
                    translateMsg(locale, "poll.mail.reminder.content", html(poll.title), html(ownerName(poll)))
                            + translateMsg(locale, "poll.mail.link", link(poll), formatDeadline(poll, locale))
                )
            }
        }
    }

    /**
     * Informs everybody of the end of the poll: the users with full access with the results as attachment, the
     * other attendees without.
     */
    fun sendFinished(poll: PollDO, excelFilename: String, excel: ByteArray?) {
        val fullAccess = pollDao.getFullAccessUserIds(poll)
        (pollDao.getAttendeeIds(poll) - fullAccess).forEach { userId ->
            send(poll, userId) { locale ->
                Content(
                    translateMsg(locale, "poll.mail.finished.subject", poll.title),
                    translateMsg(locale, "poll.mail.finished.content", html(poll.title), html(ownerName(poll)))
                )
            }
        }
        val attachments = excel?.let { listOf(MailAttachment(excelFilename, it)) }
        fullAccess.forEach { userId ->
            send(poll, userId, attachments) { locale ->
                Content(
                    translateMsg(locale, "poll.mail.finished.subject", poll.title),
                    translateMsg(locale, "poll.mail.finished.fullAccess.content", html(poll.title))
                            + translateMsg(locale, "poll.mail.resultsLink", resultsLink(poll))
                )
            }
        }
    }

    /** Informs the attendee that [by] answered on their behalf. */
    fun sendAnsweredOnBehalf(poll: PollDO, attendeeId: Long, by: PFUserDO) {
        send(poll, attendeeId) { locale ->
            Content(
                translateMsg(locale, "poll.mail.answeredOnBehalf.subject", poll.title),
                translateMsg(locale, "poll.mail.answeredOnBehalf.content", html(poll.title), html(by.displayName))
                        + translateMsg(locale, "poll.mail.link", link(poll), formatDeadline(poll, locale))
            )
        }
    }

    private class Content(val subject: String, val html: String)

    private fun send(
        poll: PollDO,
        userId: Long,
        attachments: List<MailAttachment>? = null,
        content: (Locale) -> Content,
    ) {
        val user = PfCaches.instance.getUser(userId)
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            log.info { "User $userId has no mail address, no mail of poll ${poll.id} sent." }
            return
        }
        try {
            val locale = UserLocale.determineUserLocale(user)
            val mailContent = content(locale)
            val mail = Mail()
            mail.subject = mailContent.subject
            mail.contentType = Mail.CONTENTTYPE_HTML
            // Texts of the owner (title, the text of the invitation) are escaped already: only the tags of the
            // mail texts pass, no scripts, images, forms or styles.
            mail.content = HtmlSanitizer.sanitize(mailContent.html)
            // Null: the standard sender of this installation.
            mail.from = PfCaches.instance.getUser(poll.owner?.id)?.email?.takeIf { it.isNotBlank() }
            mail.setTo(user)
            sendMail.send(mail, attachments = attachments)
        } catch (ex: Exception) {
            log.error(ex) { "Mail of poll ${poll.id} to user $userId failed: ${ex.message}" }
        }
    }

    private fun ownerName(poll: PollDO): String? = PfCaches.instance.getUser(poll.owner?.id)?.displayName

    private fun formatDeadline(poll: PollDO, locale: Locale): String =
        poll.deadline?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) ?: ""

    /** The absolute link to the answer page of the poll, on the domain of this installation. */
    fun link(poll: PollDO): String = domainService.getDomain("${Constants.NEXT_APP_PATH}poll/${poll.id}/answer")

    private fun resultsLink(poll: PollDO): String =
        domainService.getDomain("${Constants.NEXT_APP_PATH}poll/${poll.id}?tab=results")

    companion object {
        /** A text of the poll (title, a user's name, the invitation's text) as part of the HTML of a mail. */
        fun html(value: Any?): String = HtmlHelper.escapeHtml(value?.toString() ?: "", true)
    }
}
