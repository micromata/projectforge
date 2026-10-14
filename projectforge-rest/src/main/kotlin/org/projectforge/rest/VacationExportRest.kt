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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.service.VacationExcelExporter
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.favorites.AbstractFavorite
import org.projectforge.favorites.Favorites
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.VacationChartDay
import org.projectforge.rest.dto.VacationExportFavoriteInfo
import org.projectforge.rest.dto.VacationExportInitial
import org.projectforge.rest.dto.VacationExportRef
import org.projectforge.rest.dto.VacationExportRequest
import org.projectforge.rest.dto.VacationExportRow
import org.projectforge.rest.dto.VacationExportSelection
import org.projectforge.rest.dto.VacationExportVacation
import org.projectforge.rest.dto.VacationExportView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

/**
 * The team vacation calendar of projectforge-next (`/next/vacationExport`), successor of the React UILayout page
 * `VacationExportPageRest` (`react/vacationExport/dynamic`, removed): the vacations (approved and in progress) of
 * the chosen employees and of the members of the chosen groups, shown for a period ([view]) and as Excel workbook
 * with a sheet per period ([exportExcel]).
 *
 * HR and finance staff ([VacationService.hasLoggedInUserUnrestrictedVacationView]) may pick any employee and group;
 * anybody else only their own groups and the employees of these groups, and nothing before 1 January of the current
 * year (no previous years). The endpoints drop whatever else is asked for, so the restriction doesn't depend on the
 * pickers ([employees], [groups]) and the page's paging.
 */
@RestController
@RequestMapping("${Rest.URL}/vacationExport")
class VacationExportRest {
    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var vacationService: VacationService

    @AccessChecked("Any user; stored employees and groups filtered by what the user may pick")
    @GetMapping("initial")
    fun getInitial(): VacationExportInitial {
        val pref = getUserPref()
        val access = Access()
        return persistenceService.runReadOnly { _ ->
            val selection = selection(pref.employeeIds, pref.groupIds, access)
            VacationExportInitial(
                employees = selection.employees,
                groups = selection.groups,
                periodKind = pref.periodKind,
                dayWidth = pref.dayWidth,
                unrestricted = access.unrestricted,
                earliestDate = access.earliestDate,
                favorites = favoriteInfos(pref),
            )
        }
    }

    /** Saves the employees and groups of [request] under [name] (a free one, if taken). */
    @AccessChecked("Own user prefs")
    @PostMapping("favorites/create")
    fun createFavorite(
        @RequestParam("name") name: String,
        @RequestBody request: VacationExportRequest,
    ): List<VacationExportFavoriteInfo> = changeFavorites { favorites ->
        favorites.add(VacationExportFavorite(name, request.employeeIds?.distinct(), request.groupIds?.distinct()))
    }

    /** Overwrites the employees and groups of the favorite [id] with those of [request]. */
    @AccessChecked("Own user prefs")
    @PostMapping("favorites/update")
    fun updateFavorite(
        @RequestParam("id") id: Long,
        @RequestBody request: VacationExportRequest,
    ): List<VacationExportFavoriteInfo> = changeFavorites { favorites ->
        favorites.get(id)?.let {
            it.employeeIds = request.employeeIds?.distinct()
            it.groupIds = request.groupIds?.distinct()
        }
    }

    @AccessChecked("Own user prefs")
    @PostMapping("favorites/rename")
    fun renameFavorite(
        @RequestParam("id") id: Long,
        @RequestParam("newName") newName: String,
    ): List<VacationExportFavoriteInfo> = changeFavorites { favorites -> favorites.rename(id, newName) }

    @AccessChecked("Own user prefs")
    @PostMapping("favorites/delete")
    fun deleteFavorite(@RequestParam("id") id: Long): List<VacationExportFavoriteInfo> =
        changeFavorites { favorites -> favorites.remove(id) }

    /**
     * The employees and groups of the favorite [id], only those the user may pick (the favorite is kept as it is:
     * a group left meanwhile is simply not applied).
     */
    @AccessChecked("Own user prefs; only employees and groups the user may pick")
    @PostMapping("favorites/select")
    fun selectFavorite(@RequestParam("id") id: Long): VacationExportSelection {
        val favorite = Favorites(getUserPref().favorites.orEmpty()).get(id)
        return persistenceService.runReadOnly { _ ->
            selection(favorite?.employeeIds, favorite?.groupIds, Access())
        }
    }

    @AccessChecked("Any user; only the active employees the user may pick (own groups unless HR/finance)")
    @GetMapping("employees")
    fun employees(
        @RequestParam("search", required = false) search: String?,
        @RequestParam("maxResults", required = false) maxResults: Int?,
    ): List<VacationExportRef> {
        val access = Access()
        val filter = BaseSearchFilter()
        filter.searchString = search
        filter.searchFields = arrayOf("user.username", "user.firstname", "user.lastname", "user.email")
        return persistenceService.runReadOnly { _ ->
            employeeDao.selectWithActiveStatus(filter, checkAccess = false, showOnlyActiveEntries = true)
                .filter { isActive(it) && access.mayPick(it) }
                .map { ref(it) }
                .sortedBy { it.displayName?.lowercase() }
                .take(maxResultsOf(maxResults))
        }
    }

