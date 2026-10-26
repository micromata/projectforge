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
 * Confidential messages of direct chats ([ChatSecretRelay]): only the two of the direct chat, each step only by its
 * side and in its state, the ciphertext taken once, nothing left after taking, cancelling or expiring.
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

    private val payload = ChatSecretRelay.Payload(publicKey, base64(ByteArray(12)), base64(ByteArray(40)))

    /** The direct chat of sender and recipient (in a group, so they see each other), opened by the sender. */
    private fun directChat(prefix: String): Long {
        logon(TEST_ADMIN_USER)
        initTestDB.addUser("$prefix-sender")
        initTestDB.addUser("$prefix-recipient")
        initTestDB.addGroup("$prefix-group", "$prefix-sender", "$prefix-recipient")
        logon("$prefix-sender")
        return chatDirectService.open(getUserId("$prefix-recipient")).conversationId
    }

    @Test
    fun `the ciphertext goes from the sender to the recipient once, nobody else sees the session`() {
        val conversationId = directChat("chatSecret1")
        val session = relay.offer(conversationId)
        assertEquals(getUserId("chatSecret1-recipient"), session.recipientId)
        assertTrue(relay.pending(conversationId).isEmpty(), "the sender is no recipient")

        suppressErrorLogs {
            listOf(TEST_USER, TEST_ADMIN_USER).forEach { username ->
                logon(username)
                assertNull(relay.get(session.id))
                assertThrows(AccessException::class.java) { relay.pending(conversationId) }
                assertThrows(AccessException::class.java) { relay.accept(session.id, publicKey) }
                assertThrows(AccessException::class.java) { relay.take(session.id) }
                relay.cancel(session.id) // Does nothing.
            }
            logon("chatSecret1-sender")
            assertThrows(AccessException::class.java) { relay.accept(session.id, publicKey) }
            assertThrows(UserException::class.java) { relay.deliver(session.id, payload) } // Not accepted yet.
        }

        val recipientId = getUserId("chatSecret1-recipient")
        val prefix = ChatNotifier.secretPrefix(conversationId, recipientId)
        val notification = notificationDao.findPendingByDedupPrefix(recipientId, prefix).single()
        assertTrue(notification.title!!.startsWith(getUser("chatSecret1-sender").getFullname()), notification.title)

        logon("chatSecret1-recipient")
        assertEquals(listOf(session.id), relay.pending(conversationId).map { it.id })
        suppressErrorLogs {
            assertThrows(UserException::class.java) { relay.accept(session.id, base64(ByteArray(64))) }
            assertThrows(AccessException::class.java) { relay.deliver(session.id, payload) }
        }
        relay.accept(session.id, publicKey)
        assertEquals(ChatSecretRelay.State.ACCEPTED, relay.get(session.id)!!.state)
        assertTrue(notificationDao.findPendingByDedupPrefix(recipientId, prefix).isEmpty(), "resolved by accepting")

        logon("chatSecret1-sender")
        assertEquals(publicKey, relay.get(session.id)!!.recipientKey)
        suppressErrorLogs {
            val tooLarge = ChatSecretRelay.Payload(publicKey, payload.iv, base64(ByteArray(ChatSecretRelay.MAX_CIPHERTEXT_BYTES + 1)))
            assertThrows(UserException::class.java) { relay.deliver(session.id, tooLarge) }
            assertThrows(UserException::class.java) { relay.deliver(session.id, ChatSecretRelay.Payload(publicKey, base64(ByteArray(8)), payload.ciphertext)) }
        }
        relay.deliver(session.id, payload)

        logon("chatSecret1-recipient")
        assertEquals(payload.ciphertext, relay.take(session.id).ciphertext)
        assertTrue(relay.pending(conversationId).isEmpty(), "taken: nothing to accept or take anymore")
        suppressErrorLogs {
            assertThrows(UserException::class.java) { relay.take(session.id) }
        }
        logon("chatSecret1-sender")
        assertEquals(ChatSecretRelay.State.TAKEN, relay.get(session.id)!!.state, "the sender learns it was opened")
        assertNull(relay.get(session.id)!!.payload)
    }

    @Test
    fun `only in direct chats, cancelled by either side, gone after expiry`() {
        val conversationId = directChat("chatSecret2")
        val cancelled = relay.offer(conversationId)
        logon("chatSecret2-recipient")
        relay.cancel(cancelled.id)
        assertTrue(relay.pending(conversationId).isEmpty())

        logon("chatSecret2-sender")
        val expiring = relay.offer(conversationId)
        assertNotNull(relay.get(expiring.id))
        val now = System.currentTimeMillis()
        relay.clock = { now + ChatSecretRelay.TTL_MILLIS + 1 }
        assertNull(relay.get(expiring.id))
        relay.purge()
        relay.clock = System::currentTimeMillis
        assertNull(relay.get(expiring.id), "deleted by the purge")

        // A room isn't a direct chat.
        val owner = logon(TEST_USER)
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = "chatSecret room"
            it.ownerId = owner.id
            it.participantUserIds = "${getUserId(TEST_USER2)}"
        })
        val roomConversation = chatService.open(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL)!!.conversationId
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { relay.offer(roomConversation) }
        }
    }

    @Test
    fun `seen in the open chat, the notification of an offer is resolved`() {
        val conversationId = directChat("chatSecret3")
        val session = relay.offer(conversationId)
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
