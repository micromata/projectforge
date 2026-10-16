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
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserPrefDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.rest.dto.VacationExportRequest
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VacationExportRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var vacationExportRest: VacationExportRest

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Test
    fun `a normal user picks only from their own groups, HR and finance staff from everybody`() {
        val own = createEmployee("own")
        val colleague = createEmployee("colleague")
        val stranger = createEmployee("stranger")
        val finance = createEmployee("finance", finance = true)
        logon(TEST_ADMIN_USER)
        val team = initTestDB.addGroup("$PREFIX.team", own.user!!.username!!, colleague.user!!.username!!)
        val other = initTestDB.addGroup("$PREFIX.other", stranger.user!!.username!!)

        logon(own.user!!)
        val pickable = vacationExportRest.employees(PREFIX, null).map { it.id }
        assertTrue(pickable.containsAll(listOf(own.id, colleague.id)), "Own group.")
        assertFalse(pickable.contains(stranger.id), "Not of an own group.")
        assertEquals(listOf(team.id), vacationExportRest.groups(PREFIX, null).map { it.id })
        // Asked for anyway: dropped.
        assertEquals(
            setOf(colleague.id),
            ids(VacationExportRequest(employeeIds = listOf(colleague.id!!, stranger.id!!), groupIds = listOf(other.id!!))),
        )
        assertEquals(setOf(own.id, colleague.id), ids(VacationExportRequest(groupIds = listOf(team.id!!))))

        logon(finance.user!!)
        assertTrue(vacationExportRest.employees(PREFIX, null).map { it.id }.contains(stranger.id))
        assertTrue(vacationExportRest.groups(PREFIX, null).map { it.id }.containsAll(listOf(team.id, other.id)))
        assertEquals(
            setOf(own.id, colleague.id, stranger.id),
            ids(VacationExportRequest(employeeIds = listOf(stranger.id!!), groupIds = listOf(team.id!!))),
        )
    }

    @Test
    fun `the view shows the vacations of the period and stores the choice`() {
        val own = createEmployee("view-own")
        val colleague = createEmployee("view-colleague")
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("$PREFIX.view", own.user!!.username!!, colleague.user!!.username!!)
        val year = LocalDate.now().year + 2
        insertVacation(colleague, own, LocalDate.of(year, 3, 30), LocalDate.of(year, 4, 3), VacationStatus.IN_PROGRESS)
        insertVacation(colleague, own, LocalDate.of(year, 8, 3), LocalDate.of(year, 8, 4), VacationStatus.APPROVED)
        insertVacation(colleague, own, LocalDate.of(year, 4, 6), LocalDate.of(year, 4, 7), VacationStatus.REJECTED)

        logon(own.user!!)
        val request = VacationExportRequest(
            employeeIds = listOf(colleague.id!!), periodKind = "quarter",
            from = LocalDate.of(year, 4, 1), until = LocalDate.of(year, 6, 30),
        )
        val view = vacationExportRest.view(request)
        assertEquals(91, view.days.size)
        assertEquals(1, view.rows.size)
        // Only the one overlapping the quarter and neither rejected nor outside of it, cut to the quarter.
        val vacations = view.rows[0].vacations
        assertEquals(listOf(LocalDate.of(year, 4, 1)), vacations.map { it.startDate })
        assertEquals(listOf(LocalDate.of(year, 4, 3)), vacations.map { it.endDate })
        assertEquals(VacationStatus.IN_PROGRESS, vacations[0].status)

        val initial = vacationExportRest.getInitial()
        assertEquals(listOf(colleague.id), initial.employees.map { it.id })
        assertEquals("quarter", initial.periodKind)
        assertFalse(initial.unrestricted)
    }

    @Test
    fun `availabilities are shown only if asked for, as the vacations of the same employees`() {
        val own = createEmployee("avail-own")
        val colleague = createEmployee("avail-colleague")
        val finance = createEmployee("avail-finance", finance = true)
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("$PREFIX.avail", own.user!!.username!!, colleague.user!!.username!!)
        val year = LocalDate.now().year + 2
        logon(TEST_HR_USER)
        availabilityDao.insert(AvailabilityDO().also {
            it.user = colleague.user
            it.startDate = LocalDate.of(year, 3, 25)
            it.endDate = LocalDate.of(year, 4, 10)
            it.type = "Workation"
            it.status = AvailabilityStatus.REACHABLE_IF_NEEDED
            it.comment = "public"
            it.internalComment = "internal"
        })
        val request = VacationExportRequest(
            employeeIds = listOf(colleague.id!!), from = LocalDate.of(year, 4, 1), until = LocalDate.of(year, 6, 30),
        )

        logon(own.user!!)
        assertTrue(vacationExportRest.view(request).rows.single().availabilities.isEmpty(), "Not asked for.")
        request.showAvailabilities = true
        val availability = vacationExportRest.view(request).rows.single().availabilities.single()
        // Cut to the quarter, with the public comment only (the dto has no internal one).
        assertEquals(LocalDate.of(year, 4, 1), availability.startDate)
        assertEquals(LocalDate.of(year, 4, 10), availability.endDate)
        assertEquals("public", availability.comment)
        // A colleague is told the status only, the type may be personal (AvailabilityDao.labelFor).
        assertEquals(translate("availability.status.reachableIfNeeded"), availability.label)
        assertFalse(availability.absent)
        assertTrue(vacationExportRest.getInitial().showAvailabilities, "Stored for next time.")
        logon(colleague.user!!)
        assertTrue(vacationExportRest.view(request).rows.single().availabilities.single().label.startsWith("Workation ("))
        logon(own.user!!)

        // Finance staff may pick everybody and see their availability entries as their vacations, but neither the type
        // nor the comment (not visible to them in the availability list).
        logon(finance.user!!)
        vacationExportRest.view(request).rows.single().availabilities.single().let {
            assertEquals(translate("availability.status.reachableIfNeeded"), it.label)
            assertNull(it.comment)
        }
    }

    @Test
    fun `a normal user sees nothing of previous years, HR and finance staff do`() {
        val own = createEmployee("past-own")
        val colleague = createEmployee("past-colleague")
        val finance = createEmployee("past-finance", finance = true)
        // Only HR staff may enter a vacation beginning in the past.
        val hr = createEmployee("past-hr", hrAccess = true)
        logon(TEST_ADMIN_USER)
        initTestDB.addGroup("$PREFIX.past", own.user!!.username!!, colleague.user!!.username!!)
        val year = LocalDate.now().year
        insertVacation(
            colleague, own, LocalDate.of(year - 1, 12, 28), LocalDate.of(year, 1, 2), VacationStatus.APPROVED, hr.user!!,
        )
        fun request(from: LocalDate, until: LocalDate) =
            VacationExportRequest(employeeIds = listOf(colleague.id!!), dayWidth = "fit", from = from, until = until)

        logon(own.user!!)
        val previousQuarter = vacationExportRest.view(request(LocalDate.of(year - 1, 10, 1), LocalDate.of(year - 1, 12, 31)))
        assertTrue(previousQuarter.days.isEmpty() && previousQuarter.rows.isEmpty(), "Previous year: nothing.")
        // Across the turn of the year: from 1 January on, the vacation cut off there.
        val turn = vacationExportRest.view(request(LocalDate.of(year - 1, 12, 1), LocalDate.of(year, 1, 31)))
        assertEquals(LocalDate.of(year, 1, 1), turn.days.first().date)
        assertEquals(31, turn.days.size)
        // Its days of the previous year aren't told.
        assertEquals(LocalDate.of(year, 1, 1), turn.rows.single().vacations.single().startDate)
        val initial = vacationExportRest.getInitial()
        assertEquals(LocalDate.of(year, 1, 1), initial.earliestDate)
        assertEquals("fit", initial.dayWidth)

        logon(finance.user!!)
        val financeView = vacationExportRest.view(request(LocalDate.of(year - 1, 10, 1), LocalDate.of(year - 1, 12, 31)))
        assertEquals(LocalDate.of(year - 1, 10, 1), financeView.days.first().date)
        assertEquals(1, financeView.rows.single().vacations.size)
        assertNull(vacationExportRest.getInitial().earliestDate)
    }

    @Test
    fun `favorites store a choice of employees and groups, applied as far as the user may pick it`() {
        val own = createEmployee("fav-own")
        val colleague = createEmployee("fav-colleague")
        val stranger = createEmployee("fav-stranger")
        logon(TEST_ADMIN_USER)
        val team = initTestDB.addGroup("$PREFIX.fav", own.user!!.username!!, colleague.user!!.username!!)

        logon(own.user!!)
        var favorites = vacationExportRest.createFavorite(
            "Team",
            VacationExportRequest(employeeIds = listOf(colleague.id!!, stranger.id!!), groupIds = listOf(team.id!!)),
        )
        favorites = vacationExportRest.createFavorite("Team", VacationExportRequest(employeeIds = listOf(own.id!!)))
        assertEquals(2, favorites.size)
        assertEquals(2, favorites.map { it.name }.toSet().size, "Names are unique.")
        val first = favorites.first { it.employeeIds.contains(colleague.id) }
        assertEquals(listOf(team.id), first.groupIds)
        // As selectFavorite applies it: without the stranger, so the page's choice reads as the favorite.
        assertEquals(listOf(colleague.id), first.employeeIds)
        assertEquals(favorites.map { it.id }, vacationExportRest.getInitial().favorites.map { it.id })

        // The stranger isn't of an own group: not applied.
        val selection = vacationExportRest.selectFavorite(first.id)
        assertEquals(listOf(colleague.id), selection.employees.map { it.id })
        assertEquals(listOf(team.id), selection.groups.map { it.id })

        favorites = vacationExportRest.updateFavorite(first.id, VacationExportRequest(groupIds = listOf(team.id!!)))
        assertTrue(favorites.first { it.id == first.id }.employeeIds.isEmpty())
        favorites = vacationExportRest.renameFavorite(first.id, "Renamed")
        assertEquals("Renamed", favorites.first { it.id == first.id }.name)
        favorites = vacationExportRest.deleteFavorite(first.id)
        assertEquals(1, favorites.size)

        // As the user pref is stored (JSON of the fields) and read again.
        val pref = VacationExportRest.VacationExportUserPref(
            favorites = listOf(VacationExportRest.VacationExportFavorite("Team", listOf(1L), listOf(2L)).also { it.id = 3 }),
        )
        val json = UserPrefDao.objectMapper.writeValueAsString(pref)
        val read = UserPrefDao.objectMapper.readValue(json, VacationExportRest.VacationExportUserPref::class.java)
        val favorite = read.favorites!!.single()
        assertEquals(listOf("Team", 3L, listOf(1L), listOf(2L)), listOf(favorite.name, favorite.id, favorite.employeeIds, favorite.groupIds))
    }

    private fun ids(request: VacationExportRequest): Set<Long?> =
        persistenceService.runReadOnly { vacationExportRest.resolveEmployees(request).map { it.id }.toSet() }

    private fun insertVacation(
        employee: EmployeeDO,
        manager: EmployeeDO,
        startDate: LocalDate,
        endDate: LocalDate,
        status: VacationStatus,
        asUser: PFUserDO? = null,
    ) {
        val loggedInUser = ThreadLocalUserContext.loggedInUser
        asUser?.let { logon(it) } ?: logon(TEST_ADMIN_USER)
        vacationDao.insert(VacationDO().also {
            it.employee = employee
            it.manager = manager
            it.replacement = manager
            it.startDate = startDate
            it.endDate = endDate
            it.status = status
            it.special = true
        }, checkAccess = false)
        loggedInUser?.let { logon(it) }
    }

    private fun createEmployee(name: String, finance: Boolean = false, hrAccess: Boolean = false): EmployeeDO {
        val loggedInUser = ThreadLocalUserContext.loggedInUser
        logon(TEST_ADMIN_USER)
        val user = PFUserDO()
        user.firstname = "$PREFIX.$name"
        user.lastname = "$name.lastname"
        user.username = "$PREFIX.$name.username"
        if (hrAccess) {
            user.addRight(UserRightDO(UserRightId.HR_VACATION, UserRightValue.READWRITE))
        }
        initTestDB.addUser(user)
        // The HR right is available to members of the HR group only.
        listOfNotNull(
            ProjectForgeGroup.HR_GROUP.takeIf { hrAccess },
            ProjectForgeGroup.FINANCE_GROUP.takeIf { finance },
        ).forEach {
            val group = getGroup(it.toString())
            group.assignedUsers!!.add(user)
            groupDao.update(group)
        }
        val employee = EmployeeDO()
        employee.user = user
        employeeDao.insert(employee, checkAccess = false)
        loggedInUser?.let { logon(it) }
        return employee
    }

    companion object {
        private const val PREFIX = "VacationExportRestTest"
    }
}