    @AccessChecked("Any user; only the groups the user may pick (own groups unless HR/finance)")
    @GetMapping("groups")
    fun groups(
        @RequestParam("search", required = false) search: String?,
        @RequestParam("maxResults", required = false) maxResults: Int?,
    ): List<VacationExportRef> {
        val access = Access()
        val term = search?.trim()?.lowercase().orEmpty()
        return userGroupCache.allGroups
            .filter { it.deleted != true && it.id?.let { id -> access.mayPickGroup(id) } == true }
            .filter { term.isEmpty() || it.name?.lowercase()?.contains(term) == true }
            .mapNotNull { group -> group.id?.let { VacationExportRef(it, group.name) } }
            .sortedBy { it.displayName?.lowercase() }
            .take(maxResultsOf(maxResults))
    }

    /**
     * The calendar of the period asked for, at most [MAX_DAYS] days and not before [Access.earliestDate]. Stores the
     * choice for next time.
     */
    @AccessChecked("Any user; only employees the user may pick (own groups unless HR/finance)")
    @PostMapping("view")
    fun view(@RequestBody request: VacationExportRequest): VacationExportView {
        storeUserPref(request)
        val access = Access()
        val from = access.limit(request.from ?: PFDay.now().beginOfYear.date)
        val until = (request.until ?: from).let { if (ChronoUnit.DAYS.between(from, it) > MAX_DAYS) from.plusDays(MAX_DAYS) else it }
        if (until < from) {
            return VacationExportView(emptyList(), emptyList())
        }
        return persistenceService.runReadOnly { _ ->
            val employees = resolveEmployees(request, access)
            val vacations = if (employees.isEmpty()) emptyList() else vacationService.getVacationOfEmployees(
                employees, from, until, withSpecial = true, trimVacations = false,
                VacationStatus.APPROVED, VacationStatus.IN_PROGRESS,
            )
            val rows = vacations.map { entry ->
                VacationExportRow(ref(entry.employee), entry.vacations.map { VacationExportVacation(it) })
            }.sortedBy { it.employee.displayName?.lowercase() }
            VacationExportView(VacationChartDay.listOf(from, until), rows)
        }
    }

    /**
     * The workbook of [VacationExcelExporter], its sheets starting in the month of `from` (not before
     * [Access.earliestDate]: then no sheet reaches into a previous year).
     */
    @AccessChecked("Any user; only employees the user may pick (own groups unless HR/finance)")
    @PostMapping("exportExcel")
    fun exportExcel(@RequestBody request: VacationExportRequest): ResponseEntity<Resource> {
        storeUserPref(request)
        val access = Access()
        val startDate = access.limit(request.from ?: LocalDate.now())
        return persistenceService.runReadOnly { _ ->
            val employees = resolveEmployees(request, access)
            log.info { "Exporting Excel sheet with vacations of ${employees.size} employees (from $startDate)." }
            val periodBegin = PFDay.from(startDate).beginOfYear
            val periodEnd = periodBegin.plusYears(1).endOfYear
            val vacations = if (employees.isEmpty()) emptyList() else vacationService.getVacationOfEmployees(
                employees, periodBegin.date, periodEnd.date, withSpecial = true, trimVacations = false,
                VacationStatus.APPROVED, VacationStatus.IN_PROGRESS,
            )
            val excel = VacationExcelExporter.export(startDate, vacations)
            RestUtils.downloadFile("${translate("vacation")}-${PFDateTime.now().format4Filenames()}.xlsx", excel)
        }
    }

    /** The chosen employees and the members of the chosen groups: active ones the user may pick, each once. */
    internal fun resolveEmployees(request: VacationExportRequest, access: Access = Access()): List<EmployeeDO> {
        val employees = linkedMapOf<Long, EmployeeDO>()
        request.employeeIds.orEmpty().mapNotNull { employeeCache.getEmployee(it) }.forEach { employee ->
            employee.id?.let { employees[it] = employee }
        }
        request.groupIds.orEmpty().filter { access.mayPickGroup(it) }.forEach { groupId ->
            userGroupCache.getGroup(groupId)?.assignedUsers?.forEach { user ->
                employeeCache.getEmployeeByUserId(user.id)?.let { employee -> employee.id?.let { employees[it] = employee } }
            }
        }
        return employees.values.filter { isActive(it) && access.mayPick(it) }
    }

