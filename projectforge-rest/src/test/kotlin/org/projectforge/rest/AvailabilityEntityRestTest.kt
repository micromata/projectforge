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


package org.projectforge.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.availability.AvailabilityConflictsFilter
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.Availability
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import java.time.LocalDate

class AvailabilityEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var availabilityEntityRest: AvailabilityEntityRest

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Test
    fun `the internal comment is offered to the user and HR staff, not to colleagues`() {
        val own = createUser("own")
        val colleague = createUser("colleague")
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("$PREFIX.team", own.username!!, colleague.username!!)
        logon(TEST_HR_USER)
        val entry = AvailabilityDO().also {
            it.user = own
            it.startDate = LocalDate.of(2032, 2, 2)
            it.endDate = LocalDate.of(2032, 2, 6)
            it.type = "Removed type"
            it.status = AvailabilityStatus.NOT_AVAILABLE
            it.comment = "public"
            it.internalComment = "internal"
        }
        availabilityDao.insert(entry)

        val hrView = item(entry.id!!)
        assertEquals("internal", hrView.internalComment)
        assertTrue(hrView.internalCommentVisible == true && hrView.userEditable == true)
        // A type removed from the configuration meanwhile is still offered.
        assertTrue(hrView.availableTypes!!.contains("Removed type"))

        logon(own)
        val ownView = item(entry.id!!)
        assertEquals("internal", ownView.internalComment)
        assertTrue(ownView.internalCommentVisible == true)
        assertFalse(ownView.userEditable == true)

        logon(colleague)
        val colleagueView = item(entry.id!!)
        assertEquals("public", colleagueView.comment)
        assertNull(colleagueView.internalComment)
        assertFalse(colleagueView.internalCommentVisible == true)
    }

    @Test
    fun `a new entry is of the own user, also without an employee record, and is validated`() {
        // A freelancer: a user without an employee record.
        val own = createUser("validate")
        logon(own)
        val dto = availabilityEntityRest.getNewEntry(MockHttpServletRequest()).body as Availability
        assertEquals(own.id, dto.user?.id, "Preset with the own user.")
        assertFalse(dto.userEditable == true)
        // Only HR staff may preset another one.
        val request = MockHttpServletRequest().also { it.setParameter("user", getUser(TEST_HR_USER).id.toString()) }
        assertEquals(own.id, (availabilityEntityRest.getNewEntry(request).body as Availability).user?.id)
        dto.startDate = LocalDate.of(2032, 3, 10)
        dto.endDate = LocalDate.of(2032, 3, 9)
        assertEquals(listOf("endDate"), errors(dto).map { it.fieldId })
        dto.user = null
        assertEquals(listOf("user"), errors(dto).map { it.fieldId })
        dto.user = User().also { it.id = own.id }
        dto.endDate = dto.startDate
        assertTrue(errors(dto).isEmpty())
        dto.status = null
        assertEquals(listOf("status"), errors(dto).map { it.fieldId })
    }

    @Test
    fun `the validation tells nothing about the entries of somebody else, nor do filter and order of the list`() {
        val own = createUser("probe")
        val other = createUser("probe-other")
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("$PREFIX.probe", own.username!!, other.username!!)
        logon(TEST_HR_USER)
        val start = LocalDate.of(2033, 4, 4)
        availabilityDao.insert(AvailabilityDO().also {
            it.user = other
            it.startDate = start
            it.endDate = start.plusDays(4)
            it.type = "Sickness"
            it.status = AvailabilityStatus.NOT_AVAILABLE
            it.internalComment = "internal"
        })
        val probe = Availability().also {
            it.user = User().also { user -> user.id = other.id }
            it.startDate = start
            it.endDate = start
            it.status = AvailabilityStatus.NOT_AVAILABLE
        }
        assertEquals(listOf("endDate"), errors(probe).map { it.fieldId }, "HR staff: the collision.")
        logon(own)
        assertTrue(errors(probe).isEmpty(), "Refused by the write instead.")

        val filter = MagicFilter().also {
            it.entries.add(MagicFilterEntry("type", "Sickness"))
            it.entries.add(MagicFilterEntry("internalComment", "internal"))
            it.sortProperties.add(SortProperty("internalComment"))
            it.sortProperties.add(SortProperty("startDate"))
        }
        availabilityEntityRest.preProcessMagicFilter(QueryFilter(), filter)
        assertTrue(filter.entries.all { it.synthetic == true }, "Not applied to the query.")
        assertEquals(listOf("startDate"), filter.sortProperties.map { it.property })
    }

    @Test
    fun `the substitutes' absences in the form as the user may see them, the conflict in the list and its filter`() {
        val own = createUser("substitutes") // A freelancer: a user without an employee record.
        val substitute = createEmployee("substitute")
        val manager = createEmployee("manager")
        val monday = LocalDate.now().plusYears(6).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        logon(TEST_HR_USER)
        vacationDao.insert(VacationDO().also {
            it.employee = substitute
            it.manager = manager
            it.replacement = manager
            it.startDate = monday.plusDays(1)
            it.endDate = monday.plusDays(2)
            it.status = VacationStatus.APPROVED
            it.special = true
            it.comment = "private"
        })
        logon(own)
        val dto = Availability().also {
            it.user = User().also { user -> user.id = own.id }
            it.startDate = monday
            it.endDate = monday.plusDays(4)
            it.status = AvailabilityStatus.NOT_AVAILABLE
            it.substitutes = listOf(User().also { user -> user.id = own.id })
        }
        assertEquals(listOf("substitutes"), errors(dto).map { it.fieldId }, "Not the own substitute.")
        dto.substitutes = listOf(User().also { it.id = substitute.user!!.id })
        // The posted user is ignored: the stats are of the own entry.
        dto.user = User().also { it.id = getUser(TEST_HR_USER).id }
        val stats = availabilityEntityRest.stats(PostData(dto, null, null))
        assertTrue(stats.conflict == true)
        // Not readable for the freelancer (no group shared): its days and status only.
        val vacation = stats.substituteVacations!!.single()
        assertNull(vacation.id)
        assertNull(vacation.comment)
        assertEquals(monday.plusDays(1), vacation.startDate)
        assertEquals(substitute.user!!.id, vacation.employee?.user?.id, "Keyed by the substitute's user.")
        assertEquals(2, stats.chartDays!!.count { it.uncovered })

        dto.user = User().also { it.id = own.id }
        val entry = AvailabilityDO()
        dto.copyTo(entry)
        val id = availabilityDao.insert(entry)
        val item = item(id)
        assertTrue(item.conflict == true, "The cache flags it.")
        assertEquals(substitute.user!!.id, item.substitutes?.single()?.id)
        assertEquals(1, item.substituteVacations?.size)

        val filter = MagicFilter().also { it.entries.add(MagicFilterEntry("conflicts", "true")) }
        val filters = availabilityEntityRest.preProcessMagicFilter(QueryFilter(), filter)
        assertTrue(filters.any { it is AvailabilityConflictsFilter })
    }

    private fun errors(dto: Availability): List<ValidationError> =
        mutableListOf<ValidationError>().also { availabilityEntityRest.validate(it, dto) }

    private fun item(id: Long): Availability = availabilityEntityRest.getItem(id).body as Availability

    private fun createEmployee(name: String): EmployeeDO {
        logon(TEST_ADMIN_USER)
        val employee = EmployeeDO()
        employee.user = createUser(name)
        employeeDao.insert(employee, checkAccess = false)
        return employee
    }

    private fun createUser(name: String): PFUserDO {
        val user = PFUserDO()
        user.firstname = "$PREFIX.$name"
        user.lastname = "$name.lastname"
        user.username = "$PREFIX.$name.username"
        return initTestDB.addUser(user)
    }

    companion object {
        private const val PREFIX = "AvailabilityEntityRestTest"
    }
}
