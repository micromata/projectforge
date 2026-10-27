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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.notification.NotificationDao
import org.projectforge.business.notification.NotificationDisplay
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.datatransfer.DataTransferBridge
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

/**
 * Direct messages (PF-CHAT-DM): only with visible users, one conversation per pair, only the two read it (admins
 * neither), read-only without the system access of the other one; their files go into the personal box of the
 * recipient, the files of rooms into the area of the conversation (with an in-memory data transfer).
 */
class ChatDirectTest : AbstractTestBase() {
    @Autowired
    private lateinit var chatDirectService: ChatDirectService

    @Autowired
    private lateinit var chatFileService: ChatFileService

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var dataTransferBridge: DataTransferBridge

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    private val dataTransfer = InMemoryDataTransfer()

    @BeforeEach
    fun setUp() {
        chatNotifier.executor = java.util.concurrent.Executor { it.run() }
        dataTransferBridge.register(dataTransfer)
    }

    @AfterEach
    fun tearDown() {
        dataTransferBridge.register(null)
    }

    @Test
    fun `only visible users are written to, the pair has one conversation read by both only`() {
        val user1 = initTestDB.addUser("chatDirect-1").id!!
        val user2 = initTestDB.addUser("chatDirect-2").id!!
        val invisible = initTestDB.addUser("chatDirect-3").id!! // In no group of user 1.
        initTestDB.addGroup("chatDirect-group", "chatDirect-1", "chatDirect-2")
        logon("chatDirect-1")
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { chatDirectService.open(invisible) }
            assertThrows(UserException::class.java) { chatDirectService.open(user1) }
        }
        val context = chatDirectService.open(user2)
        assertEquals(getUser("chatDirect-2").getFullname(), context.title)
        assertFalse(context.isModerator)
        assertTrue(chatDirectService.list().none { it.conversation.id == context.conversationId }, "empty: not listed")

        chatService.send(context.conversationId, null, "Hello")
        assertEquals(listOf(user2), chatDirectService.list().map { it.partnerId })
        val notification = notificationDao.findPendingByDedupPrefix(user2, ChatNotifier.newPrefix(context.conversationId, user2))
            .single()
        assertEquals(NotificationDisplay.BANNER, notification.display)
        assertTrue(notification.title!!.startsWith(getUser("chatDirect-1").getFullname()), notification.title)

        assertEquals(0, chatDirectService.countUnread(user2), "own message")
        logon("chatDirect-3")
        assertEquals(0, chatDirectService.countUnread(user1), "no conversation")

        // The other one: the same conversation, the other title, may answer.
        logon("chatDirect-2")
        assertEquals(1, chatDirectService.countUnread(user1))
        val other = chatDirectService.open(user1)
        assertEquals(context.conversationId, other.conversationId)
        assertEquals(getUser("chatDirect-1").getFullname(), other.title)
        chatService.send(other.conversationId, null, "Hi")