    /**
     * What the logged-in user may pick: everything for HR and finance staff, otherwise their own groups and the
     * employees of these groups (and themself), from 1 January of the current year on ([earliestDate]).
     */
    internal inner class Access {
        val unrestricted = vacationService.hasLoggedInUserUnrestrictedVacationView()

        /** The first day the user may see; null: no limit. */
        val earliestDate: LocalDate? = if (unrestricted) null else PFDay.now().beginOfYear.date

        /** [date], or [earliestDate] if that is later. */
        fun limit(date: LocalDate): LocalDate = earliestDate?.takeIf { it > date } ?: date

        private val loggedInUser: PFUserDO? = ThreadLocalUserContext.loggedInUser
        private val ownGroupIds: Set<Long> =
            if (unrestricted) emptySet() else userGroupCache.getUserGroups(loggedInUser)?.toSet().orEmpty()

        fun mayPickGroup(groupId: Long): Boolean = unrestricted || ownGroupIds.contains(groupId)

        fun mayPick(employee: EmployeeDO): Boolean {
            if (unrestricted) {
                return true
            }
            val user = employeeCache.getUser(employee) ?: return false
            return user.id == loggedInUser?.id || userGroupCache.getUserGroups(user).orEmpty().any { it in ownGroupIds }
        }
    }

    /** As the old page: active employees with a user neither deactivated nor deleted. */
    private fun isActive(employee: EmployeeDO): Boolean {
        val user = employeeCache.getUser(employee) ?: return false
        return employee.active && !user.deactivated && !user.deleted
    }

    /** The lookup asks for more results by raising `maxResults` (it has no offset). */
    private fun maxResultsOf(maxResults: Int?): Int = (maxResults ?: MAX_RESULTS).coerceIn(1, MAX_RESULTS_LIMIT)

    private fun ref(employee: EmployeeDO): VacationExportRef =
        VacationExportRef(employee.id!!, employeeCache.getUser(employee)?.getFullname())

    /** The employees and groups of these ids the user may pick, sorted by name. */
    private fun selection(employeeIds: List<Long>?, groupIds: List<Long>?, access: Access) = VacationExportSelection(
        employees = employeeIds.orEmpty()
            .mapNotNull { employeeCache.getEmployee(it) }
            .filter { isActive(it) && access.mayPick(it) }
            .map { ref(it) }
            .sortedBy { it.displayName?.lowercase() },
        groups = groupIds.orEmpty().filter { access.mayPickGroup(it) }.mapNotNull { groupRef(it) }
            .sortedBy { it.displayName?.lowercase() },
    )

    /** Changes the favorites (unique names and ids, see [Favorites]) and stores them. */
    private fun changeFavorites(change: (Favorites<VacationExportFavorite>) -> Unit): List<VacationExportFavoriteInfo> {
        val pref = getUserPref()
        val favorites = Favorites(pref.favorites.orEmpty())
        change(favorites)
        // The pref is the stored object (ensureEntry), changed in place as the other fields.
        pref.favorites = favorites.idTitleList.mapNotNull { favorites.get(it.id) }
        return favoriteInfos(pref)
    }

    private fun favoriteInfos(pref: VacationExportUserPref): List<VacationExportFavoriteInfo> =
        Favorites(pref.favorites.orEmpty()).idTitleList.mapNotNull { idTitle ->
            pref.favorites?.find { it.id == idTitle.id }?.let {
                VacationExportFavoriteInfo(idTitle.id, idTitle.name, it.employeeIds.orEmpty(), it.groupIds.orEmpty())
            }
        }

    private fun groupRef(groupId: Long): VacationExportRef? =
        userGroupCache.getGroup(groupId)?.takeIf { it.deleted != true }?.let { VacationExportRef(groupId, it.name) }

    private fun storeUserPref(request: VacationExportRequest) {
        val pref = getUserPref()
        pref.employeeIds = request.employeeIds?.distinct()
        pref.groupIds = request.groupIds?.distinct()
        pref.periodKind = request.periodKind
        pref.dayWidth = request.dayWidth
    }

    class VacationExportUserPref(
        var employeeIds: List<Long>? = null,
        var groupIds: List<Long>? = null,
        var periodKind: String? = null,
        var dayWidth: String? = null,
        var favorites: List<VacationExportFavorite>? = null,
    )

    /** A named choice of employees and groups. */
    class VacationExportFavorite(
        name: String? = null,
        var employeeIds: List<Long>? = null,
        var groupIds: List<Long>? = null,
    ) : AbstractFavorite(name)

    private fun getUserPref(): VacationExportUserPref =
        userPrefService.ensureEntry(USER_PREF_AREA, USER_PREF_NAME, VacationExportUserPref())

    companion object {
        private const val USER_PREF_AREA = "vacation"

        // Not "export": the removed React page stored another class there.
        private const val USER_PREF_NAME = "teamCalendar"

        private const val MAX_RESULTS = 30
        private const val MAX_RESULTS_LIMIT = 500

        /** A calendar year and some: the longest period the page asks for. */
        private const val MAX_DAYS = 400L
    }
}
