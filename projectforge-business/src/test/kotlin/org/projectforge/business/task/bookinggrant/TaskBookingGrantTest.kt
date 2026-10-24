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
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired

/**
 * Ad hoc booking grants: the users of a grant book own time sheets on exactly its tasks within the period, without
 * any group; who may edit the time sheets of others on all of its tasks grants.
 */
class TaskBookingGrantTest : AbstractTestBase() {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var taskBookingGrantDao: TaskBookingGrantDao

    @Autowired
    private lateinit var taskBookingGrantService: TaskBookingGrantService

    @Autowired
    private lateinit var taskBookingGrantCache: TaskBookingGrantCache

    private class Fixture(
        val project: TaskDO,
        val granted: TaskDO,
        val second: TaskDO,
        val sibling: TaskDO,
        /** Not below the project: its manager may not grant on it. */
        val foreign: TaskDO,
        val manager: PFUserDO,
        val helper: PFUserDO,
        val helper2: PFUserDO,
        val other: PFUserDO,
    )

    /**
     * A project with three work packages, a manager with all rights on it (foreign time sheets included), and three
     * users without any group: two helpers and somebody else.
     */
    private fun createFixture(prefix: String): Fixture {
        lateinit var fixture: Fixture
        logon(TEST_ADMIN_USER)
        persistenceService.runInTransaction { _ ->
            val project = initTestDB.addTask(prefix, "root")
            val granted = initTestDB.addTask("$prefix.granted", prefix)
            val second = initTestDB.addTask("$prefix.second", prefix)
            val sibling = initTestDB.addTask("$prefix.sibling", prefix)
            val foreign = initTestDB.addTask("$prefix-foreign", "root")
            val manager = initTestDB.addUser("$prefix-manager")
            val helper = initTestDB.addUser("$prefix-helper")
            val helper2 = initTestDB.addUser("$prefix-helper2")
            val other = initTestDB.addUser("$prefix-other")
            val group = initTestDB.addGroup("$prefix-managers", manager.username!!)
            val access = initTestDB.createGroupTaskAccess(group, project)
            AccessType.entries.filter { it != AccessType.GROUP }.forEach { type ->
                access.ensureAndGetAccessEntry(type).setAccess(true, true, true, true)
            }
            access.recursive = true
            accessDao.update(access, checkAccess = false)
            fixture = Fixture(project, granted, second, sibling, foreign, manager, helper, helper2, other)
        }
        return fixture
    }

    private fun grant(
        fixture: Fixture,
        users: List<PFUserDO> = listOf(fixture.helper),
        tasks: List<TaskDO> = listOf(fixture.granted),
        validFrom: PFDay = PFDay.now().minusDays(5),
        validUntil: PFDay = PFDay.now().plusDays(5),
        grantedBy: PFUserDO = fixture.manager,
    ): List<TaskBookingGrantDO> {
        logon(grantedBy)
        return taskBookingGrantService.grant(
            userIds = users.map { it.id!! },
            positions = tasks.map { TaskBookingGrantService.Position(it.id!!) },
            validFrom = validFrom.localDate,
            validUntil = validUntil.localDate,
            comment = null,
        )
    }

    /** A sheet of the given user, the given number of days ago, 8:00 - 9:00. */
    private fun sheet(user: PFUserDO, task: TaskDO, daysAgo: Long = 1, hour: Int = 8): TimesheetDO {
        val start = PFDateTime.now().minusDays(daysAgo).beginOfDay.withHour(hour)
        return TimesheetDO().also {
            it.user = user
            it.task = task
            it.startTime = start.utilDate
            it.stopTime = start.plusHours(1).utilDate
            it.description = "ad hoc"
        }
    }

    private fun mayInsert(user: PFUserDO, sheet: TimesheetDO): Boolean {
        return timesheetDao.hasInsertAccess(user, sheet, throwException = false)
    }

