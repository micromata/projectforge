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

package org.projectforge.rest.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.availability.AbsenceKind
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.VacationCache
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VacationProviderTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var vacationCache: VacationCache

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Autowired
    private lateinit var absenceProvider: AbsenceProvider

    @Test
    fun `the calendar shows no rejected vacations and the comments to whom they belong only`() {
        val owner = createEmployee("owner")
        val colleague = createEmployee("colleague")
        val hr = createEmployee("hr", hrAccess = true)
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("$PREFIX.team", owner.user!!.username!!, colleague.user!!.username!!)
        val monday = LocalDate.of(LocalDate.now().year + 4, 3, 4).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        logon(hr.user!!)
        val approvedId = vacationDao.insert(
            vacation(owner, hr, monday, monday.plusDays(4), VacationStatus.APPROVED).also {
                it.comment = "$PREFIX private reason"
            }
        )
        vacationDao.insert(vacation(owner, hr, monday.plusDays(7), monday.plusDays(8), VacationStatus.REJECTED))

        // Filled on the thread of the colleague, who may not read the comment: kept for the owner nevertheless.
        logon(colleague.user!!)
        vacationCache.forceReload()
        fun events(): List<FullCalendarEvent> = mutableListOf<FullCalendarEvent>().also { events ->
            absenceProvider.addEvents(
                PFDateTime.from(monday.minusDays(1)), PFDateTime.from(monday.plusDays(14)), events,
                groupIds = null, userIds = setOf(owner.user!!.id), kinds = setOf(AbsenceKind.VACATION),
                settings = CalendarSettings(),
            )
        }
        events().let {
            assertEquals(listOf(approvedId), it.map { event -> event.extendedProps?.dbId }, "Not the rejected one.")
            assertFalse(it.single().extendedProps!!.tooltip!!.text.contains("private reason"))
        }

        logon(owner.user!!)
        assertTrue(events().single().extendedProps!!.tooltip!!.text.contains("private reason"))
    }

    private fun vacation(
        employee: EmployeeDO,
        replacement: EmployeeDO,
        startDate: LocalDate,
        endDate: LocalDate,
        status: VacationStatus,
    ) = VacationDO().also {
        it.employee = employee
        it.manager = replacement
        it.replacement = replacement
        it.startDate = startDate
        it.endDate = endDate
        it.status = status
        it.special = true
    }

    private fun createEmployee(name: String, hrAccess: Boolean = false): EmployeeDO {
        val loggedInUser = ThreadLocalUserContext.loggedInUser
        logon(TEST_ADMIN_USER)
        val user = PFUserDO()
        user.firstname = "$PREFIX.$name"
        user.lastname = "$name.lastname"
        user.username = "$PREFIX.$name.username"
        if (hrAccess) {
            user.addRight(UserRightDO(UserRightId.HR_VACATION, UserRightValue.READWRITE))
        }
        initTestDB.addUser(user)
        // The HR right is available to members of the HR group only.
        if (hrAccess) {
            val group = getGroup(ProjectForgeGroup.HR_GROUP.toString())
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
        private const val PREFIX = "VacationProviderTest"
    }
}
