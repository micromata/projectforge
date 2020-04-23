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
import org.projectforge.business.employee.EmployeeTest
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VacationDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Autowired
    private lateinit var vacationService: VacationService

    @Test
    fun availableStatusValuesTest() {
        val employee = createEmployee("normal2")
        val manager = createEmployee("manager2")
        val replacement = createEmployee("replacement2")
        val hrEmployee = createEmployee("HR2", hrAccess = true)

        val vacation = createVacation(employee, manager, replacement, VacationStatus.IN_PROGRESS)
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED))
        assertStatusList(manager, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED, VacationStatus.APPROVED))
        vacation.status = VacationStatus.APPROVED
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.APPROVED)) // Only deletable

        vacation.startDate = null
        vacation.status = VacationStatus.IN_PROGRESS
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED))
        assertStatusList(manager, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED, VacationStatus.APPROVED))
        vacation.status = VacationStatus.APPROVED
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.APPROVED)) // Only deletable

        vacation.startDate = LocalDate.now().minusDays(100) // Vacation in past
        vacation.status = VacationStatus.IN_PROGRESS
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS))
        assertStatusList(manager, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.APPROVED))
        vacation.status = VacationStatus.REJECTED
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.REJECTED))
        assertStatusList(manager, hrEmployee, vacation, arrayOf(VacationStatus.REJECTED, VacationStatus.APPROVED))
        vacation.status = VacationStatus.APPROVED
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.APPROVED))
        assertStatusList(manager, hrEmployee, vacation, arrayOf(VacationStatus.APPROVED))

        // special vacation
        vacation.startDate = LocalDate.now().plusDays(100) // Vacation in future
        vacation.special = true
        vacation.status = VacationStatus.IN_PROGRESS
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED))
        assertStatusList(manager, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED))
        vacation.status = VacationStatus.REJECTED
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED))
        assertStatusList(manager, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED))
        vacation.status = VacationStatus.APPROVED
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.APPROVED))
        assertStatusList(manager, hrEmployee, vacation, arrayOf(VacationStatus.APPROVED, VacationStatus.REJECTED, VacationStatus.IN_PROGRESS))
    }

    @Test
    fun vacationAccessTest() {
        val employee = createEmployee("normal")
        val manager = createEmployee("manager")
        val replacement = createEmployee("replacement")
        val vacation = createVacation(employee, manager, replacement, VacationStatus.IN_PROGRESS)
        val foreignVacation = createVacation(replacement, manager, manager, VacationStatus.IN_PROGRESS)
        checkAccess(employee.user, vacation, "own vacation in progress", true, true, true, true, true)
        checkAccess(employee.user, foreignVacation, "foreign vacation", false, false, false, false, false)

        checkAccess(employee.user, vacation, "changed foreign vacation", true, true, false, false, true, foreignVacation)

        vacation.status = VacationStatus.APPROVED
        checkAccess(employee.user, vacation, "own approved vacation", true, false, false, true, true)

        vacation.status = VacationStatus.IN_PROGRESS
        checkAccess(employee.user, vacation, "changed foreign vacation", true, true, false, false, true, foreignVacation)

        val pastVacation = createVacation(employee, manager, replacement, VacationStatus.IN_PROGRESS, future = false)
        checkAccess(employee.user, pastVacation, "own past vacation in progress", true, false, false, true, true)

        pastVacation.status = VacationStatus.APPROVED
        checkAccess(employee.user, pastVacation, "own past approved vacation", true, false, false, false, true)

        // Check self approve
        vacation.manager = employee
        val error = VacationValidator.validate(vacationService, vacation, vacation, false)
        Assertions.assertNotNull(error)
        Assertions.assertEquals(VacationValidator.Error.NOT_ALLOWED_TO_APPROVE.messageKey, error!!.messageKey)
        vacation.manager = manager
        Assertions.assertNull(VacationValidator.validate(vacationService, vacation, vacation, false))

        // Check full access of HR staff:
        val hrEmployee = createEmployee("HR", hrAccess = true)
        checkAccess(hrEmployee.user, vacation, "hr access", true, true, true, true, true)
        checkAccess(hrEmployee.user, foreignVacation, "hr access", true, true, true, true, true)
        checkAccess(hrEmployee.user, pastVacation, "hr access", true, true, true, true, true)

        // Check manager access:
        val approvedVacation = createVacation(employee, manager, replacement, VacationStatus.IN_PROGRESS)
        checkAccess(manager.user, approvedVacation, "manager access", true, false, true, false, true, vacation)

        approvedVacation.startDate = approvedVacation.startDate!!.plusDays(1)
        checkAccess(manager.user, approvedVacation, "manager access", true, false, false, false, true, vacation)

        approvedVacation.startDate = approvedVacation.endDate!!.plusDays(1)
        checkAccess(manager.user, approvedVacation, "manager access", true, false, false, false, true, vacation)

        approvedVacation.startDate = approvedVacation.startDate!!.minusDays(1)
        checkAccess(manager.user, approvedVacation, "manager access", true, false, false, false, true, vacation)

        approvedVacation.startDate = approvedVacation.endDate!!.minusDays(1)
        checkAccess(manager.user, approvedVacation, "manager access", true, false, false, false, true, vacation)

        approvedVacation.special = true
        checkAccess(manager.user, approvedVacation, "manager access not allowed for special approved vacations", true, false, false, false, true, vacation)
    }

    private fun checkAccess(user: PFUserDO?, vacation: VacationDO, msg: String, select: Boolean, insert: Boolean, update: Boolean, delete: Boolean, history: Boolean, dbVacation: VacationDO? = null) {
        user!!
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
        val startDate = if (future) LocalDate.now().plusDays(2) else LocalDate.now().minusDays(10)
        val endDate = if (future) LocalDate.now().plusDays(10) else LocalDate.now().minusDays(2)
        return createVacation(employee, manager, replacement, startDate, endDate, status)
    }

    private fun createEmployee(name: String, hrAccess: Boolean = false): EmployeeDO {
        return EmployeeTest.createEmployee(employeeService, employeeDao, this, name, hrAccess, groupDao)
    }

    private fun assertStatusList(employee: EmployeeDO, hrEmployee: EmployeeDO, vacation: VacationDO, expected: Array<VacationStatus>) {
        assertStatusList(vacationDao.getAllowedStatus(employee.user!!, vacation), expected)
        assertStatusList(vacationDao.getAllowedStatus(hrEmployee.user!!, vacation), arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED, VacationStatus.APPROVED))
    }

    private fun assertStatusList(values: List<VacationStatus>, expected: Array<VacationStatus>) {
        Assertions.assertEquals(expected.size, values.size, "Expected values: ${expected.joinToString { it.name }}, actual: ${values.joinToString { it.name }}")
        expected.forEach {
            Assertions.assertTrue(values.contains(it), "Expected value $it not found.")
        }
    }

    companion object {
        fun createVacation(vacationer: EmployeeDO, manager: EmployeeDO, replacement: EmployeeDO, startDate: LocalDate, endDate: LocalDate, status: VacationStatus): VacationDO {
            val vacation = VacationDO()
            vacation.employee = vacationer
            vacation.manager = manager
            vacation.replacement = replacement
            vacation.startDate = startDate
            vacation.endDate = endDate
            vacation.status = status
            return vacation
        }
    }
}
