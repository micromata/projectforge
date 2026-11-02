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
 * Blocking users and reporting messages (PF-CHAT-DM-07, PF-CHAT-MSG-12): a block ends the direct messages between the
 * two and keeps the messages of the blocked one out of the unread counts and the search of the blocker; a report is
 * stored once, notifies the administrators and may block the author.
 */
class ChatBlockReportTest : AbstractTestBase() {
    @Autowired
    private lateinit var blockService: ChatBlockService

    @Autowired
    private lateinit var chatDirectService: ChatDirectService

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var reportService: ChatReportService

    @BeforeEach
    fun setUp() {
        chatNotifier.executor = java.util.concurrent.Executor { it.run() }
    }

    @Test
    fun `a block ends the direct messages of both, an unblock restores them, others are not affected`() {
        logon(TEST_ADMIN_USER) // A test before may have left a restricted user, who can't create groups.
        val a = initTestDB.addUser("chatBlock-a").id!!
        val b = initTestDB.addUser("chatBlock-b").id!!
        val c = initTestDB.addUser("chatBlock-c").id!!
        initTestDB.addGroup("chatBlock-group", "chatBlock-a", "chatBlock-b", "chatBlock-c")
        logon("chatBlock-a")
        val conversationId = chatDirectService.open(b).conversationId
        chatService.send(conversationId, null, "before the block")

        suppressErrorLogs {
            assertThrows(UserException::class.java) { blockService.block(a) } // Not oneself.
        }
        blockService.block(b)
        blockService.block(b) // Again: nothing happens.
        assertEquals(listOf(b), blockService.list())
        assertTrue(blockService.isBlocked(a, b) && blockService.isBlocked(b, a), "in both directions")
        assertTrue(blockService.hasBlocked(a, b))
        assertFalse(blockService.hasBlocked(b, a))
        assertWriteRefused(conversationId, a, b)

        logon("chatBlock-b")
        assertWriteRefused(conversationId, b, a)
        assertTrue(blockService.list().isEmpty(), "the blocked one has blocked nobody")

        logon("chatBlock-c")
        val other = chatDirectService.open(a)
        chatService.send(other.conversationId, null, "c writes to a")

        logon("chatBlock-a")
        blockService.unblock(b)
        blockService.unblock(b) // Again: nothing happens.
        assertTrue(blockService.list().isEmpty())
        chatService.send(chatDirectService.open(b).conversationId, null, "after the unblock")
    }

    private fun assertWriteRefused(conversationId: Long, userId: Long, otherUserId: Long) {
        suppressErrorLogs {
            assertFalse(chatService.requireContext(conversationId).canWrite, "user #$userId may still write")
            assertThrows(AccessException::class.java) { chatService.send(conversationId, null, "refused") }
            assertThrows(UserException::class.java) { chatDirectService.open(otherUserId) }
        }
    }

    @Test
    fun `the messages of a blocked author are no unread and no search hits for the blocker`() {
        val conversationId = createRoom("chat block room")
        logon(TEST_USER2)
        chatService.send(conversationId, null, "blockedmarker77 hello")
        val author = getUserId(TEST_USER2)

        logon(TEST_USER)
        val me = getUserId(TEST_USER)
        val context = chatService.requireContext(conversationId)
        assertEquals(1, chatService.countUnread(conversationId, me))
        assertEquals(1, chatService.search(context, "blockedmarker77", null).size)

        blockService.block(author)
        assertEquals(0, chatService.countUnread(conversationId, me))
        assertEquals(0, chatService.countUnread(listOf(conversationId), me).getOrDefault(conversationId, 0))
        assertTrue(chatService.search(context, "blockedmarker77", null).isEmpty())
        assertTrue(chatService.searchAll("blockedmarker77").isEmpty())
        assertTrue(chatService.mentionCandidates(context, null).none { it.id == author })

        // Only for the blocker.
        logon(TEST_USER2)
        assertEquals(1, chatService.search(chatService.requireContext(conversationId), "blockedmarker77", null).size)

        logon(TEST_USER)
        blockService.unblock(author)
        assertEquals(1, chatService.countUnread(conversationId, me))
    }

    @Test
    fun `a report is stored once, notifies the administrators and may block the author`() {
        val conversationId = createRoom("chat report room")
        logon(TEST_USER2)
        val message = chatService.send(conversationId, null, "reportedmarker88 rude text")
        val author = getUserId(TEST_USER2)
        val admin = getUserId(TEST_ADMIN_USER)

        // Nobody outside of the conversation reports, nobody their own message.
        suppressErrorLogs {
            assertThrows(UserException::class.java) { reportService.report(message.id, "my own") }
            logon(TEST_FINANCE_USER)
            assertThrows(AccessException::class.java) { reportService.report(message.id, "spy") }
            logon(TEST_USER)
            assertThrows(UserException::class.java) { reportService.report(-1, "unknown") }
        }

        reportService.report(message.id, "  spam  ", blockAuthor = true)
        reportService.report(message.id, "again")
        val reports = persistenceService.executeQuery(
            "from ChatReportDO r where r.messageId = :messageId",
            ChatReportDO::class.java,
            Pair("messageId", message.id),
        )
        assertEquals(1, reports.size)
        reports.single().let {
            assertEquals(getUserId(TEST_USER), it.reporterId)
            assertEquals(author, it.authorId, "from the stored message")
            assertEquals(conversationId, it.conversationId)
            assertEquals("spam", it.reason)
            assertEquals("reportedmarker88 rude text", it.contentSnapshot)
            assertEquals(ChatReportDO.Status.OPEN, it.status)
        }
        assertEquals(listOf(author), blockService.list(), "blocked with the report")
        blockService.unblock(author) // The database is shared with other tests.
        val notified = notificationDao.findPendingByDedupPrefix(admin, "chat:report:${message.id}:")
        assertEquals(1, notified.size, "once per administrator")
        assertTrue(notified.single().body!!.contains("reportedmarker88 rude text"))

        // The reports go with the conversation.
        val hostId = chatService.requireContext(conversationId).conversation.hostId!!
        chatService.deleteConversations(ChatRoomChatHost.HOST_TYPE, hostId)
        assertEquals(0, persistenceService.executeQuery(
            "from ChatReportDO r where r.conversationId = :conversationId",
            ChatReportDO::class.java,
            Pair("conversationId", conversationId),
        ).size)
    }

    private fun createRoom(name: String): Long {
        val owner = logon(TEST_USER)
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = name
            it.ownerId = owner.id
            it.participantUserIds = "${getUserId(TEST_USER2)}"
        })
        return chatService.open(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL)!!.conversationId
    }
}
