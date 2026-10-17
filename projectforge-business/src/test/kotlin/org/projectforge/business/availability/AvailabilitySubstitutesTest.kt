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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.employee.EmployeeTest
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * The optional substitutes of availability entries: stored and cached with the entry, and a conflict if all of them
 * are away on a day of it (on vacation or not available), as for the substitutes of a vacation.
 */
class AvailabilitySubstitutesTest : AbstractTestBase() {
    @Autowired
    private lateinit var availabilityCache: AvailabilityCache

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var conflictingAvailabilitiesCache: ConflictingAvailabilitiesCache

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Test
    fun `the substitutes are stored and cached, the own user is none`() {
        logon(TEST_HR_USER)
        val user = createUser("stored")
        val substitute = createUser("stored-substitute")
        val start = LocalDate.now().plusYears(4)
        assertEquals(
            AvailabilityDao.ERROR_SUBSTITUTE_SELF,
            assertThrows<UserException> {
                availabilityDao.insert(create(user, start, start, AvailabilityStatus.NOT_AVAILABLE, user))
            }.i18nKey,
        )
        val id = availabilityDao.insert(create(user, start, start.plusDays(4), AvailabilityStatus.NOT_AVAILABLE, substitute))
        assertEquals(setOf(substitute.id), availabilityCache.getEntry(id)?.substitutes?.map { it.id }?.toSet())
        // Also after a refresh, from the join table.
        availabilityCache.forceReload()
        assertEquals(setOf(substitute.id), availabilityCache.getEntry(id)?.substitutes?.map { it.id }?.toSet())
        val loaded = availabilityDao.find(id)!!
        loaded.substitutes = mutableSetOf()
        availabilityDao.update(loaded)
        assertTrue(availabilityCache.getEntry(id)?.substitutes.isNullOrEmpty(), "Removed on update.")
    }

    @Test
    fun `a conflict if all substitutes are away, by vacation or not available`() {
        val owner = createEmployee("owner")
        val onVacation = createEmployee("on-vacation")
        val absent = createUser("absent") // A freelancer: availability entries only.
        val remote = createUser("remote")
        val manager = createEmployee("manager")
        logon(TEST_HR_USER)
        val monday = LocalDate.now().plusYears(5).let { it.plusDays((8L - it.dayOfWeek.value) % 7) }
        val entry = create(owner.user!!, monday, monday.plusDays(4), AvailabilityStatus.NOT_AVAILABLE, onVacation.user!!)
        val entryId = availabilityDao.insert(entry)
        assertFalse(conflictingAvailabilitiesCache.hasConflict(entryId), "The substitute is on duty.")
        // The substitute's vacation on Tuesday and Wednesday: nobody on duty then.
        val vacationId = vacationDao.insert(vacation(onVacation, manager, monday.plusDays(1), monday.plusDays(2)))
        assertTrue(conflictingAvailabilitiesCache.hasConflict(entryId))
        assertEquals(1, conflictingAvailabilitiesCache.numberOfConflicts(owner.user!!.id))
        // A further substitute on duty covers it.
        availabilityDao.find(entryId)!!.let {
            it.substitutes = mutableSetOf(onVacation.user!!, absent)
            availabilityDao.update(it)
        }
        assertFalse(conflictingAvailabilitiesCache.hasConflict(entryId))
        // Unless not available on one of those days; a remotely reachable one counts for nothing.
        availabilityDao.insert(create(absent, monday.plusDays(2), monday.plusDays(3), AvailabilityStatus.NOT_AVAILABLE))
        assertTrue(conflictingAvailabilitiesCache.hasConflict(entryId))
        availabilityDao.find(entryId)!!.let {
            it.substitutes = mutableSetOf(onVacation.user!!, absent, remote)
            availabilityDao.update(it)
        }
        availabilityDao.insert(create(remote, monday, monday.plusDays(4), AvailabilityStatus.REMOTE_REACHABLE))
        assertFalse(conflictingAvailabilitiesCache.hasConflict(entryId), "Remotely reachable: on duty.")
        // A rejected vacation leaves the substitute on duty.
        availabilityDao.find(entryId)!!.let {
            it.substitutes = mutableSetOf(onVacation.user!!)
            availabilityDao.update(it)
        }
        assertTrue(conflictingAvailabilitiesCache.hasConflict(entryId))
        vacationDao.find(vacationId)!!.let {
            it.status = VacationStatus.REJECTED
            vacationDao.update(it)
        }
        assertFalse(conflictingAvailabilitiesCache.hasConflict(entryId))
        assertEquals(0, conflictingAvailabilitiesCache.numberOfConflicts(owner.user!!.id))
    }

    private fun create(
        user: PFUserDO,
        startDate: LocalDate,
        endDate: LocalDate,
        status: AvailabilityStatus,
        vararg substitutes: PFUserDO,
    ) = AvailabilityDO().also {
        it.user = user
        it.startDate = startDate
        it.endDate = endDate
        it.status = status
        it.substitutes = substitutes.toMutableSet().takeIf { set -> set.isNotEmpty() }
    }

    private fun vacation(employee: EmployeeDO, manager: EmployeeDO, startDate: LocalDate, endDate: LocalDate) =
        VacationDO().also {
            it.employee = employee
            it.manager = manager
            it.replacement = manager
            it.startDate = startDate
            it.endDate = endDate
            it.status = VacationStatus.APPROVED
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
