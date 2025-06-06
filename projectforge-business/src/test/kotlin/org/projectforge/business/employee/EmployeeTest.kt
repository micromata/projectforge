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

package org.projectforge.business.employee

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.test.HistoryTester
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class EmployeeTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    private lateinit var employeeList: List<EmployeeDO>

    @BeforeEach
    fun init() {
        logon(TEST_FULL_ACCESS_USER)
        employeeList = employeeDao.selectAll(checkAccess = false)
        Assertions.assertTrue(employeeList.isNotEmpty(), "Keine Mitarbeiter in der Test DB!")
    }

    @AfterEach
    fun clean() {
        //Get initial infos
        baseLog.info("Cleanup deleted employess -> undelete")
        baseLog.info("Count employees: " + employeeList.size)
        Assertions.assertTrue(employeeList.isNotEmpty())
        persistenceService.runInTransaction { _ ->
            for (e in employeeList) {
                baseLog.info("Employee: $e")
                if (e.deleted) {
                    //Undelete
                    employeeDao.undelete(e, checkAccess = false)
                }
            }
        }
    }

    @Test
    fun testMarkAsDeleted() {
        //Get initial infos
        baseLog.info("Count employees: " + employeeList.size)
        Assertions.assertTrue(employeeList.isNotEmpty())
        val e = employeeList[0]
        baseLog.info("Employee: $e")

        //Mark as deleted
        employeeDao.markAsDeleted(e)

        //Check updates
        val updatdEmployee = employeeDao.find(e.id)
        Assertions.assertTrue(updatdEmployee!!.deleted)
        employeeDao.update(e)

        // TODO: Test unique constraint violation
        // TODO: Insert deleted validity attr again (to be implemented)
    }

    @Test
    fun testStaffNumber() {
        Assertions.assertTrue(employeeList.isNotEmpty())
        val e = employeeList[0]
        val historyEntriesBefore = employeeDao.loadHistory(e).sortedEntries
        val staffNumber = "123abc456def"
        e.staffNumber = staffNumber
        Assertions.assertEquals(e.staffNumber, staffNumber)
        employeeDao.update(e)

        // test history
        val historyEntriesAfter = employeeDao.loadHistory(e).sortedEntries
        Assertions.assertEquals(historyEntriesBefore.size + 1, historyEntriesAfter.size)
        HistoryTester.assertHistoryAttr(historyEntriesAfter[0], "staffNumber", value = staffNumber, oldValue = null)

    }

    companion object {
        /**
         * @param email: Optional mail address of the user.
         */
        fun createEmployee(
            employeeService: EmployeeService, employeeDao: EmployeeDao, test: AbstractTestBase,
            name: String,
            hrAccess: Boolean = false,
            groupDao: GroupDao? = null,
            email: String? = null,
        ): EmployeeDO {
            val loggedInUser = ThreadLocalUserContext.loggedInUser
            test.logon(TEST_ADMIN_USER)
            val user = PFUserDO()
            val useName = "${test.javaClass.simpleName}.$name"
            user.firstname = "$name.firstname"
            user.lastname = "$name.lastname"
            user.username = "$useName.username"
            user.email = email
            if (hrAccess) {
                user.addRight(UserRightDO(UserRightId.HR_VACATION, UserRightValue.READWRITE))
            }
            test.initTestDB.addUser(user)
            if (hrAccess) {
                val group = test.getGroup(ProjectForgeGroup.HR_GROUP.toString())
                group.assignedUsers!!.add(user)
                groupDao!!.update(group)
            }
            val employee = EmployeeDO()
            employee.user = user
            employeeDao.insert(employee, checkAccess = false)
            employeeService.insertAnnualLeaveDays(employee, LocalDate.now().minusYears(2), BigDecimal(25), false)
            employeeService.insertAnnualLeaveDays(employee, LocalDate.now().minusYears(1), BigDecimal(30), false)
            loggedInUser?.let { test.logon(it) }
            return employee
        }
    }
}