    @Test
    fun `user books on the granted task only, within the period`() {
        val fixture = createFixture("bg-book")
        Assertions.assertFalse(
            mayInsert(fixture.helper, sheet(fixture.helper, fixture.granted)),
            "No booking without grant (and without group).",
        )
        grant(fixture)
        Assertions.assertTrue(mayInsert(fixture.helper, sheet(fixture.helper, fixture.granted)))
        Assertions.assertFalse(
            mayInsert(fixture.helper, sheet(fixture.helper, fixture.sibling)),
            "The grant is for exactly the granted task.",
        )
        Assertions.assertFalse(
            mayInsert(fixture.helper, sheet(fixture.helper, fixture.project)),
            "Not for the ancestors either.",
        )
        Assertions.assertFalse(
            mayInsert(fixture.helper, sheet(fixture.helper, fixture.granted, daysAgo = 10)),
            "Not before the period.",
        )
        Assertions.assertFalse(
            mayInsert(fixture.other, sheet(fixture.other, fixture.granted)),
            "Not for another user.",
        )
        Assertions.assertFalse(
            mayInsert(fixture.helper, sheet(fixture.other, fixture.granted)),
            "Not for the time sheets of others.",
        )

        // Booked and read by the helper; the sheets of others stay invisible.
        logon(fixture.helper)
        val own = sheet(fixture.helper, fixture.granted)
        timesheetDao.insert(own)
        logon(fixture.manager)
        val managers = sheet(fixture.manager, fixture.granted, hour = 10)
        timesheetDao.insert(managers)
        Assertions.assertTrue(timesheetDao.hasUserSelectAccess(fixture.helper, own, false))
        Assertions.assertFalse(timesheetDao.hasUserSelectAccess(fixture.helper, managers, false))
    }

    @Test
    fun `several users and tasks give one grant per user with all tasks`() {
        val fixture = createFixture("bg-multi")
        val grants = grant(
            fixture,
            users = listOf(fixture.helper, fixture.helper2),
            tasks = listOf(fixture.granted, fixture.second),
        )
        Assertions.assertEquals(2, grants.size)
        Assertions.assertEquals(setOf(fixture.helper.id, fixture.helper2.id), grants.map { it.user?.id }.toSet())
        listOf(fixture.helper, fixture.helper2).forEach { user ->
            Assertions.assertTrue(mayInsert(user, sheet(user, fixture.granted)))
            Assertions.assertTrue(mayInsert(user, sheet(user, fixture.second)))
            Assertions.assertFalse(mayInsert(user, sheet(user, fixture.sibling)))
        }
        val stored = taskBookingGrantDao.selectByTasks(listOf(fixture.second.id!!))
        Assertions.assertEquals(2, stored.size)
        stored.forEach { grant ->
            Assertions.assertEquals(
                setOf(fixture.granted.id, fixture.second.id),
                grant.positions!!.map { it.task?.id }.toSet(),
            )
        }
        val badges = taskBookingGrantCache.getCurrentGrantsOnTask(fixture.second.id)
        Assertions.assertEquals(setOf(fixture.helper.id, fixture.helper2.id), badges.map { it.userId }.toSet())
        Assertions.assertTrue(taskBookingGrantCache.getCurrentGrantsOnTask(fixture.sibling.id).isEmpty())
    }

    @Test
    fun `nothing is granted if one task may not be granted on`() {
        val fixture = createFixture("bg-all")
        assertThrows<AccessException> {
            grant(fixture, users = listOf(fixture.helper, fixture.helper2), tasks = listOf(fixture.granted, fixture.foreign))
        }
        Assertions.assertTrue(taskBookingGrantDao.selectByTasks(listOf(fixture.granted.id!!)).isEmpty())
        Assertions.assertFalse(mayInsert(fixture.helper, sheet(fixture.helper, fixture.granted)))
        // No task, no user, a task twice: refused as well.
        assertThrows<UserException> { grant(fixture, tasks = emptyList()) }
        assertThrows<UserException> { grant(fixture, users = emptyList()) }
        assertThrows<UserException> { grant(fixture, tasks = listOf(fixture.granted, fixture.granted)) }
    }

