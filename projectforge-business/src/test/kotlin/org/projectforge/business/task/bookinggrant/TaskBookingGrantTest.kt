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
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired

/**
 * Ad hoc booking grants: the user of a grant books own time sheets on exactly the granted task within the period,
 * without any group; who may edit the time sheets of others on the task grants; an invitation link is accepted by
 * its user only.
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
        val sibling: TaskDO,
        val manager: PFUserDO,
        val helper: PFUserDO,
        val other: PFUserDO,
    )

    /**
     * A project with two work packages, a manager with all rights on it (foreign time sheets included), and two
     * users without any group: the helper and somebody else.
     */
    private fun createFixture(prefix: String): Fixture {
        lateinit var fixture: Fixture
        logon(TEST_ADMIN_USER)
        persistenceService.runInTransaction { _ ->
            val project = initTestDB.addTask("$prefix", "root")
            val granted = initTestDB.addTask("$prefix.granted", prefix)
            val sibling = initTestDB.addTask("$prefix.sibling", prefix)
            val manager = initTestDB.addUser("$prefix-manager")
            val helper = initTestDB.addUser("$prefix-helper")
            val other = initTestDB.addUser("$prefix-other")
            val group = initTestDB.addGroup("$prefix-managers", manager.username!!)
            val access = initTestDB.createGroupTaskAccess(group, project)
            AccessType.entries.filter { it != AccessType.GROUP }.forEach { type ->
                access.ensureAndGetAccessEntry(type).setAccess(true, true, true, true)
            }
            access.recursive = true
            accessDao.update(access, checkAccess = false)
            fixture = Fixture(project, granted, sibling, manager, helper, other)
        }
        return fixture
    }

    private fun grant(
        fixture: Fixture,
        asInvitation: Boolean = false,
        validFrom: PFDay = PFDay.now().minusDays(5),
        validUntil: PFDay = PFDay.now().plusDays(5),
        grantedBy: PFUserDO = fixture.manager,
    ): TaskBookingGrantService.GrantResult {
        logon(grantedBy)
        val obj = TaskBookingGrantDO()
        obj.task = fixture.granted
        obj.user = fixture.helper
        obj.validFrom = validFrom.localDate
        obj.validUntil = validUntil.localDate
        return taskBookingGrantService.grant(obj, asInvitation)
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
    fun `revoked or expired grant keeps the own time sheets readable only`() {
        val fixture = createFixture("bg-revoke")
        val result = grant(fixture)
        logon(fixture.helper)
        val own = sheet(fixture.helper, fixture.granted)
        timesheetDao.insert(own)

        logon(fixture.manager)
        taskBookingGrantDao.markAsDeleted(taskBookingGrantDao.find(result.grant.id)!!)

        val stored = timesheetDao.find(own.id, checkAccess = false)!!
        Assertions.assertTrue(timesheetDao.hasUserSelectAccess(fixture.helper, stored, false))
        Assertions.assertFalse(mayInsert(fixture.helper, sheet(fixture.helper, fixture.granted, hour = 12)))
        stored.description = "changed"
        Assertions.assertFalse(timesheetDao.hasUpdateAccess(fixture.helper, stored, own, false))

        // An expired grant: nothing to book, not even within its former period.
        val expired = createFixture("bg-expired")
        grant(expired, validFrom = PFDay.now().minusDays(10), validUntil = PFDay.now().minusDays(1))
        Assertions.assertFalse(mayInsert(expired.helper, sheet(expired.helper, expired.granted, daysAgo = 2)))
        Assertions.assertTrue(taskBookingGrantCache.getCurrentTaskIds(expired.helper.id).isEmpty())
    }

    @Test
    fun `only who may edit foreign time sheets on the task grants`() {
        val fixture = createFixture("bg-right")
        // Refused for an ordinary user, even one with groups (TEST_USER), and for the helper itself.
        assertThrows<AccessException> { grant(fixture, grantedBy = getUser(TEST_USER)) }
        assertThrows<AccessException> { grant(fixture, grantedBy = fixture.helper) }
        // Allowed for the manager of the project and the finance group.
        Assertions.assertNotNull(grant(fixture).grant.id)
        Assertions.assertNotNull(grant(fixture, grantedBy = getUser(TEST_FINANCE_USER)).grant.id)

        // The helper reads the own grant, but doesn't change it.
        logon(fixture.helper)
        val own = taskBookingGrantDao.find(taskBookingGrantDao.selectByTask(fixture.granted.id!!).first().id)!!
        own.validUntil = PFDay.now().plusDays(100).localDate
        assertThrows<AccessException> { taskBookingGrantDao.update(own) }
        assertThrows<AccessException> { taskBookingGrantDao.markAsDeleted(own) }
        // The other user doesn't even read it.
        logon(fixture.other)
        assertThrows<AccessException> { taskBookingGrantDao.find(own.id) }
    }

    @Test
    fun `grant on a task can't be moved to another task or user`() {
        val fixture = createFixture("bg-move")
        val result = grant(fixture)
        logon(fixture.manager)
        val stored = taskBookingGrantDao.find(result.grant.id)!!
        stored.task = fixture.sibling
        assertThrows<UserException> { taskBookingGrantDao.update(stored) }
    }

    @Test
    fun `invitation link is accepted by its user only, once`() {
        val fixture = createFixture("bg-invite")
        val result = grant(fixture, asInvitation = true)
        val url = result.invitationUrl!!
        val token = url.substringAfter("token=")
        Assertions.assertTrue(token.length >= 32)
        Assertions.assertNull(result.grant.acceptedAt)
        Assertions.assertNotEquals(token, result.grant.invitationTokenHash, "Only the hash is stored.")
        Assertions.assertFalse(
            mayInsert(fixture.helper, sheet(fixture.helper, fixture.granted)),
            "No access before the invitation is accepted.",
        )

        // Somebody else (who got the forwarded link) learns nothing and accepts nothing.
        logon(fixture.other)
        Assertions.assertNull(taskBookingGrantService.getInvitation(token))
        assertThrows<UserException> { taskBookingGrantService.accept(token) }
        Assertions.assertFalse(mayInsert(fixture.other, sheet(fixture.other, fixture.granted)))

        logon(fixture.helper)
        Assertions.assertNotNull(taskBookingGrantService.getInvitation(token))
        taskBookingGrantService.accept(token)
        Assertions.assertTrue(mayInsert(fixture.helper, sheet(fixture.helper, fixture.granted)))
        // Used once.
        assertThrows<UserException> { taskBookingGrantService.accept(token) }
        assertThrows<UserException> { taskBookingGrantService.accept("unknown-token") }
    }

    @Test
    fun `the mail names the task path, the period and the link`() {
        val fixture = createFixture("bg-mail")
        val direct = grant(fixture)
        taskBookingGrantService.prepareMail(direct.grant, fixture.helper, null).let { mail ->
            Assertions.assertEquals(fixture.helper.email, mail.to.single().address)
            Assertions.assertTrue(mail.subject!!.contains("bg-mail | bg-mail.granted"), mail.subject)
            Assertions.assertTrue(mail.content!!.contains("next/timesheet/new?taskId=${fixture.granted.id}"), mail.content)
            Assertions.assertTrue(mail.content!!.contains(PFDay.now().minusDays(5).isoString), mail.content)
        }
        val invitation = grant(fixture, asInvitation = true)
        taskBookingGrantService.prepareMail(invitation.grant, fixture.helper, invitation.invitationUrl).let { mail ->
            Assertions.assertTrue(mail.content!!.contains(invitation.invitationUrl!!), mail.content)
        }
    }

    @Test
    fun `the tree shows the granted task and its ancestors only`() {
        val fixture = createFixture("bg-tree")
        Assertions.assertTrue(taskBookingGrantCache.getVisiblePathTaskIds(fixture.helper.id).isEmpty())
        grant(fixture)
        val visible = taskBookingGrantCache.getVisiblePathTaskIds(fixture.helper.id)
        Assertions.assertEquals(setOf(fixture.project.id, fixture.granted.id), visible)
        Assertions.assertTrue(taskBookingGrantCache.getVisiblePathTaskIds(fixture.other.id).isEmpty())
    }
}
