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

package org.projectforge.rest.hr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.vacation.model.LeaveAccountEntryDO
import org.projectforge.business.vacation.repository.LeaveAccountEntryDao
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.LeaveAccountEntry
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The leave account entries of projectforge-next ([LeaveAccountEntryEntityRest]): the preset of a new entry and the
 * check of the correction value.
 */
class LeaveAccountEntryEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var leaveAccountEntryEntityRest: LeaveAccountEntryEntityRest

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var leaveAccountEntryDao: LeaveAccountEntryDao

    /** The vacation account adds an entry for the employee it shows (`?employee=<id>`), dated today. */
    @Test
    fun `a new entry is preset with the employee given and today`() {
        logon(TEST_ADMIN_USER)
        val employee = EmployeeDO().also { it.user = getUser(TEST_USER) }
        employeeDao.insert(employee, checkAccess = false)
        logon(TEST_HR_USER)
        val request = MockHttpServletRequest().also { it.setParameter("employee", "${employee.id}") }
        val dto = leaveAccountEntryEntityRest.newBaseDTO(request)
        assertEquals(employee.id, dto.employee?.id)
        assertEquals(PFDateTime.now().localDate, dto.date)
        // Without the parameter, nobody is preset.
        assertNull(leaveAccountEntryEntityRest.newBaseDTO(MockHttpServletRequest()).employee)
    }

    /** The form posts no `created` and `lastUpdate`: an update keeps the first and sets the second itself. */
    @Test
    fun `an update keeps the creation date`() {
        logon(TEST_ADMIN_USER)
        val employee = EmployeeDO().also { it.user = getUser(TEST_USER2) }
        employeeDao.insert(employee, checkAccess = false)
        logon(TEST_HR_USER)
        val id = leaveAccountEntryDao.insert(LeaveAccountEntryDO().also {
            it.employee = employee
            it.date = LocalDate.of(2026, 6, 18)
            it.amount = BigDecimal("-5")
        })
        val stored = leaveAccountEntryDao.find(id)!!
        val created = stored.created
        assertNotNull(created)
        val lastUpdate = stored.lastUpdate!!
        val dto = LeaveAccountEntry(
            employee = Employee().also { it.id = employee.id },
            date = LocalDate.of(2026, 6, 19),
            amount = BigDecimal("-7"),
        ).also { it.id = id }
        leaveAccountEntryDao.update(leaveAccountEntryEntityRest.transformForDB(dto))
        val updated = leaveAccountEntryDao.find(id)!!
        assertEquals(created, updated.created)
        assertTrue(updated.lastUpdate!! >= lastUpdate)
        // Nothing but the dates differ: no change, nothing written.
        assertEquals(
            EntityCopyStatus.NONE,
            leaveAccountEntryDao.update(leaveAccountEntryEntityRest.transformForDB(dto)),
        )
    }

    /** A correction value has at most one decimal place. */
    @Test
    fun `a correction value with two decimal places is refused`() {
        logon(TEST_HR_USER)
        assertTrue(validate("1.5").isEmpty())
        assertTrue(validate("-2").isEmpty())
        assertEquals(listOf("amount"), validate("1.25").map { it.fieldId })
    }

    private fun validate(amount: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        leaveAccountEntryEntityRest.validate(errors, LeaveAccountEntry(amount = BigDecimal(amount)))
        return errors
    }
}
