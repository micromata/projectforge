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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.employee.EmployeeTest
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * The one access to absences ([AbsenceService]): the vacations and availability entries of people by their kinds, as
 * far as the viewer may see them, and what the viewer is told of them.
 */
class AbsenceServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var absenceService: AbsenceService

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Test
    fun `the absences of the kinds asked for, rejected vacations left out`() {
        val owner = createEmployee("kinds-owner")
        val manager = createEmployee("kinds-manager")
        logon(TEST_HR_USER)
        val monday = LocalDate.now().plusYears(6).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        val vacationId = vacationDao.insert(vacation(owner, manager, monday, monday.plusDays(1), VacationStatus.APPROVED))
        vacationDao.insert(vacation(owner, manager, monday.plusDays(7), monday.plusDays(8), VacationStatus.REJECTED))
        val absentId = availabilityDao.insert(availability(owner.user!!, monday.plusDays(2), AvailabilityStatus.NOT_AVAILABLE))
        val remoteId = availabilityDao.insert(availability(owner.user!!, monday.plusDays(4), AvailabilityStatus.REMOTE_REACHABLE))
        fun absences(vararg kinds: AbsenceKind) =
            absenceService.getAbsences(monday, monday.plusDays(14), kinds.toSet(), userIds = setOf(owner.user!!.id))

        absences(*AbsenceKind.entries.toTypedArray()).let {
            assertEquals(listOf(vacationId), it.vacations.map { vacation -> vacation.id }, "Not the rejected one.")
            assertEquals(listOf(absentId, remoteId), it.availabilities.map { availability -> availability.id })
        }
        absences(AbsenceKind.VACATION).let {
            assertEquals(1, it.vacations.size)
            assertTrue(it.availabilities.isEmpty())
        }
        absences(AbsenceKind.REMOTE_REACHABLE, AbsenceKind.REACHABLE_IF_URGENT).let {
            assertTrue(it.vacations.isEmpty())
            assertEquals(listOf(remoteId), it.availabilities.map { availability -> availability.id })
        }
        absences().let { assertTrue(it.vacations.isEmpty() && it.availabilities.isEmpty()) }
    }

    @Test
    fun `per entry only what the viewer may see, pre-authorized all of them`() {
        val owner = createUser("visibility-owner")
        val colleague = createUser("visibility-colleague")
        val stranger = createUser("visibility-stranger")
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("${javaClass.simpleName}.visibility", owner.username!!, colleague.username!!)
        logon(TEST_HR_USER)
        // Ended longer ago than group mates may see it (AvailabilityDao.VISIBLE_PAST_DAYS).
        val pastStart = LocalDate.now().minusDays(AvailabilityDao.VISIBLE_PAST_DAYS + 10)
        val pastId = availabilityDao.insert(availability(owner, pastStart, AvailabilityStatus.NOT_AVAILABLE))
        val futureId = availabilityDao.insert(availability(owner, LocalDate.now().plusDays(10), AvailabilityStatus.NOT_AVAILABLE))
        fun ids(visibility: AbsenceService.Visibility) = absenceService.getAbsences(
            pastStart, LocalDate.now().plusDays(20), AbsenceKind.entries.toSet(),
            userIds = setOf(owner.id), visibility = visibility,
        ).availabilities.map { it.id }

        logon(colleague)
        assertEquals(listOf(futureId), ids(AbsenceService.Visibility.PER_ENTRY))
        assertEquals(listOf(pastId, futureId), ids(AbsenceService.Visibility.PRE_AUTHORIZED))
        logon(stranger)
        assertTrue(ids(AbsenceService.Visibility.PER_ENTRY).isEmpty(), "Not of the same group.")
        logon(owner)
        assertEquals(listOf(pastId, futureId), ids(AbsenceService.Visibility.PER_ENTRY))
    }

    @Test
    fun `the type and the internal comment for the user and HR staff only, the neutral label for everybody`() {
        val owner = createUser("label-owner")
        val colleague = createUser("label-colleague")
        logon(TEST_HR_USER)
        val id = availabilityDao.insert(availability(owner, LocalDate.now().plusYears(6), AvailabilityStatus.REACHABLE_IF_NEEDED))
        val entry = absenceService.getAbsences(
            LocalDate.now().plusYears(6), LocalDate.now().plusYears(6), AbsenceKind.entries.toSet(),
            userIds = setOf(owner.id), visibility = AbsenceService.Visibility.PRE_AUTHORIZED,
        ).availabilities.single { it.id == id }
        val neutral = translate("availability.status.reachableIfNeeded")
        assertTrue(absenceService.labelFor(owner, entry).startsWith("Workation ("))
        assertTrue(absenceService.isInternalCommentVisible(owner, entry))
        assertEquals(neutral, absenceService.labelFor(colleague, entry))
        assertFalse(absenceService.isInternalCommentVisible(colleague, entry))
        assertEquals(neutral, absenceService.labelFor(owner, entry, neutral = true))
        assertEquals(neutral, absenceService.labelFor(null, entry))
    }

    @Test
    fun `absence kinds stored as csv, null for all of them`() {
        assertNull(AbsenceKind.toCsv(null))
        assertNull(AbsenceKind.toCsv(emptyList()))
        assertNull(AbsenceKind.toCsv(AbsenceKind.entries.reversed()))
        assertEquals("VACATION,NOT_AVAILABLE", AbsenceKind.toCsv(listOf(AbsenceKind.NOT_AVAILABLE, AbsenceKind.VACATION)))
        assertNull(AbsenceKind.parse(null))
        assertNull(AbsenceKind.parse(" "))
        assertEquals(
            setOf(AbsenceKind.VACATION, AbsenceKind.NOT_AVAILABLE),
            AbsenceKind.parse("VACATION, NOT_AVAILABLE,UNKNOWN"),
        )
        assertEquals(AbsenceKind.entries.toSet(), AbsenceKind.orAll(AbsenceKind.parse(null)))
    }

    private fun availability(user: PFUserDO, startDate: LocalDate, status: AvailabilityStatus) = AvailabilityDO().also {
        it.user = user
        it.startDate = startDate
        it.endDate = startDate.plusDays(1)
        it.status = status
        it.type = "Workation"
        it.internalComment = "internal"
    }

    private fun vacation(
        employee: EmployeeDO,
        manager: EmployeeDO,
        startDate: LocalDate,
        endDate: LocalDate,
        status: VacationStatus,
    ) = VacationDO().also {
        it.employee = employee
        it.manager = manager
        it.replacement = manager
        it.startDate = startDate
        it.endDate = endDate
        it.status = status
        it.special = true // No leave days needed.
    }

    private fun createUser(name: String): PFUserDO {
        val user = PFUserDO()
        user.firstname = "$name.firstname"
        user.lastname = "$name.lastname"
        user.username = "${this.javaClass.simpleName}.$name.username"
        return initTestDB.addUser(user)
    }

    private fun createEmployee(name: String): EmployeeDO =
        EmployeeTest.createEmployee(employeeService, employeeDao, this, name, groupDao = groupDao)
}
