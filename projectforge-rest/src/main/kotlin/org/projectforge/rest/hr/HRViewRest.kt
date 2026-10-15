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

import org.projectforge.business.humanresources.HRFilter
import org.projectforge.business.humanresources.HRPlanningDao
import org.projectforge.business.humanresources.HRViewDao
import org.projectforge.business.humanresources.HRViewUserEntryData
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.HRView
import org.projectforge.rest.dto.HRViewCell
import org.projectforge.rest.dto.HRViewColumn
import org.projectforge.rest.dto.HRViewFilter
import org.projectforge.rest.dto.HRViewRow
import org.projectforge.rest.dto.HRViewUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The HR view ("Personalplanung"), successor of Wicket's `HRListPage` (`wa/hrList`, removed). A non-entity,
 * standalone page like the monthly employee report: the matrix of the planned and booked man days of the
 * employees, computed by [HRViewDao.getResources], as plain JSON for the page of projectforge-next.
 *
 * Gated by the right of the HR planning (read-only or read-write), as its menu entry; a denied request throws
 * [org.projectforge.framework.access.AccessException] (HTTP 403). Not 2FA-gated (the legacy page wasn't either).
 */
@RestController
@RequestMapping("${Rest.URL}/hrView")
class HRViewRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var hrPlanningDao: HRPlanningDao

    @Autowired
    private lateinit var hrViewDao: HRViewDao

    @Autowired
    private lateinit var userPrefService: UserPrefService

    /**
     * Every parameter left out is taken from the filter of the last visit (stored per user, as the legacy page's),
     * the period at first being the current week. The effective filter is stored again.
     *
     * @param startDay ISO date, the first day of the period.
     * @param stopDay ISO date, the last day of the period. Defaults to the end of the start day's week.
     */
    @AccessChecked("PM_HR_PLANNING right (READONLY/READWRITE) + DAO selects")
    @GetMapping
    fun getView(
        @RequestParam("startDay", required = false) startDay: String?,
        @RequestParam("stopDay", required = false) stopDay: String?,
        @RequestParam("showPlanning", required = false) showPlanning: Boolean?,
        @RequestParam("showBookedTimesheets", required = false) showBookedTimesheets: Boolean?,
        @RequestParam("onlyMyProjects", required = false) onlyMyProjects: Boolean?,
        @RequestParam("allProjectsGroupedByCustomer", required = false) allProjectsGroupedByCustomer: Boolean?,
        @RequestParam("otherProjectsGroupedByCustomer", required = false) otherProjectsGroupedByCustomer: Boolean?,
    ): HRView {
        accessChecker.hasLoggedInUserRight(
            UserRightId.PM_HR_PLANNING, true, UserRightValue.READONLY, UserRightValue.READWRITE,
        )
        val stored = userPrefService.getEntry(PREF_AREA, PREF_FILTER, HRViewFilter::class.java) ?: HRViewFilter()
        val requestedStart = parseDay(startDay)
        val start = requestedStart ?: parseDay(stored.startDay) ?: PFDay.now().beginOfWeek.localDate
        // A stored stop day only belongs to the stored start day:
        val stop = (parseDay(stopDay) ?: if (requestedStart == null) parseDay(stored.stopDay) else null)
            ?.takeIf { !it.isBefore(start) }
            ?: PFDay.from(start).endOfWeek.localDate
        val filter = HRViewFilter(
            startDay = start.toString(),
            stopDay = stop.toString(),
            showPlanning = showPlanning ?: stored.showPlanning,
            showBookedTimesheets = showBookedTimesheets ?: stored.showBookedTimesheets,
            onlyMyProjects = onlyMyProjects ?: stored.onlyMyProjects,
            allProjectsGroupedByCustomer = allProjectsGroupedByCustomer ?: stored.allProjectsGroupedByCustomer,
            otherProjectsGroupedByCustomer = otherProjectsGroupedByCustomer ?: stored.otherProjectsGroupedByCustomer,
        )
        userPrefService.putEntry(PREF_AREA, PREF_FILTER, filter, true)
        return compute(filter, start, stop)
    }

    private fun compute(filter: HRViewFilter, start: LocalDate, stop: LocalDate): HRView {
        val hrFilter = HRFilter().also {
            it.startDay = start
            it.stopDay = stop
            it.isShowPlanning = filter.showPlanning
            it.isShowBookedTimesheets = filter.showBookedTimesheets
            it.isOnlyMyProjects = filter.onlyMyProjects
            it.isAllProjectsGroupedByCustomer = filter.allProjectsGroupedByCustomer
            it.isOtherProjectsGroupedByCustomer = filter.otherProjectsGroupedByCustomer
        }
        val data = hrViewDao.getResources(hrFilter)
        val projects = data.projects
        val customers = data.customers
        val columns = projects.map { HRViewColumn(projectKey(it.id), it.projektIdentifierDisplayName, it.task?.id) } +
                customers.map { HRViewColumn(customerKey(it.nummer), it.kundeIdentifierDisplayName) }
        val rows = data.userDatas.map { userData ->
            val cells = mutableMapOf<String, HRViewCell>()
            projects.forEach { project -> userData.getEntry(project)?.let { cells[projectKey(project.id)] = cell(it) } }
            customers.forEach { customer -> userData.getEntry(customer)?.let { cells[customerKey(customer.nummer)] = cell(it) } }
            HRViewRow(
                userId = userData.userId,
                userName = userData.user.getFullname(),
                planningId = userData.planningId,
                deleted = userData.isDeleted,
                sum = HRViewCell(nonZero(userData.plannedDaysSum), nonZero(userData.actualDaysSum)),
                rest = HRViewCell(nonZero(userData.plannedDaysRestSum), nonZero(userData.actualDaysRestSum)),
                cells = cells.filterValues { it.planned != null || it.actual != null },
            )
        }
        val unplannedUsers = if (filter.onlyMyProjects) {
            emptyList()
        } else {
            hrViewDao.getUnplannedResources(data).map { HRViewUser(it.id, it.getFullname()) }
        }
        return HRView(
            filter = filter,
            calendarWeeks = calendarWeeks(start, stop),
            columns = columns,
            rows = rows,
            unplannedUsers = unplannedUsers,
            fullAccess = hrPlanningDao.hasLoggedInUserInsertAccess(null, false),
        )
    }

    private fun calendarWeeks(start: LocalDate, stop: LocalDate): String {
        val from = "%02d".format(PFDay.from(start).weekOfYear)
        val to = "%02d".format(PFDay.from(stop).weekOfYear)
        return "${translate("calendar.weekOfYearShortLabel")} $from${if (to != from) "-$to" else ""}"
    }

    private fun cell(entry: HRViewUserEntryData) = HRViewCell(nonZero(entry.plannedDays), nonZero(entry.actualDays))

    private fun nonZero(value: BigDecimal?): BigDecimal? = value?.takeIf { it.signum() != 0 }

    private fun parseDay(iso: String?): LocalDate? = iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    companion object {
        private const val PREF_AREA = "hrView"
        private const val PREF_FILTER = "filter"

        private fun projectKey(id: Long?) = "p$id"

        private fun customerKey(nummer: Long?) = "k$nummer"
    }
}
