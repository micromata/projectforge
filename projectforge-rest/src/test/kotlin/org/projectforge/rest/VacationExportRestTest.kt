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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.VacationExportRequest
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VacationExportRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var vacationExportRest: VacationExportRest

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Test
    fun `a normal user picks only from their own groups, HR and finance staff from everybody`() {
        val own = createEmployee("own")
        val colleague = createEmployee("colleague")
        val stranger = createEmployee("stranger")
        val finance = createEmployee("finance", finance = true)
        logon(TEST_ADMIN_USER)
        val team = initTestDB.addGroup("$PREFIX.team", own.user!!.username!!, colleague.user!!.username!!)
        val other = initTestDB.addGroup("$PREFIX.other", stranger.user!!.username!!)

        logon(own.user!!)
        val pickable = vacationExportRest.employees(PREFIX, null).map { it.id }
        assertTrue(pickable.containsAll(listOf(own.id, colleague.id)), "Own group.")
        assertFalse(pickable.contains(stranger.id), "Not of an own group.")
        assertEquals(listOf(team.id), vacationExportRest.groups(PREFIX, null).map { it.id })
        // Asked for anyway: dropped.
        assertEquals(
            setOf(colleague.id),
            ids(VacationExportRequest(employeeIds = listOf(colleague.id!!, stranger.id!!), groupIds = listOf(other.id!!))),
        )
        assertEquals(setOf(own.id, colleague.id), ids(VacationExportRequest(groupIds = listOf(team.id!!))))

        logon(finance.user!!)
        assertTrue(vacationExportRest.employees(PREFIX, null).map { it.id }.contains(stranger.id))
        assertEquals(setOf(team.id, other.id), vacationExportRest.groups(PREFIX, null).map { it.id }.toSet())
        assertEquals(
            setOf(own.id, colleague.id, stranger.id),
            ids(VacationExportRequest(employeeIds = listOf(stranger.id!!), groupIds = listOf(team.id!!))),
        )
    }

    @Test
    fun `the view shows the vacations of the period and stores the choice`() {
        val own = createEmployee("view-own")
        val colleague = createEmployee("view-colleague")
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("$PREFIX.view", own.user!!.username!!, colleague.user!!.username!!)
        val year = LocalDate.now().year + 2
        insertVacation(colleague, own, LocalDate.of(year, 3, 30), LocalDate.of(year, 4, 3), VacationStatus.IN_PROGRESS)
        insertVacation(colleague, own, LocalDate.of(year, 8, 3), LocalDate.of(year, 8, 4), VacationStatus.APPROVED)
        insertVacation(colleague, own, LocalDate.of(year, 4, 6), LocalDate.of(year, 4, 7), VacationStatus.REJECTED)

        logon(own.user!!)
        val request = VacationExportRequest(
            employeeIds = listOf(colleague.id!!), periodKind = "quarter",
            from = LocalDate.of(year, 4, 1), until = LocalDate.of(year, 6, 30),
        )
        val view = vacationExportRest.view(request)
        assertEquals(91, view.days.size)
        assertEquals(1, view.rows.size)
        // Only the one overlapping the quarter and neither rejected nor outside of it.
        val vacations = view.rows[0].vacations
        assertEquals(listOf(LocalDate.of(year, 3, 30)), vacations.map { it.startDate })
        assertEquals(VacationStatus.IN_PROGRESS, vacations[0].status)

        val initial = vacationExportRest.getInitial()
        assertEquals(listOf(colleague.id), initial.employees.map { it.id })
        assertEquals("quarter", initial.periodKind)
        assertFalse(initial.unrestricted)
    }

    private fun ids(request: VacationExportRequest): Set<Long?> =
        persistenceService.runReadOnly { vacationExportRest.resolveEmployees(request).map { it.id }.toSet() }

    private fun insertVacation(
        employee: EmployeeDO,
        manager: EmployeeDO,
        startDate: LocalDate,
        endDate: LocalDate,
        status: VacationStatus,
    ) {
        val loggedInUser = ThreadLocalUserContext.loggedInUser
        logon(TEST_ADMIN_USER)
        vacationDao.insert(VacationDO().also {
            it.employee = employee
            it.manager = manager
            it.replacement = manager
            it.startDate = startDate
            it.endDate = endDate
            it.status = status
            it.special = true
        }, checkAccess = false)
        loggedInUser?.let { logon(it) }
    }

    private fun createEmployee(name: String, finance: Boolean = false): EmployeeDO {
        val loggedInUser = ThreadLocalUserContext.loggedInUser
        logon(TEST_ADMIN_USER)
        val user = PFUserDO()
        user.firstname = "$PREFIX.$name"
        user.lastname = "$name.lastname"
        user.username = "$PREFIX.$name.username"
        initTestDB.addUser(user)
        if (finance) {
            val group = getGroup(ProjectForgeGroup.FINANCE_GROUP.toString())
            group.assignedUsers!!.add(user)
            groupDao.update(group)
        }
        val employee = EmployeeDO()
        employee.user = user
        employeeDao.insert(employee, checkAccess = false)
        loggedInUser?.let { logon(it) }
        return employee
    }

    companion object {
        private const val PREFIX = "VacationExportRestTest"
    }
}
