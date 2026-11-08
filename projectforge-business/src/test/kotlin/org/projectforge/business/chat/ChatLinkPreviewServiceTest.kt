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

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import javax.imageio.ImageIO

/** The previews of links: fetched after sending, replaced by an edit, dismissed by the author, readers only. */
class ChatLinkPreviewServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

    @Autowired
    private lateinit var linkPreviewService: ChatLinkPreviewService

    private lateinit var originalFetcher: (String) -> ChatLinkPreviewService.Fetched?

    private lateinit var originalExecutor: Executor

    private val fetched = mutableListOf<String>()

    @PostConstruct
    private fun postConstruct() {
        initTestFileStore() // The thumbnails.
    }

    @BeforeEach
    fun setUp() {
        chatNotifier.executor = Executor { it.run() }
        originalExecutor = linkPreviewService.executor
        originalFetcher = linkPreviewService.fetcher
        linkPreviewService.executor = Executor { it.run() }
        linkPreviewService.fetcher = { url ->
            fetched += url
            if (url.contains("missing")) null else ChatLinkPreviewService.Fetched(
                ChatLinkPreview.Page("Title of $url", "Description", "Site", null),
                ChatLinkPreviewService.thumbnail(png()),
            )
        }
    }

    @AfterEach
    fun tearDown() {
        linkPreviewService.executor = originalExecutor
        linkPreviewService.fetcher = originalFetcher
    }

    @Test
    fun `the preview is fetched after sending and replaced by an edit changing the link`() {
        val conversationId = createRoom("chat link preview test")
        val seq = chatService.currentSeq(conversationId)
        val message = chatService.send(conversationId, null, """<p>look <a href="https://example.com/one">here</a></p>""")
        val preview = chatService.findMessage(message.id)!!.readLinkPreview()!!
        assertEquals("https://example.com/one", preview.url)
        assertEquals("Title of https://example.com/one", preview.title)
        assertNotNull(preview.imageFileId)
        assertTrue(chatService.currentSeq(conversationId) > seq + 1) // The readers get the preview as a change.
        assertNotNull(chatService.readLinkPreviewImage(message.id!!))

        // Another participant reads it, a stranger doesn't.
        logon(TEST_USER2)
        assertNotNull(chatService.readLinkPreviewImage(message.id!!))
        suppressErrorLogs {
            logon(TEST_FINANCE_USER)
            assertThrows(AccessException::class.java) { chatService.readLinkPreviewImage(message.id!!) }
            logon(TEST_USER2)
            assertThrows(AccessException::class.java) { chatService.dismissLinkPreview(message.id!!) }
        }

        logon(TEST_USER)
        chatService.edit(message.id!!, """<p>look <a href="https://example.com/one">here</a>, edited</p>""")
        assertEquals(preview.imageFileId, chatService.findMessage(message.id)!!.readLinkPreview()!!.imageFileId)
        assertEquals(1, fetched.size) // Same link: kept, not fetched again.

        chatService.edit(message.id!!, """<p>now <a href="https://example.com/two">there</a></p>""")
        val replaced = chatService.findMessage(message.id)!!.readLinkPreview()!!
        assertEquals("https://example.com/two", replaced.url)
        assertNull(linkPreviewService.readImage(conversationId, preview.imageFileId!!)) // The old thumbnail is deleted.

        chatService.dismissLinkPreview(message.id!!)
        val dismissed = chatService.findMessage(message.id)!!.readLinkPreview()!!
        assertTrue(dismissed.dismissed)
        assertNull(dismissed.title)
        assertNull(chatService.readLinkPreviewImage(message.id!!))
        assertNull(linkPreviewService.readImage(conversationId, replaced.imageFileId!!))
        chatService.edit(message.id!!, """<p>now <a href="https://example.com/two">there</a>!</p>""")
        assertTrue(chatService.findMessage(message.id)!!.readLinkPreview()!!.dismissed) // Stays dismissed.
        assertEquals(2, fetched.size)

        chatService.edit(message.id!!, "<p>no link any more</p>")
        assertNull(chatService.findMessage(message.id)!!.linkPreview)
    }

    @Test
    fun `nothing is stored without a page, nothing is fetched when switched off`() {
        val conversationId = createRoom("chat link preview test 2")
        val missing = chatService.send(conversationId, null, "<p>https://example.com/missing</p>")
        assertNull(chatService.findMessage(missing.id)!!.linkPreview)
        linkPreviewService.enabled = false
        try {
            val off = chatService.send(conversationId, null, "<p>https://example.com/off</p>")
            assertNull(chatService.findMessage(off.id)!!.linkPreview)
            assertEquals(listOf("https://example.com/missing"), fetched)
        } finally {
            linkPreviewService.enabled = true
        }
        val deleted = chatService.send(conversationId, null, "<p>https://example.com/deleted</p>")
        val fileId = chatService.findMessage(deleted.id)!!.readLinkPreview()!!.imageFileId!!
        chatService.delete(deleted.id!!)
        assertNull(linkPreviewService.readImage(conversationId, fileId))
    }

    @Test
    fun `the preview shown while writing is taken when sending, or removed by the author`() {
        val conversationId = createRoom("chat link preview test 3")
        val (url, page) = chatService.previewLink(conversationId, "<p>see https://example.com/draft</p>")!!
        assertEquals("https://example.com/draft", url)
        assertEquals(40, page.thumbnail!!.width)
        assertNull(chatService.previewLink(conversationId, "<p>no link</p>"))
        val sent = chatService.send(conversationId, null, "<p>see https://example.com/draft</p>")
        assertEquals(40, chatService.findMessage(sent.id)!!.readLinkPreview()!!.imageWidth)
        assertEquals(1, fetched.size) // From the cache.

        val removed = chatService.send(conversationId, null, "<p>see https://example.com/removed</p>", linkPreview = false)
        assertTrue(chatService.findMessage(removed.id)!!.readLinkPreview()!!.dismissed)
        assertEquals(1, fetched.size)
        suppressErrorLogs {
            logon(TEST_FINANCE_USER)
            assertThrows(AccessException::class.java) { chatService.previewLink(conversationId, "<p>https://example.com/x</p>") }
        }
    }

    /** A room of TEST_USER (owner) with TEST_USER2 as participant. */
    private fun createRoom(name: String): Long {
        val owner = logon(TEST_USER)
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = name
            it.ownerId = owner.id
            it.participantUserIds = "${getUserId(TEST_USER2)}"
        })
        return chatService.open(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL)!!.conversationId
    }

    private fun png(): ByteArray {
        return ByteArrayOutputStream().also { ImageIO.write(BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", it) }
            .toByteArray()
    }
}
