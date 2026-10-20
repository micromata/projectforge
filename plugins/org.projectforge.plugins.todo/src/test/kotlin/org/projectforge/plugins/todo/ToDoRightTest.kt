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
package org.projectforge.plugins.todo

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * On update and delete the stored to-do has to pass as well: a foreign to-do posted with the own user as assignee
 * is refused.
 */
class ToDoRightTest {
    private val right = ToDoRight()

    private val reporter = PFUserDO().also { it.id = 1 }
    private val assignee = PFUserDO().also { it.id = 2 }
    private val otherUser = PFUserDO().also { it.id = 3 }

    private fun storedToDo() = ToDoDO().also {
        it.id = 5
        it.reporter = reporter
        it.assignee = assignee
    }

    @Test
    fun `nobody takes over a foreign to-do by posting the own user as assignee`() {
        val dbObj = storedToDo()
        val posted = storedToDo().also { it.assignee = otherUser }
        assertFalse(right.hasUpdateAccess(otherUser, posted, dbObj))
        assertFalse(right.hasDeleteAccess(otherUser, posted, dbObj))
        assertFalse(right.hasDeleteAccess(otherUser, dbObj, dbObj))
    }

    @Test
    fun `reporter and assignee may update and delete`() {
        val dbObj = storedToDo()
        for (user in listOf(reporter, assignee)) {
            assertTrue(right.hasUpdateAccess(user, storedToDo().also { it.subject = "changed" }, dbObj), "user ${user.id}")
            assertTrue(right.hasDeleteAccess(user, dbObj, dbObj), "user ${user.id}")
        }
        assertTrue(right.hasAccess(reporter, storedToDo().also { it.id = null }, null, OperationType.INSERT))
    }
}
