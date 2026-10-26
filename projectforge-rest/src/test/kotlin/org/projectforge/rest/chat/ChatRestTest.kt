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

package org.projectforge.rest.chat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.chat.ChatNotifier
import org.projectforge.business.chat.InMemoryDataTransfer
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.datatransfer.DataTransferBridge
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

/**
 * The chat endpoints as the next app uses them: rooms with participants the user may see only, the messages as
 * the logged-in user sees them.
 */
class ChatRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var chatRest: ChatRest

    @Autowired
    private lateinit var chatRoomRest: ChatRoomRest

    @Autowired
    private lateinit var chatDirectRest: ChatDirectRest

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

    @Autowired
    private lateinit var dataTransferBridge: DataTransferBridge

    @BeforeEach
    fun setUp() {
        chatNotifier.executor = java.util.concurrent.Executor { it.run() }
    }

    @Test
    fun `a room takes only participants the user may see, the owner from the logged-in user`() {
        logon(TEST_USER)
        val result = chatRoomRest.save(ChatRoom(
            name = " rest room ",
            description = "<p>The <strong>rest</strong> room</p>",
            owner = ChatRef(getUserId(TEST_USER2), "posted owner"),
            // TEST_USER2 shares a group with TEST_USER, the finance user doesn't.
            users = listOf(ChatRef(getUserId(TEST_USER2), "x"), ChatRef(getUserId(TEST_FINANCE_USER), "y")),
        ))
        assertNotNull(result.conversationId)
        val room = chatRoomRest.get(result.id)
        assertEquals("rest room", room.name)
        assertEquals(getUserId(TEST_USER), room.owner!!.id)
        assertEquals(listOf(getUserId(TEST_USER2)), room.users.map { it.id })
        assertTrue(room.canEdit)
        assertTrue(chatRoomRest.list().any { it.id == result.id && it.conversationId == result.conversationId })
        chatRest.getConversation(result.conversationId!!).let {
            assertEquals("<p>The <strong>rest</strong> room</p>", it.description)
            assertTrue(it.isModerator)
        }
        suppressErrorLogs {
            // Rich text isn't cut off (its markup would be broken), but refused.
            assertThrows(UserException::class.java) {
                chatRoomRest.save(chatRoomRest.get(result.id).also {
                    it.description = "x".repeat(ChatRoomRest.DESCRIPTION_MAX_LENGTH + 1)
                })
            }
        }

        logon(TEST_USER2)
        assertFalse(chatRoomRest.get(result.id).canEdit)
        assertFalse(chatRest.getConversation(result.conversationId!!).isModerator)
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { chatRoomRest.save(room.also { it.name = "taken over" }) }
            logon(TEST_FINANCE_USER)
            assertThrows(AccessException::class.java) { chatRoomRest.get(result.id) }
            assertTrue(chatRoomRest.list().none { it.id == result.id })
            assertThrows(AccessException::class.java) { chatRest.getConversation(result.conversationId!!) }
        }
    }

    @Test
    fun `messages show what the user may see, a hidden one only to moderators`() {
        logon(TEST_USER)
        val conversationId = chatRoomRest.save(ChatRoom(
            name = "rest messages",
            users = listOf(ChatRef(getUserId(TEST_USER2), "x")),
        )).conversationId!!
        logon(TEST_USER2)
        val message = chatRest.send(conversationId, ChatSendRequest(html = "<p>hello</p>"))
        assertTrue(message.own)
        chatRest.react(message.id, ChatReactionRequest("👍"))
        logon(TEST_USER)
        val conversation = chatRest.getConversation(conversationId)
        assertTrue(conversation.isModerator)
        assertTrue(conversation.canWrite)
        chatRest.hide(message.id, ChatHideRequest(true, "spam"))
        val seen = chatRest.getMessages(conversationId, null, null).messages.single()
        assertEquals("<p>hello</p>", seen.content, "moderators see the content")
        assertEquals("spam", seen.hiddenReason)
        assertEquals(1, seen.reactions.single().count)
        assertFalse(seen.reactions.single().mine)

        logon(TEST_USER2)
        val hidden = chatRest.getChanges(conversationId, 0).messages.single { it.id == message.id }
        assertTrue(hidden.hidden)
        assertNull(hidden.content)
        assertNull(hidden.hiddenReason)
        assertTrue(hidden.reactions.isEmpty(), "no reactions (and names) of a hidden message")
        assertTrue(chatRest.mentionCandidates(conversationId, "").any { it.id == getUserId(TEST_USER) })
    }

    @Test
    fun `direct messages are listed with a message only, files are downloads unless images`() {
        val user2 = getUserId(TEST_USER2)
        logon(TEST_USER)
        suppressErrorLogs {
            // Without the data transfer: no files.
            val conversationId = chatDirectRest.open(ChatDirectOpenRequest(user2)).conversationId
            val conversation = chatRest.getConversation(conversationId)
            assertEquals(user2, conversation.directPartner!!.id)
            assertFalse(conversation.filesEnabled)
            assertThrows(UserException::class.java) {
                chatRest.sendWithFiles(conversationId, ChatSendRequest(html = "x"), listOf(file("a.txt", "a".toByteArray())))
            }
            assertThrows(AccessException::class.java) { chatDirectRest.open(ChatDirectOpenRequest(getUserId("user3"))) }
        }
        val dataTransfer = InMemoryDataTransfer()
        dataTransferBridge.register(dataTransfer)
        try {
            val conversationId = chatDirectRest.open(ChatDirectOpenRequest(user2)).conversationId
            assertTrue(chatRest.getConversation(conversationId).filesEnabled)
            val png = java.io.ByteArrayOutputStream().also {
                javax.imageio.ImageIO.write(java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", it)
            }.toByteArray()
            val message = chatRest.sendWithFiles(
                conversationId,
                ChatSendRequest(html = "<p>files</p>"),
                listOf(file("page.html", "<script>alert(1)</script>".toByteArray()), file("pic.png", png)),
            )
            assertTrue(chatDirectRest.list().any { it.conversationId == conversationId && it.partner.id == user2 })
            val (html, image) = message.files
            assertFalse(html.image)
            assertTrue(html.available)
            assertTrue(image.image)
            chatRest.downloadFile(conversationId, html.fileId, null).let { response ->
                assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.headers.contentType)
                assertTrue(response.headers.contentDisposition.isAttachment)
                assertEquals("nosniff", response.headers.getFirst("X-Content-Type-Options"))
            }
            chatRest.downloadFile(conversationId, image.fileId, null).let { response ->
                assertEquals(MediaType.IMAGE_PNG, response.headers.contentType)
                assertTrue(response.headers.contentDisposition.isInline)
            }
            // Expired in the data transfer: still listed, not available.
            dataTransfer.areas.values.forEach { it.files.remove(html.fileId) }
            assertFalse(chatRest.getMessages(conversationId, null, null).messages.first().files.first().available)
            assertEquals(404, chatRest.downloadFile(conversationId, html.fileId, null).statusCode.value())
            suppressErrorLogs {
                logon(TEST_FINANCE_USER)
                assertThrows(AccessException::class.java) { chatRest.downloadFile(conversationId, image.fileId, null) }
            }
        } finally {
            dataTransferBridge.register(null)
        }
    }

    private fun file(name: String, content: ByteArray) =
        org.springframework.mock.web.MockMultipartFile("files", name, "application/octet-stream", content)
}
