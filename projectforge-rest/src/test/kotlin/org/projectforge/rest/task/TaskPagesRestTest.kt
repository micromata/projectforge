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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessType
import org.projectforge.rest.dto.Task
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The per-field access flags of the task DTO, which decide what the hand built next form offers. The DAO
 * stays the authority - what is under test is only whether the flags say the same as the DAO would answer
 * on write (see TaskTest.checkKost2AndTimesheetBookingStatusAccess for the refusals themselves).
 *
 * `writeAccess`/`deleteAccess` are not tested here: they are not this class's, they are filled for every
 * entity by `AbstractEntityRest.getById` (see `EntityAccessSupport`).
 */
class TaskPagesRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var taskPagesRest: TaskPagesRest

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Test
    fun `the flags mirror what the DAO would accept`() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val task = initTestDB.addTask("taskPagesRestAccess", "root")
            val projectManagers = initTestDB.addGroup(
                "taskPagesRestAccessGroup",
                TEST_PROJECT_MANAGER_USER,
                TEST_PROJECT_ASSISTANT_USER,
            )
            initTestDB.createGroupTaskAccess(projectManagers, task, AccessType.TASKS, true, true, true, true)
            val projekt = ProjektDO()
            projekt.name = "taskPagesRestAccess"
            projekt.internKost2_4 = 765
            projekt.nummer = 2
            projekt.projektManagerGroup = projectManagers
            projekt.task = task
            projektDao.insert(projekt)

            // The finance user may change everything.
            logon(TEST_FINANCE_USER)
            transformFromDB(task).let {
                assertTrue(it.kost2AndBookingStatusWriteAccess)
                assertTrue(it.protectTimesheetsUntilWriteAccess)
            }

            // The project assistant of this project may change the kost2 fields, but not the protection.
            logon(TEST_PROJECT_ASSISTANT_USER)
            transformFromDB(task).let {
                assertTrue(it.kost2AndBookingStatusWriteAccess)
                assertFalse(it.protectTimesheetsUntilWriteAccess)
            }

            // A plain user is neither.
            logon(TEST_USER)
            transformFromDB(task).let {
                assertFalse(it.kost2AndBookingStatusWriteAccess)
                assertFalse(it.protectTimesheetsUntilWriteAccess)
            }

            // A new task below the same parent: the rights are the parent's, so the assistant must see the
            // kost2 fields open. hasAccessForKost2AndTimesheetBookingStatus resolves them through
            // parentTaskId, which newBaseDO fills from the request.
            logon(TEST_PROJECT_ASSISTANT_USER)
            val newTask = TaskDO()
            taskDao.setParentTask(newTask, task.id!!)
            transformFromDB(newTask).let {
                assertTrue(it.kost2AndBookingStatusWriteAccess, "The rights of a new task are the parent's.")
                assertFalse(it.protectTimesheetsUntilWriteAccess)
            }
            // Without the parent nothing can be resolved, so the fields stay closed.
            transformFromDB(TaskDO()).let {
                assertFalse(it.kost2AndBookingStatusWriteAccess)
            }
            null
        }
    }

    /**
     * Filling the flags costs a project resolution and a group lookup, so it is done for the edit page only -
     * transformFromDB runs per list row as well.
     */
    @Test
    fun `a list row carries no flags`() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val task = initTestDB.addTask("taskPagesRestListRow", "root")
            val dto = taskPagesRest.transformFromDB(task, editMode = false)
            assertFalse(dto.kost2AndBookingStatusWriteAccess)
            assertFalse(dto.protectTimesheetsUntilWriteAccess)
            null
        }
    }

    /**
     * The rules Wicket enforces in its form and the backend didn't, so a save through the rest api slipped
     * past them. At the boundaries, because that is where an off-by-one shows.
     */
    @Test
    fun `validate refuses what the Wicket form refuses`() {
        logon(TEST_FINANCE_USER)
        assertNoError(Task(progress = 0, maxHours = 0, duration = BigDecimal.ZERO))
        assertNoError(Task(progress = 100, maxHours = 9999, duration = BigDecimal(10000)))
        // Scheduled by duration or by end date, never by both - TaskEditForm's only IFormValidator.
        assertNoError(Task(duration = BigDecimal.ONE))
        assertNoError(Task(endDate = LocalDate.of(2026, 3, 4)))
        assertError(
            Task(duration = BigDecimal.ONE, endDate = LocalDate.of(2026, 3, 4)),
            "gantt.error.durationAndEndDateAreMutuallyExclusive",
            "endDate",
        )
        val outOfRange = "validation.error.range.integerOutOfRange"
        assertError(Task(progress = 101), outOfRange, "progress")
        assertError(Task(progress = -1), outOfRange, "progress")
        assertError(Task(maxHours = 10000), outOfRange, "maxHours")
        assertError(Task(maxHours = -1), outOfRange, "maxHours")
        assertError(Task(duration = BigDecimal(10001)), outOfRange, "duration")
        assertError(Task(duration = BigDecimal(-1)), outOfRange, "duration")
    }

    private fun assertNoError(dto: Task) {
        val errors = mutableListOf<ValidationError>()
        taskPagesRest.validate(errors, dto)
        assertTrue(errors.isEmpty(), "Expected no validation error, but got: $errors")
    }

    private fun assertError(dto: Task, messageId: String, fieldId: String) {
        val errors = mutableListOf<ValidationError>()
        taskPagesRest.validate(errors, dto)
        assertEquals(1, errors.size, "Expected exactly one validation error, but got: $errors")
        assertEquals(messageId, errors[0].messageId)
        assertEquals(fieldId, errors[0].fieldId, "The message must land at its field, not in the general area.")
    }

    private fun transformFromDB(task: TaskDO) = taskPagesRest.transformFromDB(task, editMode = true)
}
