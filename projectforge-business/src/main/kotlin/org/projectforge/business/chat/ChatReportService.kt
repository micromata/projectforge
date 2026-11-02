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

package org.projectforge.business.chat

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import org.projectforge.business.notification.NotificationChannel
import org.projectforge.business.notification.NotificationDelivery
import org.projectforge.business.notification.NotificationDeliveryStep
import org.projectforge.business.notification.NotificationDisplay
import org.projectforge.business.notification.NotificationService
import org.projectforge.business.notification.NotificationSeverity
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * Reporting a message (PF-CHAT-MSG-12), the way for a user to bring an abusive message to the administrators, who have
 * no access to conversations. The report keeps an excerpt of the message ([ChatReportDO]) and every administrator is
 * notified (in the app and by mail) with the reporter, the author, the conversation, the reason and the excerpt, the
 * only place where chat content leaves a conversation (SEC-09). The administrators act on the account of the author
 * (deactivate it); the reporter may also block the author ([ChatBlockService]).
 */
@Service
open class ChatReportService {
    @Autowired
    private lateinit var chatAccess: ChatAccess

    @Autowired
    private lateinit var blockService: ChatBlockService

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /**
     * Reports a message of another user, which the logged-in user may read. Author, conversation and excerpt are
     * taken from the stored message. Reporting a message again stores and notifies nothing. With [blockAuthor] the
     * logged-in user blocks the author as well, also for a message reported before.
     * @throws AccessException if the logged-in user may not read the conversation of the message.
     * @throws UserException for an own, deleted or unknown message.
     */
    open fun report(messageId: Long?, reason: String?, blockAuthor: Boolean = false) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val message = chatService.findMessage(messageId) ?: throw UserException(ERROR_UNKNOWN_MESSAGE)
        val context = chatAccess.requireContext(message.conversationId, user)
        if (message.deleted) { // A hidden one may be reported: the moderators' decision can be questioned.
            throw UserException(ERROR_UNKNOWN_MESSAGE)
        }
        if (message.authorId == user.id) {
            throw UserException(ERROR_OWN_MESSAGE)
        }
        val trimmedReason = reason?.trim()?.takeIf { it.isNotEmpty() }?.take(ChatReportDO.REASON_LENGTH)
        val snapshot = message.contentText?.trim()?.take(ChatReportDO.SNAPSHOT_LENGTH)
        fun findExisting(): ChatReportDO? = persistenceService.selectSingleResult(
            "from ChatReportDO r where r.reporterId = :reporterId and r.messageId = :messageId",
            ChatReportDO::class.java,
            Pair("reporterId", user.id),
            Pair("messageId", message.id),
        )
        val created = try {
            persistenceService.runInTransaction { pc ->
                val existing = pc.selectSingleResult(
                    "from ChatReportDO r where r.reporterId = :reporterId and r.messageId = :messageId",
                    ChatReportDO::class.java,
                    Pair("reporterId", user.id),
                    Pair("messageId", message.id),
                )
                if (existing != null) {
                    return@runInTransaction false
                }
                pc.em.persist(ChatReportDO().also {
                    it.created = Date()
                    it.messageId = message.id
                    it.conversationId = message.conversationId
                    it.reporterId = user.id
                    it.authorId = message.authorId
                    it.reason = trimmedReason
                    it.contentSnapshot = snapshot
                })
                pc.em.flush()
                true
            }
        } catch (ex: Exception) {
            // Reported at the same time (unique constraint)? Then the administrators are notified already. Any other
            // failure must not look like a report made.
            if (findExisting() == null) {
                throw ex
            }
            log.info { "Report of message #${message.id} stored concurrently: ${ex.message}" }
            false
        }
        if (created) {
            notifyAdmins(user.id!!, message, context.title, trimmedReason, snapshot)
        }
        if (blockAuthor) {
            blockService.block(message.authorId!!)
        }
    }

    private fun notifyAdmins(
        reporterId: Long,
        message: ChatMessageDO,
        conversationTitle: String,
        reason: String?,
        snapshot: String?,
    ) {
        val reporterName = userGroupCache.getUser(reporterId)?.getFullname() ?: "?"
        val authorName = userGroupCache.getUser(message.authorId)?.getFullname() ?: "?"
        userGroupCache.adminUsers.forEach { adminId ->
            try {
                val admin = userGroupCache.getUser(adminId)?.takeIf { !it.deleted && it.hasSystemAccess() }
                    ?: return@forEach
                // All texts are user data (names, the room name, the reason, the message): escaped, so they can't bring
                // markup into the notification.
                val text = StringEscapeUtils.escapeHtml4(
                    I18nHelper.getLocalizedMessage(
                        admin, "chat.report.notification.text", reporterName, authorName, conversationTitle,
                    )
                )
                val body = StringBuilder("<p>$text</p>")
                reason?.let {
                    body.append("<p><b>${escape(admin, "chat.report.reason")}:</b> ${StringEscapeUtils.escapeHtml4(it)}</p>")
                }
                snapshot?.let {
                    body.append(
                        "<p><b>${escape(admin, "chat.report.message")}:</b> ${StringEscapeUtils.escapeHtml4(it)}</p>"
                    )
                }
                notificationService.notifyDirect(
                    recipientId = adminId,
                    title = I18nHelper.getLocalizedMessage(admin, "chat.report.notification.title", reporterName),
                    body = body.toString(),
                    link = null, // The administrators may not open the conversation.
                    dedupKey = "chat:report:${message.id}:$reporterId:$adminId",
                    delivery = REPORT_DELIVERY,
                    display = NotificationDisplay.BANNER,
                    severity = NotificationSeverity.IMPORTANT,
                )
            } catch (ex: Exception) {
                log.error(ex) { "Notification of administrator #$adminId about message #${message.id} failed: ${ex.message}" }
            }
        }
    }

    private fun escape(admin: org.projectforge.framework.persistence.user.entities.PFUserDO, i18nKey: String): String {
        return StringEscapeUtils.escapeHtml4(I18nHelper.getLocalizedMessage(admin, i18nKey))
    }

    companion object {
        const val ERROR_UNKNOWN_MESSAGE = "chat.report.error.unknownMessage"
        const val ERROR_OWN_MESSAGE = "chat.report.error.ownMessage"

        private val REPORT_DELIVERY = NotificationDelivery(
            listOf(
                NotificationDeliveryStep(NotificationChannel.IN_APP, 0),
                NotificationDeliveryStep(NotificationChannel.MAIL, 0),
            )
        )
    }
}
