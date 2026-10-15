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


package org.projectforge.business.availability

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.employee.EmployeeTest
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.common.i18n.UserException
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AvailabilityDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Test
    fun `period is validated on insert and update`() {
        logon(TEST_HR_USER)
        val employee = createEmployee("validate")
        val other = createEmployee("validate-other")
        val start = LocalDate.of(2030, 3, 2)
        // End before start:
        assertError(AvailabilityDao.ERROR_END_DATE_BEFORE_START_DATE) {
            availabilityDao.insert(create(employee, start, start.minusDays(1)))
        }
        val first = create(employee, start, start.plusDays(9))
        availabilityDao.insert(first)
        // Overlapping the first one (also by a single day at its end):
        assertError(AvailabilityDao.ERROR_COLLISION) {
            availabilityDao.insert(create(employee, start.plusDays(9), start.plusDays(12)))
        }
        // Adjacent periods and other employees don't collide:
        val second = create(employee, start.plusDays(10), start.plusDays(12))
        availabilityDao.insert(second)
        availabilityDao.insert(create(other, start, start.plusDays(9)))
        // An entry doesn't collide with itself on update, but with another one:
        first.endDate = start.plusDays(8)
        availabilityDao.update(first)
        first.endDate = start.plusDays(10)
        assertError(AvailabilityDao.ERROR_COLLISION) { availabilityDao.update(first) }
        // A deleted entry frees its period, and blocks its undelete if it's taken meanwhile:
        availabilityDao.markAsDeleted(second)
        first.endDate = start.plusDays(12)
        availabilityDao.update(first)
        assertError(AvailabilityDao.ERROR_COLLISION) { availabilityDao.undelete(second) }
    }

    @Test
    fun `internal comment is only visible to the employee and HR staff`() {
        logon(TEST_HR_USER)
        val employee = createEmployee("comment")
        val colleague = createEmployee("comment-colleague")
        val entry = create(employee, LocalDate.of(2031, 5, 4), LocalDate.of(2031, 5, 8))
        entry.comment = "public"
        entry.internalComment = "internal"
        availabilityDao.insert(entry)
        Assertions.assertTrue(availabilityDao.isInternalCommentVisibleFor(getUser(TEST_HR_USER), entry))
        Assertions.assertTrue(availabilityDao.isInternalCommentVisibleFor(employee.user!!, entry))
        Assertions.assertFalse(availabilityDao.isInternalCommentVisibleFor(colleague.user!!, entry))

        Assertions.assertEquals("internal", availabilityDao.find(entry.id)!!.internalComment)
        logon(employee.user!!)
        Assertions.assertEquals("internal", availabilityDao.find(entry.id)!!.internalComment)
        // A colleague (e.g. of the same group) reading it: the public comment only.
        logon(colleague.user!!)
        val copy = create(employee, entry.startDate!!, entry.endDate!!).also {
            it.comment = entry.comment
            it.internalComment = entry.internalComment
        }
        availabilityDao.afterLoad(copy)
        Assertions.assertEquals("public", copy.comment)
        Assertions.assertNull(copy.internalComment)
    }

    private fun assertError(expectedKey: String, block: () -> Unit) {
        val ex = assertThrows<UserException> { block() }
        Assertions.assertEquals(expectedKey, ex.i18nKey)
    }

    private fun create(employee: EmployeeDO, startDate: LocalDate, endDate: LocalDate): AvailabilityDO {
        val obj = AvailabilityDO()
        obj.employee = employee
        obj.startDate = startDate
        obj.endDate = endDate
        obj.type = "Workation"
        obj.status = AvailabilityStatus.REACHABLE_IF_NEEDED
        return obj
    }

    private fun createEmployee(name: String): EmployeeDO {
        return EmployeeTest.createEmployee(employeeService, employeeDao, this, name, groupDao = groupDao)
    }
}