    @Test
    fun `cost units of a task restrict its bookings`() {
        val today = PFDay.now().localDate
        // In memory only: the cache's rule, without a project with cost units in the test data.
        val grant = TaskBookingGrantDO().also {
            it.id = 987654321L
            it.user = PFUserDO().also { user -> user.id = 987654321L }
            it.validFrom = today.minusDays(1)
            it.validUntil = today.plusDays(1)
            it.addPosition(TaskBookingGrantTaskDO().also { pos ->
                pos.task = TaskDO().also { task -> task.id = 1001L }
                pos.kost2IdSet = setOf(1L, 2L)
            })
            it.addPosition(TaskBookingGrantTaskDO().also { pos ->
                pos.task = TaskDO().also { task -> task.id = 1002L }
            })
        }
        // Loaded first: a later first load would replace the grant only known in memory.
        taskBookingGrantCache.getCurrentGrantsOnTask(1001L)
        taskBookingGrantCache.afterInsertOrModify(grant, OperationType.INSERT)
        fun allows(taskId: Long, kost2Id: Long?) =
            taskBookingGrantCache.allows(987654321L, taskId, OperationType.INSERT, today, today, kost2Id)
        Assertions.assertTrue(allows(1001L, 1L))
        Assertions.assertTrue(allows(1001L, 2L))
        Assertions.assertFalse(allows(1001L, 3L), "Not a cost unit of the position.")
        Assertions.assertFalse(allows(1001L, null))
        Assertions.assertTrue(allows(1002L, 3L), "No cost units: every one of the task.")
        Assertions.assertFalse(allows(1003L, 1L), "Not a task of the grant.")
        Assertions.assertEquals(setOf(1L, 2L), taskBookingGrantCache.getAllowedKost2Ids(987654321L, 1001L))
        Assertions.assertNull(taskBookingGrantCache.getAllowedKost2Ids(987654321L, 1002L))
        // Revoked: nothing to book any more.
        grant.deleted = true
        taskBookingGrantCache.afterInsertOrModify(grant, OperationType.UPDATE)
        Assertions.assertFalse(allows(1002L, 3L))
    }

    @Test
    fun `revoked or expired grant keeps the own time sheets readable only`() {
        val fixture = createFixture("bg-revoke")
        val granted = grant(fixture, tasks = listOf(fixture.granted, fixture.second)).single()
        logon(fixture.helper)
        val own = sheet(fixture.helper, fixture.granted)
        timesheetDao.insert(own)

        logon(fixture.manager)
        taskBookingGrantDao.markAsDeleted(taskBookingGrantDao.selectWithPositions(granted.id!!)!!)

        val stored = timesheetDao.find(own.id, checkAccess = false)!!
        Assertions.assertTrue(timesheetDao.hasUserSelectAccess(fixture.helper, stored, false))
        Assertions.assertFalse(mayInsert(fixture.helper, sheet(fixture.helper, fixture.granted, hour = 12)))
        Assertions.assertFalse(mayInsert(fixture.helper, sheet(fixture.helper, fixture.second, hour = 12)))
        stored.description = "changed"
        Assertions.assertFalse(timesheetDao.hasUpdateAccess(fixture.helper, stored, own, false))
        Assertions.assertTrue(taskBookingGrantCache.getCurrentGrantsOnTask(fixture.granted.id).isEmpty())

        // An expired grant: nothing to book, not even within its former period.
        val expired = createFixture("bg-expired")
        grant(expired, validFrom = PFDay.now().minusDays(10), validUntil = PFDay.now().minusDays(1))
        Assertions.assertFalse(mayInsert(expired.helper, sheet(expired.helper, expired.granted, daysAgo = 2)))
        Assertions.assertTrue(taskBookingGrantCache.getCurrentTaskIds(expired.helper.id).isEmpty())
    }

