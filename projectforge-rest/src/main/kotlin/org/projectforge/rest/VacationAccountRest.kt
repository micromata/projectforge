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

import org.projectforge.Constants
import org.projectforge.business.availability.AbsenceService
import org.projectforge.business.availability.AvailabilityCache
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.availability.ConflictingAvailabilitiesCache
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.vacation.VacationCache
import org.projectforge.business.vacation.repository.LeaveAccountEntryDao
import org.projectforge.business.vacation.repository.RemainingLeaveDao
import org.projectforge.business.vacation.service.ConflictingVacationsCache
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStatsFormatted
import org.projectforge.common.DateFormatType
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Availability
import org.projectforge.rest.dto.LeaveAccountEntry
import org.projectforge.rest.dto.Vacation
import org.projectforge.rest.dto.VacationAccount
import org.projectforge.rest.dto.VacationAccountAvailability
import org.projectforge.rest.dto.VacationAccountEmployee
import org.projectforge.rest.dto.VacationAccountLeaveEntry
import org.projectforge.rest.dto.VacationAccountVacation
import org.projectforge.rest.dto.VacationAccountYear
import org.projectforge.rest.hr.LeaveAccountEntryPagesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.Month
import java.time.Year

/**
 * The vacation account ("Urlaubskonto") of projectforge-next, successor of the React UILayout page
 * `VacationAccountPageRest` (`react/vacationAccount/dynamic`, removed). A non-entity, standalone page like the HR
 * view: the statistics, vacations, leave account entries and availability entries of one employee as plain JSON
 * ([VacationAccount]).
 *
 * HR staff ([VacationService.hasLoggedInUserHRVacationAccess]) may show any employee — the one asked for, else the
 * one shown last (stored per user), else their own; everybody else always sees their own. Not 2FA-gated (the
 * legacy page wasn't either).
 */
@RestController
@RequestMapping("${Rest.URL}/vacationAccount")
class VacationAccountRest {
    @Autowired
    private lateinit var absenceService: AbsenceService

    @Autowired
    private lateinit var availabilityCache: AvailabilityCache

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var conflictingAvailabilitiesCache: ConflictingAvailabilitiesCache

    @Autowired
    private lateinit var conflictingVacationsCache: ConflictingVacationsCache

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var leaveAccountEntryDao: LeaveAccountEntryDao

    @Autowired
    private lateinit var remainingLeaveDao: RemainingLeaveDao

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var vacationCache: VacationCache

    @Autowired
    private lateinit var vacationService: VacationService

    /**
     * @param employeeId The employee to show; HR staff only (ignored for anybody else). Stored as the one to show
     * next time.
     */
    @AccessChecked("Own employee only; another employee (employeeId): HR vacation access")
    @GetMapping
    fun getAccount(@RequestParam("employeeId", required = false) employeeId: Long?): VacationAccount {
        val hrAccess = vacationService.hasLoggedInUserHRVacationAccess()
        val ownEmployeeId = ThreadLocalUserContext.userContext?.employeeId
        val id = if (hrAccess) {
            employeeId?.also { getUserPref().employeeId = it } ?: getUserPref().employeeId ?: ownEmployeeId
        } else {
            ownEmployeeId
        }
        // A stored employee deleted meanwhile falls back to the own one.
        val employee = id?.let { employeeDao.find(it, checkAccess = false) }
            ?: ownEmployeeId?.takeIf { it != id }?.let { employeeDao.find(it, checkAccess = false) }
        return build(employee, hrAccess)
    }

    /**
     * Drops the stored carry of the current year, so it is computed anew from what was left of the previous one.
     */
    @AccessChecked("HR vacation access")
    @PostMapping("recalculate")
    fun recalculateRemainingLeave(@RequestParam("employeeId") employeeId: Long): VacationAccount {
        if (!vacationService.hasLoggedInUserHRVacationAccess()) {
            throw AccessException("access.exception.userHasNotRight")
        }
        remainingLeaveDao.markAsDeleted(employeeId, Year.now().value, checkAccess = false)
        return getAccount(employeeId)
    }

