/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class TaskTest {

    @Test
    fun dtoTest() {
        val root = TaskDO()
        root.id = 1
        root.title = "root"
        root.description = "This is the root node."

        var dest = Task()
        dest.copyFrom(root)
        assertEquals(1, dest.id)
        assertEquals("root", dest.title)
        assertEquals("This is the root node.", dest.description)
        assertNull(dest.parentTask)

        val task = TaskDO()
        task.parentTask = root
        task.id = 2
        task.title = "t1"
        task.description = "description"
        task.isProtectionOfPrivacy = true
        task.isKost2IsBlackList = false
        val responsibleUser = PFUserDO()
        responsibleUser.id = 3
        responsibleUser.username = "kai"
        responsibleUser.email = "email"
        task.responsibleUser = responsibleUser
        dest = Task()
        dest.copyFrom(task)

        assertEquals("t1", dest.title)
        assertEquals("description", dest.description)
        assertEquals(2, dest.id)
        checkMinimalTask(dest.parentTask, 1, "root")
        assertNotNull(dest.responsibleUser)
        assertEquals("kai", dest.responsibleUser?.username)
        assertNull(dest.responsibleUser?.email, "Do not copy email for minimal copy.")
        assertTrue(dest.protectionOfPrivacy == true)
        assertFalse(dest.kost2IsBlackList == true)


        dest = Task()
        dest.copyFrom(task)
        assertNotNull(dest.parentTask)
        assertEquals(1, dest.parentTask?.id)
        assertEquals("root", dest.parentTask?.title)
        assertNull(dest.parentTask?.description, "Do not copy description for minimal copy.")

        val subTask = TaskDO()
        subTask.id = 3
        subTask.title = "sub task"
        subTask.description = "sub task desc"
        subTask.parentTask = task

        dest = Task()
        dest.copyFrom(subTask)
        checkMinimalTask(dest.parentTask, 2, "t1")
        checkMinimalTask(dest.parentTask?.parentTask, 1, "root")
    }

    private fun checkMinimalTask(task: Task?, id: Int, title: String) {
        assertNotNull(task)
        assertEquals(id, task?.id)
        assertEquals(title, task?.title)
        assertNull(task?.description, "Do not copy description for minimal copy.")
        assertNull(task?.responsibleUser, "Do not copy responsible user for minimal copy.")
    }
}
