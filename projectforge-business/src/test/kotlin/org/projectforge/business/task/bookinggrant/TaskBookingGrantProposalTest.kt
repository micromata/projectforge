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

package org.projectforge.business.task.bookinggrant

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.notification.TimesheetsToAcceptRuleHandler
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.common.i18n.UserException
import org.projectforge.common.task.TaskStatus
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired

/**
 * Ad hoc booking grants proposed by an employee without the right to grant (who books on the tasks by the groups):
 * the helper books right away, the time sheets are to be accepted by the project management, maybe moved.
 */
class TaskBookingGrantProposalTest : AbstractTestBase() {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var taskBookingGrantDao: TaskBookingGrantDao

    @Autowired
    private lateinit var taskBookingGrantService: TaskBookingGrantService

    @Autowired
    private lateinit var taskBookingGrantCache: TaskBookingGrantCache

    @Autowired
    private lateinit var timesheetsToAcceptRuleHandler: TimesheetsToAcceptRuleHandler

    private class Fixture(
        val project: TaskDO,
        val granted: TaskDO,
        val sibling: TaskDO,
        /** Not below the project: the employee books nothing there. */
        val foreign: TaskDO,
        val manager: PFUserDO,
        /** Books own time sheets on the project by the groups, no right to grant. */
        val employee: PFUserDO,
        val helper: PFUserDO,
        val former: PFUserDO,
    )

