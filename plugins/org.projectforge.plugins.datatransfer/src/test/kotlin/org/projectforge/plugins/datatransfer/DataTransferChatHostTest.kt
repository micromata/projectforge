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
import org.projectforge.business.chat.ChatFileService
import org.projectforge.business.chat.ChatNotifier
import org.projectforge.business.chat.ChatService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.jcr.FileObject
import org.springframework.beans.factory.annotation.Autowired

/**
 * Every data transfer area is a chat room (PF-CHAT-DT): read and written by its admins, access users and members of
 * its access groups, moderated by its admins; the files of its messages are in the area itself. Personal boxes have
 * no chat of their own (their chat is the direct message).
 */
class DataTransferChatHostTest : AbstractTestBase() {
    @Autowired
    private lateinit var chatHost: DataTransferChatHost

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

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
        initJCRTestRepo(MODUL_NAME, "chatHostTestRepo")
    }

    @Test
    fun `the chat of an area is the one of the users with access`() {
        chatNotifier.executor = java.util.concurrent.Executor { it.run() }
        val admin = logon(TEST_HR_USER)
        val area = testService.createArea(
            "chat host test",
            accessUsers = listOf(getUser(TEST_FINANCE_USER)),
            accessGroups = listOf(getGroup(TEST_GROUP)),
        )
        val areaId = area.id!!
        val conversationId = open(areaId)!!
        assertTrue(chatHost.isModerator(admin, areaId, DataTransferChatHost.CHANNEL), "the admin moderates")
        assertEquals(
            setOf(TEST_HR_USER, TEST_FINANCE_USER, TEST_USER, TEST_USER2).map { getUserId(it) }.toSet(),
            chatHost.participantIds(areaId, DataTransferChatHost.CHANNEL),
        )

        // The file of a message goes into the area itself, no synthetic area.
        val message = chatService.send(conversationId, null, "", listOf(ChatFileService.Upload("notes.txt", "notes".toByteArray())))
        val attachment = message.readAttachments().single()
        assertEquals(areaId, attachment.areaId)
        assertNull(dataTransferAreaDao.getAreaHost(area), "an ordinary area")
        val file = FileObject().also { it.fileId = attachment.fileId; it.fileName = attachment.name }

        // The access user and the members of the access group read and write, they don't moderate.
        listOf(TEST_FINANCE_USER, TEST_USER2).forEach { username ->
            val user = logon(username)
            assertEquals(conversationId, open(areaId), username)
            assertTrue(chatHost.hasWriteAccess(user, areaId, DataTransferChatHost.CHANNEL), username)
            assertFalse(chatHost.isModerator(user, areaId, DataTransferChatHost.CHANNEL), username)
            assertNotNull(testService.downloadFile(area, file), username)
            assertEquals(1, chatService.countUnread(DataTransferChatHost.HOST_TYPE, areaId, DataTransferChatHost.CHANNEL), username)
            assertTrue(chatService.listConversations(DataTransferChatHost.HOST_TYPE).any { it.conversationId == conversationId }, username)
        }
        // Others don't, administrators neither.
        suppressErrorLogs {
            listOf(TEST_ADMIN_USER, TEST_EMPLOYEE_USER).forEach { username ->
                val user = logon(username)
                assertThrows(AccessException::class.java, { open(areaId) }, username)
                assertFalse(chatHost.hasReadAccess(user, areaId, DataTransferChatHost.CHANNEL), username)
                assertEquals(0, chatService.countUnread(DataTransferChatHost.HOST_TYPE, areaId, DataTransferChatHost.CHANNEL), username)
                assertFalse(chatService.listConversations(DataTransferChatHost.HOST_TYPE).any { it.conversationId == conversationId }, username)
            }
        }

        // The conversation is deleted with the area.
        logon(TEST_HR_USER)
        dataTransferAreaDao.delete(dataTransferAreaDao.find(areaId)!!)
        assertNull(chatService.findConversations(DataTransferChatHost.HOST_TYPE, listOf(areaId))[areaId])
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { open(areaId) }
        }
    }

    @Test
    fun `the chat of an area with external access says that external users don't see it`() {
        val user = logon(TEST_USER)
        val internal = testService.createArea("chat host internal")
        val external = testService.createArea("chat host external", externalDownloadEnabled = true)
        assertNull(chatHost.notice(internal.id!!, DataTransferChatHost.CHANNEL, user))
        assertNotNull(chatHost.notice(external.id!!, DataTransferChatHost.CHANNEL, user))
    }

    @Test
    fun `a personal box has no chat of its own`() {
        val user = logon(TEST_USER)
        val box = testService.createPersonalBox(user)
        assertFalse(chatHost.isChatArea(box))
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { open(box.id!!) }
        }
    }

    private fun open(areaId: Long): Long? {
        return chatService.open(DataTransferChatHost.HOST_TYPE, areaId, DataTransferChatHost.CHANNEL)?.conversationId
    }
}
