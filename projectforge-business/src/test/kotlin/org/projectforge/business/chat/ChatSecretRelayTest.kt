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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.notification.NotificationDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import java.util.Base64

/**
 * Confidential conversations of direct chats ([ChatSecretRelay]): only the two of the direct chat, each step only by its
 * side and in its state, messages taken once, nothing left after taking, cancelling or expiring.
 */
class ChatSecretRelayTest : AbstractTestBase() {
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
    private lateinit var relay: ChatSecretRelay

    @BeforeEach
    fun setUp() {
        chatNotifier.executor = java.util.concurrent.Executor { it.run() }
    }

    @AfterEach
    fun tearDown() {
        relay.clock = System::currentTimeMillis
    }

    private val publicKey = base64(ByteArray(65).also { it[0] = 4 })

    private fun message(counter: Long) = ChatSecretRelay.Message(counter, base64(ByteArray(12)), base64(ByteArray(40)))

    /** The direct chat of initiator and recipient (in a group, so they see each other), opened by the initiator. */
    private fun directChat(prefix: String): Long {
        logon(TEST_ADMIN_USER)
        initTestDB.addUser("$prefix-sender")
        initTestDB.addUser("$prefix-recipient")
        initTestDB.addGroup("$prefix-group", "$prefix-sender", "$prefix-recipient")
        logon("$prefix-sender")
        return chatDirectService.open(getUserId("$prefix-recipient")).conversationId
    }

    @Test
    fun `both sides write back and forth, messages are taken once, nobody else sees the session`() {
        val conversationId = directChat("chatSecret1")
        suppressErrorLogs {
            assertThrows(UserException::class.java) { relay.offer(conversationId, base64(ByteArray(64))) }
        }
        val session = relay.offer(conversationId, publicKey)
        assertEquals(getUserId("chatSecret1-recipient"), session.recipientId)
        assertTrue(relay.pending(conversationId).isEmpty(), "the initiator is no recipient")

        suppressErrorLogs {
            listOf(TEST_USER, TEST_ADMIN_USER).forEach { username ->
                logon(username)
                assertNull(relay.get(session.id))
                assertThrows(AccessException::class.java) { relay.pending(conversationId) }
                assertThrows(AccessException::class.java) { relay.accept(session.id, publicKey) }
                assertThrows(AccessException::class.java) { relay.send(session.id, message(1)) }
                assertThrows(AccessException::class.java) { relay.take(session.id) }
                relay.cancel(session.id) // Does nothing.
            }
            logon("chatSecret1-sender")
            assertThrows(AccessException::class.java) { relay.accept(session.id, publicKey) }
            assertThrows(UserException::class.java) { relay.send(session.id, message(1)) } // Not accepted yet.
        }

        val recipientId = getUserId("chatSecret1-recipient")
        val prefix = ChatNotifier.secretPrefix(conversationId, recipientId)
        val notification = notificationDao.findPendingByDedupPrefix(recipientId, prefix).single()
        assertTrue(notification.title!!.startsWith(getUser("chatSecret1-sender").getFullname()), notification.title)

        logon("chatSecret1-recipient")
        assertEquals(listOf(session.id), relay.pending(conversationId).map { it.id })
        suppressErrorLogs {
            assertThrows(UserException::class.java) { relay.accept(session.id, base64(ByteArray(64))) }
        }
        relay.accept(session.id, publicKey)
        assertEquals(ChatSecretRelay.State.OPEN, relay.get(session.id)!!.state)
        assertTrue(notificationDao.findPendingByDedupPrefix(recipientId, prefix).isEmpty(), "resolved by accepting")
        suppressErrorLogs {
            assertThrows(UserException::class.java) { relay.accept(session.id, publicKey) } // Already open.
        }

        logon("chatSecret1-sender")
        assertEquals(publicKey, relay.get(session.id)!!.recipientKey)
        suppressErrorLogs {
            val tooLarge = ChatSecretRelay.Message(1, message(1).iv, base64(ByteArray(ChatSecretRelay.MAX_CIPHERTEXT_BYTES + 1)))
            assertThrows(UserException::class.java) { relay.send(session.id, tooLarge) }
            assertThrows(UserException::class.java) { relay.send(session.id, ChatSecretRelay.Message(1, base64(ByteArray(8)), message(1).ciphertext)) }
        }
        relay.send(session.id, message(1))
        relay.send(session.id, message(2))
        suppressErrorLogs {
            assertThrows(UserException::class.java) { relay.send(session.id, message(2)) } // Counter must rise.
            assertThrows(UserException::class.java) { relay.send(session.id, message(1)) }
        }
        assertEquals(0, relay.get(session.id)!!.waiting(getUserId("chatSecret1-sender")), "own messages aren't waiting")

        logon("chatSecret1-recipient")
        assertEquals(2, relay.get(session.id)!!.waiting(recipientId))
        assertEquals(listOf(1L, 2L), relay.take(session.id).map { it.counter })
        assertTrue(relay.take(session.id).isEmpty(), "taken once")
        relay.send(session.id, message(1)) // The recipient counts its own messages.

        logon("chatSecret1-sender")
        val senderId = getUserId("chatSecret1-sender")
        assertEquals(2L, relay.get(session.id)!!.read(senderId), "the initiator learns its messages were taken")
        assertEquals(listOf(1L), relay.take(session.id).map { it.counter })
        logon("chatSecret1-recipient")
        assertEquals(1L, relay.get(session.id)!!.read(recipientId))
        assertEquals(2L, relay.get(session.id)!!.sent(senderId), "the highest accepted counter, to resume after a lost answer")
    }

