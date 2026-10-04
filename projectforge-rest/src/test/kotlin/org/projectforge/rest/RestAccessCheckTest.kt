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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Rechnung
import org.projectforge.rest.fibu.OutgoingInvoiceEntityRest
import org.projectforge.rest.task.TaskFavoritesRest
import org.projectforge.rest.task.TaskServicesRest
import org.springframework.beans.factory.annotation.Autowired

/**
 * Access checks added to endpoints of the Next migration that bypassed the DAO: a user without the
 * corresponding right gets an [AccessException] (or, for the recent tasks, the task is silently dropped).
 */
class RestAccessCheckTest : AbstractTestBase() {
    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var taskServicesRest: TaskServicesRest

    @Autowired
    private lateinit var taskFavoritesRest: TaskFavoritesRest

    @Test
    fun `createCancellation needs the invoice insert right`() {
        logon(TEST_USER)
        assertThrows<AccessException> {
            outgoingInvoiceEntityRest.createCancellation(PostData(Rechnung(), null, null))
        }
    }

    @Test
    fun `recent tasks and favorites accept only tasks the user may select`() {
        val (task, user) = persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            Pair(initTestDB.addTask("restAccessCheckTask", "root"), initTestDB.addUser("restAccessCheckUser"))
        }
        val taskId = task.id!!
        logon(TEST_ADMIN_USER)
        assertTrue(taskServicesRest.selectRecent(taskId).any { it.id == taskId })

        // A user without any group has no task access at all.
        logon(user)
        assertFalse(taskServicesRest.selectRecent(taskId).any { it.id == taskId })
        assertThrows<AccessException> { taskFavoritesRest.createPost(taskId, "fav") }
        assertThrows<AccessException> { taskFavoritesRest.new(taskId, "fav") }
        assertThrows<AccessException> { taskFavoritesRest.updatePost(-1L, taskId) }
    }
}