    private fun build(employee: EmployeeDO?, hrAccess: Boolean): VacationAccount {
        val vacationListUrl = PagesResolver.getListPageUrl(VacationEntityRest::class.java, absolute = true)
        val exportUrl = "/${Constants.NEXT_APP_PATH}vacationExport"
        val employeeId = employee?.id
            ?: return VacationAccount(null, hrAccess, vacationListUrl = vacationListUrl, exportUrl = exportUrl)
        val year = Year.now().value
        val prevStats = vacationService.getVacationStats(employee, year - 1)
        val currentStats = vacationService.getVacationStats(employee, year)
        val nextStats = vacationService.getVacationStats(employee, year + 1)
        val endOfCarry = vacationService.getEndOfCarryVacationOfPreviousYear(year)
        val endOfCarryString = PFDateTimeUtils.ensureUsersDateTimeFormat(DateFormatType.DATE_WITHOUT_YEAR).format(endOfCarry)
        val leaveEntries = leaveAccountEntryDao.getList(
            employeeId,
            PFDay.of(year - 1, Month.JANUARY, 1).localDate,
            PFDay.of(year, Month.JANUARY, 1).endOfYear.localDate,
        ).orEmpty().map { LeaveAccountEntry(it) }.sortedByDescending { it.date }
        return VacationAccount(
            employee = VacationAccountEmployee(employeeId, employee.displayName),
            hrAccess = hrAccess,
            canRecalculate = hrAccess &&
                    !NumberHelper.isEqual(currentStats.remainingLeaveFromPreviousYear, prevStats.vacationDaysLeftInYear),
            statistics = listOf(prevStats, currentStats, nextStats).map { VacationStatsFormatted(it) },
            previousYearLeaveUnusedLabel = translateMsg("vacation.previousyearleaveunused", endOfCarryString),
            years = listOf(year + 1, year, year - 1).map { VacationAccountYear(it, readVacations(employeeId, it)) },
            leaveAccountEntries = leaveEntries.map {
                VacationAccountLeaveEntry(
                    id = it.id,
                    dateFormatted = it.dateFormatted,
                    amountFormatted = it.amountFormatted,
                    description = it.description,
                    editUrl = PagesResolver.getEditPageUrl(
                        LeaveAccountEntryPagesRest::class.java, it.id, absolute = true, returnToCaller = RETURN_TO_CALLER,
                    ),
                )
            },
            // Only of the employee's own user: an employee without one has no time sheets to average.
            workingHoursStatistics = employee.user?.let {
                vacationService.getAverageWorkingTimeStats(it, PFDay.fromOrNull(employee.eintrittsDatum)).localizedMessage
            },
            newVacationUrl = vacationEditUrl(null, mapOf("employee" to employeeId)),
            availabilities = employee.user?.id?.let { readAvailabilities(it, year) }.orEmpty(),
            newAvailabilityUrl = employee.user?.id?.let { availabilityEditUrl(null, mapOf("user" to it)) },
            vacationListUrl = vacationListUrl,
            exportUrl = exportUrl,
        )
    }

    private fun readVacations(employeeId: Long, year: Int): List<VacationAccountVacation> {
        val begin = PFDay.of(year, Month.JANUARY, 1)
        val vacations =
            vacationService.getVacationsListForPeriod(employeeId, begin.localDate, begin.endOfYear.localDate, true, true)
                // Employees from the caches, the other replacements of all entries in one query (no N+1).
                .also { vacationCache.hydrate(it) }
        // The days bar's red days, of all flagged entries of the year at once (as the vacation list does).
        val uncovered = vacations
            .filter { conflictingVacationsCache.hasConflict(it.id) && VacationEntityRest.inBarRange(it) }
            .takeIf { it.isNotEmpty() }
            ?.let { vacationService.getUncoveredDays(it) }
            .orEmpty()
        return vacations
            .map { obj ->
                val vacation = Vacation(obj)
                VacationAccountVacation(
                    id = vacation.id,
                    startDateFormatted = vacation.startDateFormatted,
                    endDateFormatted = vacation.endDateFormatted,
                    statusString = vacation.statusString,
                    workingDaysFormatted = vacation.workingDaysFormatted,
                    specialFormatted = vacation.specialFormatted,
                    replacement = vacation.replacement?.displayName,
                    otherReplacements = vacation.otherReplacementsAsString,
                    manager = vacation.manager?.displayName,
                    vacationModeString = vacation.vacationModeString,
                    comment = vacation.comment,
                    conflict = conflictingVacationsCache.hasConflict(vacation.id),
                    startDate = vacation.startDate,
                    endDate = vacation.endDate,
                    halfDayBegin = vacation.halfDayBegin,
                    halfDayEnd = vacation.halfDayEnd,
                    nonWorkingDays = VacationEntityRest.barNonWorkingDays(obj),
                    uncoveredDays = uncovered[vacation.id],
                    editUrl = vacationEditUrl(vacation.id),
                )
            }
    }