    private fun createFixture(prefix: String, withProject: Boolean = false): Fixture {
        logon(TEST_ADMIN_USER)
        return persistenceService.runInTransaction { _ ->
            val project = initTestDB.addTask(prefix, "root")
            val granted = initTestDB.addTask("$prefix.granted", prefix)
            val sibling = initTestDB.addTask("$prefix.sibling", prefix)
            val foreign = initTestDB.addTask("$prefix-foreign", "root")
            val manager = initTestDB.addUser("$prefix-manager")
            val employee = initTestDB.addUser("$prefix-employee")
            val helper = initTestDB.addUser("$prefix-helper")
            // A former manager, deactivated: still a member of the group.
            val former = initTestDB.addUser(PFUserDO().also {
                it.username = "$prefix-former"
                it.email = "devnull@localhost"
                it.deactivated = true
            })
            val managers = initTestDB.addGroup("$prefix-managers", manager.username!!, former.username!!)
            initTestDB.createGroupTaskAccess(managers, project).let { access ->
                AccessType.entries.filter { it != AccessType.GROUP }.forEach { type ->
                    access.ensureAndGetAccessEntry(type).setAccess(true, true, true, true)
                }
                access.recursive = true
                accessDao.update(access, checkAccess = false)
            }
            val employees = initTestDB.addGroup("$prefix-employees", employee.username!!)
            initTestDB.createGroupTaskAccess(employees, project).let { access ->
                access.ensureAndGetAccessEntry(AccessType.TASKS).setAccess(true, false, false, false)
                access.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS).setAccess(true, true, true, true)
                access.recursive = true
                accessDao.update(access, checkAccess = false)
            }
            if (withProject) {
                val projekt = ProjektDO().also {
                    it.nummer = (prefix.hashCode() and 0x7fff) % 1000
                    it.name = prefix
                    it.task = project
                    it.projektManagerGroup = managers
                }
                projektDao.insert(projekt, checkAccess = false)
            }
            Fixture(project, granted, sibling, foreign, manager, employee, helper, former)
        }
    }

    private fun propose(
        fixture: Fixture,
        tasks: List<TaskDO> = listOf(fixture.granted),
        validFrom: PFDay = PFDay.now().minusDays(5),
        validUntil: PFDay = PFDay.now().plusDays(5),
        by: PFUserDO = fixture.employee,
        user: PFUserDO = fixture.helper,
    ): TaskBookingGrantDO {
        logon(by)
        return taskBookingGrantService.grant(
            userIds = listOf(user.id!!),
            positions = tasks.map { TaskBookingGrantService.Position(it.id!!) },
            validFrom = validFrom.localDate,
            validUntil = validUntil.localDate,
            comment = null,
        ).single()
    }

    /** Booked by the helper (logged in), the given number of days ago, 8:00 - 9:00. */
    private fun book(fixture: Fixture, task: TaskDO, daysAgo: Long = 1, hour: Int = 8): TimesheetDO {
        logon(fixture.helper)
        val start = PFDateTime.now().minusDays(daysAgo).beginOfDay.withHour(hour)
        val sheet = TimesheetDO().also {
            it.user = fixture.helper
            it.task = task
            it.startTime = start.utilDate
            it.stopTime = start.plusHours(1).utilDate
            it.description = "helping"
        }
        timesheetDao.insert(sheet)
        return timesheetDao.find(sheet.id, checkAccess = false)!!
    }

    @Test
    fun `an employee proposes on own tasks only, the helper books, the sheet is to be accepted`() {
        val fixture = createFixture("bgp-book")
        // Refused for an ordinary user (TEST_USER), on a task the employee doesn't book on and for a long period.
        assertThrows<AccessException> { propose(fixture, by = getUser(TEST_USER)) }
        assertThrows<AccessException> { propose(fixture, tasks = listOf(fixture.granted, fixture.foreign)) }
        assertThrows<UserException> { propose(fixture, validUntil = PFDay.now().plusDays(40)) }
        // Not chained either: 21 days, but ending later than 31 days from today.
        assertThrows<UserException> {
            propose(fixture, validFrom = PFDay.now().plusDays(20), validUntil = PFDay.now().plusDays(40))
        }.let { Assertions.assertEquals(TaskBookingGrantDao.ERROR_PROPOSAL_TOO_FAR_AHEAD, it.i18nKey) }

        val grant = propose(fixture)
        Assertions.assertTrue(grant.acceptanceRequired)
        val sheet = book(fixture, fixture.granted)
        Assertions.assertEquals(grant.id, sheet.bookingGrant?.id)
        Assertions.assertTrue(sheet.acceptancePending)
        Assertions.assertEquals(1, taskBookingGrantCache.getPendingTimesheetCount(fixture.granted.id))

        // The helper passes nothing on: no rights by the groups.
        assertThrows<AccessException> { propose(fixture, by = fixture.helper, user = getUser(TEST_USER)) }

        // A normal save (of the helper, of the manager) doesn't accept.
        logon(fixture.helper)
        sheet.description = "changed"
        sheet.acceptancePending = false
        timesheetDao.update(sheet)
        Assertions.assertTrue(timesheetDao.find(sheet.id, checkAccess = false)!!.acceptancePending)
        logon(fixture.manager)
        timesheetDao.update(timesheetDao.find(sheet.id)!!.also { it.description = "by manager" })
        Assertions.assertTrue(timesheetDao.find(sheet.id, checkAccess = false)!!.acceptancePending)

        // Booked by a grant of the project management: refers to it, but nothing to accept.
        logon(fixture.manager)
        val byManager = taskBookingGrantService.grant(
            userIds = listOf(fixture.helper.id!!),
            positions = listOf(TaskBookingGrantService.Position(fixture.sibling.id!!)),
            validFrom = PFDay.now().minusDays(5).localDate,
            validUntil = PFDay.now().plusDays(100).localDate,
            comment = null,
        ).single()
        Assertions.assertFalse(byManager.acceptanceRequired)
        val other = book(fixture, fixture.sibling, hour = 10)
        Assertions.assertEquals(byManager.id, other.bookingGrant?.id)
        Assertions.assertFalse(other.acceptancePending)
    }

    @Test
    fun `the proposer reads and revokes the own proposal only`() {
        val fixture = createFixture("bgp-own")
        val grant = propose(fixture)
        logon(fixture.employee)
        Assertions.assertNotNull(taskBookingGrantDao.find(grant.id))
        logon(getUser(TEST_USER))
        assertThrows<AccessException> { taskBookingGrantDao.find(grant.id) }
        // Neither confirmed by the proposer.
        logon(fixture.employee)
        assertThrows<AccessException> { taskBookingGrantService.confirm(grant.id!!) }
        taskBookingGrantDao.markAsDeleted(taskBookingGrantDao.selectWithPositions(grant.id!!)!!)
        Assertions.assertTrue(taskBookingGrantDao.find(grant.id, checkAccess = false)!!.deleted)
    }

    @Test
    fun `the manager accepts, maybe moved, the helper still reads the own sheet`() {
        val fixture = createFixture("bgp-accept")
        propose(fixture)
        val sheet = book(fixture, fixture.granted)
        // Refused for the proposer, the helper and an ordinary user.
        for (user in listOf(fixture.employee, fixture.helper, getUser(TEST_USER))) {
            logon(user)
            assertThrows<AccessException> {
                timesheetDao.accept(timesheetDao.find(sheet.id, checkAccess = false)!!)
            }
        }
        Assertions.assertTrue(timesheetDao.find(sheet.id, checkAccess = false)!!.acceptancePending)

        logon(fixture.manager)
        timesheetDao.accept(timesheetDao.find(sheet.id)!!.also { it.task = fixture.sibling })
        val accepted = timesheetDao.find(sheet.id, checkAccess = false)!!
        Assertions.assertFalse(accepted.acceptancePending)
        Assertions.assertEquals(fixture.sibling.id, accepted.taskId)
        Assertions.assertEquals(0, taskBookingGrantCache.getPendingTimesheetCount(fixture.granted.id))

        // Moved: the helper reads it, but changes nothing on the task outside the grant.
        Assertions.assertTrue(timesheetDao.hasUserSelectAccess(fixture.helper, accepted, false))
        Assertions.assertNotEquals(TimesheetDao.HIDDEN_FIELD_MARKER, accepted.description)
        val changed = timesheetDao.find(sheet.id, checkAccess = false)!!.also { it.description = "late change" }
        Assertions.assertFalse(timesheetDao.hasUpdateAccess(fixture.helper, changed, accepted, false))
    }

    @Test
    fun `a sheet booked anew by a proposal is to be accepted again`() {
        val fixture = createFixture("bgp-again")
        propose(fixture)
        val sheet = book(fixture, fixture.granted)
        logon(fixture.manager)
        timesheetDao.accept(timesheetDao.find(sheet.id)!!)

        // A changed description keeps it accepted, a changed time doesn't.
        logon(fixture.helper)
        timesheetDao.update(timesheetDao.find(sheet.id)!!.also { it.description = "typo" })
        Assertions.assertFalse(timesheetDao.find(sheet.id, checkAccess = false)!!.acceptancePending)
        timesheetDao.update(timesheetDao.find(sheet.id)!!.also {
            it.stopTime = PFDateTime.from(it.stopTime!!).plusHours(5).utilDate
        })
        Assertions.assertTrue(timesheetDao.find(sheet.id, checkAccess = false)!!.acceptancePending)

        // Booked by a grant of the project management and moved onto the task of the proposal: to be accepted.
        logon(fixture.manager)
        val byManager = taskBookingGrantService.grant(
            userIds = listOf(fixture.helper.id!!),
            positions = listOf(TaskBookingGrantService.Position(fixture.sibling.id!!)),
            validFrom = PFDay.now().minusDays(5).localDate,
            validUntil = PFDay.now().plusDays(10).localDate,
            comment = null,
        ).single()
        val other = book(fixture, fixture.sibling, daysAgo = 2)
        Assertions.assertFalse(other.acceptancePending)
        logon(fixture.helper)
        timesheetDao.update(timesheetDao.find(other.id)!!.also { it.task = fixture.granted })
        timesheetDao.find(other.id, checkAccess = false)!!.let {
            Assertions.assertTrue(it.acceptancePending)
            Assertions.assertNotEquals(byManager.id, it.bookingGrant?.id)
        }
    }

    @Test
    fun `a sheet moved by the helper to a task of the own groups is not to be accepted anymore`() {
        val fixture = createFixture("bgp-own-group")
        persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            val group = initTestDB.addGroup("bgp-own-group-helpers", fixture.helper.username!!)
            initTestDB.createGroupTaskAccess(group, fixture.sibling).let { access ->
                access.ensureAndGetAccessEntry(AccessType.TASKS).setAccess(true, false, false, false)
                access.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS).setAccess(true, true, true, true)
                accessDao.update(access, checkAccess = false)
            }
        }
        propose(fixture)
        val sheet = book(fixture, fixture.granted)
        Assertions.assertTrue(sheet.acceptancePending)
        logon(fixture.helper)
        timesheetDao.update(timesheetDao.find(sheet.id)!!.also { it.task = fixture.sibling })
        timesheetDao.find(sheet.id, checkAccess = false)!!.let {
            Assertions.assertEquals(fixture.sibling.id, it.taskId)
            Assertions.assertFalse(it.acceptancePending)
        }
    }

    @Test
    fun `a confirmed proposal is revoked by the project management only, confirmed also on a closed task`() {
        val fixture = createFixture("bgp-confirmed")
        val grant = propose(fixture)
        // The task is closed after the proposal: confirming works anyway.
        persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            taskDao.update(taskDao.find(fixture.granted.id)!!.also { it.status = TaskStatus.C }, checkAccess = false)
        }
        logon(fixture.manager)
        Assertions.assertTrue(taskBookingGrantService.confirm(grant.id!!))
        // The proposer reads, but doesn't revoke it anymore.
        logon(fixture.employee)
        Assertions.assertNotNull(taskBookingGrantDao.find(grant.id))
        assertThrows<AccessException> {
            taskBookingGrantDao.markAsDeleted(taskBookingGrantDao.selectWithPositions(grant.id!!)!!)
        }
        logon(fixture.manager)
        taskBookingGrantDao.markAsDeleted(taskBookingGrantDao.selectWithPositions(grant.id!!)!!)
        Assertions.assertTrue(taskBookingGrantDao.find(grant.id, checkAccess = false)!!.deleted)
    }

    @Test
    fun `deleting and undeleting keep the sheet to be accepted`() {
        val fixture = createFixture("bgp-delete")
        val grant = propose(fixture)
        val sheet = book(fixture, fixture.granted)
        // As posted by the client: without the grant and the acceptance.
        fun posted() = TimesheetDO().also {
            it.id = sheet.id
            it.user = sheet.user
            it.task = sheet.task
            it.startTime = sheet.startTime
            it.stopTime = sheet.stopTime
            it.description = sheet.description
        }
        logon(fixture.helper)
        timesheetDao.markAsDeleted(posted())
        timesheetDao.find(sheet.id, checkAccess = false)!!.let {
            Assertions.assertTrue(it.deleted)
            Assertions.assertTrue(it.acceptancePending)
            Assertions.assertEquals(grant.id, it.bookingGrant?.id)
        }
        Assertions.assertEquals(0, taskBookingGrantCache.getPendingTimesheetCount(fixture.granted.id))
        timesheetDao.undelete(posted())
        timesheetDao.find(sheet.id, checkAccess = false)!!.let {
            Assertions.assertFalse(it.deleted)
            Assertions.assertTrue(it.acceptancePending)
            Assertions.assertEquals(grant.id, it.bookingGrant?.id)
        }
        Assertions.assertEquals(1, taskBookingGrantCache.getPendingTimesheetCount(fixture.granted.id))
    }

    @Test
    fun `a grant without task is refused as such, not as missing access`() {
        val fixture = createFixture("bgp-notask")
        logon(fixture.manager)
        val ex = assertThrows<UserException> {
            taskBookingGrantService.grant(
                userIds = listOf(fixture.helper.id!!),
                positions = emptyList(),
                validFrom = PFDay.now().localDate,
                validUntil = PFDay.now().plusDays(60).localDate,
                comment = null,
            )
        }
        Assertions.assertEquals(TaskBookingGrantDao.ERROR_NO_TASK, ex.i18nKey)
    }

    @Test
    fun `a confirmed proposal books without acceptance from now on`() {
        val fixture = createFixture("bgp-confirm")
        val grant = propose(fixture)
        val before = book(fixture, fixture.granted)
        logon(fixture.manager)
        Assertions.assertTrue(taskBookingGrantService.confirm(grant.id!!))
        Assertions.assertFalse(taskBookingGrantService.confirm(grant.id!!), "Confirmed already.")
        val after = book(fixture, fixture.granted, hour = 10)
        Assertions.assertFalse(after.acceptancePending)
        Assertions.assertTrue(timesheetDao.find(before.id, checkAccess = false)!!.acceptancePending)
    }

    @Test
    fun `the notification concerns the managers of the project only`() {
        val fixture = createFixture("bgp-notify", withProject = true)
        propose(fixture)
        val sheet = book(fixture, fixture.granted)
        val concerned = timesheetsToAcceptRuleHandler.timesheetsToAccept()
        Assertions.assertEquals(1, concerned[fixture.manager.id]?.size)
        Assertions.assertNull(concerned[fixture.employee.id])
        Assertions.assertNull(concerned[fixture.helper.id])
        Assertions.assertNull(concerned[fixture.former.id], "Deactivated.")
        logon(fixture.manager)
        timesheetDao.accept(timesheetDao.find(sheet.id)!!)
        Assertions.assertNull(timesheetsToAcceptRuleHandler.timesheetsToAccept()[fixture.manager.id])
    }
}
