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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.notification.NotificationDao
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

/** The notifications of chat messages show the beginning of the message to their recipient, as long as it may read it. */
class ChatNotificationPreviewTest : AbstractTestBase() {
    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var preview: ChatNotificationPreview

    @BeforeEach
    fun setUp() {
        chatNotifier.executor = java.util.concurrent.Executor { it.run() }
    }

    @Test
    fun `the beginning of the message, with the author where the title lacks it, for the recipient only`() {
        val owner = logon(TEST_USER)
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = "chat preview test"
            it.ownerId = owner.id
            it.participantUserIds = "${getUserId(TEST_USER2)}"
        })
        val conversationId = chatService.open(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL)!!.conversationId
        val long = "word ".repeat(60)
        val message = chatService.send(conversationId, null, "<p>Hello <b>all</b>,</p><p>$long</p>")
        val user2 = getUser(TEST_USER2)
        val notification = notificationDao.findPendingByDedupPrefix(user2.id!!, ChatNotifier.newPrefix(conversationId, user2.id!!)).single()

        val text = preview.preview(notification, user2)!!
        val expectedStart = "${owner.getFullname()}: Hello all, word word"
        assertEquals(expectedStart, text.take(expectedStart.length))
        assertEquals(owner.getFullname()!!.length + 2 + ChatNotificationPreview.MAX_LENGTH, text.length)
        // Not shown to somebody else, nor once the message is deleted.
        assertNull(preview.preview(notification, getUser(TEST_FINANCE_USER)))
        chatService.delete(message.id!!)
        assertNull(preview.preview(notification, user2))

        // A mention names the author in its title already.
        val mention = chatService.send(conversationId, null, """<p><span data-type="mention" data-id="${user2.id}">x</span> look</p>""")
        val mentioned = notificationDao.findPendingByDedupPrefix(user2.id!!, ChatNotifier.mentionPrefix(conversationId, user2.id!!))
            .single { it.link!!.endsWith("message=${mention.id}") }
        assertEquals("@${user2.getFullname()} look", preview.preview(mentioned, user2))
    }
}
