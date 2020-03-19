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
import org.junit.jupiter.api.fail
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
        val vacation = createVacation(employee, manager, replacement, VacationStatus.IN_PROGRESS)
        val foreignVacation = createVacation(replacement, manager, manager, VacationStatus.IN_PROGRESS)
        checkAccess(employee.user, vacation, "own vacation", true, true, true, true, true)
        checkAccess(employee.user, foreignVacation, "foreign vacation", false, false, false, false, false)
/*
        Assertions.assertTrue(vacationDao.hasUserSelectAccess(employee.user, vacation, true))
        Assertions.assertFalse(vacationDao.hasUpdateAccess(employee.user, vacation, foreignVacation, false), "Update of not approved foreign vacation entries allowed.")
        Assertions.assertFalse(vacationDao.hasUpdateAccess(employee.user, foreignVacation, vacation, false), "Update of not approved foreign vacation entries allowed.")
        vacation.status = VacationStatus.APPROVED
        checkNoInsertUpdateAccess(employee.user, vacation)
        checkReadAndDeleteAccess(employee.user, vacation)
        Assertions.assertFalse(vacationDao.hasDeleteAccess(employee.user, vacation, foreignVacation, false))
        Assertions.assertFalse(vacationDao.hasDeleteAccess(employee.user, foreignVacation, vacation, false))

        val oldVacation = createVacation(employee, manager, replacement, VacationStatus.IN_PROGRESS, future = false)
        checkReadAndDeleteAccess(employee.user, oldVacation)
        checkNoInsertUpdateAccess(employee.user, oldVacation)
        oldVacation.status = VacationStatus.APPROVED
        Assertions.assertTrue(vacationDao.hasHistoryAccess(employee.user, oldVacation, true), "History access of own vacation entries allowed.")
        Assertions.assertTrue(vacationDao.hasUserSelectAccess(employee.user, oldVacation, true), "Select access of own vacation entries allowed.")
        Assertions.assertFalse(vacationDao.hasDeleteAccess(employee.user, oldVacation, oldVacation, true), "Deletion of own old approved vacations not allowed.")

        // Check full access of HR staff:
        val hrEmployee = createEmployee("VacationAccessTest.HR", true)
        checkFullAccess(hrEmployee.user, vacation)
        checkFullAccess(hrEmployee.user, foreignVacation)
        checkFullAccess(hrEmployee.user, oldVacation)*/
    }

    private fun checkAccess(user: PFUserDO?, vacation: VacationDO, msg: String, select: Boolean, insert: Boolean, update: Boolean, delete: Boolean, history: Boolean, dbVacation: VacationDO? = null) {
        if (select) {
            Assertions.assertTrue(vacationDao.hasUserSelectAccess(user, vacation, false), "Select access allowed: $msg.")
        } else {
            Assertions.assertFalse(vacationDao.hasUserSelectAccess(user, vacation, false), "Select access not allowed: $msg.")
            try {
                vacationDao.hasHistoryAccess(user, vacation, true)
                fail("Exception expected, select access not allowed: $msg.")
            } catch (ex: Exception) {
                // OK
            }
        }
        if (insert) {
            Assertions.assertTrue(vacationDao.hasInsertAccess(user, vacation, false), "Insert access allowed: $msg.")
        } else {
            Assertions.assertFalse(vacationDao.hasInsertAccess(user, vacation, false), "Insert access not allowed: $msg.")
            try {
                vacationDao.hasInsertAccess(user, vacation, true)
                fail("Exception expected, insert access not allowed: $msg.")
            } catch (ex: Exception) {
                // OK
            }
        }
        if (update) {
            Assertions.assertTrue(vacationDao.hasUpdateAccess(user, vacation, dbVacation, false), "Update access allowed: $msg.")
        } else {
            Assertions.assertFalse(vacationDao.hasUpdateAccess(user, vacation, dbVacation, false), "Update access not allowed: $msg.")
            try {
                vacationDao.hasUpdateAccess(user, vacation, dbVacation, true)
                fail("Exception expected, update access not allowed: $msg.")
            } catch (ex: Exception) {
                // OK
            }
        }
        if (delete) {
            Assertions.assertTrue(vacationDao.hasDeleteAccess(user, vacation, dbVacation, false), "Delete access allowed: $msg.")
        } else {
            Assertions.assertFalse(vacationDao.hasDeleteAccess(user, vacation, dbVacation, false), "Delete access not allowed: $msg.")
            try {
                vacationDao.hasDeleteAccess(user, vacation, dbVacation, true)
                fail("Exception expected, delete access not allowed: $msg.")
            } catch (ex: Exception) {
                // OK
            }
        }
        if (history) {
            Assertions.assertTrue(vacationDao.hasHistoryAccess(user, vacation, false), "History access allowed: $msg.")
        } else {
            Assertions.assertFalse(vacationDao.hasHistoryAccess(user, vacation, false), "History access not allowed: $msg.")
            try {
                vacationDao.hasHistoryAccess(user, vacation, true)
                fail("Exception expected, history access not allowed: $msg.")
            } catch (ex: Exception) {
                // OK
            }
        }
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
            vacation.startDate = LocalDate.now().minusDays(10)
            vacation.endDate = LocalDate.now().minusDays(2)
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
