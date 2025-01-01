/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.user.GroupDao
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.test.HistoryTester
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
    private lateinit var vacationDao: VacationDao

    @Autowired
    private lateinit var vacationService: VacationService

    @Test
    fun `test vacation history entries`() {
        logon(TEST_HR_USER)
        val prefix = "${this.javaClass.simpleName}-test-history"
        lateinit var employee: EmployeeDO
        lateinit var manager: EmployeeDO
        lateinit var replacement: EmployeeDO
        lateinit var other1: EmployeeDO
        lateinit var other2: EmployeeDO
        lateinit var other3: EmployeeDO
        lateinit var vacation: VacationDO
        persistenceService.runInTransaction { _ ->
            employee = createEmployee("$prefix normal")
            manager = createEmployee("$prefix manager")
            replacement = createEmployee("$prefix replacement")
            other1 = createEmployee("$prefix other1")
            other2 = createEmployee("$prefix other2")
            other3 = createEmployee("$prefix other3")
            vacation = createVacation(employee, manager, replacement, VacationStatus.IN_PROGRESS)
            vacation.otherReplacements = mutableSetOf(other1, other2)
            vacationDao.insert(vacation)
        }
        vacation = vacationDao.find(vacation.id)!!
        vacation.otherReplacements = mutableSetOf(other1, other3)
        val hist = createHistoryTester()
        vacationDao.update(vacation)
        hist.loadRecentHistoryEntries(1, 1)
        hist.recentEntries.let { entries ->
            Assertions.assertEquals(1, entries!!.size)
            val entry = entries[0]
            HistoryTester.assertHistoryAttr(
                entry,
                "otherReplacements",
                value = other3.id.toString(),
                oldValue = other2.id.toString(),
                propertyTypeClass = EmployeeDO::class,
            )
        }
        vacation.otherReplacements = mutableSetOf()
        hist.reset()
        vacationDao.update(vacation)
        hist.loadRecentHistoryEntries(1, 1)
        hist.recentEntries.let { entries ->
            Assertions.assertEquals(1, entries!!.size)
            val entry = entries[0]
            HistoryTester.assertHistoryAttr(
                entry,
                "otherReplacements",
                value = null,
                oldValue = "${other1.id},${other3.id}",
                propertyTypeClass = EmployeeDO::class,
            )
        }
    }

    @Test
    fun availableStatusValuesTest() {
        lateinit var employee: EmployeeDO
        lateinit var manager: EmployeeDO
        lateinit var replacement: EmployeeDO
        lateinit var hrEmployee: EmployeeDO
        persistenceService.runInTransaction { _ ->
            employee = createEmployee("normal2")
            manager = createEmployee("manager2")
            replacement = createEmployee("replacement2")
            hrEmployee = createEmployee("HR2", hrAccess = true)
        }
        logon(TEST_HR_USER)
        val vacation = createVacation(employee, manager, replacement, VacationStatus.IN_PROGRESS)
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED))
        assertStatusList(
            manager,
            hrEmployee,
            vacation,
            arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED, VacationStatus.APPROVED)
        )
        vacation.status = VacationStatus.APPROVED
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.APPROVED)) // Only deletable

        vacation.startDate = null
        vacation.status = VacationStatus.IN_PROGRESS
        assertStatusList(employee, hrEmployee, vacation, arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED))
        assertStatusList(
            manager,
            hrEmployee,
            vacation,
            arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED, VacationStatus.APPROVED)
        )
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
        assertStatusList(
            manager,
            hrEmployee,
            vacation,
            arrayOf(VacationStatus.APPROVED, VacationStatus.REJECTED, VacationStatus.IN_PROGRESS)
        )
    }

    @Test
    fun vacationAccessTest() {
        lateinit var employee: EmployeeDO
        lateinit var hrEmployee: EmployeeDO
        lateinit var manager: EmployeeDO
        lateinit var replacement: EmployeeDO
        lateinit var otherReplacement: EmployeeDO
        lateinit var uninvolvedEmployee: EmployeeDO
        persistenceService.runInTransaction { _ ->
            employee = createEmployee("normal")
            manager = createEmployee("manager")
            replacement = createEmployee("replacement")
            otherReplacement = createEmployee("otherReplacement")
            uninvolvedEmployee = createEmployee("uninvolvedEmployee")
            hrEmployee = createEmployee("HR", hrAccess = true)
        }
        val vacation =
            createVacation(
                employee,
                manager,
                replacement,
                VacationStatus.IN_PROGRESS,
                otherReplacement = otherReplacement
            )
        val foreignVacation = createVacation(
            replacement,
            manager,
            manager,
            VacationStatus.IN_PROGRESS,
            otherReplacement = otherReplacement
        )
        checkAccess(employee.user, vacation, "own vacation in progress", true, true, true, true, true)
        checkAccess(employee.user, foreignVacation, "foreign vacation", false, false, false, false, false)

        checkAccess(employee.user, vacation, "own vacation in progress", true, true, true, true, true)

        vacation.status = VacationStatus.APPROVED
        checkAccess(employee.user, vacation, "own approved vacation", true, false, false, true, true)

        vacation.status = VacationStatus.IN_PROGRESS
        checkAccess(
            employee.user,
            vacation,
            "changed foreign vacation",
            true,
            true,
            false,
            false,
            true,
            foreignVacation
        )

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
        checkAccess(
            manager.user,
            approvedVacation,
            "manager access not allowed for special approved vacations",
            true,
            false,
            false,
            false,
            true,
            vacation
        )

        // Check replacement users (substitutes)
        checkAccess(
            replacement.user,
            vacation,
            "access by substitute (replacement)",
            true,
            false,
            false,
            false,
            false,
            vacation
        )
        checkAccess(
            otherReplacement.user,
            vacation,
            "access by other substitute (other replacements)",
            true,
            false,
            false,
            false,
            false,
            vacation
        )
        checkAccess(uninvolvedEmployee.user, vacation, "no access", false, false, false, false, false, vacation)
    }

    private fun checkAccess(
        user: PFUserDO?,
        vacation: VacationDO,
        msg: String,
        select: Boolean,
        insert: Boolean,
        update: Boolean,
        delete: Boolean,
        history: Boolean,
        dbVacation: VacationDO? = null
    ) {
        user!!
        if (select) {
            Assertions.assertTrue(
                vacationDao.hasUserSelectAccess(user, vacation, false),
                "Select access allowed: $msg."
            )
        } else {
            Assertions.assertFalse(
                vacationDao.hasUserSelectAccess(user, vacation, false),
                "Select access not allowed: $msg."
            )
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
            Assertions.assertFalse(
                vacationDao.hasInsertAccess(user, vacation, false),
                "Insert access not allowed: $msg."
            )
            try {
                vacationDao.hasInsertAccess(user, vacation, true)
                fail("Exception expected, insert access not allowed: $msg.")
            } catch (ex: Exception) {
                // OK
            }
        }
        if (update) {
            Assertions.assertTrue(
                vacationDao.hasUpdateAccess(user, vacation, dbVacation, false),
                "Update access allowed: $msg."
            )
        } else {
            Assertions.assertFalse(
                vacationDao.hasUpdateAccess(user, vacation, dbVacation, false),
                "Update access not allowed: $msg."
            )
            try {
                vacationDao.hasUpdateAccess(user, vacation, dbVacation, true)
                fail("Exception expected, update access not allowed: $msg.")
            } catch (ex: Exception) {
                // OK
            }
        }
        if (delete) {
            Assertions.assertTrue(
                vacationDao.hasDeleteAccess(user, vacation, dbVacation, false),
                "Delete access allowed: $msg."
            )
        } else {
            Assertions.assertFalse(
                vacationDao.hasDeleteAccess(user, vacation, dbVacation, false),
                "Delete access not allowed: $msg."
            )
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
            Assertions.assertFalse(
                vacationDao.hasHistoryAccess(user, vacation, false),
                "History access not allowed: $msg."
            )
            try {
                vacationDao.hasHistoryAccess(user, vacation, true)
                fail("Exception expected, history access not allowed: $msg.")
            } catch (ex: Exception) {
                // OK
            }
        }
    }

    private fun createVacation(
        employee: EmployeeDO,
        manager: EmployeeDO,
        replacement: EmployeeDO,
        status: VacationStatus,
        future: Boolean = true,
        otherReplacement: EmployeeDO? = null,
    ): VacationDO {
        val startDate = if (future) LocalDate.now().plusDays(2) else LocalDate.now().minusDays(10)
        val endDate = if (future) LocalDate.now().plusDays(10) else LocalDate.now().minusDays(2)
        return createVacation(employee, manager, replacement, startDate, endDate, status, otherReplacement)
    }

    private fun createEmployee(name: String, hrAccess: Boolean = false): EmployeeDO {
        return EmployeeTest.createEmployee(
            employeeService,
            employeeDao,
            this,
            name,
            hrAccess,
            groupDao,
        )
    }

    private fun assertStatusList(
        employee: EmployeeDO,
        hrEmployee: EmployeeDO,
        vacation: VacationDO,
        expected: Array<VacationStatus>
    ) {
        assertStatusList(vacationDao.getAllowedStatus(employee.user!!, vacation), expected)
        assertStatusList(
            vacationDao.getAllowedStatus(hrEmployee.user!!, vacation),
            arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED, VacationStatus.APPROVED)
        )
    }

    private fun assertStatusList(values: List<VacationStatus>, expected: Array<VacationStatus>) {
        Assertions.assertEquals(
            expected.size,
            values.size,
            "Expected values: ${expected.joinToString { it.name }}, actual: ${values.joinToString { it.name }}"
        )
        expected.forEach {
            Assertions.assertTrue(values.contains(it), "Expected value $it not found.")
        }
    }

    companion object {
        fun createVacation(
            vacationer: EmployeeDO,
            manager: EmployeeDO,
            replacement: EmployeeDO,
            startDate: LocalDate,
            endDate: LocalDate,
            status: VacationStatus,
            otherReplacement: EmployeeDO? = null,
        ): VacationDO {
            val vacation = VacationDO()
            vacation.employee = vacationer
            vacation.manager = manager
            vacation.replacement = replacement
            vacation.startDate = startDate
            vacation.endDate = endDate
            vacation.status = status
            otherReplacement?.let {
                vacation.otherReplacements = mutableSetOf(it)
            }
            return vacation
        }
    }
}
