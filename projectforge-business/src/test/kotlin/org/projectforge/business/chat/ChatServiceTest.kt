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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.notification.NotificationDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired

/**
 * The chat with a room as host: access by the conversation from the database, own messages only, moderators,
 * threads one level deep, the change sequence, reactions, mentions and their notifications.
 */
class ChatServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @BeforeEach
    fun setUp() {
        chatNotifier.executor = java.util.concurrent.Executor { it.run() }
    }

    /** A room of TEST_USER (owner) with TEST_USER2 as participant, its conversation opened by the owner. */
    private fun createRoom(name: String): Long {
        val owner = logon(TEST_USER)
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = name
            it.ownerId = owner.id
            it.participantUserIds = "${getUserId(TEST_USER2)}"
        })
        return chatService.open(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL)!!.conversationId
    }

    @Test
    fun `only participants read and write, only the author edits and deletes`() {
        val conversationId = createRoom("chat access test")
        val message = chatService.send(conversationId, null, "<p>Hello <b>all</b><script>alert(1)</script></p>")
        assertEquals("<p>Hello <b>all</b></p>", message.content)

        suppressErrorLogs {
            logon(TEST_FINANCE_USER)
            assertThrows(AccessException::class.java) { chatService.requireContext(conversationId) }
            assertThrows(AccessException::class.java) { chatService.send(conversationId, null, "intruder") }
            assertThrows(AccessException::class.java) { chatService.toggleReaction(message.id!!, "👍") }
            logon(TEST_ADMIN_USER)
            assertThrows(AccessException::class.java) { chatService.requireContext(conversationId) }
            logon(TEST_USER2)
            assertThrows(AccessException::class.java) { chatService.edit(message.id!!, "changed") }
            assertThrows(AccessException::class.java) { chatService.delete(message.id!!) }
            assertThrows(AccessException::class.java) { chatService.hide(message.id!!, true, "spam") }
            assertThrows(UserException::class.java) { chatService.send(conversationId, null, "<p> </p>") }
        }
        val context = chatService.requireContext(conversationId)
        assertFalse(context.isModerator)
        val reply = chatService.send(conversationId, message.id, "a reply")

        logon(TEST_USER)
        val edited = chatService.edit(message.id!!, "Hello everybody")
        assertEquals("Hello everybody", edited.content)
        assertTrue(edited.editedAt != null)
        chatService.hide(reply.id!!, true, "off topic") // The owner moderates.
        chatService.delete(message.id!!)
        val (root, replies) = chatService.selectThread(chatService.requireContext(conversationId), message.id!!)!!
        assertTrue(root.deleted)
        assertEquals(1, replies.size)
        assertEquals(getUserId(TEST_USER), replies[0].hiddenById)
    }

    @Test
    fun `a reply to a reply belongs to the thread of the message, changes are fetched by sequence`() {
        val conversationId = createRoom("chat thread test")
        val context = chatService.requireContext(conversationId)
        val message = chatService.send(conversationId, null, "question")
        val seq = chatService.currentSeq(conversationId)
        logon(TEST_USER2)
        val reply = chatService.send(conversationId, message.id, "answer")
        val replyToReply = chatService.send(conversationId, reply.id, "more")
        assertEquals(message.id, reply.threadRootId)
        assertEquals(message.id, replyToReply.threadRootId)

        val (root, replies) = chatService.selectThread(context, message.id!!)!!
        assertEquals(2, root.replyCount)
        assertEquals(listOf(reply.id, replyToReply.id), replies.map { it.id })
        assertEquals(setOf(getUserId(TEST_USER), getUserId(TEST_USER2)), chatService.threadFollowers(root))
        assertEquals(listOf(message.id), chatService.selectMessages(context, null, 50).map { it.id })

        val changes = chatService.selectChanges(context, seq)
        assertEquals(setOf(message.id, reply.id, replyToReply.id), changes.messages.map { it.id }.toSet())
        assertEquals(chatService.currentSeq(conversationId), changes.changeSeq)
        assertTrue(chatService.selectChanges(context, changes.changeSeq).messages.isEmpty())

        chatService.setFollowing(message.id!!, false)
        assertEquals(setOf(getUserId(TEST_USER)), chatService.threadFollowers(chatService.findMessage(message.id)!!))
        // A message of another conversation isn't accepted as root or as read message.
        val otherConversationId = createRoom("chat other room")
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { chatService.send(otherConversationId, message.id, "moved") }
            assertThrows(AccessException::class.java) { chatService.markRead(otherConversationId, message.id!!) }
        }
    }

    @Test
    fun `reactions are toggled per user`() {
        val conversationId = createRoom("chat reaction test")
        val message = chatService.send(conversationId, null, "react")
        chatService.toggleReaction(message.id!!, "👍")
        logon(TEST_USER2)
        chatService.toggleReaction(message.id!!, "👍")
        chatService.toggleReaction(message.id!!, "🎉")
        assertEquals(
            mapOf("👍" to listOf(getUserId(TEST_USER), getUserId(TEST_USER2)), "🎉" to listOf(getUserId(TEST_USER2))),
            chatService.findMessage(message.id)!!.readReactions(),
        )
        chatService.toggleReaction(message.id!!, "👍")
        chatService.toggleReaction(message.id!!, "🎉")
        assertEquals(mapOf("👍" to listOf(getUserId(TEST_USER))), chatService.findMessage(message.id)!!.readReactions())
        suppressErrorLogs {
            assertThrows(UserException::class.java) { chatService.toggleReaction(message.id!!, "<b>") }
        }
    }

    @Test
    fun `mentions of readers are notified once, others become text, muting suppresses new messages only`() {
        val conversationId = createRoom("chat mention test")
        val user2 = getUserId(TEST_USER2)
        val finance = getUserId(TEST_FINANCE_USER)
        val html = """<p>Hi <span data-type="mention" data-id="$user2">@fake</span> and
            |<span data-type="mention" data-id="$finance">@Finance</span></p>""".trimMargin()
        val message = chatService.send(conversationId, null, html)
        assertEquals("$user2", message.mentionedIds)
        assertTrue(message.content!!.contains("""data-id="$user2""""))
        assertTrue(message.content!!.contains("@${getUser(TEST_USER2).getFullname()}"))
        assertFalse(message.content!!.contains("""data-id="$finance""""))
        val mentionPrefix = ChatNotifier.mentionPrefix(conversationId, user2)
        assertEquals(1, notificationDao.findPendingByDedupPrefix(user2, mentionPrefix).size)
        assertTrue(notificationDao.findPendingByDedupPrefix(finance, ChatNotifier.mentionPrefix(conversationId, finance)).isEmpty())
        // Mentioned: no further notification about the message itself.
        assertTrue(notificationDao.findPendingByDedupPrefix(user2, ChatNotifier.newPrefix(conversationId, user2)).isEmpty())

        // An edit doesn't notify the mention again.
        chatService.edit(message.id!!, html.replace("Hi", "Hello"))
        assertEquals(1, notificationDao.findPendingByDedupPrefix(user2, mentionPrefix).size)

        // Reading resolves the notifications.
        logon(TEST_USER2)
        chatService.markRead(conversationId, message.id!!)
        assertTrue(notificationDao.findPendingByDedupPrefix(user2, mentionPrefix).isEmpty())
        assertEquals(0, chatService.countUnread(conversationId, user2))
        assertEquals(emptyMap<Long, Long>(), chatService.countUnread(listOf(conversationId), user2))

        chatService.setMuted(conversationId, true)
        logon(TEST_USER)
        chatService.send(conversationId, null, "nobody notified")
        assertTrue(notificationDao.findPendingByDedupPrefix(user2, ChatNotifier.newPrefix(conversationId, user2)).isEmpty())
        assertEquals(1, chatService.countUnread(conversationId, user2))
        assertEquals(mapOf(conversationId to 1L), chatService.countUnread(listOf(conversationId), user2))
        chatService.send(conversationId, null, """<span data-type="mention" data-id="$user2">x</span>""")
        assertEquals(1, notificationDao.findPendingByDedupPrefix(user2, mentionPrefix).size, "mentions despite muting")

        logon(TEST_USER2)
        chatService.setMuted(conversationId, false)
        logon(TEST_USER)
        chatService.send(conversationId, null, "first")
        chatService.send(conversationId, null, "second")
        assertEquals(1, notificationDao.findPendingByDedupPrefix(user2, ChatNotifier.newPrefix(conversationId, user2)).size)
        assertNull(chatService.getMemberState(conversationId, finance))
    }

    @Test
    fun `marking a message unread moves the read state back, the newest reply of others marks a thread`() {
        val conversationId = createRoom("chat unread test")
        val first = chatService.send(conversationId, null, "first")
        val second = chatService.send(conversationId, null, "second")
        val user2 = getUserId(TEST_USER2)

        logon(TEST_USER2)
        chatService.markRead(conversationId, second.id!!)
        assertEquals(0, chatService.countUnread(conversationId, user2))
        chatService.markUnread(conversationId, first.id!!)
        assertEquals(2, chatService.countUnread(conversationId, user2))
        assertEquals(first.id!! - 1, chatService.getMemberState(conversationId, user2)!!.lastReadMessageId)
        chatService.markUnread(conversationId, second.id!!) // Also forwards: read up to the message before.
        assertEquals(1, chatService.countUnread(conversationId, user2))

        val ownReply = chatService.send(conversationId, first.id, "reply of user2")
        logon(TEST_USER)
        val reply = chatService.send(conversationId, first.id, "reply of the owner")
        val summary = chatService.selectReplySummaries(listOf(first.id!!))[first.id]!!
        assertEquals(listOf(user2, getUserId(TEST_USER)), summary.authorIds)
        assertEquals(reply.id, summary.newestIdOfOthers(user2))
        assertEquals(ownReply.id, summary.newestIdOfOthers(getUserId(TEST_USER)))

        suppressErrorLogs {
            logon(TEST_FINANCE_USER)
            assertThrows(AccessException::class.java) { chatService.markUnread(conversationId, first.id!!) }
            val otherId = chatService.send(createRoom("chat unread other room"), null, "other").id!!
            logon(TEST_USER2)
            assertThrows(AccessException::class.java) { chatService.markUnread(conversationId, otherId) }
        }
    }

    @Test
    fun `replies count as unread in followed threads and with a mention, mentions in threads are resolved by opening them`() {
        val conversationId = createRoom("chat thread unread test")
        val user2 = getUserId(TEST_USER2)
        val root = chatService.send(conversationId, null, "root")
        logon(TEST_USER2)
        chatService.markRead(conversationId, root.id!!)

        logon(TEST_USER)
        chatService.send(conversationId, root.id, "a reply nobody but the owner follows")
        assertEquals(0, chatService.countUnread(conversationId, user2), "thread not followed")
        val mention = chatService.send(conversationId, root.id, """<span data-type="mention" data-id="$user2">x</span>""")
        assertEquals(1, chatService.countUnread(conversationId, user2), "mentioned")
        assertEquals(mapOf(conversationId to 1L), chatService.countUnread(listOf(conversationId), user2))
        val threadMentions = "${ChatNotifier.mentionPrefix(conversationId, user2)}t${root.id}:"
        assertEquals(1, notificationDao.findPendingByDedupPrefix(user2, threadMentions).size)

        logon(TEST_USER2)
        chatService.markRead(conversationId, mention.id!!)
        assertEquals(1, notificationDao.findPendingByDedupPrefix(user2, threadMentions).size, "not resolved by the conversation")
        chatService.markThreadRead(conversationId, root.id!!)
        assertTrue(notificationDao.findPendingByDedupPrefix(user2, threadMentions).isEmpty())
        chatService.setFollowing(root.id!!, true)

        logon(TEST_USER)
        val third = chatService.send(conversationId, root.id, "third")
        assertEquals(1, chatService.countUnread(conversationId, user2), "followed")
        chatService.hide(third.id!!, true, "off topic")
        assertEquals(0, chatService.countUnread(conversationId, user2), "hidden")
        chatService.hide(third.id!!, false, null) // A hidden message isn't deleted by its author.
        chatService.delete(third.id!!)
        assertEquals(chatService.findMessage(mention.id)!!.created, chatService.findMessage(root.id)!!.lastReplyAt)

        val hiddenRoot = chatService.send(conversationId, null, "hidden root")
        chatService.hide(hiddenRoot.id!!, true, "spam")
        chatService.delete(root.id!!)
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { chatService.send(conversationId, root.id, "to a deleted message") }
            assertThrows(AccessException::class.java) { chatService.send(conversationId, mention.id, "to its reply") }
            assertThrows(AccessException::class.java) { chatService.send(conversationId, hiddenRoot.id, "to a hidden message") }
            logon(TEST_FINANCE_USER)
            assertThrows(AccessException::class.java) { chatService.markThreadRead(conversationId, root.id!!) }
            logon(TEST_USER2)
            assertThrows(AccessException::class.java) { chatService.markThreadRead(conversationId, mention.id!!) }
        }
    }

    @Test
    fun `the unread messages are listed from the given read state on, replies with their thread`() {
        val conversationId = createRoom("chat unread list test")
        val roomId = chatService.requireContext(conversationId).hostId
        val first = chatService.send(conversationId, null, "first")
        logon(TEST_USER2)
        chatService.markRead(conversationId, first.id!!)
        chatService.setFollowing(first.id!!, true)

        logon(TEST_USER)
        val reply = chatService.send(conversationId, first.id, "reply")
        val second = chatService.send(conversationId, null, "second")
        logon(TEST_USER2)
        assertEquals(2, chatService.countUnread(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL))
        chatService.send(conversationId, null, "own") // Read up to here, not unread for its author anyway.
        assertEquals(0, chatService.countUnread(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL))
        val context = chatService.requireContext(conversationId)
        assertEquals(listOf(Pair(reply.id!!, first.id), Pair(second.id!!, null)), chatService.selectUnread(context, first.id!!))
        assertEquals(listOf(first.id!!, reply.id!!, second.id!!), chatService.selectUnread(context, 0).map { it.first })

        logon(TEST_FINANCE_USER) // No participant.
        assertEquals(0, chatService.countUnread(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL))
    }

    @Test
    fun `the search finds wildcards as text`() {
        val conversationId = createRoom("chat search test")
        val context = chatService.requireContext(conversationId)
        val literal = chatService.send(conversationId, null, "rename foo_bar to 100%")
        chatService.send(conversationId, null, "rename fooXbar to 1000")
        assertEquals(listOf(literal.id), chatService.search(context, "foo_bar", null).map { it.id })
        assertEquals(listOf(literal.id), chatService.search(context, "100%", null).map { it.id })
        assertTrue(chatService.search(context, "!", null).isEmpty())
    }

    @Test
    fun `deleting a room deletes its conversation`() {
        val conversationId = createRoom("chat delete test")
        val user2 = getUserId(TEST_USER2)
        val message = chatService.send(conversationId, null, "bye")
        chatService.edit(message.id!!, """bye <span data-type="mention" data-id="$user2">x</span>""")
        assertEquals(1, notificationDao.findPendingByDedupPrefix(null, "chat:mention:$conversationId:").size)
        assertTrue(historyCount(message.id!!) > 0)
        val roomId = chatService.requireContext(conversationId).hostId
        suppressErrorLogs {
            logon(TEST_USER2)
            assertThrows(AccessException::class.java) { chatService.deleteRoom(roomId) }
        }
        logon(TEST_USER)
        chatService.deleteRoom(roomId)
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { chatService.requireContext(conversationId) }
        }
        assertTrue(chatService.findConversations(ChatRoomChatHost.HOST_TYPE, listOf(roomId)).isEmpty())
        // No delayed mail about a deleted conversation, no former texts in the history.
        assertTrue(notificationDao.findPendingByDedupPrefix(null, "chat:mention:$conversationId:").isEmpty())
        assertTrue(notificationDao.findPendingByDedupPrefix(null, "chat:new:$conversationId:").isEmpty())
        assertEquals(0, historyCount(message.id!!))
    }

    private fun historyCount(messageId: Long): Long {
        return persistenceService.selectSingleResult(
            "select count(h) from HistoryEntryDO h where h.entityName = :entityName and h.entityId = :id",
            java.lang.Long::class.java,
            Pair("entityName", ChatMessageDO::class.java.name),
            Pair("id", messageId),
        )!!.toLong()
    }
}
