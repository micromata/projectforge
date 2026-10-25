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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.TimesheetEntityRest
import org.projectforge.rest.core.saveOrUpdate
import org.projectforge.rest.dto.Kost2
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Task
import org.projectforge.rest.dto.Timesheet
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest

/**
 * What a user booking by an ad hoc booking grant sees of the structure tree (the granted task and the titles of its
 * ancestors, nothing else), who manages the grants of a task, and who sees the grants as badge in the tree.
 */
class TaskBookingGrantRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var taskServicesRest: TaskServicesRest

    @Autowired
    private lateinit var taskBookingGrantRest: TaskBookingGrantRest

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var timesheetEntityRest: TimesheetEntityRest

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    private class Fixture(
        val project: TaskDO,
        val granted: TaskDO,
        val sibling: TaskDO,
        val manager: PFUserDO,
        val helper: PFUserDO,
        val other: PFUserDO,
    )

    private fun createFixture(prefix: String): Fixture {
        logon(TEST_ADMIN_USER)
        return persistenceService.runInTransaction { _ ->
            val project = initTestDB.addTask(prefix, "root")
            val granted = initTestDB.addTask("$prefix.granted", prefix)
            val sibling = initTestDB.addTask("$prefix.sibling", prefix)
            val manager = initTestDB.addUser("$prefix-manager")
            val helper = initTestDB.addUser("$prefix-helper")
            val other = initTestDB.addUser("$prefix-other")
            val access = initTestDB.createGroupTaskAccess(initTestDB.addGroup("$prefix-managers", manager.username!!), project)
            AccessType.entries.filter { it != AccessType.GROUP }.forEach { type ->
                access.ensureAndGetAccessEntry(type).setAccess(true, true, true, true)
            }
            access.recursive = true
            accessDao.update(access, checkAccess = false)
            Fixture(project, granted, sibling, manager, helper, other)
        }
    }

    private fun grant(fixture: Fixture, tasks: List<TaskDO> = listOf(fixture.granted)): TaskBookingGrantRest.Result {
        return taskBookingGrantRest.grant(
            TaskBookingGrantRest.GrantRequest(
                taskId = fixture.project.id,
                userIds = listOf(fixture.helper.id!!),
                positions = tasks.map { TaskBookingGrantRest.PositionRequest(it.id) },
                validFrom = PFDay.now().minusDays(1).localDate,
                validUntil = PFDay.now().plusDays(10).localDate,
                comment = "Helping out",
            )
        )
    }

    @Test
    fun `the helper sees the granted task and the titles of its ancestors only`() {
        val fixture = createFixture("bgrest-tree")
        logon(fixture.manager)
        assertTrue(grant(fixture).success)

        logon(fixture.helper)
        val nodes = tree().nodes
        assertEquals(listOf(fixture.project.id, fixture.granted.id), nodes.map { it.id })
        nodes.single { it.id == fixture.project.id }.let {
            assertEquals(TaskServicesRest.BookingGrantNode.PATH, it.bookingGrant)
            assertNull(it.consumption)
            assertEquals(TaskServicesRest.TreeStatus.OPENED, it.treeStatus, "Opened on the way to the grant.")
        }
        nodes.single { it.id == fixture.granted.id }.let {
            assertEquals(TaskServicesRest.BookingGrantNode.GRANT, it.bookingGrant)
            assertNull(it.consumption)
            assertEquals(TaskServicesRest.TreeStatus.LEAF, it.treeStatus)
        }

        // The info: reduced for the granted task and its ancestors, nothing of the sibling.
        info(fixture.granted).let {
            assertEquals(HttpStatus.OK, it.statusCode)
            assertEquals(TaskServicesRest.BookingGrantNode.GRANT, it.body!!.bookingGrant)
            assertNull(it.body!!.consumption)
            assertNull(it.body!!.projekt)
            assertEquals(listOf(fixture.project.id), it.body!!.path?.map { node -> node.id })
        }
        assertEquals(TaskServicesRest.BookingGrantNode.PATH, info(fixture.project).body!!.bookingGrant)
        assertEquals(HttpStatus.NOT_FOUND, info(fixture.sibling).statusCode)

        // The type-ahead of the time sheet form offers the granted task.
        val found = taskServicesRest.autosearch(search = null, maxResults = 10, onlyBookable = true)
        assertEquals(listOf(fixture.granted.id), found.map { it.id })

        // Somebody else sees nothing of it.
        logon(fixture.other)
        assertTrue(tree().nodes.none { it.id == fixture.project.id || it.id == fixture.granted.id })
        assertEquals(HttpStatus.NOT_FOUND, info(fixture.granted).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, info(fixture.project).statusCode)

        // The manager sees the whole task info, as before.
        logon(fixture.manager)
        info(fixture.granted).let {
            assertEquals(HttpStatus.OK, it.statusCode)
            assertNull(it.body!!.bookingGrant)
            // Only the full info carries the references (createTask).
            assertNotNull(it.body!!.timesheetReferenceList)
        }
    }

    @Test
    fun `grants are managed by who may edit foreign time sheets on the task only`() {
        val fixture = createFixture("bgrest-manage")
        // Refused for an ordinary user (TEST_USER) and for the helper: no grants sent, none given.
        for (user in listOf(getUser(TEST_USER), fixture.helper)) {
            logon(user)
            taskBookingGrantRest.getInfo(fixture.granted.id!!).let {
                assertFalse(it.mayGrant)
                assertNull(it.grants)
            }
            assertFalse(grant(fixture).success)
        }

        logon(fixture.manager)
        // The project offers itself and its work packages to grant on.
        taskBookingGrantRest.getInfo(fixture.project.id!!).let {
            assertTrue(it.mayGrant)
            assertEquals(
                setOf(fixture.project.id, fixture.granted.id, fixture.sibling.id),
                it.tasks!!.map { task -> task.taskId }.toSet(),
            )
        }
        val result = grant(fixture, tasks = listOf(fixture.granted, fixture.sibling))
        assertTrue(result.success, result.message)
        val entry = result.info!!.grants!!.single()
        assertEquals(TaskBookingGrantRest.Status.ACTIVE, entry.status)
        assertEquals(fixture.helper.id, entry.userId)
        assertTrue(entry.revocable)
        // Who granted comes from the history of the insert.
        assertEquals(fixture.manager.getFullname(), entry.grantedByName)
        assertEquals(
            listOf("bgrest-manage | bgrest-manage.granted", "bgrest-manage | bgrest-manage.sibling"),
            entry.positions.map { it.path }.sorted(),
        )
        // Listed on each of its tasks as well.
        assertEquals(entry.id, taskBookingGrantRest.getInfo(fixture.sibling.id!!).grants!!.single().id)

        // The helper may not revoke it, the manager may.
        logon(fixture.helper)
        assertFalse(taskBookingGrantRest.revoke(entry.id, fixture.project.id!!).success)
        logon(fixture.manager)
        taskBookingGrantRest.revoke(entry.id, fixture.project.id!!).let {
            assertTrue(it.success)
            assertEquals(TaskBookingGrantRest.Status.REVOKED, it.info!!.grants!!.single().status)
        }
        // Revoked: the tree shows the helper nothing any more.
        logon(fixture.helper)
        assertTrue(tree().nodes.none { it.id == fixture.project.id })
    }

    @Test
    fun `the badge of the grants is shown to who may grant only`() {
        val fixture = createFixture("bgrest-badge")
        logon(fixture.manager)
        assertTrue(grant(fixture).success)
        val nodes = tree(highlighted = fixture.granted).nodes
        nodes.single { it.id == fixture.granted.id }.let {
            assertEquals(1, it.bookingGrants?.size)
            assertFalse(it.bookingGrants!!.single().upcoming)
        }
        assertNull(nodes.single { it.id == fixture.sibling.id }.bookingGrants)
        // Not in the select popover.
        tree(select = true, highlighted = fixture.granted).nodes.find { it.id == fixture.granted.id }?.let { assertNull(it.bookingGrants) }

        // Neither the helper nor an ordinary user (who may see the task) get it.
        logon(fixture.helper)
        assertNull(tree().nodes.single { it.id == fixture.granted.id }.bookingGrants)
        logon(getUser(TEST_USER))
        tree(highlighted = fixture.granted).nodes.find { it.id == fixture.granted.id }?.let { assertNull(it.bookingGrants) }
    }

    @Test
    fun `an employee proposes, sees the own proposals only, and the manager sees the sheets to accept`() {
        val fixture = createFixture("bgrest-propose")
        val employee = persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            val employee = initTestDB.addUser("bgrest-propose-employee")
            val group = initTestDB.addGroup("bgrest-propose-employees", employee.username!!)
            val access = initTestDB.createGroupTaskAccess(group, fixture.project)
            access.ensureAndGetAccessEntry(AccessType.TASKS).setAccess(true, false, false, false)
            access.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS).setAccess(true, true, true, true)
            access.recursive = true
            accessDao.update(access, checkAccess = false)
            employee
        }
        // A grant of the manager, which the employee doesn't see.
        logon(fixture.manager)
        assertTrue(grant(fixture, tasks = listOf(fixture.sibling)).success)

        logon(employee)
        taskBookingGrantRest.getInfo(fixture.project.id!!).let {
            assertFalse(it.mayGrant)
            assertTrue(it.mayPropose)
            assertTrue(it.grants!!.isEmpty())
        }
        val result = grant(fixture)
        assertTrue(result.success, result.message)
        val entry = result.info!!.grants!!.single()
        assertTrue(entry.acceptanceRequired)
        assertTrue(entry.revocable)
        assertFalse(entry.confirmable)

        // The helper books, the manager sees the sheet to accept: in the info and as badge in the tree.
        logon(fixture.helper)
        val start = PFDateTime.now().minusDays(1).beginOfDay.withHour(8)
        timesheetDao.insert(TimesheetDO().also {
            it.user = fixture.helper
            it.task = fixture.granted
            it.startTime = start.utilDate
            it.stopTime = start.plusHours(1).utilDate
            it.description = "helping"
        })
        logon(fixture.manager)
        taskBookingGrantRest.getInfo(fixture.project.id!!).grants!!.single { it.id == entry.id }.let {
            assertEquals(1, it.pendingTimesheets)
            assertTrue(it.confirmable)
        }
        assertEquals(1, tree(highlighted = fixture.granted).nodes.single { it.id == fixture.granted.id }.pendingTimesheets)
        // Not for the employee (who may not grant).
        logon(employee)
        assertNull(tree(highlighted = fixture.granted).nodes.single { it.id == fixture.granted.id }.pendingTimesheets)
    }

    @Test
    fun `a revoked proposal isn't confirmed, a confirmed one isn't revoked by the proposer, pending of the subtree`() {
        val fixture = createFixture("bgrest-confirm")
        val employee = persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            val employee = initTestDB.addUser("bgrest-confirm-employee")
            val access = initTestDB.createGroupTaskAccess(initTestDB.addGroup("bgrest-confirm-employees", employee.username!!), fixture.project)
            access.ensureAndGetAccessEntry(AccessType.TASKS).setAccess(true, false, false, false)
            access.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS).setAccess(true, true, true, true)
            access.recursive = true
            accessDao.update(access, checkAccess = false)
            employee
        }
        logon(employee)
        val proposal = grant(fixture, tasks = listOf(fixture.granted, fixture.sibling)).info!!.grants!!.single()
        // A sheet on the granted task: counted for the project and the granted task, not for the sibling.
        logon(fixture.helper)
        val start = PFDateTime.now().minusDays(1).beginOfDay.withHour(8)
        timesheetDao.insert(TimesheetDO().also {
            it.user = fixture.helper
            it.task = fixture.granted
            it.startTime = start.utilDate
            it.stopTime = start.plusHours(1).utilDate
            it.description = "helping"
        })
        logon(fixture.manager)
        assertEquals(1, taskBookingGrantRest.getInfo(fixture.project.id!!).grants!!.single().pendingTimesheets)
        assertEquals(0, taskBookingGrantRest.getInfo(fixture.sibling.id!!).grants!!.single().pendingTimesheets)

        // Confirmed: the proposer may revoke it no more.
        assertTrue(taskBookingGrantRest.confirm(proposal.id, fixture.project.id!!).success)
        assertFalse(taskBookingGrantRest.confirm(proposal.id, fixture.project.id!!).success, "Confirmed already.")
        logon(employee)
        assertFalse(taskBookingGrantRest.getInfo(fixture.project.id!!).grants!!.single().revocable)
        assertFalse(taskBookingGrantRest.revoke(proposal.id, fixture.project.id!!).success)

        // Revoked: not to be confirmed.
        val second = grant(fixture).info!!.grants!!.single { it.id != proposal.id }
        assertTrue(taskBookingGrantRest.revoke(second.id, fixture.project.id!!).success)
        logon(fixture.manager)
        taskBookingGrantRest.confirm(second.id, fixture.project.id!!).let {
            assertFalse(it.success)
            assertNotNull(it.message)
        }
    }

    @Test
    fun `grants of a part of the subtree without the right to grant are not listed`() {
        val fixture = createFixture("bgrest-part")
        // The manager edits no time sheets of others on the sibling: its own access overrides the project's.
        persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            initTestDB.createGroupTaskAccess(
                initTestDB.getGroup("bgrest-part-managers"), fixture.sibling, AccessType.TIMESHEETS,
                accessSelect = true, accessInsert = false, accessUpdate = false, accessDelete = false,
            )
        }
        logon(getUser(TEST_FINANCE_USER))
        assertTrue(grant(fixture, tasks = listOf(fixture.sibling)).success)
        logon(fixture.manager)
        assertTrue(grant(fixture).success)
        val grants = taskBookingGrantRest.getInfo(fixture.project.id!!).grants!!
        assertEquals(listOf(listOf(fixture.granted.id)), grants.map { entry -> entry.positions.map { it.taskId } })
        // The finance group sees both.
        logon(getUser(TEST_FINANCE_USER))
        assertEquals(2, taskBookingGrantRest.getInfo(fixture.project.id!!).grants!!.size)
    }

    @Test
    fun `the helper books through the time sheet form on a cost unit of the grant`() {
        val fixture = createFixture("bgrest-kost2")
        val kost2s = persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            val projekt = ProjektDO().also {
                it.name = "bgrest-kost2"
                it.nummer = 78
                it.task = fixture.project
            }
            projektDao.insert(projekt, checkAccess = false)
            val nummer = projekt.projektnummer!!
            (94L..96L).map { artId ->
                val art = kost2ArtDao.find(artId, checkAccess = false) ?: Kost2ArtDO().also {
                    it.id = artId
                    it.name = "bgrest-kost2-$artId"
                    kost2ArtDao.insert(it, checkAccess = false)
                }
                Kost2DO().also {
                    it.nummernkreis = nummer / 100_000
                    it.bereich = nummer % 100_000 / 100
                    it.teilbereich = nummer % 100
                    it.kost2Art = art
                    it.projekt = projekt
                    kost2Dao.insert(it, checkAccess = false)
                }
            }
        }
        logon(fixture.manager)
        val result = taskBookingGrantRest.grant(
            TaskBookingGrantRest.GrantRequest(
                taskId = fixture.project.id,
                userIds = listOf(fixture.helper.id!!),
                positions = listOf(TaskBookingGrantRest.PositionRequest(fixture.granted.id, listOf(kost2s[0].id!!, kost2s[1].id!!))),
                validFrom = PFDay.now().minusDays(1).localDate,
                validUntil = PFDay.now().plusDays(10).localDate,
            )
        )
        assertTrue(result.success, result.message)

        logon(fixture.helper)
        // The form offers the cost units of the grant only.
        assertEquals(
            setOf(kost2s[0].id, kost2s[1].id),
            info(fixture.granted).body!!.kost2List!!.map { it.id }.toSet(),
        )
        // Posted as the form does: references by id.
        val start = PFDateTime.now().minusDays(1).beginOfDay.withHour(8)
        val dto = Timesheet(
            task = Task(id = fixture.granted.id),
            user = User().also { it.id = fixture.helper.id },
            kost2 = Kost2(id = kost2s[1].id),
            startTime = start.utilDate,
            stopTime = start.plusHours(1).utilDate,
            description = "helping",
        )
        fun insert(dto: Timesheet): org.springframework.http.ResponseEntity<org.projectforge.ui.ResponseAction> {
            val postData = PostData(dto, watchFieldsTriggered = null, serverData = null)
            val obj = timesheetEntityRest.transformForDB(dto)
            return saveOrUpdate(
                MockHttpServletRequest(), timesheetDao, obj, postData, timesheetEntityRest,
                timesheetEntityRest.validate(obj, postData), OperationType.INSERT,
            )
        }
        // Without a cost unit: asked for at the field, not refused as access violation.
        insert(Timesheet(task = dto.task, user = dto.user, startTime = dto.startTime, stopTime = dto.stopTime)).let {
            assertEquals(HttpStatus.NOT_ACCEPTABLE, it.statusCode)
            assertEquals(listOf("kost2"), it.body?.validationErrors?.map { error -> error.fieldId })
        }
        insert(dto).let {
            assertEquals(HttpStatus.OK, it.statusCode, "${it.body?.validationErrors?.map { error -> error.message }}")
        }
    }

    private fun info(task: TaskDO) = taskServicesRest.getTaskInfo(task.id)

    /** @param highlighted Opens the ancestors of this task. */
    private fun tree(select: Boolean? = null, highlighted: TaskDO? = null) = taskServicesRest.getTree(
        MockHttpServletRequest(), initial = true, open = null, close = null, highlightedTaskId = highlighted?.id,
        table = true, searchString = null, opened = null, notOpened = null, closed = null, deleted = null,
        showRootForAdmins = null, select = select, rootTaskId = null,
    )
}
