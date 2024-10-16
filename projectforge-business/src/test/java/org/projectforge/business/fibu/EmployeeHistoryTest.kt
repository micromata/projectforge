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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Test
import org.projectforge.business.employee.EmployeeTest.Companion.createEmployee
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class EmployeeHistoryTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Test
    fun testHistory() {
        logon(TEST_FULL_ACCESS_USER)
        val employee = createEmployee(employeeService, employeeDao, this, "EmployeeHistoryTest")
        val hist = createHistoryTester()
        employeeService.addNewStatus(employee, LocalDate.of(2024, 1, 1), EmployeeStatus.FEST_ANGESTELLTER)
        hist.loadRecentHistoryEntries(1)
        hist.loadHistory(employee, 2)
        hist.getEntry(0)
    }
}
