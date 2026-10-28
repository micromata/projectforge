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
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The chat (PF-CHAT): conversations about host objects ([ChatHost]), messages, threads, reactions, read states.
 *
 * Every call acts for the logged-in user and checks the access by the conversation taken from the database
 * ([ChatAccess]), never by ids of the request: a message is changed only within its own conversation.
 *
 * Every change of a message increments the change sequence of its conversation first ([nextSeq]), which serializes
 * the changes of a conversation (row lock until commit) and lets clients fetch the changes since the last one they
 * know ([selectChanges]). The [ChatEventListener]s are informed after the change is written.
 */
@Service
open class ChatService {
    @Autowired
    private lateinit var chatAccess: ChatAccess

    /** Lazy: the counter itself uses this service. */
    @Autowired
    @Lazy
    private lateinit var unreadCounter: ChatUnreadCounter

    @Autowired
    private lateinit var chatMessageDao: ChatMessageDao

    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Autowired
    private lateinit var hostRegistry: ChatHostRegistry

    @Autowired
    private lateinit var fileService: ChatFileService

    @Autowired
    private lateinit var notifier: ChatNotifier

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired(required = false)
    private var eventListeners: List<ChatEventListener> = emptyList()

    /** The changes since a sequence number, see [selectChanges]. */
    class Changes(
        val messages: List<ChatMessageDO>,
        /** The sequence number to ask for the next changes with. */
        val changeSeq: Long,
        /** False, if there are more changes than given (ask again with [changeSeq]). */
        val complete: Boolean,
    )

    /**
     * The conversation of the host object and channel, created if missing and the user may write in it (CTX-02), with
     * the area of its files.
     * @return The context, null if there is no conversation yet and the user may not write (nothing to show).
     * @throws AccessException if the user may not read it, or the host object doesn't exist.
     */
    open fun open(hostType: String, hostId: Long, channel: String): ChatAccess.Context? {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val host = hostRegistry.get(hostType) ?: throw AccessException(user, NO_ACCESS)
        if (host.title(hostId, channel) == null || !host.hasReadAccess(user, hostId, channel)) {
            throw AccessException(user, NO_ACCESS)
        }
        val conversation = chatAccess.findConversation(hostType, hostId, channel)
            ?: if (!host.hasWriteAccess(user, hostId, channel)) {
                return null
            } else {
                createConversation(hostType, hostId, channel)
            }
        val context = chatAccess.getContext(conversation, user) ?: throw AccessException(user, NO_ACCESS)
        if (context.canWrite) {
            fileService.ensureArea(context) // The area of its files in the data transfer, see ChatFileService.
        }
        return context
    }

    private fun createConversation(hostType: String, hostId: Long, channel: String): ChatConversationDO {
        return try {
            persistenceService.runInTransaction { context ->
                ChatConversationDO().also {
                    it.created = Date()
                    it.hostType = hostType
                    it.hostId = hostId
                    it.channel = channel
                    context.em.persist(it)
                    context.em.flush()
                }
            }
        } catch (ex: Exception) {
            // Created by somebody else at the same time (unique constraint)?
            chatAccess.findConversation(hostType, hostId, channel) ?: throw ex
        }
    }

    /** The context of the conversation for the logged-in user; throws an [AccessException] without read access. */
    open fun requireContext(conversationId: Long?): ChatAccess.Context {
        return chatAccess.requireContext(conversationId, ThreadLocalUserContext.requiredLoggedInUser)
    }

    /** The newest messages of the conversation (not of threads) older than [beforeId], newest first. */
    open fun selectMessages(context: ChatAccess.Context, beforeId: Long?, limit: Int): List<ChatMessageDO> {
        val where = if (beforeId != null) " and m.id < :beforeId" else ""
        val params = listOfNotNull(Pair("conversationId", context.conversationId), beforeId?.let { Pair("beforeId", it) })
        return persistenceService.executeQuery(
            "from ChatMessageDO m where m.conversationId = :conversationId and m.threadRootId is null$where order by m.id desc",
            ChatMessageDO::class.java,
            *params.toTypedArray(),
            maxResults = limit.coerceIn(1, MAX_PAGE_SIZE),
        )
    }

    /** The message with its thread (oldest reply first), null if it isn't a message of the conversation. */
    open fun selectThread(context: ChatAccess.Context, rootId: Long): Pair<ChatMessageDO, List<ChatMessageDO>>? {
        val root = findMessage(rootId)?.takeIf { it.conversationId == context.conversationId && it.threadRootId == null }
            ?: return null
        val replies = persistenceService.executeQuery(
            "from ChatMessageDO m where m.conversationId = :conversationId and m.threadRootId = :rootId order by m.id",
            ChatMessageDO::class.java,
            Pair("conversationId", context.conversationId),
            Pair("rootId", rootId),
        )
        return Pair(root, replies)
    }

