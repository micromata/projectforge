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


package org.projectforge.business.availability

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.employee.EmployeeTest
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * The rule set of [AbsenceVisibility]: who sees which part of the vacations and availability entries of others. Each
 * row of its table with entries of the past, running ones, future ones and one across the turn of the year.
 */
class AbsenceVisibilityTest : AbstractTestBase() {
    @Autowired
    private lateinit var absenceVisibility: AbsenceVisibility

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    /** A fixed day, so the clipping doesn't depend on when the test runs. */
    private val today = LocalDate.of(2031, 3, 15)
    private val newYear = LocalDate.of(2031, 1, 1)

    private val lastYear = period(LocalDate.of(2030, 12, 1), LocalDate.of(2030, 12, 20))
    private val acrossNewYear = period(LocalDate.of(2030, 12, 28), LocalDate.of(2031, 1, 5))
    private val thisYearPast = period(LocalDate.of(2031, 2, 2), LocalDate.of(2031, 2, 6))
    private val endedYesterday = period(LocalDate.of(2031, 3, 10), LocalDate.of(2031, 3, 14))
    private val endingToday = period(LocalDate.of(2031, 3, 10), LocalDate.of(2031, 3, 15))
    private val running = period(LocalDate.of(2031, 3, 1), LocalDate.of(2031, 3, 31))
    private val future = period(LocalDate.of(2031, 5, 4), LocalDate.of(2031, 5, 8))
    private val all = listOf(lastYear, acrossNewYear, thisYearPast, endedYesterday, endingToday, running, future)

    private data class Period(val start: LocalDate, val end: LocalDate)

    private fun period(start: LocalDate, end: LocalDate) = Period(start, end)

    @Test
    fun `vacations - full for the employee, HR and finance staff, manager and substitutes`() {
        val people = createVacationPeople("vacation-full")
        val hrReadonly = createHrGroupMember("vacation-full-hr-readonly", UserRightValue.READONLY)
        val hrGroupOnly = createHrGroupMember("vacation-full-hr-group", null)
        val viewers = listOf(
            people.owner.user!! to "employee",
            people.manager.user!! to "manager",
            people.replacement.user!! to "replacement",
            people.otherReplacement.user!! to "other replacement",
            getUser(TEST_HR_USER) to "HR staff",
            hrReadonly to "HR staff read-only",
            hrGroupOnly to "HR group",
            getUser(TEST_FINANCE_USER) to "finance group",
            getUser(TEST_CONTROLLING_USER) to "controlling group",
        )
        all.forEach { period ->
            val vacation = vacation(people, period)
            viewers.forEach { (viewer, role) ->
                assertEquals(AbsenceVisibility.FULL, absenceVisibility.visibleFrom(viewer, vacation, today), "$role, $period")
            }
        }
    }

    @Test
    fun `vacations - group mates from 1 January of the current year, nothing for anybody else`() {
        val people = createVacationPeople("vacation-group")
        val (groupMate, stranger) = createGroupMateAndStranger("vacation-group", people.owner.user!!)
        assertNull(absenceVisibility.visibleFrom(groupMate, vacation(people, lastYear), today), "Ended last year.")
        listOf(acrossNewYear, thisYearPast, endedYesterday, endingToday, running, future).forEach { period ->
            assertEquals(newYear, absenceVisibility.visibleFrom(groupMate, vacation(people, period), today), "$period")
        }
        all.forEach { period ->
            assertNull(absenceVisibility.visibleFrom(stranger, vacation(people, period), today), "Stranger, $period")
        }
        assertNull(absenceVisibility.visibleFrom(null, vacation(people, future), today), "No viewer.")
        // On 1 January, the vacations of the year before are gone.
        assertNull(absenceVisibility.visibleFrom(groupMate, vacation(people, lastYear), newYear))
        assertEquals(newYear, absenceVisibility.visibleFrom(groupMate, vacation(people, acrossNewYear), newYear))
    }

    @Test
    fun `availability entries - full for the user, HR staff and the substitutes`() {
        val owner = createUser("availability-full-owner")
        val substitute = createUser("availability-full-substitute")
        val hrReadonly = createHrGroupMember("availability-full-hr-readonly", UserRightValue.READONLY)
        val viewers = listOf(
            owner to "user",
            substitute to "substitute",
            getUser(TEST_HR_USER) to "HR staff",
            hrReadonly to "HR staff read-only",
        )
        all.forEach { period ->
            val entry = availability(owner, period, substitute)
            viewers.forEach { (viewer, role) ->
                assertEquals(AbsenceVisibility.FULL, absenceVisibility.visibleFrom(viewer, entry, today), "$role, $period")
            }
        }
    }

