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

package org.projectforge.rest.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class TaskFavoritesRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var taskFavoritesRest: TaskFavoritesRest

    @Test
    fun `update points a favorite at another task and keeps its name`() {
        val (taskA, taskB) = persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            Pair(initTestDB.addTask("taskFavoritesA", "root"), initTestDB.addTask("taskFavoritesB", "root"))
        }
        logon(TEST_ADMIN_USER)
        val created = taskFavoritesRest.createPost(taskA.id!!, "fav").single { it.name == "fav" }
        assertEquals(taskA.id, created.taskId)

        val updated = taskFavoritesRest.updatePost(created.id!!, taskB.id!!).single { it.id == created.id }
        assertEquals("fav", updated.name)
        assertEquals(taskB.id, updated.taskId)
        assertEquals(taskB.id, taskFavoritesRest.selectPost(created.id))
    }
}