    /** The messages (of the conversation and its threads) changed since the given sequence number, oldest first. */
    open fun selectChanges(context: ChatAccess.Context, since: Long): Changes {
        val currentSeq = currentSeq(context.conversationId)
        val messages = persistenceService.executeQuery(
            "from ChatMessageDO m where m.conversationId = :conversationId and m.changeSeq > :since order by m.changeSeq",
            ChatMessageDO::class.java,
            Pair("conversationId", context.conversationId),
            Pair("since", since),
            maxResults = MAX_CHANGES + 1,
        )
        if (messages.size > MAX_CHANGES) {
            // One change may write several messages with the same sequence (a reply and its thread root): the page
            // ends before the sequence of the first message left out, the next one starts with it.
            val boundary = messages[MAX_CHANGES].changeSeq
            val page = messages.take(MAX_CHANGES).filter { it.changeSeq < boundary }
            if (page.isNotEmpty()) {
                return Changes(page, page.last().changeSeq, complete = false)
            }
            val sameSeq = persistenceService.executeQuery(
                "from ChatMessageDO m where m.conversationId = :conversationId and m.changeSeq = :seq",
                ChatMessageDO::class.java,
                Pair("conversationId", context.conversationId),
                Pair("seq", boundary),
            )
            return Changes(sameSeq, boundary, complete = false)
        }
        return Changes(messages, maxOf(currentSeq, messages.lastOrNull()?.changeSeq ?: 0), complete = true)
    }

    open fun currentSeq(conversationId: Long): Long {
        return persistenceService.selectSingleResult(
            "select c.changeSeq from ChatConversationDO c where c.id = :id",
            java.lang.Long::class.java,
            Pair("id", conversationId),
        )?.toLong() ?: 0
    }

    /**
     * Sends a message, or a reply in the thread of [threadRootId] (a reply to a reply belongs to the same thread).
     * @param files The files of the message, stored in the data transfer ([ChatFileService.store]) before; deleted
     * again if the message isn't saved.
     * @param quotedMessageId A message of the same conversation quoted by this one (MSG-08), not deleted or hidden.
     */
    open fun send(
        conversationId: Long,
        threadRootId: Long?,
        html: String?,
        files: List<ChatFileService.Upload> = emptyList(),
        quotedMessageId: Long? = null,
    ): ChatMessageDO {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val context = chatAccess.requireWriteContext(conversationId, user)
        val content = sanitize(context, html)
        if (content.html.isEmpty() && files.isEmpty()) {
            throw UserException(ERROR_EMPTY)
        }
        if (threadRootId != null) {
            // Checked before the files are stored: a root of this conversation, not deleted or hidden.
            val root = findMessage(threadRootId)?.let { if (it.threadRootId != null) findMessage(it.threadRootId) else it }
            if (root?.conversationId != conversationId || root.deleted || root.isHidden) {
                throw AccessException(user, NO_ACCESS)
            }
        }
        if (quotedMessageId != null) {
            // The conversation of the quoted message from the database: no quoting across conversations.
            val quoted = findMessage(quotedMessageId)
            if (quoted?.conversationId != conversationId || quoted.deleted || quoted.isHidden) {
                throw UserException(ERROR_QUOTE_NOT_FOUND)
            }
        }
        val attachments = fileService.store(context, files)
        try {
            return send(context, threadRootId, content, attachments, quotedMessageId)
        } catch (ex: Exception) {
            fileService.delete(attachments)
            throw ex
        }
    }

    private fun send(
        context: ChatAccess.Context,
        threadRootId: Long?,
        content: ChatHtmlSanitizer.Result,
        attachments: List<ChatAttachment>,
        quotedMessageId: Long?,
    ): ChatMessageDO {
        val user = context.user
        val conversationId = context.conversationId
        val now = Date()
        var followers = emptySet<Long>()
        val (message, seq) = persistenceService.runInTransaction { pc ->
            val seq = nextSeq(pc, conversationId)
            var root = threadRootId?.let { loadMessage(pc, it) }
            if (threadRootId != null) {
                if (root?.conversationId != conversationId) {
                    throw AccessException(user, NO_ACCESS)
                }
                root.threadRootId?.let { root = loadMessage(pc, it) } // One level deep (THR-02).
                val thread = root
                if (thread == null || thread.deleted || thread.isHidden) {
                    throw AccessException(user, NO_ACCESS)
                }
            }
            val message = ChatMessageDO().also {
                it.conversationId = conversationId
                it.authorId = user.id
                it.threadRootId = root?.id
                it.quotedMessageId = quotedMessageId
                it.content = content.html
                it.contentText = content.text.take(ChatMessageDO.CONTENT_TEXT_LENGTH)
                it.writeAttachments(attachments)
                it.mentionedIds = ChatRoomDO.toCsv(content.mentionedIds)
                it.changeSeq = seq
            }
            chatMessageDao.insert(message)
            root?.let {
                val rootFollowers = ChatRoomDO.toIds(it.followerIds).toMutableSet()
                if (user.id!! !in ChatRoomDO.toIds(it.unfollowerIds)) {
                    rootFollowers += user.id!!
                }
                followers = threadFollowers(it.authorId, ChatRoomDO.toCsv(rootFollowers), it.unfollowerIds)
                pc.executeUpdate(
                    "update ChatMessageDO m set m.replyCount = m.replyCount + 1, m.lastReplyAt = :now," +
                            " m.followerIds = :followerIds, m.changeSeq = :seq where m.id = :id",
                    Pair("now", now),
                    Pair("followerIds", ChatRoomDO.toCsv(rootFollowers)),
                    Pair("seq", seq),
                    Pair("id", it.id),
                )
            }
            pc.executeUpdate(
                "update ChatConversationDO c set c.lastMessageAt = :now where c.id = :id",
                Pair("now", now),
                Pair("id", conversationId),
            )
            updateLastRead(pc, conversationId, user.id!!, message.id!!) // The author has read it.
            Pair(message, seq)
        }
        publish(conversationId, seq)
        notifier.onMessage(messageInfo(context, message, content.mentionedIds, isNew = true, followers))
        return message
    }