    @Test
    fun `availability entries - group mates from today on, also HR group and finance staff, nothing for others`() {
        val owner = createUser("availability-group-owner")
        val (groupMate, stranger) = createGroupMateAndStranger("availability-group", owner)
        val hrGroupOnly = createHrGroupMember("availability-group-hr-group", null)
        val finance = getUser(TEST_FINANCE_USER)
        // HR group (without the HR right) and finance staff as group mates: the entries as for anybody else.
        val groupName = "${javaClass.simpleName}.availability-group-staff"
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup(groupName, owner.username!!, hrGroupOnly.username!!, finance.username!!)
        listOf(groupMate to "group mate", hrGroupOnly to "HR group", finance to "finance").forEach { (viewer, role) ->
            listOf(lastYear, acrossNewYear, thisYearPast, endedYesterday).forEach { period ->
                assertNull(absenceVisibility.visibleFrom(viewer, availability(owner, period), today), "$role, $period")
            }
            listOf(endingToday, running, future).forEach { period ->
                assertEquals(today, absenceVisibility.visibleFrom(viewer, availability(owner, period), today), "$role, $period")
            }
        }
        all.forEach { period ->
            assertNull(absenceVisibility.visibleFrom(stranger, availability(owner, period), today), "Stranger, $period")
            assertNull(
                absenceVisibility.visibleFrom(getUser(TEST_CONTROLLING_USER), availability(owner, period), today),
                "Controlling, not of the same group, $period",
            )
        }
        assertNull(absenceVisibility.visibleFrom(null, availability(owner, future), today), "No viewer.")
    }

    @Test
    fun `the substitutes of a stored entry are known by the cache, the dao's access check follows the rules`() {
        val owner = createUser("dao-owner")
        val substitute = createUser("dao-substitute")
        val (groupMate, stranger) = createGroupMateAndStranger("dao", owner)
        logon(TEST_HR_USER)
        val now = LocalDate.now()
        val past = AvailabilityDO().also {
            it.user = owner
            it.startDate = now.minusDays(20)
            it.endDate = now.minusDays(1)
            it.status = AvailabilityStatus.NOT_AVAILABLE
            it.substitutes = mutableSetOf(substitute)
        }
        val id = availabilityDao.insert(past)
        // Read from the database: the substitutes are lazy there, the cache tells them.
        val loaded = availabilityDao.find(id, checkAccess = false)!!
        assertTrue(availabilityDao.hasUserSelectAccess(substitute, loaded, false), "Substitute.")
        assertTrue(availabilityDao.hasUserSelectAccess(owner, loaded, false), "Own entry.")
        assertFalse(availabilityDao.hasUserSelectAccess(groupMate, loaded, false), "Ended yesterday.")
        assertFalse(availabilityDao.hasUserSelectAccess(stranger, loaded, false))
        val people = createVacationPeople("dao-vacation")
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("${javaClass.simpleName}.dao-vacation", people.owner.user!!.username!!, groupMate.username!!)
        val beginOfYear = LocalDate.of(now.year, 1, 1)
        val vacation = vacation(people, period(beginOfYear.minusDays(10), beginOfYear.minusDays(2)))
        assertFalse(vacationDao.hasUserSelectAccess(groupMate, vacation, false), "Ended last year.")
        assertTrue(vacationDao.hasUserSelectAccess(people.replacement.user!!, vacation, false), "Replacement.")
        vacation.endDate = beginOfYear
        assertTrue(vacationDao.hasUserSelectAccess(groupMate, vacation, false), "Ends this year.")
    }

    private class VacationPeople(
        val owner: EmployeeDO,
        val manager: EmployeeDO,
        val replacement: EmployeeDO,
        val otherReplacement: EmployeeDO,
    )

    private fun createVacationPeople(name: String) = VacationPeople(
        createEmployee("$name-owner"),
        createEmployee("$name-manager"),
        createEmployee("$name-replacement"),
        createEmployee("$name-other-replacement"),
    )

    /** A member of a new group with [user], and a user of none of the groups of [user]. */
    private fun createGroupMateAndStranger(name: String, user: PFUserDO): Pair<PFUserDO, PFUserDO> {
        val groupMate = createUser("$name-group-mate")
        val stranger = createUser("$name-stranger")
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("${javaClass.simpleName}.$name", user.username!!, groupMate.username!!)
        return groupMate to stranger
    }

    /** A member of the HR group, with the HR vacation right of the given value (none: null). */
    private fun createHrGroupMember(name: String, right: UserRightValue?): PFUserDO {
        logon(TEST_ADMIN_USER)
        val user = PFUserDO()
        user.firstname = "$name.firstname"
        user.lastname = "$name.lastname"
        user.username = "${javaClass.simpleName}.$name.username"
        right?.let { user.addRight(UserRightDO(UserRightId.HR_VACATION, it)) }
        initTestDB.addUser(user)
        val group = getGroup(ProjectForgeGroup.HR_GROUP.toString())
        group.assignedUsers!!.add(user)
        groupDao.update(group)
        return user
    }

    private fun vacation(people: VacationPeople, period: Period) = VacationDO().also {
        it.employee = people.owner
        it.manager = people.manager
        it.replacement = people.replacement
        it.otherReplacements = mutableSetOf(people.otherReplacement)
        it.startDate = period.start
        it.endDate = period.end
        it.status = VacationStatus.APPROVED
    }

    private fun availability(user: PFUserDO, period: Period, vararg substitutes: PFUserDO) = AvailabilityDO().also {
        it.user = user
        it.startDate = period.start
        it.endDate = period.end
        it.status = AvailabilityStatus.NOT_AVAILABLE
        it.substitutes = substitutes.toMutableSet()
    }

    private fun createUser(name: String): PFUserDO {
        val user = PFUserDO()
        user.firstname = "$name.firstname"
        user.lastname = "$name.lastname"
        user.username = "${javaClass.simpleName}.$name.username"
        return initTestDB.addUser(user)
    }

    private fun createEmployee(name: String): EmployeeDO =
        EmployeeTest.createEmployee(employeeService, employeeDao, this, name, groupDao = groupDao)
}