    /**
     * The availability entries of the user overlapping the previous, current and next year, newest first. The account
     * is the own one or shown to HR staff, who see the entries in full (AbsenceService.shownTo); the cached entries
     * have their users and substitutes resolved.
     */
    private fun readAvailabilities(userId: Long, year: Int): List<VacationAccountAvailability> {
        val viewer = ThreadLocalUserContext.requiredLoggedInUser
        val from = PFDay.of(year - 1, Month.JANUARY, 1).localDate
        val until = PFDay.of(year + 1, Month.JANUARY, 1).endOfYear.localDate
        val entries = availabilityCache.getEntriesOfUsers(setOf(userId), from, until)
            .mapNotNull { absenceService.shownTo(viewer, it) }
            .sortedByDescending { it.startDate }
        // The days bar's red days, of all flagged entries at once (as the availability list does): from today on only
        // for anybody but HR staff, as the conflict flag counts them.
        val today = if (vacationService.hasLoggedInUserHRVacationAccess()) null else LocalDate.now()
        val uncovered = entries
            .filter {
                conflictingAvailabilitiesCache.hasConflict(it.id) &&
                        SubstituteAbsenceReducer.inChartRange(it.startDate, it.endDate)
            }
            .takeIf { it.isNotEmpty() }
            ?.let { vacationService.getUncoveredDaysOfAvailabilities(it) }
            .orEmpty()
            .mapValues { (_, days) -> if (today == null) days else days.filter { it >= today } }
        return entries
            .map { entry ->
                val dto = Availability().also { it.copyFrom(entry) }
                VacationAccountAvailability(
                    id = entry.id,
                    startDateFormatted = PFDayUtils.format(entry.startDate, DateFormatType.DATE),
                    endDateFormatted = PFDayUtils.format(entry.endDate, DateFormatType.DATE),
                    type = entry.type?.takeIf { availabilityDao.isInternalCommentVisibleFor(viewer, entry) },
                    statusString = entry.status?.let { translate(it.i18nKey) },
                    locationString = entry.location?.let { translate(it.i18nKey) },
                    comment = entry.comment,
                    substitutes = dto.substitutesAsString,
                    conflict = conflictingAvailabilitiesCache.hasConflict(entry.id),
                    workingDaysFormatted = dto.workingDaysFormatted,
                    startDate = entry.startDate,
                    endDate = entry.endDate,
                    nonWorkingDays = SubstituteAbsenceReducer.barNonWorkingDays(entry.startDate, entry.endDate),
                    uncoveredDays = uncovered[entry.id],
                    editUrl = availabilityEditUrl(entry.id),
                )
            }
    }

    /** The form of projectforge-next, which offers the way back here (`AVAILABILITY_PAGE.returnTargets`). */
    private fun availabilityEditUrl(id: Long?, params: Map<String, Any?> = emptyMap()): String =
        PagesResolver.getEditPageUrl(
            AvailabilityEntityRest::class.java, id, params = params + ("returnTo" to RETURN_ROUTE), absolute = true,
        )

    /** The form of projectforge-next, which offers the way back here (`VACATION_PAGE.returnTargets`). */
    private fun vacationEditUrl(id: Long?, params: Map<String, Any?> = emptyMap()): String =
        PagesResolver.getEditPageUrl(
            VacationEntityRest::class.java, id, params = params + ("returnTo" to RETURN_ROUTE), absolute = true,
        )

    class VacationAccountUserPref(var employeeId: Long? = null)

    private fun getUserPref(): VacationAccountUserPref =
        userPrefService.ensureEntry("vacation", "account", VacationAccountUserPref())

    companion object {
        /** The route of this page in projectforge-next, as a `?returnTo=` of the vacation form names it. */
        const val RETURN_ROUTE = "/vacationAccount"

        /** The `returnToCaller` of the (React) leave account entry form, see [LeaveAccountEntryPagesRest]. */
        const val RETURN_TO_CALLER = "account"
    }
}
