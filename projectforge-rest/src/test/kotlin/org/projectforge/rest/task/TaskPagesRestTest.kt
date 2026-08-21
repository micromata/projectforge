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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.framework.access.AccessType
import org.projectforge.rest.core.ValidationUtils
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
        // The writes only: the flags resolve the project through ProjektCache, which refreshes on a second
        // connection and would deadlock against the still uncommitted writer.
        val task = persistenceService.runInTransaction { _ ->
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
            task
        }

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
    }

    /**
     * Filling the flags costs a project resolution and a group lookup, so it is done for the edit page only -
     * transformFromDB runs per list row as well.
     */
    @Test
    fun `a list row carries no flags`() {
        val task = persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            initTestDB.addTask("taskPagesRestListRow", "root")
        }
        logon(TEST_FINANCE_USER)

        val dto = taskPagesRest.transformFromDB(task, editMode = false)
        assertFalse(dto.kost2AndBookingStatusWriteAccess)
        assertFalse(dto.protectTimesheetsUntilWriteAccess)
    }

    /**
     * The lean row of the hand built next list: the ten columns of `task.page.tsx` and the two audit ones,
     * and nothing the edit form needs (see `Task.copyFrom4ListRow`).
     *
     * What it must *not* carry is the point of the override — every field left out is one Spring omits from
     * the wire per row (`JsonInclude.Include.NON_NULL`).
     */
    @Test
    fun `the list row carries the columns of the list and nothing else`() {
        val (parent, task) = persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val parent = initTestDB.addTask("taskListRowParent", "root")
            val task = initTestDB.addTask("taskListRow", "taskListRowParent", "A short description")
            task.reference = "the reference"
            task.priority = Priority.HIGH
            task.description = "The long description no column of the list shows."
            // A budget, so the bar has something to fill: `Consumption.create` answers null for a task with
            // neither planned nor booked effort - there is no consumption to show then, in either perspective.
            task.maxHours = 8
            task.kost2BlackWhiteList = "5.123.45.11"
            task.responsibleUser = getUser(TEST_FINANCE_USER)
            taskDao.update(task, checkAccess = false)
            parent to task
        }
        logon(TEST_FINANCE_USER)

        val row = Task()
        row.copyFrom4ListRow(taskDao.find(task.id, checkAccess = false)!!)

        assertEquals(task.id, row.id)
        assertEquals("taskListRow", row.title)
        assertEquals("A short description", row.shortDescription)
        assertEquals("the reference", row.reference)
        assertEquals(Priority.HIGH, row.priority)
        assertEquals(TaskStatus.N, row.status)
        assertEquals(getUser(TEST_FINANCE_USER).displayName, row.responsibleUser?.displayName)
        // The two columns every next list offers, hidden until the user switches them on.
        assertNotNull(row.created, "The created column is offered by every list.")
        assertNotNull(row.lastUpdate, "The lastUpdate column is offered by every list.")
        // Computed from the task tree, which holds the task: a row without it would show three empty
        // columns for a task the list does show.
        assertNotNull(row.consumption, "The consumption bar is one of the ten columns.")

        assertNull(row.description, "Not a column of the list — it would be sent per row for nothing.")
        assertNull(row.kost2BlackWhiteList, "Not a column of the list.")
        assertNull(row.parentTask, "No column reads the parent; the path is the tree perspective's.")
        assertFalse(row.kost2AndBookingStatusWriteAccess, "The access flags are the edit page's.")
        assertFalse(row.protectTimesheetsUntilWriteAccess)
        assertNotNull(parent.id) // The parent exists, so `parentTask` being null is the override's doing.
    }

    /**
     * The one rule Wicket enforces in its form and the backend didn't, so a save through the rest api
     * slipped past it: scheduled by duration or by end date, never by both - `TaskEditForm`'s only
     * `IFormValidator`.
     *
     * The numeric ranges are *not* here any more: they are `@PropertyInfo(min/max)` on the `TaskDO` and are
     * enforced for every entity by `ValidationUtils.validateFields` (see the range test below), so this
     * override may not restate them.
     */
    @Test
    fun `validate refuses what the Wicket form refuses`() {
        logon(TEST_FINANCE_USER)
        assertNoError(Task(duration = BigDecimal.ONE))
        assertNoError(Task(endDate = LocalDate.of(2026, 3, 4)))
        assertError(
            Task(duration = BigDecimal.ONE, endDate = LocalDate.of(2026, 3, 4)),
            "gantt.error.durationAndEndDateAreMutuallyExclusive",
            "endDate",
        )
    }

    /**
     * The numeric bounds of the three Gantt fields, at the boundaries because that is where an off-by-one
     * shows.
     *
     * Against the `TaskDO`, not the DTO: the bounds are declared there (`@PropertyInfo(min/max)`) and
     * `AbstractEntityRest.validate` runs `ValidationUtils.validateFields` over the *DO* it is about to
     * save. The same three numbers the next schema derives from the generated metadata, so neither side
     * writes them out a second time.
     */
    @Test
    fun `the ranges of the entity are enforced for every frontend`() {
        logon(TEST_FINANCE_USER)
        assertInRange(progress = 0, maxHours = 0, duration = BigDecimal.ZERO)
        assertInRange(progress = 100, maxHours = 9999, duration = BigDecimal(10000))
        assertOutOfRange("progress", progress = 101)
        assertOutOfRange("progress", progress = -1)
        assertOutOfRange("maxHours", maxHours = 10000)
        assertOutOfRange("maxHours", maxHours = -1)
        assertOutOfRange("duration", duration = BigDecimal(10001))
        assertOutOfRange("duration", duration = BigDecimal(-1))
    }

    private fun rangeErrors(
        progress: Int? = null,
        maxHours: Int? = null,
        duration: BigDecimal? = null,
    ): List<ValidationError> {
        val task = TaskDO()
        task.progress = progress
        task.maxHours = maxHours
        task.duration = duration
        // Only the range rule: a bare TaskDO is missing its title as well, and that is not what is under test.
        return ValidationUtils.validateFields(task)
            .filter { it.messageId == "validation.error.range.integerOutOfRange" }
    }

    private fun assertInRange(progress: Int? = null, maxHours: Int? = null, duration: BigDecimal? = null) {
        val errors = rangeErrors(progress, maxHours, duration)
        assertTrue(errors.isEmpty(), "Expected no range error, but got: $errors")
    }

    private fun assertOutOfRange(
        fieldId: String,
        progress: Int? = null,
        maxHours: Int? = null,
        duration: BigDecimal? = null,
    ) {
        val errors = rangeErrors(progress, maxHours, duration)
        assertEquals(1, errors.size, "Expected exactly one range error, but got: $errors")
        assertEquals(fieldId, errors[0].fieldId, "The message must land at its field, not in the general area.")
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