    @Test
    fun `a full queue is refused until the other side takes`() {
        val conversationId = directChat("chatSecret4")
        val session = relay.offer(conversationId, publicKey)
        logon("chatSecret4-recipient")
        relay.accept(session.id, publicKey)
        logon("chatSecret4-sender")
        (1L..ChatSecretRelay.MAX_QUEUED).forEach { relay.send(session.id, message(it)) }
        suppressErrorLogs {
            assertThrows(UserException::class.java) { relay.send(session.id, message(ChatSecretRelay.MAX_QUEUED + 1L)) }
        }
        logon("chatSecret4-recipient")
        assertEquals(ChatSecretRelay.MAX_QUEUED, relay.take(session.id).size)
        logon("chatSecret4-sender")
        relay.send(session.id, message(ChatSecretRelay.MAX_QUEUED + 1L))
    }

    @Test
    fun `only in direct chats, ended by either side, gone after idle time, maximum or absence`() {
        val conversationId = directChat("chatSecret2")
        val cancelled = relay.offer(conversationId, publicKey)
        logon("chatSecret2-recipient")
        relay.cancel(cancelled.id)
        assertTrue(relay.pending(conversationId).isEmpty())
        logon("chatSecret2-sender")
        assertNull(relay.get(cancelled.id), "gone for the other side too")

        val now = System.currentTimeMillis()
        // Idle: nothing sent for ten minutes, although both are there.
        val idle = relay.offer(conversationId, publicKey)
        logon("chatSecret2-recipient")
        relay.accept(idle.id, publicKey)
        for (minutes in 1..11) {
            relay.clock = { now + minutes * 60_000L }
            logon("chatSecret2-recipient")
            relay.get(idle.id)
            logon("chatSecret2-sender")
            relay.get(idle.id)
        }
        assertNull(relay.get(idle.id), "no message for ${ChatSecretRelay.IDLE_MILLIS / 60_000} minutes")
        relay.purge()
        assertEquals(0, relay.size)

        // A message keeps it going; the maximum ends it nevertheless.
        relay.clock = { now }
        val active = relay.offer(conversationId, publicKey)
        logon("chatSecret2-recipient")
        relay.accept(active.id, publicKey)
        var counter = 0L
        var minutes = 0L
        // Both sides keep asking (the browsers poll), a message every 5 minutes keeps the session from going idle.
        while (minutes < ChatSecretRelay.MAX_MILLIS / 60_000L - 1) {
            minutes += 1
            relay.clock = { now + minutes * 60_000L }
            logon("chatSecret2-sender")
            relay.get(active.id)
            if (minutes % 5 == 0L) {
                relay.send(active.id, message(++counter))
            }
            logon("chatSecret2-recipient")
            relay.take(active.id)
        }
        assertNotNull(relay.get(active.id))
        relay.clock = { now + ChatSecretRelay.MAX_MILLIS + 1 }
        assertNull(relay.get(active.id))
        relay.purge()
        assertEquals(0, relay.size)

        // A side not asking anymore (page closed) ends it for both.
        relay.clock = { now }
        logon("chatSecret2-sender")
        val absent = relay.offer(conversationId, publicKey)
        logon("chatSecret2-recipient")
        relay.accept(absent.id, publicKey)
        relay.clock = { now + ChatSecretRelay.PRESENCE_MILLIS - 1 }
        logon("chatSecret2-sender")
        assertNotNull(relay.get(absent.id))
        relay.clock = { now + ChatSecretRelay.PRESENCE_MILLIS + 1 }
        assertNull(relay.get(absent.id), "the recipient wasn't seen for ${ChatSecretRelay.PRESENCE_MILLIS} ms")
        relay.clock = System::currentTimeMillis

        // A room isn't a direct chat.
        val owner = logon(TEST_USER)
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = "chatSecret room"
            it.ownerId = owner.id
            it.participantUserIds = "${getUserId(TEST_USER2)}"
        })
        val roomConversation = chatService.open(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL)!!.conversationId
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { relay.offer(roomConversation, publicKey) }
        }
    }

    @Test
    fun `seen in the open chat, the notification of an offer is resolved`() {
        val conversationId = directChat("chatSecret3")
        val session = relay.offer(conversationId, publicKey)
        val recipientId = getUserId("chatSecret3-recipient")
        val prefix = ChatNotifier.secretPrefix(conversationId, recipientId)
        assertEquals(1, notificationDao.findPendingByDedupPrefix(recipientId, prefix).size)
        suppressErrorLogs {
            logon(TEST_USER)
            assertThrows(AccessException::class.java) { relay.seen(conversationId) }
        }
        assertEquals(1, notificationDao.findPendingByDedupPrefix(recipientId, prefix).size, "not by others")
        logon("chatSecret3-recipient")
        relay.seen(conversationId)
        assertTrue(notificationDao.findPendingByDedupPrefix(recipientId, prefix).isEmpty())
        assertEquals(listOf(session.id), relay.pending(conversationId).map { it.id }, "the offer stays")
    }

    private fun base64(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