    /** The author edits the own message (historised, MSG-03), its files stay. Newly mentioned users are notified. */
    open fun edit(messageId: Long, html: String?): ChatMessageDO {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val stored = findMessage(messageId) ?: throw AccessException(user, NO_ACCESS)
        val context = chatAccess.requireWriteContext(stored.conversationId, user)
        if (stored.authorId != user.id || stored.deleted || stored.isHidden) {
            throw AccessException(user, NO_ACCESS)
        }
        val content = sanitize(context, html)
        if (content.html.isEmpty() && stored.readAttachments().isEmpty()) {
            throw UserException(ERROR_EMPTY)
        }
        var newlyMentioned = emptyList<Long>()
        val (message, seq) = persistenceService.runInTransaction { pc ->
            val seq = nextSeq(pc, context.conversationId)
            val message = findMessage(messageId)!!
            val previous = ChatRoomDO.toIds(message.mentionedIds)
            newlyMentioned = content.mentionedIds.filter { it !in previous }
            message.content = content.html
            message.contentText = content.text.take(ChatMessageDO.CONTENT_TEXT_LENGTH)
            message.mentionedIds = ChatRoomDO.toCsv(content.mentionedIds)
            message.editedAt = Date()
            message.changeSeq = seq
            chatMessageDao.update(message)
            Pair(message, seq)
        }
        publish(context.conversationId, seq)
        if (newlyMentioned.isNotEmpty()) {
            notifier.onMessage(messageInfo(context, message, newlyMentioned, isNew = false))
        }
        return message
    }

