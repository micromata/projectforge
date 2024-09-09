/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
package org.projectforge.fibu

import jakarta.persistence.NoResultException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class EmployeeServiceTest : AbstractTestBase() {
    @Autowired
    private val employeeService: EmployeeService? = null

    @Test
    fun testInsertDelete() {
        logon(TEST_FULL_ACCESS_USER)
        val pfUserDO = getUser(TEST_FINANCE_USER)
        val employeeDO = EmployeeDO()
        employeeDO.accountHolder = "Horst Mustermann"
        employeeDO.abteilung = "Finance"
        employeeDO.user = pfUserDO
        Assertions.fail<Any>("TODO: Implement employeeService.save")
        val id: Int = employeeService.save(employeeDO)
        Assertions.assertTrue(id != null && id > 0)
        employeeService.delete(employeeDO)
        var employeeDO1: EmployeeDO? = null
        val exceptionList: MutableList<Exception> = ArrayList()
        try {
            employeeDO1 = employeeService.selectByPkDetached(id)
        } catch (e: NoResultException) {
            exceptionList.add(e)
        }

        Assertions.assertEquals(1, exceptionList.size)
        Assertions.assertNull(employeeDO1)
    }

    @Test
    fun testUpdateAttribute() {
        logon(TEST_FULL_ACCESS_USER)
        val pfUserDO = getUser(TEST_PROJECT_ASSISTANT_USER)
        val employeeDO = EmployeeDO()
        employeeDO.accountHolder = "Vorname Name"
        val abteilung = "Test"
        employeeDO.abteilung = abteilung
        employeeDO.user = pfUserDO
        //    employeeService.save(employeeDO);
        val expectedNewAccountHolder = "Firstname Lastname"
        //  employeeService.updateAttribute(pfUserDO.getId(), expectedNewAccountHolder, "accountHolder");
        val employeeByUserId = employeeService!!.getEmployeeByUserId(pfUserDO.id)
        Assertions.assertEquals(employeeByUserId!!.abteilung, abteilung)
        Assertions.assertEquals(employeeByUserId.accountHolder, expectedNewAccountHolder)
    }

    @get:Test
    val isEmployeeActiveWithoutAustrittsdatumTest: Unit
        get() {
            val employee = EmployeeDO()
            val result = employeeService!!.isEmployeeActive(employee)
            Assertions.assertTrue(result)
        }

    @get:Test
    val isEmployeeActiveWithAustrittsdatumTest: Unit
        get() {
            val employee = EmployeeDO()
            val dt = LocalDate.now().plusMonths(1)
            employee.austrittsDatum = dt
            val result = employeeService!!.isEmployeeActive(employee)
            Assertions.assertTrue(result)
        }

    @get:Test
    val isEmployeeActiveWithAustrittsdatumBeforeTest: Unit
        get() {
            val employee = EmployeeDO()
            val dt = LocalDate.now().minusMonths(1)
            employee.austrittsDatum = dt
            val result = employeeService!!.isEmployeeActive(employee)
            Assertions.assertFalse(result)
        }

    @get:Test
    val isEmployeeActiveWithAustrittsdatumNowTest: Unit
        get() {
            val employee = EmployeeDO()
            val dt = LocalDate.now()
            employee.austrittsDatum = dt
            val result = employeeService!!.isEmployeeActive(employee)
            Assertions.assertFalse(result)
        }
}
