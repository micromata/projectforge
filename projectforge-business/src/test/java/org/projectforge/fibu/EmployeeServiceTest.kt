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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.EmployeeValidityPeriodAttrDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class EmployeeServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Test
    fun isEmployeeActiveWithoutAustrittsdatumTest() {
        val employee = EmployeeDO()
        val result = employeeService.isEmployeeActive(employee)
        Assertions.assertTrue(result)
    }

    @Test
    fun isEmployeeActiveWithAustrittsdatumTest() {
        val employee = EmployeeDO()
        val dt = LocalDate.now().plusMonths(1)
        employee.austrittsDatum = dt
        val result = employeeService.isEmployeeActive(employee)
        Assertions.assertTrue(result)
    }

    @Test
    fun isEmployeeActiveWithAustrittsdatumBeforeTest() {
        val employee = EmployeeDO()
        val dt = LocalDate.now().minusMonths(1)
        employee.austrittsDatum = dt
        val result = employeeService.isEmployeeActive(employee)
        Assertions.assertFalse(result)
    }

    @Test
    fun isEmployeeActiveWithAustrittsdatumNowTest() {
        val employee = EmployeeDO()
        val dt = LocalDate.now()
        employee.austrittsDatum = dt
        val result = employeeService.isEmployeeActive(employee)
        Assertions.assertFalse(result)
    }

    /**
     * Test if the method returns the correct active entry.
     */
    @Test
    fun validityTest() {
        Assertions.assertNull(employeeService.getActiveEntry(emptyList()))
        Assertions.assertNull(employeeService.getActiveEntry(emptyList(), LocalDate.of(2024, Month.SEPTEMBER, 8)))
        val list = mutableListOf(createValidityEntry(0, null))
        // 0 - null
        // 1 - 2023-09-01
        // 2 - 2024-09-01
        Assertions.assertEquals(0, employeeService.getActiveEntry(list)!!.id)
        Assertions.assertEquals(0, employeeService.getActiveEntry(list, LocalDate.of(2024, Month.SEPTEMBER, 8))!!.id)
        list.add(createValidityEntry(2, LocalDate.of(2024, Month.SEPTEMBER, 1)))
        Assertions.assertEquals(2, employeeService.getActiveEntry(list, LocalDate.of(2024, Month.SEPTEMBER, 8))!!.id)
        Assertions.assertEquals(2, employeeService.getActiveEntry(list)!!.id)
        Assertions.assertEquals(0, employeeService.getActiveEntry(list, LocalDate.of(2024, Month.JANUARY, 8))!!.id)
        // Test other list order:
        list.removeAt(1)
        list.add(0, createValidityEntry(2, LocalDate.of(2024, Month.SEPTEMBER, 1))) // prepend entry
        Assertions.assertEquals(2, employeeService.getActiveEntry(list, LocalDate.of(2024, Month.SEPTEMBER, 8))!!.id)
        Assertions.assertEquals(2, employeeService.getActiveEntry(list)!!.id)
        Assertions.assertEquals(0, employeeService.getActiveEntry(list, LocalDate.of(2024, Month.JANUARY, 8))!!.id)

        list.add(createValidityEntry(1, LocalDate.of(2023, Month.SEPTEMBER, 1)))
        Assertions.assertEquals(2, employeeService.getActiveEntry(list, LocalDate.of(2024, Month.SEPTEMBER, 8))!!.id)
        Assertions.assertEquals(2, employeeService.getActiveEntry(list)!!.id)
        Assertions.assertEquals(1, employeeService.getActiveEntry(list, LocalDate.of(2024, Month.JANUARY, 8))!!.id)
        Assertions.assertEquals(0, employeeService.getActiveEntry(list, LocalDate.of(2023, Month.JANUARY, 8))!!.id)
        Assertions.assertEquals(0, employeeService.getActiveEntry(list, LocalDate.of(2022, Month.JANUARY, 8))!!.id)

        list.reverse() // Check in reverse order
        Assertions.assertEquals(2, employeeService.getActiveEntry(list, LocalDate.of(2024, Month.SEPTEMBER, 8))!!.id)
        Assertions.assertEquals(2, employeeService.getActiveEntry(list)!!.id)
        Assertions.assertEquals(1, employeeService.getActiveEntry(list, LocalDate.of(2024, Month.JANUARY, 8))!!.id)
        Assertions.assertEquals(0, employeeService.getActiveEntry(list, LocalDate.of(2023, Month.JANUARY, 8))!!.id)
        Assertions.assertEquals(0, employeeService.getActiveEntry(list, LocalDate.of(2022, Month.JANUARY, 8))!!.id)
    }

    private fun createValidityEntry(id: Int?, validFrom: LocalDate?): EmployeeValidityPeriodAttrDO {
        val entry = EmployeeValidityPeriodAttrDO()
        entry.validFrom = validFrom
        entry.id = id
        return entry
    }
}
