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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.humanresources.HRPlanningDO
import org.projectforge.business.humanresources.HRPlanningDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.HRPlanning
import org.projectforge.rest.dto.HRPlanningEntry
import org.projectforge.rest.dto.User
import org.projectforge.ui.ValidationError
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the planned weeks (`HRPlanningDO`: an employee, a week and its entries),
 * serving the edit page of the hand-built projectforge-next HR planning (see components/features/hr-planning).
 * Replaces the retired `HRPlanningPagesRest` and the removed Wicket `HRPlanningEditPage`.
 *
 * The list of the HR planning lists entries, not weeks: that is [HRPlanningEntryEntityRest].
 */
@RestController
@RequestMapping("${Rest.URL}/hrPlanning")
class HRPlanningEntityRest : AbstractDTOEntityRest<HRPlanningDO, HRPlanning, HRPlanningDao>(
    baseDaoClazz = HRPlanningDao::class.java,
    i18nKeyPrefix = "hr.planning.title",
) {
    override fun transformFromDB(obj: HRPlanningDO, editMode: Boolean): HRPlanning {
        val planning = HRPlanning()
        if (editMode) {
            planning.copyFromWithCollections(obj)
        } else {
            planning.copyFrom(obj)
        }
        return planning
    }

    /**
     * Resolves the project of every entry from the cache: `HRPlanningDao.onInsertOrModify` checks a user who is
     * no HR/finance/controlling member against the project's managers and manager group, which an id-only stub
     * doesn't carry — so every user would be refused.
     */
    override fun transformForDB(dto: HRPlanning): HRPlanningDO {
        val planning = HRPlanningDO()
        dto.copyTo(planning)
        planning.entries?.forEach { entry ->
            entry.projekt = entry.projekt?.id?.let { PfCaches.instance.getProjekt(it) }
        }
        return planning
    }

    /**
     * Presets a new planned week, as the removed Wicket edit page did: the employee of parameter `userId`
     * (the logged-in user otherwise), the week containing parameter `week` (ISO date; the current one
     * otherwise), and one empty entry to start from.
     */
    override fun newBaseDTO(request: HttpServletRequest?): HRPlanning {
        val planning = HRPlanning()
        val userId = NumberHelper.parseLong(request?.getParameter("userId")) ?: ThreadLocalUserContext.loggedInUserId
        planning.user = PfCaches.instance.getUser(userId)?.let { User(it) }
        val week = PFDayUtils.parseDate(request?.getParameter("week"))
        planning.week = PFDay.fromOrNow(week).beginOfWeek.localDate
        planning.formattedWeekOfYear = HRPlanningDO().also { it.week = planning.week }.formattedWeekOfYear
        planning.entries = mutableListOf(HRPlanningEntry())
        return planning
    }

    /**
     * The checks of the removed Wicket edit form: one planning per employee and week, and every entry
     * (deleted ones aside) is either about a project or has a status, not both and not neither.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: HRPlanning) {
        super.validate(validationErrors, dto)
        val week = dto.week?.let { PFDay.from(it).beginOfWeek.localDate }
        if (baseDao.doesEntryAlreadyExist(dto.id, dto.user?.id, week)) {
            validationErrors.add(
                ValidationError.create("hr.planning.entry.error.entryDoesAlreadyExistForUserAndWeekOfYear", "week")
            )
        }
        dto.entries?.forEachIndexed { index, entry ->
            if (entry.deleted) {
                return@forEachIndexed
            }
            val fieldId = "entries[$index].projekt"
            if (entry.projekt?.id == null && entry.status == null) {
                validationErrors.add(ValidationError.create("hr.planning.entry.error.statusOrProjektRequired", fieldId))
            } else if (entry.projekt?.id != null && entry.status != null) {
                validationErrors.add(ValidationError.create("hr.planning.entry.error.statusAndProjektNotAllowed", fieldId))
            }
        }
    }

    /**
     * The entries of the week before [week] of the given employee, as new entries (no ids, deleted ones left
     * out) — what "copy from predecessor" of a new planned week takes over. Empty if there is no such week or
     * the user may not see it.
     */
    @AccessChecked("DAO: HRPlanningDao.getEntry (select access)")
    @GetMapping("predecessor")
    fun getPredecessor(
        @RequestParam("userId") userId: Long,
        @RequestParam("week") week: String,
    ): List<HRPlanningEntry> {
        val day = PFDayUtils.parseDate(week) ?: return emptyList()
        val previousWeek = PFDay.from(day).beginOfWeek.minusDays(7).localDate
        val predecessor = baseDao.getEntry(userId, previousWeek) ?: return emptyList()
        return predecessor.entries.orEmpty().filter { !it.deleted }.map { entry ->
            HRPlanningEntry().also {
                it.copyFrom(entry.newClone())
                it.id = null
            }
        }
    }
}
