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

import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.availability.AvailabilityLocation
import org.projectforge.business.availability.AvailabilityStatus
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
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStats
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The vacation account of projectforge-next ([VacationAccountRest]): whose account is shown, what it holds and
 * who may recalculate the remaining leave.
 */
class VacationAccountRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var vacationAccountRest: VacationAccountRest

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Test
    fun `non HR staff always see their own account`() {
        val own = createEmployee("own")
        val other = createEmployee("other")
        logon(own.user!!)
        vacationAccountRest.getAccount(other.id).let {
            assertEquals(own.id, it.employee?.id, "The employee asked for is ignored for non HR staff.")
            assertFalse(it.hrAccess)
            assertFalse(it.canRecalculate)
        }
        assertThrows<AccessException> { vacationAccountRest.recalculateRemainingLeave(own.id!!) }
    }

    @Test
    fun `HR staff see the employee asked for, then the one shown last`() {
        val hr = createEmployee("hr", hrAccess = true)
        val other = createEmployee("hr-other")
        logon(hr.user!!)
        assertEquals(hr.id, vacationAccountRest.getAccount(null).employee?.id, "At first the own account.")
        vacationAccountRest.getAccount(other.id).let {
            assertEquals(other.id, it.employee?.id)
            assertTrue(it.hrAccess)
        }
        assertEquals(other.id, vacationAccountRest.getAccount(null).employee?.id, "The one shown last is stored.")
        assertEquals(other.id, vacationAccountRest.recalculateRemainingLeave(other.id!!).employee?.id)
    }

    @Test
    fun `the account holds three years of statistics and the vacations with the way back`() {
        val own = createEmployee("content")
        val manager = createEmployee("content-manager")
        val year = LocalDate.now().year
        logon(own.user!!)
        val vacation = vacationDao.insert(VacationDO().also {
            it.employee = own
            it.manager = manager
            it.replacement = manager
            it.startDate = LocalDate.of(year + 1, 3, 2)
            it.endDate = LocalDate.of(year + 1, 3, 9)
            it.status = VacationStatus.IN_PROGRESS
        })
        val account = vacationAccountRest.getAccount(null)
        assertEquals(listOf(year - 1, year, year + 1), account.statistics.map { it.year })
        assertEquals(listOf(year + 1, year, year - 1), account.years.map { it.year })
        val entry = account.years.first().vacations.single()
        assertEquals(vacation, entry.id)
        // The days bar: the period and its weekends (no holiday in early March).
        assertEquals(LocalDate.of(year + 1, 3, 2), entry.startDate)
        assertEquals(LocalDate.of(year + 1, 3, 9), entry.endDate)
        val weekend = (2..9).map { LocalDate.of(year + 1, 3, it) }
            .filter { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY }
        assertEquals(weekend, entry.nonWorkingDays)
        assertEquals("/next/vacation/$vacation?returnTo=%2FvacationAccount", entry.editUrl)
        assertEquals("/next/vacation/new?employee=${own.id}&returnTo=%2FvacationAccount", account.newVacationUrl)
        assertEquals("/next/vacation", account.vacationListUrl)
    }

    @Test
    fun `the account lists the availability entries of the employee's user in its three years, newest first`() {
        val own = createEmployee("availability-own")
        val other = createEmployee("availability-other")
        val hr = createEmployee("availability-hr", hrAccess = true)
        val year = LocalDate.now().year
        logon(hr.user!!)
        fun insert(employee: EmployeeDO, start: LocalDate, end: LocalDate, type: String? = null) =
            availabilityDao.insert(AvailabilityDO().also {
                it.user = employee.user
                it.startDate = start
                it.endDate = end
                it.status = AvailabilityStatus.REMOTE_REACHABLE
                it.location = AvailabilityLocation.REMOTE
                it.type = type
                it.comment = "Workation"
            })
        val previousYear = insert(own, LocalDate.of(year - 1, 12, 20), LocalDate.of(year, 1, 5))
        val nextYear = insert(own, LocalDate.of(year + 1, 6, 1), LocalDate.of(year + 1, 6, 10), type = "Workation")
        insert(own, LocalDate.of(year - 2, 3, 1), LocalDate.of(year - 2, 3, 5)) // Before the three years.
        insert(own, LocalDate.of(year + 2, 3, 1), LocalDate.of(year + 2, 3, 5)) // After them.
        insert(other, LocalDate.of(year, 3, 1), LocalDate.of(year, 3, 5))

        logon(own.user!!)
        vacationAccountRest.getAccount(other.id).let { account ->
            assertEquals(listOf(nextYear, previousYear), account.availabilities.map { it.id }, "Only the own ones.")
            account.availabilities.first().let {
                assertEquals("Workation", it.type, "The own type.")
                assertEquals("Workation", it.comment)
                assertFalse(it.conflict)
                assertEquals("/next/availability/$nextYear?returnTo=%2FvacationAccount", it.editUrl)
                // The days bar: the period, its weekends and holidays and its working days.
                assertEquals(LocalDate.of(year + 1, 6, 1), it.startDate)
                assertEquals(LocalDate.of(year + 1, 6, 10), it.endDate)
                val days = (1..10).map { day -> LocalDate.of(year + 1, 6, day) }
                assertEquals(days.filter { day -> !Holidays.instance.isWorkingDay(day) }, it.nonWorkingDays)
                assertEquals(
                    VacationStats.format(VacationService.getVacationDays(days.first(), days.last())),
                    it.workingDaysFormatted,
                )
                assertNull(it.uncoveredDays)
            }
            assertEquals(
                "/next/availability/new?user=${own.user!!.id}&returnTo=%2FvacationAccount",
                account.newAvailabilityUrl,
            )
        }
        logon(hr.user!!)
        vacationAccountRest.getAccount(own.id).let { account ->
            assertEquals(listOf(nextYear, previousYear), account.availabilities.map { it.id }, "HR: the one shown.")
            assertEquals("Workation", account.availabilities.first().type)
        }
    }

    @Test
    fun `the account costs a constant number of statements, not one per vacation`() {
        val hr = createEmployee("statements-hr", hrAccess = true)
        val replacement = createEmployee("statements-replacement")
        val other = createEmployee("statements-other")
        val single = createEmployee("statements-single")
        val many = createEmployee("statements-many")
        val year = LocalDate.now().year + 1
        logon(hr.user!!)
        fun vacation(employee: EmployeeDO, month: Int) = vacationDao.insert(VacationDO().also {
            it.employee = employee
            it.manager = hr
            it.replacement = replacement
            it.otherReplacements = mutableSetOf(other, replacement)
            // A Tuesday and Wednesday in the middle of the month, never a weekend or a holiday.
            val start = LocalDate.of(year, month, 8).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
            it.startDate = start
            it.endDate = start.plusDays(1)
            it.status = VacationStatus.IN_PROGRESS
        })
        vacation(single, 2)
        (2..5).forEach { vacation(many, it) }
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val wasEnabled = statistics.isStatisticsEnabled
        statistics.isStatisticsEnabled = true
        try {
            fun statementsOf(employee: EmployeeDO, expectedVacations: Int): Long {
                statistics.clear()
                val account = vacationAccountRest.getAccount(employee.id)
                val vacations = account.years.flatMap { it.vacations }
                assertEquals(expectedVacations, vacations.size)
                // The other replacements are there, read without a lazy load per vacation.
                assertTrue(vacations.all { it.otherReplacements?.contains(other.displayName) == true })
                return statistics.prepareStatementCount
            }
            // Warms up what doesn't belong to the account itself: caches (users, rights) and the carry stored by the
            // first statistics of an employee.
            statementsOf(single, 1)
            statementsOf(many, 4)
            assertEquals(statementsOf(single, 1), statementsOf(many, 4))
        } finally {
            statistics.isStatisticsEnabled = wasEnabled
        }
    }

    @Test
    fun `a user without employee gets an empty account`() {
        logon(TEST_USER)
        vacationAccountRest.getAccount(null).let {
            assertNull(it.employee)
            assertTrue(it.statistics.isEmpty())
            assertNull(it.newVacationUrl)
            assertTrue(it.availabilities.isEmpty())
            assertNull(it.newAvailabilityUrl)
        }
    }

    private fun createEmployee(name: String, hrAccess: Boolean = false): EmployeeDO {
        val loggedInUser = ThreadLocalUserContext.loggedInUser
        logon(TEST_ADMIN_USER)
        val user = PFUserDO()
        user.firstname = "$name.firstname"
        user.lastname = "$name.lastname"
        user.username = "${javaClass.simpleName}.$name.username"
        if (hrAccess) {
            user.addRight(UserRightDO(UserRightId.HR_VACATION, UserRightValue.READWRITE))
        }
        initTestDB.addUser(user)
        if (hrAccess) {
            val group = getGroup(ProjectForgeGroup.HR_GROUP.toString())
            group.assignedUsers!!.add(user)
            groupDao.update(group)
        }
        val employee = EmployeeDO()
        employee.user = user
        employeeDao.insert(employee, checkAccess = false)
        employeeService.insertAnnualLeaveDays(employee, LocalDate.now().minusYears(1), BigDecimal(30), false)
        loggedInUser?.let { logon(it) }
        return employee
    }
}