        // Nobody else, administrators neither.
        suppressErrorLogs {
            listOf(TEST_ADMIN_USER, TEST_FINANCE_USER, "chatDirect-3").forEach { username ->
                logon(username)
                assertThrows(AccessException::class.java) { chatService.requireContext(context.conversationId) }
                assertThrows(AccessException::class.java) { chatService.send(context.conversationId, null, "intruder") }
            }
        }
    }

    @Test
    fun `reading a direct message is announced to the other one, once per step and not for rooms`() {
        val user1 = initTestDB.addUser("chatDirectRead-1").id!!
        val user2 = initTestDB.addUser("chatDirectRead-2").id!!
        initTestDB.addGroup("chatDirectRead-group", "chatDirectRead-1", "chatDirectRead-2")
        val reads = mutableListOf<Pair<Long, Long>>()
        val target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject<ChatService>(chatService)
        val listeners = org.springframework.test.util.ReflectionTestUtils.getField(target, "eventListeners")
        org.springframework.test.util.ReflectionTestUtils.setField(target, "eventListeners", listOf(object : ChatEventListener {
            override fun onChange(conversationId: Long, changeSeq: Long) {}
            override fun onRead(conversationId: Long, readerId: Long) {
                reads.add(Pair(conversationId, readerId))
            }
        }))
        try {
            logon("chatDirectRead-1")
            val conversationId = chatDirectService.open(user2).conversationId
            val message = chatService.send(conversationId, null, "Hello")
            reads.clear() // Sending counts as reading by the author, not announced.
            logon("chatDirectRead-2")
            chatService.markRead(conversationId, message.id!!)
            assertEquals(listOf(Pair(conversationId, user2)), reads)
            chatService.markRead(conversationId, message.id!!)
            assertEquals(1, reads.size, "not raised: not announced again")
            // The other one sees how far the partner has read.
            logon("chatDirectRead-1")
            assertEquals(message.id, chatService.getMemberState(conversationId, user2)?.lastReadMessageId)

            reads.clear()
            logon("chatDirectRead-1")
            val roomId = chatRoomDao.insert(ChatRoomDO().also {
                it.name = "chat read test"
                it.ownerId = user1
                it.participantUserIds = "$user2"
            })
            val room = chatService.open(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL)!!.conversationId
            val roomMessage = chatService.send(room, null, "Room")
            logon("chatDirectRead-2")
            chatService.markRead(room, roomMessage.id!!)
            assertTrue(reads.isEmpty(), "rooms are not announced")
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(target, "eventListeners", listeners)
        }
    }

    @Test
    fun `without the system access of the other one the conversation is read-only`() {
        val partner = initTestDB.addUser("chatDirectPartner")
        logon(TEST_ADMIN_USER) // Sees everybody.
        val conversationId = chatDirectService.open(partner.id!!).conversationId
        chatService.send(conversationId, null, "Welcome")
        val deactivated = PFUserDO().also { it.copyValuesFrom(userGroupCache.getUser(partner.id)!!); it.deactivated = true }
        userGroupCache.updateUser(deactivated)
        try {
            val context = chatService.requireContext(conversationId)
            assertFalse(context.canWrite)
            suppressErrorLogs {
                assertThrows(AccessException::class.java) { chatService.send(conversationId, null, "anybody there?") }
                assertThrows(AccessException::class.java) { chatDirectService.open(partner.id!!) }
            }
        } finally {
            userGroupCache.updateUser(partner)
        }
    }

    @Test
    fun `files of direct messages go into the personal box of the recipient, of rooms into the area of the conversation`() {
        val user2 = getUserId(TEST_USER2)
        logon(TEST_USER)
        val direct = chatDirectService.open(user2)
        val pdf = ChatFileService.Upload("../report 2026.pdf", "%PDF-1.7 test".toByteArray())
        val message = chatService.send(direct.conversationId, null, "", listOf(pdf))
        val attachment = message.readAttachments().single()
        assertEquals(dataTransfer.personalBoxes[user2], attachment.areaId)
        assertEquals("report 2026.pdf", attachment.name)
        assertFalse(attachment.image)
        assertNotNull(chatFileService.download(direct, attachment.fileId))
        assertEquals(setOf(attachment.fileId), chatFileService.existingFileIds(listOf(attachment.areaId)))
        assertNull(direct.conversation.fileAreaId, "no area of its own")

        // A room: its own area, readable by its participants only; deleting a message deletes its files.
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = "chat file test"
            it.ownerId = getUserId(TEST_USER)
            it.participantUserIds = "$user2"
        })
        val room = chatService.open(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL)!!
        val areaId = chatService.requireContext(room.conversationId).conversation.fileAreaId!!
        assertEquals("Chat: chat file test", dataTransfer.areas[areaId]!!.name)
        val image = chatService.send(room.conversationId, null, "", listOf(ChatFileService.Upload("a.png", png())))
        val imageFile = image.readAttachments().single()
        assertTrue(imageFile.image)
        assertEquals(areaId, imageFile.areaId)
        assertTrue(chatFileService.owns(areaId))
        assertTrue(chatFileService.hasReadAccess(areaId, getUser(TEST_USER2)))
        assertTrue(chatFileService.isFileAccessible(areaId, imageFile.fileId, getUser(TEST_USER2)))
        assertFalse(chatFileService.hasDeleteAccess(areaId, imageFile.fileId, getUser(TEST_USER2)), "not the author")
        assertTrue(chatFileService.hasDeleteAccess(areaId, imageFile.fileId, getUser(TEST_USER)))
        assertFalse(chatFileService.hasReadAccess(areaId, getUser(TEST_ADMIN_USER)))
        assertFalse(chatFileService.hasReadAccess(areaId, getUser("user3")))

        // Hidden messages: their files for moderators only.
        chatService.hide(image.id!!, true, "confidential")
        assertFalse(chatFileService.isFileAccessible(areaId, imageFile.fileId, getUser(TEST_USER2)))
        assertTrue(chatFileService.isFileAccessible(areaId, imageFile.fileId, getUser(TEST_USER)))
        chatService.hide(image.id!!, false, null)

        chatService.delete(image.id!!)
        assertTrue(dataTransfer.areas[areaId]!!.files.isEmpty())

        // Too large for the area, too many: nothing is stored.
        dataTransfer.maxUploadSize = 10
        suppressErrorLogs {
            assertThrows(UserException::class.java) { chatService.send(room.conversationId, null, "", listOf(pdf)) }
        }
        dataTransfer.maxUploadSize = Long.MAX_VALUE
        suppressErrorLogs {
            assertThrows(UserException::class.java) {
                chatService.send(room.conversationId, null, "", List(chatFileService.maxPerMessage + 1) { pdf })
            }
        }
        assertTrue(dataTransfer.areas[areaId]!!.files.isEmpty())

        chatService.deleteRoom(roomId)
        assertNull(dataTransfer.areas[areaId], "area deleted with the room")
        assertFalse(chatFileService.owns(areaId))
    }

    @Test
    fun `without data transfer no files are exchanged`() {
        dataTransferBridge.register(null)
        logon(TEST_USER)
        val context = chatDirectService.open(getUserId(TEST_USER2))
        assertFalse(chatFileService.filesEnabled(context))
        suppressErrorLogs {
            assertThrows(UserException::class.java) {
                chatService.send(context.conversationId, null, "", listOf(ChatFileService.Upload("a.txt", "a".toByteArray())))
            }
        }
    }

    private fun png(): ByteArray {
        val image = java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB)
        return java.io.ByteArrayOutputStream().also { javax.imageio.ImageIO.write(image, "png", it) }.toByteArray()
    }
}
