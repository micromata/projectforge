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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired

/**
 * Who reads, changes and deletes a chat room: participants read, owner and moderators change, nobody else (admins
 * neither); the owner and the moderators come from the stored room.
 */
class ChatRoomDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Test
    fun `participants read a room, others and admins don't`() {
        logon(TEST_ADMIN_USER)
        val group = initTestDB.addGroup("chatRoomDaoTestGroup", TEST_EMPLOYEE_USER)
        val owner = logon(TEST_USER)
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = "room access test"
            it.ownerId = owner.id
            it.participantUserIds = "${getUserId(TEST_USER2)}"
            it.participantGroupIds = "${group.id}"
        })
        assertNotNull(chatRoomDao.find(roomId))
        logon(TEST_USER2)
        assertNotNull(chatRoomDao.find(roomId), "participant as user")
        logon(TEST_EMPLOYEE_USER)
        assertNotNull(chatRoomDao.find(roomId), "participant as member of a group")
        suppressErrorLogs {
            logon(TEST_FINANCE_USER)
            assertThrows(AccessException::class.java) { chatRoomDao.find(roomId) }
            logon(TEST_ADMIN_USER)
            assertThrows(AccessException::class.java) { chatRoomDao.find(roomId) }
        }
        val room = chatRoomDao.find(roomId, checkAccess = false)!!
        assertTrue(chatRoomDao.isParticipant(room, getUserId(TEST_EMPLOYEE_USER)))
        assertFalse(chatRoomDao.isParticipant(room, getUserId(TEST_FINANCE_USER)))
        assertEquals(
            setOf(getUserId(TEST_USER), getUserId(TEST_USER2), getUserId(TEST_EMPLOYEE_USER)),
            chatRoomDao.getParticipantIds(room),
        )
    }

    @Test
    fun `a room can't be taken over, only owner and moderators change it`() {
        val owner = logon(TEST_USER)
        val roomId = chatRoomDao.insert(ChatRoomDO().also {
            it.name = "room takeover test"
            it.ownerId = owner.id
            it.participantUserIds = "${getUserId(TEST_USER2)}"
        })
        suppressErrorLogs {
            // A foreign owner on insert:
            assertThrows(AccessException::class.java) {
                chatRoomDao.insert(ChatRoomDO().also { it.name = "foreign"; it.ownerId = getUserId(TEST_USER2) })
            }
            // A participant posting themselves as moderator:
            val participant = logon(TEST_USER2)
            assertThrows(AccessException::class.java) {
                chatRoomDao.update(chatRoomDao.find(roomId)!!.also {
                    it.moderatorUserIds = "${participant.id}"
                    it.name = "taken over"
                })
            }
            assertThrows(AccessException::class.java) { chatRoomDao.markAsDeleted(chatRoomDao.find(roomId)!!) }
        }
        logon(owner)
        chatRoomDao.update(chatRoomDao.find(roomId)!!.also {
            it.moderatorUserIds = "${getUserId(TEST_USER2)}"
            it.ownerId = getUserId(TEST_USER2) // Ignored: the owner stays.
        })
        assertEquals(owner.id, chatRoomDao.find(roomId)!!.ownerId)
        logon(TEST_USER2)
        chatRoomDao.update(chatRoomDao.find(roomId)!!.also { it.name = "renamed by a moderator" })
        assertEquals("renamed by a moderator", chatRoomDao.find(roomId)!!.name)
        assertNull(chatRoomDao.find(roomId)!!.description)
    }
}
