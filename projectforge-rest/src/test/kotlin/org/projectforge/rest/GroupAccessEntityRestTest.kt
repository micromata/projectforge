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


package org.projectforge.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.GroupTaskAccess
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Task
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest

class GroupAccessEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var groupAccessEntityRest: GroupAccessEntityRest

    @Test
    fun `task and group are required`() {
        logon(TEST_ADMIN_USER)
        val errors = groupAccessEntityRest.validate(GroupTaskAccessDO(), postData(GroupTaskAccess()))
        assertEquals(setOf("task", "group"), errors?.map { it.fieldId }?.toSet())
    }

    @Test
    fun `an existing pair is refused, a deleted one is restored`() {
        logon(TEST_ADMIN_USER)
        val task = initTestDB.addTask("GroupAccessEntityRestTest", "root")
        val groupId = getGroupId(TEST_GROUP)
        val existingId = persistenceService.runInTransaction { _ ->
            accessDao.insert(GroupTaskAccessDO().also {
                accessDao.setTask(it, task.id!!)
                it.group = getGroup(TEST_GROUP)
            })
        }
        val dto = GroupTaskAccess(task = Task(task.id), group = Group(groupId), description = "restored")
        val refused = groupAccessEntityRest.validate(GroupTaskAccessDO(), postData(dto))
        assertEquals(listOf("task"), refused?.map { it.fieldId })

        // Editing the existing entry itself is no duplicate.
        dto.id = existingId
        assertNull(groupAccessEntityRest.validate(GroupTaskAccessDO(), postData(dto)))

        accessDao.markAsDeleted(accessDao.find(existingId)!!)
        dto.id = null
        val data = postData(dto)
        assertNull(groupAccessEntityRest.validate(GroupTaskAccessDO(), data), "a deleted pair may be re-created")
        val obj = groupAccessEntityRest.transformForDB(dto)
        groupAccessEntityRest.onBeforeSave(MockHttpServletRequest(), obj, data)
        assertEquals(existingId, obj.id)
        val restored = accessDao.find(existingId)!!
        assertFalse(restored.deleted)
        assertEquals("restored", restored.description)
        assertTrue(accessDao.selectAll(false).count { it.taskId == task.id && it.groupId == groupId } == 1)
    }

    private fun postData(dto: GroupTaskAccess) = PostData(dto, watchFieldsTriggered = null, serverData = null)
}
