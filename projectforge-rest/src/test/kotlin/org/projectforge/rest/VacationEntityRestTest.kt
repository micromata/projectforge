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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Vacation
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import java.math.BigDecimal
import java.time.LocalDate

class VacationEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var vacationEntityRest: VacationEntityRest

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Test
    fun `a new entry is of the own employee unless HR staff presets another one`() {
        val own = createEmployee("new-own")
        val other = createEmployee("new-other")
        val hr = createEmployee("new-hr", hrAccess = true)
        val request = MockHttpServletRequest().also { it.addParameter("employee", "${other.id}") }

        logon(own.user!!)
        vacationEntityRest.newBaseDTO(request).let {
            assertEquals(own.id, it.employee?.id, "The preset of somebody else is ignored for non HR staff.")
            assertEquals(false, it.employeeEditable)
            assertEquals(VacationStatus.IN_PROGRESS, it.status)
            assertFalse(it.allowedStatus!!.contains(VacationStatus.APPROVED), "The own entry can't be approved.")
        }

        logon(hr.user!!)
        vacationEntityRest.newBaseDTO(request).let {
            assertEquals(other.id, it.employee?.id)
            assertEquals(true, it.employeeEditable)
            assertEquals(VacationStatus.entries.toSet(), it.allowedStatus!!.toSet())
        }
    }

    @Test
    fun `manager and replacement are required`() {
        logon(TEST_ADMIN_USER)
        val errors = mutableListOf<ValidationError>()
        vacationEntityRest.validate(errors, Vacation())
        assertEquals(setOf("manager", "replacement"), errors.mapNotNull { it.fieldId }.toSet())
    }

    @Test
    fun `the stats follow the form and tell the days left of the own employee only`() {
        val own = createEmployee("stats-own")
        val other = createEmployee("stats-other")
        val manager = createEmployee("stats-manager")
        val hr = createEmployee("stats-hr", hrAccess = true)
        // A week of February without holidays, far enough in the future.
        val monday = LocalDate.of(LocalDate.now().year + 3, 2, 4).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        val friday = monday.plusDays(4)
        logon(hr.user!!)
        vacationDao.insert(VacationDO().also {
            it.employee = other
            it.manager = manager
            it.replacement = own
            it.startDate = monday
            it.endDate = friday
            it.status = VacationStatus.APPROVED
        })
        val form = Vacation(
            employee = Employee(other.id),
            startDate = monday,
            endDate = friday,
            halfDayEnd = true,
            manager = Employee(manager.id),
            replacement = Employee(own.id),
        )
        val leftOfOther = vacationEntityRest.stats(PostData(form, watchFieldsTriggered = null, serverData = null)).vacationDaysLeftInYear
        // The own week with the only substitute away all of it: charted with three days around, the five uncovered.
        val chart = vacationEntityRest.stats(
            PostData(
                Vacation(
                    employee = Employee(own.id), startDate = monday, endDate = friday,
                    manager = Employee(manager.id), replacement = Employee(other.id),
                ), watchFieldsTriggered = null, serverData = null
            )
        )
        val days = chart.chartDays!!
        assertEquals(monday.minusDays(3), days.first().date)
        assertEquals(friday.plusDays(3), days.last().date)
        assertEquals(11, days.size)
        assertEquals((0L..4L).map { monday.plusDays(it) }, days.filter { it.uncovered }.map { it.date })
        assertFalse(days.first { it.date == monday.minusDays(1) }.workingDay, "Sunday")
        assertTrue(chart.conflict == true)
        assertEquals(listOf(monday), chart.conflictingVacations!!.map { it.startDate }, "Not trimmed")

        logon(own.user!!)
        val stats = vacationEntityRest.stats(PostData(form, watchFieldsTriggered = null, serverData = null))
        assertEquals(0, BigDecimal("4.5").compareTo(stats.workingDays), "Half a day less: ${stats.workingDays}")
        assertNotEquals(leftOfOther, stats.vacationDaysLeftInYear, "Days left of somebody else are not told.")
        assertTrue(stats.allowedStatus!!.isNotEmpty())
    }

    @Test
    fun `the year filter applies together with the employee filter`() {
        val own = createEmployee("year-own")
        val manager = createEmployee("year-manager")
        val hr = createEmployee("year-hr", hrAccess = true)
        logon(hr.user!!)
        val year = LocalDate.now().year + 4
        listOf(year - 1, year).forEach { y ->
            vacationDao.insert(VacationDO().also {
                it.employee = own
                it.manager = manager
                it.replacement = manager
                // A Tuesday in March: a working day, no holiday.
                val day = LocalDate.of(y, 3, 4).let { d -> d.plusDays((9L - d.dayOfWeek.value) % 7) }
                it.startDate = day
                it.endDate = day
                it.status = VacationStatus.IN_PROGRESS
                it.special = true
            })
        }
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry("employee").also { it.value.id = own.id })
        // As the client sends a text filter: wrapped in the wildcards of a LIKE search.
        filter.entries.add(MagicFilterEntry("year", "*$year*"))
        val result = vacationEntityRest.listPage(
            MockHttpServletRequest(), AbstractEntityRest.ListPageRequest(filter = filter, refresh = true)
        )
        assertEquals(listOf(year), result.resultSet.map { (it as Vacation).startDate?.year })
    }

    /** An employee with a user of its own and 30 days of annual leave; HR staff with the HR vacation right. */
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
