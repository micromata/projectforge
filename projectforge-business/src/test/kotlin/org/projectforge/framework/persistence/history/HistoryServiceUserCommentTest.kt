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

package org.projectforge.framework.persistence.history

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException

class HistoryServiceUserCommentTest : AbstractTestBase() {
    @Test
    fun appendingACommentNeedsUpdateAccessToACommentableEntity() {
        val group = initTestDB.addGroup("historyCommentTestGroup")
        val groupEntryId = historyService.loadHistory(group).originUnsortedEntries.first().id

        logon(TEST_USER)
        assertThrows<AccessException> { historyService.appendUserComment(groupEntryId, "by an ordinary user") }

        logon(TEST_ADMIN_USER)
        historyService.appendUserComment(groupEntryId, "by an admin")
        val comment = historyService.findEntryAndEntityById(groupEntryId)!!.entry!!.userComment!!
        Assertions.assertTrue(comment.endsWith("by an admin"), comment)

        // A task doesn't support comments, although the admin may update it.
        val task = initTestDB.addTask("historyCommentTestTask", "root")
        val taskEntryId = historyService.loadHistory(task).originUnsortedEntries.first().id
        historyService.findEntryAndEntityById(taskEntryId)!!.let {
            Assertions.assertTrue(it.writeAccess, "admin should have update access to the task")
        }
        assertThrows<AccessException> { historyService.appendUserComment(taskEntryId, "on a task") }
    }
}
