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

package org.projectforge.plugins.datatransfer

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.chat.ChatDirectService
import org.projectforge.business.chat.ChatFileService
import org.projectforge.business.chat.ChatNotifier
import org.projectforge.business.chat.ChatRoomChatHost
import org.projectforge.business.chat.ChatRoomDO
import org.projectforge.business.chat.ChatRoomDao
import org.projectforge.business.chat.ChatService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.jcr.FileObject
import org.springframework.beans.factory.annotation.Autowired

/**
 * The files of the chat in the data transfer: the synthetic area of a room is seen by its participants only (admins
 * neither), nobody uploads into it or administers it there; the files of a direct message are in the personal box of
 * the recipient, for both of them.
 */
class DataTransferChatAreaTest : AbstractTestBase() {
    @Autowired
    private lateinit var chatDirectService: ChatDirectService

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    @Autowired
    private lateinit var testService: DataTransferTestService

    init {
        DataTransferTestService.addPluginEntitiesForTestMode()
    }

    @PostConstruct
    private fun postConstruct() {
        initJCRTestRepo(MODUL_NAME, "chatAreaTestRepo")
    }

    @Test
    fun `the area of a room is the one of its participants, read-only in the data transfer`() {
        chatNotifier.executor = java.util.concurrent.Executor { it.run() }
        val owner = logon(TEST_USER)
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = "data transfer chat test"
            it.ownerId = owner.id
            it.participantUserIds = "${getUserId(TEST_USER2)}"
        })
        val conversationId = chatService.open(ChatRoomChatHost.HOST_TYPE, roomId, ChatRoomChatHost.CHANNEL)!!.conversationId
        val message = chatService.send(conversationId, null, "", listOf(ChatFileService.Upload("notes.txt", "notes".toByteArray())))
        val attachment = message.readAttachments().single()
        val area = dataTransferAreaDao.find(attachment.areaId, checkAccess = false)!!
        assertEquals("Chat: data transfer chat test", area.areaName)
        assertNull(area.adminIds)
        assertTrue(area.externalDownloadEnabled != true)
        val file = FileObject().also { it.fileId = attachment.fileId; it.fileName = attachment.name }

        // The participants see the area in the data transfer list and download the file.
        listOf(TEST_USER, TEST_USER2).forEach { username ->
            logon(username)
            assertTrue(dataTransferAreaDao.select(QueryFilter()).any { it.id == area.id }, username)
            assertNotNull(testService.downloadFile(area, file), username)
        }
        // Nobody uploads into it or changes it in the data transfer, the participants neither.
        assertNull(testService.createFile(area, "upload.txt"))
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { dataTransferAreaDao.update(area) }
        }
        // Others don't see it, administrators neither.
        suppressErrorLogs {
            listOf(TEST_ADMIN_USER, TEST_HR_USER).forEach { username ->
                logon(username)
                assertFalse(dataTransferAreaDao.select(QueryFilter()).any { it.id == area.id }, username)
                assertNull(testService.downloadFile(area, file), username)
            }
        }

        logon(TEST_USER)
        chatService.deleteRoom(roomId)
        assertNull(dataTransferAreaDao.find(area.id, checkAccess = false), "deleted with the room")
    }

    @Test
    fun `the files of a direct message are in the personal box of the recipient`() {
        chatNotifier.executor = java.util.concurrent.Executor { it.run() }
        val recipient = getUser(TEST_USER2)
        logon(TEST_USER)
        val conversationId = chatDirectService.open(recipient.id!!).conversationId
        val message = chatService.send(conversationId, null, "for you", listOf(ChatFileService.Upload("offer.pdf", "%PDF".toByteArray())))
        val attachment = message.readAttachments().single()
        val box = dataTransferAreaDao.find(attachment.areaId, checkAccess = false)!!
        assertEquals(recipient.id, box.getPersonalBoxUserId())
        val file = FileObject().also { it.fileId = attachment.fileId; it.fileName = attachment.name }
        assertNotNull(testService.downloadFile(box, file), "the sender")
        logon(recipient)
        assertNotNull(testService.downloadFile(box, file), "the recipient")
        suppressErrorLogs {
            logon(TEST_HR_USER)
            assertNull(testService.downloadFile(box, file), "somebody else")
        }
    }
}