    /**
     * The author deletes the own message: a placeholder stays (MSG-04), the thread stays readable. Its files are
     * deleted.
     */
    open fun delete(messageId: Long) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val stored = findMessage(messageId) ?: throw AccessException(user, NO_ACCESS)
        val context = chatAccess.requireWriteContext(stored.conversationId, user)
        if (stored.authorId != user.id || stored.deleted) {
            throw AccessException(user, NO_ACCESS)
        }
        val seq = persistenceService.runInTransaction { pc ->
            val seq = nextSeq(pc, context.conversationId)
            val message = findMessage(messageId)!!
            chatMessageDao.markAsDeleted(message)
            setSeq(pc, messageId, seq)
            message.threadRootId?.let { rootId ->
                // The time of the last reply left, null if none.
                val lastReplyAt = pc.selectSingleResult(
                    "select max(m.created) from ChatMessageDO m where m.threadRootId = :rootId and m.deleted = false" +
                            " and m.id <> :id",
                    Date::class.java,
                    Pair("rootId", rootId),
                    Pair("id", messageId),
                )
                pc.executeUpdate(
                    "update ChatMessageDO m set m.replyCount = m.replyCount - 1, m.lastReplyAt = :lastReplyAt," +
                            " m.changeSeq = :seq where m.id = :id and m.replyCount > 0",
                    Pair("lastReplyAt", lastReplyAt),
                    Pair("seq", seq),
                    Pair("id", rootId),
                )
            }
            seq
        }
        publish(context.conversationId, seq)
        fileService.delete(stored.readAttachments())
    }

    /** A moderator hides a message (of anybody) with a reason, or shows it again (SEC-05, historised). */
    open fun hide(messageId: Long, hidden: Boolean, reason: String?) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val stored = findMessage(messageId) ?: throw AccessException(user, NO_ACCESS)
        val context = chatAccess.requireContext(stored.conversationId, user)
        if (!context.isModerator) {
            throw AccessException(user, NO_ACCESS)
        }
        if (hidden && reason.isNullOrBlank()) {
            throw UserException(ERROR_HIDE_REASON)
        }
        val seq = persistenceService.runInTransaction { pc ->
            val seq = nextSeq(pc, context.conversationId)
            val message = findMessage(messageId)!!
            message.hiddenById = if (hidden) user.id else null
            message.hiddenAt = if (hidden) Date() else null
            message.hiddenReason = if (hidden) reason!!.trim().take(1000) else null
            message.changeSeq = seq
            chatMessageDao.update(message, checkAccess = false) // Checked above: moderator.
            seq
        }
        publish(context.conversationId, seq)
    }

    /** Adds the reaction of the logged-in user with the emoji, or takes it back (MSG-10). */
    open fun toggleReaction(messageId: Long, emoji: String?) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val value = emoji?.trim()
        if (value.isNullOrEmpty() || value.length > MAX_EMOJI_LENGTH || value.any { it.isWhitespace() || it in "<>&\"'" }) {
            throw UserException(ERROR_EMOJI)
        }
        val stored = findMessage(messageId) ?: throw AccessException(user, NO_ACCESS)
        val context = chatAccess.requireWriteContext(stored.conversationId, user)
        if (stored.deleted || stored.isHidden) {
            throw AccessException(user, NO_ACCESS)
        }
        val seq = persistenceService.runInTransaction { pc ->
            val seq = nextSeq(pc, context.conversationId)
            val message = loadMessage(pc, messageId)!!
            val reactions = message.readReactions()
            val userIds = reactions[value].orEmpty()
            if (user.id in userIds) {
                reactions[value] = userIds - user.id!!
            } else {
                if (value !in reactions && reactions.size >= MAX_EMOJIS_PER_MESSAGE) {
                    throw UserException(ERROR_TOO_MANY_EMOJIS, MAX_EMOJIS_PER_MESSAGE)
                }
                reactions[value] = userIds + user.id!!
            }
            message.writeReactions(reactions)
            pc.executeUpdate(
                "update ChatMessageDO m set m.reactions = :reactions, m.changeSeq = :seq where m.id = :id",
                Pair("reactions", message.reactions),
                Pair("seq", seq),
                Pair("id", messageId),
            )
            seq
        }
        publish(context.conversationId, seq)
    }

    /** The logged-in user follows the thread of the message or unfollows it (THR-05). */
    open fun setFollowing(messageId: Long, follow: Boolean) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val userId = user.id!!
        val stored = findMessage(messageId) ?: throw AccessException(user, NO_ACCESS)
        val context = chatAccess.requireContext(stored.conversationId, user)
        if (stored.threadRootId != null) {
            throw AccessException(user, NO_ACCESS) // Threads are followed at their root.
        }
        val seq = persistenceService.runInTransaction { pc ->
            val seq = nextSeq(pc, context.conversationId)
            val message = loadMessage(pc, messageId)!!
            val followers = ChatRoomDO.toIds(message.followerIds).toMutableSet()
            val unfollowers = ChatRoomDO.toIds(message.unfollowerIds).toMutableSet()
            if (follow) {
                followers += userId
                unfollowers -= userId
            } else {
                followers -= userId
                unfollowers += userId
            }
            pc.executeUpdate(
                "update ChatMessageDO m set m.followerIds = :followers, m.unfollowerIds = :unfollowers," +
                        " m.changeSeq = :seq where m.id = :id",
                Pair("followers", ChatRoomDO.toCsv(followers)),
                Pair("unfollowers", ChatRoomDO.toCsv(unfollowers)),
                Pair("seq", seq),
                Pair("id", messageId),
            )
            seq
        }
        publish(context.conversationId, seq)
    }

    /**
     * The logged-in user read the conversation up to the given message (NOT-01); resolves the notifications about it.
     */
    open fun markRead(conversationId: Long, messageId: Long) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val context = chatAccess.requireContext(conversationId, user)
        val message = findMessage(messageId)?.takeIf { it.conversationId == context.conversationId }
            ?: throw AccessException(user, NO_ACCESS)
        val raised = persistenceService.runInTransaction { pc ->
            updateLastRead(pc, conversationId, user.id!!, message.id!!)
        }
        notifier.resolve(conversationId, user.id!!)
        unreadCounter.invalidate(user.id!!)
        if (raised && context.conversation.hostType == ChatDirectChatHost.HOST_TYPE) {
            publishRead(conversationId, user.id!!)
        }
    }

    /**
     * The logged-in user opened the thread of the message: resolves the notifications about mentions in its replies
     * (not resolved by [markRead], which reads the conversation). The read state stays.
     */
    open fun markThreadRead(conversationId: Long, rootId: Long) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val context = chatAccess.requireContext(conversationId, user)
        findMessage(rootId)?.takeIf { it.conversationId == context.conversationId && it.threadRootId == null }
            ?: throw AccessException(user, NO_ACCESS)
        notifier.resolveThread(conversationId, user.id!!, rootId)
    }

    /**
     * The logged-in user marks the conversation unread from the given message on: read only up to the messages
     * before it, also if read further before. The notifications resolved stay resolved.
     */
    open fun markUnread(conversationId: Long, messageId: Long) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val context = chatAccess.requireContext(conversationId, user)
        val message = findMessage(messageId)?.takeIf { it.conversationId == context.conversationId }
            ?: throw AccessException(user, NO_ACCESS)
        persistenceService.runInTransaction { pc ->
            val state = memberState(pc, conversationId, user.id!!)
            state.lastReadMessageId = message.id!! - 1
            if (state.id == null) pc.em.persist(state)
        }
        unreadCounter.invalidate(user.id!!)
    }

    /** Mutes the conversation for the logged-in user (no notifications about new messages, except mentions). */
    open fun setMuted(conversationId: Long, muted: Boolean) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        chatAccess.requireContext(conversationId, user)
        persistenceService.runInTransaction { pc ->
            val state = memberState(pc, conversationId, user.id!!)
            state.muted = muted
            if (state.id == null) pc.em.persist(state) else pc.em.merge(state)
        }
    }

    /** The read state of the user, null if the user never opened the conversation. */
    open fun getMemberState(conversationId: Long, userId: Long): ChatMemberStateDO? {
        return persistenceService.selectSingleResult(
            "from ChatMemberStateDO s where s.conversationId = :conversationId and s.userId = :userId",
            ChatMemberStateDO::class.java,
            Pair("conversationId", conversationId),
            Pair("userId", userId),
        )
    }

    /** The id of the newest message or reply of the conversation, null if none. */
    open fun newestMessageId(conversationId: Long): Long? {
        return persistenceService.selectSingleResult(
            "select max(m.id) from ChatMessageDO m where m.conversationId = :conversationId",
            java.lang.Long::class.java,
            Pair("conversationId", conversationId),
        )?.toLong()
    }

    /**
     * The number of messages of others the user didn't read yet (deleted and hidden ones excluded), with the replies
     * of the threads the user follows or is mentioned in ([UNREAD_CLAUSE]).
     */
    open fun countUnread(conversationId: Long, userId: Long): Long {
        val lastRead = getMemberState(conversationId, userId)?.lastReadMessageId ?: 0L
        return persistenceService.selectSingleResult(
            "select count(m) from ChatMessageDO m where m.conversationId = :conversationId and m.id > :lastRead" +
                    " and $UNREAD_CLAUSE",
            java.lang.Long::class.java,
            Pair("conversationId", conversationId),
            Pair("lastRead", lastRead),
            Pair("userId", userId),
            Pair("userCsv", csvPattern(userId)),
        )?.toLong() ?: 0
    }

    /**
     * The messages of others newer than [afterId] counting as unread for the user of the context ([UNREAD_CLAUSE]),
     * oldest first, at most [MAX_UNREAD]: id and thread root (null for a message of the conversation itself). For
     * jumping to the next unread message, also in a thread; [afterId] is the read state when the conversation was
     * opened, which may have moved since.
     */
    open fun selectUnread(context: ChatAccess.Context, afterId: Long): List<Pair<Long, Long?>> {
        val userId = context.user.id!!
        return persistenceService.executeQuery(
            "select m.id, m.threadRootId from ChatMessageDO m where m.conversationId = :conversationId" +
                    " and m.id > :afterId and $UNREAD_CLAUSE order by m.id",
            Array<Any?>::class.java,
            Pair("conversationId", context.conversationId),
            Pair("afterId", afterId),
            Pair("userId", userId),
            Pair("userCsv", csvPattern(userId)),
            maxResults = MAX_UNREAD,
        ).map { row -> Pair((row[0] as Number).toLong(), (row[1] as Number?)?.toLong()) }
    }

    /**
     * [countUnread] of the logged-in user in the conversation of the host object, 0 if it has none yet or the user
     * may not read it (e.g. for the chat button of the host object).
     */
    open fun countUnread(hostType: String, hostId: Long, channel: String): Long {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val conversation = chatAccess.findConversation(hostType, hostId, channel) ?: return 0
        val context = chatAccess.getContext(conversation, user) ?: return 0
        return countUnread(context.conversationId, user.id!!)
    }

    /** [countUnread] of the given conversations in one query, by conversation id (missing: none unread). */
    open fun countUnread(conversationIds: Collection<Long>, userId: Long): Map<Long, Long> {
        if (conversationIds.isEmpty()) {
            return emptyMap()
        }
        return persistenceService.executeQueryBatched(
            "select m.conversationId, count(m) from ChatMessageDO m left join ChatMemberStateDO s" +
                    " on s.conversationId = m.conversationId and s.userId = :userId" +
                    " where m.conversationId in :conversationIds and $UNREAD_CLAUSE" +
                    " and m.id > coalesce(s.lastReadMessageId, 0) group by m.conversationId",
            Array<Any?>::class.java,
            batchParam = "conversationIds",
            batchValues = conversationIds,
            Pair("userId", userId),
            Pair("userCsv", csvPattern(userId)),
        ).associate { row -> (row[0] as Number).toLong() to (row[1] as Number).toLong() }
    }

    /** The messages and replies containing the text (and of the author, if given), newest first (SRCH-01). */
    open fun search(context: ChatAccess.Context, text: String?, authorId: Long?): List<ChatMessageDO> {
        val term = text?.trim()?.lowercase().orEmpty()
        if (term.isEmpty() && authorId == null) {
            return emptyList()
        }
        val where = StringBuilder("m.conversationId = :conversationId and m.deleted = false and m.hiddenById is null")
        val params = mutableListOf<Pair<String, Any?>>(Pair("conversationId", context.conversationId))
        if (term.isNotEmpty()) {
            // The wildcards of the term are searched as text.
            where.append(" and lower(m.contentText) like :term escape '!'")
            params += Pair("term", "%${term.replace(LIKE_SPECIAL_CHARS, "!$1")}%")
        }
        if (authorId != null) {
            where.append(" and m.authorId = :authorId")
            params += Pair("authorId", authorId)
        }
        return persistenceService.executeQuery(
            "from ChatMessageDO m where $where order by m.id desc",
            ChatMessageDO::class.java,
            *params.toTypedArray(),
            maxResults = MAX_SEARCH_RESULTS,
        )
    }

    /** The messages containing the text in a conversation: their count and the newest one. */
    class SearchHit(val context: ChatAccess.Context, val hitCount: Long, val newest: ChatMessageDO)

    /**
     * The conversations the logged-in user may read with messages or replies containing the text (at least
     * [MIN_SEARCH_ALL_LENGTH] characters), the one with the newest match first, up to [maxConversations] (SRCH-02).
     * Each conversation with matches is checked by its host, the newest first, at most [maxChecked] of them: a common
     * term must not cost a check of every conversation of the system. The matches of the others are dropped, their
     * counts as well, as are those of hosts excluding the search ([ChatHost.searchableAcrossConversations]).
     */
    open fun searchAll(
        text: String?,
        maxConversations: Int = MAX_SEARCH_CONVERSATIONS,
        maxChecked: Int = MAX_SEARCH_CHECKED,
    ): List<SearchHit> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val term = text?.trim()?.lowercase().orEmpty()
        if (term.length < MIN_SEARCH_ALL_LENGTH) {
            return emptyList()
        }
        // conversationId, count, newest message id, the newest match first; the wildcards of the term are searched as text.
        val matches = persistenceService.executeQuery(
            "select m.conversationId, count(m), max(m.id) from ChatMessageDO m" +
                    " where m.deleted = false and m.hiddenById is null and lower(m.contentText) like :term escape '!'" +
                    " group by m.conversationId order by max(m.id) desc",
            Array<Any?>::class.java,
            Pair("term", "%${term.replace(LIKE_SPECIAL_CHARS, "!$1")}%"),
            maxResults = maxChecked,
        ).map { row -> Triple((row[0] as Number).toLong(), (row[1] as Number).toLong(), (row[2] as Number).toLong()) }
        val contexts = mutableListOf<ChatAccess.Context>()
        for (chunk in matches.chunked(SEARCH_CHECK_CHUNK)) {
            val conversations = persistenceService.executeQuery(
                "from ChatConversationDO c where c.id in :ids",
                ChatConversationDO::class.java,
                Pair("ids", chunk.map { it.first }),
            ).associateBy { it.id!! }
            chunk.forEach { (conversationId) ->
                if (contexts.size >= maxConversations) return@forEach
                val conversation = conversations[conversationId] ?: return@forEach
                if (hostRegistry.get(conversation.hostType)?.searchableAcrossConversations != true) return@forEach
                chatAccess.getContext(conversation, user)?.let { contexts += it }
            }
            if (contexts.size >= maxConversations) break
        }
        val byConversation = matches.associateBy { it.first }
        val newest = persistenceService.executeQueryBatched(
            "from ChatMessageDO m where m.id in :ids",
            ChatMessageDO::class.java,
            batchParam = "ids",
            batchValues = contexts.map { byConversation[it.conversationId]!!.third },
        ).associateBy { it.id!! }
        return contexts.mapNotNull { context ->
            val (_, count, newestId) = byConversation[context.conversationId]!!
            newest[newestId]?.let { SearchHit(context, count, it) }
        }
    }

    /**
     * The users who may be mentioned (MSG-07): participants with read access, matching the search (name, username),
     * sorted by name. The logged-in user isn't offered.
     */
    open fun mentionCandidates(context: ChatAccess.Context, search: String?, limit: Int = 10): List<PFUserDO> {
        val term = search?.trim()?.lowercase().orEmpty()
        return participants(context)
            .filter { it.id != context.user.id }
            .filter { user ->
                term.isEmpty() || listOfNotNull(user.getFullname(), user.username, user.nickname, user.firstname, user.lastname)
                    .any { it.lowercase().contains(term) }
            }
            .filter { chatAccess.hasReadAccess(context, it) }
            .sortedBy { it.getFullname().lowercase() }
            .take(limit.coerceIn(1, 50))
            .toList()
    }

    /**
     * The members of the conversation, shown with it: the participants of the host with read access, the logged-in user
     * included, sorted by name.
     */
    open fun members(context: ChatAccess.Context): List<PFUserDO> {
        return participants(context)
            .filter { chatAccess.hasReadAccess(context, it) }
            .sortedBy { it.getFullname().lowercase() }
            .toList()
    }

    /** The participants of the host as users, not checked for read access yet. */
    private fun participants(context: ChatAccess.Context): Sequence<PFUserDO> {
        return context.host.participantIds(context.hostId, context.channel).asSequence()
            .mapNotNull { userGroupCache.getUser(it) }
    }

    /**
     * The replies of the given messages (not deleted or hidden ones): their authors in the order of their first reply and the
     * newest reply of each author.
     */
    open fun selectReplySummaries(rootIds: Collection<Long>): Map<Long, ReplySummary> {
        if (rootIds.isEmpty()) {
            return emptyMap()
        }
        val result = mutableMapOf<Long, ReplySummary>()
        persistenceService.executeQueryBatched(
            "select m.threadRootId, m.authorId, m.id from ChatMessageDO m where m.threadRootId in :rootIds" +
                    " and m.deleted = false and m.hiddenById is null order by m.id",
            Array<Any?>::class.java,
            batchParam = "rootIds",
            batchValues = rootIds,
        ).forEach { row ->
            val rootId = (row[0] as? Number)?.toLong() ?: return@forEach
            val authorId = (row[1] as? Number)?.toLong() ?: return@forEach
            val id = (row[2] as? Number)?.toLong() ?: return@forEach
            val summary = result.getOrPut(rootId) { ReplySummary() }
            if (authorId !in summary.newestIdByAuthor) summary.authorIds += authorId
            summary.newestIdByAuthor[authorId] = id // Ordered by id: the last one is the newest.
        }
        return result
    }

    /** The replies of a thread, see [selectReplySummaries]. */
    class ReplySummary {
        val authorIds = mutableListOf<Long>()
        val newestIdByAuthor = mutableMapOf<Long, Long>()

        /** The newest reply of an author other than the given user, null if none. */
        fun newestIdOfOthers(userId: Long?): Long? = newestIdByAuthor.filterKeys { it != userId }.values.maxOrNull()
    }

    /** The followers of a thread: the author of its message and the followers, without those who unfollowed it. */
    open fun threadFollowers(message: ChatMessageDO): Set<Long> {
        return threadFollowers(message.authorId, message.followerIds, message.unfollowerIds)
    }

    private fun threadFollowers(authorId: Long?, followerIds: String?, unfollowerIds: String?): Set<Long> {
        return (listOfNotNull(authorId) + ChatRoomDO.toIds(followerIds)).toSet() - ChatRoomDO.toIds(unfollowerIds)
    }

    /** The conversations of the given host objects, by host id (for lists of the host, e.g. the rooms). */
    open fun findConversations(hostType: String, hostIds: Collection<Long>): Map<Long, ChatConversationDO> {
        if (hostIds.isEmpty()) {
            return emptyMap()
        }
        return persistenceService.executeQueryBatched(
            "from ChatConversationDO c where c.hostType = :hostType and c.hostId in :hostIds",
            ChatConversationDO::class.java,
            batchParam = "hostIds",
            batchValues = hostIds,
            Pair("hostType", hostType),
        ).associateBy { it.hostId!! }
    }

    /**
     * The conversations of the host type with at least one message the logged-in user may read (e.g. the chats of the
     * data transfer areas), newest first.
     */
    open fun listConversations(hostType: String): List<ChatAccess.Context> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        hostRegistry.get(hostType) ?: return emptyList()
        return persistenceService.executeQuery(
            "from ChatConversationDO c where c.hostType = :hostType and c.lastMessageAt is not null order by c.lastMessageAt desc",
            ChatConversationDO::class.java,
            Pair("hostType", hostType),
        ).mapNotNull { chatAccess.getContext(it, user) }
    }

    /**
     * Deletes the room (marked as deleted, checked by [ChatRoomDao]) and its conversation with all messages and
     * files (ROOM-07).
     */
    open fun deleteRoom(roomId: Long) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val room = chatRoomDao.find(roomId, checkAccess = false) ?: throw AccessException(user, NO_ACCESS)
        chatRoomDao.markAsDeleted(room)
        deleteConversations(ChatRoomChatHost.HOST_TYPE, roomId)
    }

    /**
     * Deletes the conversations of the host object with their messages (also their history, which holds former texts),
     * read states and files (the area in the data transfer), e.g. with the deletion of the host object (CTX-05). The notifications about them are
     * resolved. No access check: the caller decides.
     */
    open fun deleteConversations(hostType: String, hostId: Long) {
        findConversations(hostType, listOf(hostId)).values.forEach { conversation ->
            val conversationId = conversation.id!!
            persistenceService.runInTransaction { pc ->
                val params = Pair("conversationId", conversationId)
                val entityName = Pair("entityName", ChatMessageDO::class.java.name)
                val historyOfMessages = "select h.id from HistoryEntryDO h where h.entityName = :entityName and" +
                        " h.entityId in (select m.id from ChatMessageDO m where m.conversationId = :conversationId)"
                pc.executeUpdate(
                    "delete from HistoryEntryAttrDO a where a.parent.id in ($historyOfMessages)",
                    params,
                    entityName,
                )
                pc.executeUpdate(
                    "delete from HistoryEntryDO h where h.id in ($historyOfMessages)",
                    params,
                    entityName,
                )
                pc.executeUpdate("delete from ChatMemberStateDO s where s.conversationId = :conversationId", params)
                pc.executeUpdate(
                    "delete from ChatMessageDO m where m.conversationId = :conversationId and m.threadRootId is not null",
                    params,
                )
                pc.executeUpdate("delete from ChatMessageDO m where m.conversationId = :conversationId", params)
                pc.executeUpdate("delete from ChatConversationDO c where c.id = :conversationId", params)
            }
            fileService.deleteArea(conversation)
            notifier.resolveAll(conversationId)
            log.info { "Chat conversation #$conversationId of $hostType #$hostId deleted." }
        }
    }

    /** The message, without access check. */
    open fun findMessage(id: Long?): ChatMessageDO? {
        // Not via ChatMessageDao.find: a message deleted meanwhile (a client acting on a stale id) is no error to log.
        id ?: return null
        return persistenceService.find(ChatMessageDO::class.java, id)
    }

    /** The messages of the conversation with the given ids (e.g. the quoted ones), without access check. */
    open fun findMessages(conversationId: Long, ids: Collection<Long>): List<ChatMessageDO> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return persistenceService.executeQuery(
            "from ChatMessageDO m where m.conversationId = :conversationId and m.id in :ids",
            ChatMessageDO::class.java,
            Pair("conversationId", conversationId),
            Pair("ids", ids.distinct()),
        )
    }

    private fun loadMessage(pc: PfPersistenceContext, id: Long): ChatMessageDO? {
        return pc.selectSingleResult(
            "from ChatMessageDO m where m.id = :id",
            ChatMessageDO::class.java,
            Pair("id", id),
        )
    }

    /**
     * Increments the change sequence of the conversation and returns it. Locks the conversation until the end of the
     * transaction, so call it first in a change.
     */
    private fun nextSeq(pc: PfPersistenceContext, conversationId: Long): Long {
        pc.executeUpdate(
            "update ChatConversationDO c set c.changeSeq = c.changeSeq + 1 where c.id = :id",
            Pair("id", conversationId),
        )
        return pc.selectSingleResult(
            "select c.changeSeq from ChatConversationDO c where c.id = :id",
            java.lang.Long::class.java,
            Pair("id", conversationId),
        )!!.toLong()
    }

    private fun setSeq(pc: PfPersistenceContext, messageId: Long, seq: Long) {
        pc.executeUpdate(
            "update ChatMessageDO m set m.changeSeq = :seq where m.id = :id",
            Pair("seq", seq),
            Pair("id", messageId),
        )
    }

    private fun memberState(pc: PfPersistenceContext, conversationId: Long, userId: Long): ChatMemberStateDO {
        return pc.selectSingleResult(
            "from ChatMemberStateDO s where s.conversationId = :conversationId and s.userId = :userId",
            ChatMemberStateDO::class.java,
            Pair("conversationId", conversationId),
            Pair("userId", userId),
            attached = true,
        ) ?: ChatMemberStateDO().also {
            it.conversationId = conversationId
            it.userId = userId
        }
    }

    /** Raises the read state; false if it was not lower than [messageId]. */
    private fun updateLastRead(pc: PfPersistenceContext, conversationId: Long, userId: Long, messageId: Long): Boolean {
        val state = memberState(pc, conversationId, userId)
        if ((state.lastReadMessageId ?: 0) >= messageId) {
            return false
        }
        state.lastReadMessageId = messageId
        if (state.id == null) pc.em.persist(state)
        return true
    }

    /** The sanitized content: mentions only of users with read access, named by the user cache. */
    private fun sanitize(context: ChatAccess.Context, html: String?): ChatHtmlSanitizer.Result {
        return ChatHtmlSanitizer.sanitize(html) { userId ->
            userGroupCache.getUser(userId)?.takeIf { chatAccess.hasReadAccess(context, it) }?.getFullname()
        }
    }

    private fun messageInfo(
        context: ChatAccess.Context,
        message: ChatMessageDO,
        newlyMentionedIds: List<Long>,
        isNew: Boolean,
        threadFollowerIds: Set<Long> = emptySet(),
    ) = ChatNotifier.MessageInfo(
        conversationId = context.conversationId,
        hostType = context.conversation.hostType!!,
        hostId = context.hostId,
        channel = context.channel,
        title = context.title,
        messageId = message.id!!,
        threadRootId = message.threadRootId,
        authorId = message.authorId!!,
        html = message.content.orEmpty(),
        newlyMentionedIds = newlyMentionedIds,
        isNew = isNew,
        threadFollowerIds = threadFollowerIds,
    )

    private fun publish(conversationId: Long, changeSeq: Long) {
        eventListeners.forEach { listener ->
            try {
                listener.onChange(conversationId, changeSeq)
            } catch (ex: Exception) {
                log.warn(ex) { "Chat event listener failed for conversation #$conversationId: ${ex.message}" }
            }
        }
    }

    private fun publishRead(conversationId: Long, readerId: Long) {
        eventListeners.forEach { listener ->
            try {
                listener.onRead(conversationId, readerId)
            } catch (ex: Exception) {
                log.warn(ex) { "Chat event listener failed for conversation #$conversationId: ${ex.message}" }
            }
        }
    }

    companion object {
        const val MAX_PAGE_SIZE = 200
        const val MAX_CHANGES = 500
        const val MAX_SEARCH_RESULTS = 100
        const val MAX_SEARCH_CONVERSATIONS = 50
        const val MAX_SEARCH_CHECKED = 1000
        const val MIN_SEARCH_ALL_LENGTH = 3
        private const val SEARCH_CHECK_CHUNK = 100
        const val MAX_UNREAD = 500
        const val MAX_EMOJI_LENGTH = 16
        const val MAX_EMOJIS_PER_MESSAGE = 20

        /**
         * The messages counting as unread for the user `:userId` (`:userCsv`: [csvPattern]): of others, not deleted or
         * hidden; replies only in threads the user follows (author of the message or follower, not unfollowed, see
         * [threadFollowers]) or is mentioned in.
         */
        private const val UNREAD_CLAUSE = "m.authorId <> :userId and m.deleted = false and m.hiddenById is null" +
                " and (m.threadRootId is null or concat(',', m.mentionedIds, ',') like :userCsv" +
                " or exists (select r.id from ChatMessageDO r where r.id = m.threadRootId" +
                " and (r.authorId = :userId or concat(',', r.followerIds, ',') like :userCsv)" +
                " and (r.unfollowerIds is null or concat(',', r.unfollowerIds, ',') not like :userCsv)))"

        /** Matches the id in a list of ids ([ChatRoomDO.toCsv]) enclosed in commas. */
        private fun csvPattern(id: Long) = "%,$id,%"

        /** The wildcards of LIKE and its escape character (`!`, see [search]). */
        private val LIKE_SPECIAL_CHARS = Regex("([%_!])")

        const val ERROR_EMPTY = "chat.message.error.empty"
        const val ERROR_EMOJI = "chat.reaction.error.invalid"
        const val ERROR_TOO_MANY_EMOJIS = "chat.reaction.error.tooMany"
        const val ERROR_HIDE_REASON = "chat.message.error.hideReason"
        const val ERROR_QUOTE_NOT_FOUND = "chat.message.error.quoteNotFound"

        private const val NO_ACCESS = "access.exception.noAccess"
    }
}
