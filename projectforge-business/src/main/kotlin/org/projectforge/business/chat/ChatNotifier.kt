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
import org.projectforge.business.notification.NotificationChannel
import org.projectforge.business.notification.NotificationDelivery
import org.projectforge.business.notification.NotificationDeliveryStep
import org.projectforge.business.notification.NotificationDisplay
import org.projectforge.business.notification.NotificationService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.menu.builder.MenuItemDefId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private val log = KotlinLogging.logger {}

/**
 * Notifies about chat messages via the notification system (PF-CHAT NOT-02 – NOT-05), in the background (a mail
 * mustn't delay sending a message):
 * - a mentioned user at once, in the app (banner until read, menu badge) and by mail;
 * - the participants about a new message of the conversation, the followers of a thread about a new reply: once per
 *   conversation until read (in the app, a mail after [NEW_MESSAGE_MAIL_DELAY_MINUTES] minutes if not seen), not if
 *   muted or watching the conversation right now.
 * Every recipient is checked against the read access of the host again. The notifications name only the conversation,
 * its host title and the author; the text of the message only if the host allows it.
 * Reading the conversation resolves the notifications ([resolve]).
 */
@Service
class ChatNotifier {
    @Autowired
    private lateinit var hostRegistry: ChatHostRegistry

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired(required = false)
    private var eventListeners: List<ChatEventListener> = emptyList()

    /** Single thread: the notifications of one message are created in the order of the messages. Tests run inline. */
    var executor: Executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ChatNotifier").also { it.isDaemon = true }
    }

    /** What is notified about a message, collected while it is sent (the message itself may change meanwhile). */
    class MessageInfo(
        val conversationId: Long,
        val hostType: String,
        val hostId: Long,
        val channel: String,
        val title: String,
        val messageId: Long,
        val threadRootId: Long?,
        val authorId: Long,
        /** The sanitized HTML of the message. */
        val html: String,
        /** The users mentioned for the first time by this message (or this edit). */
        val newlyMentionedIds: List<Long>,
        /** Notify about a new message (not for an edit). */
        val isNew: Boolean,
        /** The followers of the thread, for a reply. */
        val threadFollowerIds: Set<Long> = emptySet(),
    )

    fun onMessage(info: MessageInfo) {
        executor.execute {
            try {
                notify(info)
            } catch (ex: Exception) {
                log.error(ex) { "Notifications of chat message #${info.messageId} failed: ${ex.message}" }
            }
        }
    }

    /** Resolves the notifications of the user about the conversation (read). */
    fun resolve(conversationId: Long, userId: Long) {
        executor.execute {
            try {
                notificationService.resolveByDedupPrefix(userId, newPrefix(conversationId, userId))
                notificationService.resolveByDedupPrefix(userId, mentionPrefix(conversationId, userId))
            } catch (ex: Exception) {
                log.error(ex) { "Resolving chat notifications of conversation #$conversationId failed: ${ex.message}" }
            }
        }
    }

    internal fun notify(info: MessageInfo) {
        val host = hostRegistry.get(info.hostType) ?: return
        val author = userGroupCache.getUser(info.authorId)
        val authorName = author?.getFullname() ?: "?"
        val link = "/chat/${info.conversationId}?message=${info.messageId}"
        val withContent = host.mailContentAllowed(info.hostId, info.channel)
        fun mayRead(user: PFUserDO?): Boolean {
            return user != null && !user.deleted && user.hasSystemAccess() &&
                    host.hasReadAccess(user, info.hostId, info.channel)
        }

        val mentioned = mutableSetOf<Long>()
        info.newlyMentionedIds.filter { it != info.authorId }.forEach { userId ->
            val user = userGroupCache.getUser(userId)
            if (!mayRead(user)) {
                return@forEach
            }
            mentioned += userId
            notificationService.notifyDirect(
                recipientId = userId,
                title = I18nHelper.getLocalizedMessage(user, "chat.notification.mention.title", authorName, info.title),
                body = body(user!!, "chat.notification.mention.text", authorName, info, withContent),
                link = link,
                dedupKey = "${mentionPrefix(info.conversationId, userId)}${info.messageId}",
                delivery = MENTION_DELIVERY,
                display = NotificationDisplay.BANNER,
                menuBadge = MenuItemDefId.CHAT,
            )
        }
        if (!info.isNew) {
            return
        }
        val recipients = if (info.threadRootId == null) {
            host.participantIds(info.hostId, info.channel) + host.extraRecipientIds(info.hostId, info.channel)
        } else {
            info.threadFollowerIds
        }
        val muted = mutedUserIds(info.conversationId)
        recipients.asSequence()
            .filter { it != info.authorId && it !in mentioned && it !in muted }
            .filter { userId -> eventListeners.none { it.isWatching(info.conversationId, userId) } }
            .forEach { userId ->
                val user = userGroupCache.getUser(userId)
                if (!mayRead(user)) {
                    return@forEach
                }
                val prefix = newPrefix(info.conversationId, userId)
                if (notificationService.hasPendingByDedupPrefix(userId, prefix)) {
                    return@forEach // Already notified about this conversation, not read since.
                }
                val i18nKey = if (info.threadRootId == null) "chat.notification.new" else "chat.notification.reply"
                notificationService.notifyDirect(
                    recipientId = userId,
                    title = I18nHelper.getLocalizedMessage(user, "$i18nKey.title", authorName, info.title),
                    body = body(user!!, "$i18nKey.text", authorName, info, withContent),
                    link = link,
                    dedupKey = "$prefix${info.messageId}",
                    delivery = NEW_MESSAGE_DELIVERY,
                    display = NotificationDisplay.TOAST,
                    menuBadge = MenuItemDefId.CHAT,
                )
            }
    }

    private fun body(user: PFUserDO, i18nKey: String, authorName: String, info: MessageInfo, withContent: Boolean): String {
        val text = I18nHelper.getLocalizedMessage(user, i18nKey, authorName, info.title)
        return if (withContent && info.html.isNotBlank()) "<p>$text</p>${info.html}" else text
    }

    private fun mutedUserIds(conversationId: Long): Set<Long> {
        return persistenceService.executeQuery(
            "select s.userId from ChatMemberStateDO s where s.conversationId = :conversationId and s.muted = true",
            java.lang.Long::class.java,
            Pair("conversationId", conversationId),
        ).mapTo(mutableSetOf()) { it.toLong() }
    }

    companion object {
        const val NEW_MESSAGE_MAIL_DELAY_MINUTES = 60

        private val MENTION_DELIVERY = NotificationDelivery(
            listOf(
                NotificationDeliveryStep(NotificationChannel.IN_APP, 0),
                NotificationDeliveryStep(NotificationChannel.MAIL, 0),
            )
        )

        private val NEW_MESSAGE_DELIVERY = NotificationDelivery(
            listOf(
                NotificationDeliveryStep(NotificationChannel.IN_APP, 0),
                NotificationDeliveryStep(NotificationChannel.MAIL, NEW_MESSAGE_MAIL_DELAY_MINUTES, onlyIfUnacknowledged = true),
            )
        )

        internal fun newPrefix(conversationId: Long, userId: Long) = "chat:new:$conversationId:$userId:"

        internal fun mentionPrefix(conversationId: Long, userId: Long) = "chat:mention:$conversationId:$userId:"
    }
}