    @Test
    fun `only who may edit foreign time sheets on all tasks grants and revokes`() {
        val fixture = createFixture("bg-right")
        // Refused for an ordinary user, even one with groups (TEST_USER), and for the helper itself.
        assertThrows<AccessException> { grant(fixture, grantedBy = getUser(TEST_USER)) }
        assertThrows<AccessException> { grant(fixture, grantedBy = fixture.helper) }
        // Allowed for the manager of the project and the finance group.
        val byManager = grant(fixture).single()
        Assertions.assertNotNull(byManager.id)
        val byFinance = grant(fixture, tasks = listOf(fixture.granted, fixture.foreign), grantedBy = getUser(TEST_FINANCE_USER))
            .single()
        // Who granted is the user of the insert in the history.
        Assertions.assertEquals(
            mapOf(byManager.id to fixture.manager.id, byFinance.id to getUser(TEST_FINANCE_USER).id),
            taskBookingGrantDao.selectGrantedByUserIds(listOf(byManager.id!!, byFinance.id!!)),
        )

        // The manager may not revoke a grant with a task outside the project.
        logon(fixture.manager)
        assertThrows<AccessException> {
            taskBookingGrantDao.markAsDeleted(taskBookingGrantDao.selectWithPositions(byFinance.id!!)!!)
        }

        // The helper reads the own grant, but doesn't change it.
        logon(fixture.helper)
        val ownId = taskBookingGrantDao.selectByTasks(listOf(fixture.granted.id!!)).first().id
        val own = taskBookingGrantDao.find(ownId)!!
        own.validUntil = PFDay.now().plusDays(100).localDate
        assertThrows<AccessException> { taskBookingGrantDao.update(own) }
        assertThrows<AccessException> { taskBookingGrantDao.markAsDeleted(own) }
        // The other user doesn't even read it.
        logon(fixture.other)
        assertThrows<AccessException> { taskBookingGrantDao.find(ownId) }
    }

    @Test
    fun `the mail names the task paths, the period and the links`() {
        val fixture = createFixture("bg-mail")
        val single = grant(fixture).single()
        taskBookingGrantService.prepareMail(single, fixture.helper, fixture.manager).let { mail ->
            Assertions.assertEquals(fixture.helper.email, mail.to.single().address)
            Assertions.assertTrue(mail.subject!!.contains("bg-mail | bg-mail.granted"), mail.subject)
            Assertions.assertTrue(mail.content!!.contains("next/timesheet/new?taskId=${fixture.granted.id}"), mail.content)
            Assertions.assertTrue(mail.content!!.contains(PFDay.now().minusDays(5).isoString), mail.content)
        }
        val multi = grant(fixture, tasks = listOf(fixture.granted, fixture.second)).single()
        taskBookingGrantService.prepareMail(multi, fixture.helper, fixture.manager).let { mail ->
            Assertions.assertTrue(mail.content!!.contains("bg-mail | bg-mail.second"), mail.content)
            Assertions.assertTrue(mail.content!!.contains("next/timesheet/new?taskId=${fixture.second.id}"), mail.content)
        }
    }

    @Test
    fun `the tree shows the granted tasks and their ancestors only`() {
        val fixture = createFixture("bg-tree")
        Assertions.assertTrue(taskBookingGrantCache.getVisiblePathTaskIds(fixture.helper.id).isEmpty())
        grant(fixture, tasks = listOf(fixture.granted, fixture.second))
        val visible = taskBookingGrantCache.getVisiblePathTaskIds(fixture.helper.id)
        Assertions.assertEquals(setOf(fixture.project.id, fixture.granted.id, fixture.second.id), visible)
        Assertions.assertTrue(taskBookingGrantCache.getVisiblePathTaskIds(fixture.other.id).isEmpty())
    }
}
