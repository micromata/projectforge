/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VacationDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Test
    fun vacationAccessTest() {
        val employee = createEmployee("VacationAccessTest.normal", false)
        val manager = createEmployee("VacationAccessTest.manager", false)
        val replacement = createEmployee("VacationAccessTest.replacement", false)
        logon(employee.user)
        var vacation = VacationDO()
        vacation.employee = employee
        vacation.manager = manager
        vacation.replacement = replacement
        vacation.startDate = LocalDate.now().plusDays(2)
        vacation.endDate = LocalDate.now().plusDays(3)
        Assertions.assertTrue(vacationDao.hasUserSelectAccess(employee.user, vacation, true))
        vacation.status = VacationStatus.IN_PROGRESS
        Assertions.assertTrue(vacationDao.hasInsertAccess(employee.user, vacation, true))
        vacation.status = VacationStatus.APPROVED
        Assertions.assertFalse(vacationDao.hasInsertAccess(employee.user, vacation, false))
        try {
            vacationDao.hasInsertAccess(employee.user, vacation, true)
            Assertions.fail("Exception expected")
        } catch (ex: Exception) {
            // OK
        }
        val hrEmployee = createEmployee("VacationAccessTest.HR", true)
    }

    private fun createVacation(employee: EmployeeDO, manager: EmployeeDO, replacement: EmployeeDO, status: VacationStatus, future: Boolean = true): VacationDO {
        var vacation = VacationDO()
        vacation.employee = employee
        vacation.manager = manager
        vacation.replacement = replacement
        if (future) {
            vacation.startDate = LocalDate.now().plusDays(2)
            vacation.endDate = LocalDate.now().plusDays(10)
        } else {
            vacation.startDate = LocalDate.now().plusDays(-10)
            vacation.endDate = LocalDate.now().plusDays(-2)
        }
        vacation.status = status
        return vacation
    }

    private fun createEmployee(name: String, hrAccess: Boolean): EmployeeDO {
        val user = PFUserDO()
        user.firstname = name
        user.lastname = name
        user.username = "$name.$name"
        if (hrAccess) {
            user.addRight(UserRightDO(UserRightId.HR_EMPLOYEE, UserRightValue.READWRITE))
        }
        userDao.internalSave(user)
        val employee = EmployeeDO()
        employee.user = user
        employeeDao.internalSave(employee)
        return employee
    }
}
