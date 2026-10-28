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

package org.projectforge.rest.calendar

import org.projectforge.business.availability.AbsenceService
import org.projectforge.business.calendar.CalendarStyle
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Builds the calendar events of vacations, as [AbsenceProvider] got them ([AbsenceService]).
 */
@Component
open class VacationProvider {
    @Autowired
    private lateinit var absenceService: AbsenceService

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var vacationService: VacationService

    /**
     * Adds an event for each of the given vacations, but not twice for the same person and period (e.g. of two team
     * calendars).
     * @param startHidden A vacation shown from its start date on only, its real begin isn't told
     * ([AbsenceService.Absences.isStartHidden]).
     */
    open fun addEvents(
        vacations: List<VacationDO>,
        events: MutableList<FullCalendarEvent>,
        settings: CalendarSettings,
        style: CalendarStyle? = null,
        startHidden: (VacationDO) -> Boolean = { false },
    ) {
        val useStyle = style ?: CalendarStyle(settings.vacationsColorOrDefault)
        val viewer = ThreadLocalUserContext.loggedInUser
        vacations.forEach { vacation ->
            val employeeUser = absenceService.userOf(vacation)
            val title = "${translate("vacation")}: ${employeeUser?.getFullname()}"
            if (!events.any { it.title == title && FullCalendarEvent.samePeriod(it, vacation.startDate, vacation.endDate) }) {
                val duration = VacationService.getVacationDays(vacation)
                val unit = if (duration == BigDecimal.ONE) "fibu.common.workingDay" else "fibu.common.workingDays"
                // Event doesn't yet exist:
                val event = FullCalendarEvent.createAllDayEvent(
                    id = vacation.id,
                    category = FullCalendarEvent.Category.VACATION,
                    title = title,
                    start = vacation.startDate!!,
                    end = vacation.endDate!!,
                    style = useStyle,
                    dbId = vacation.id,
                    classNames = "vacation-event",
                    formattedDuration = "$duration ${translate(unit)}",
                    calendarSettings = settings,
                )
                val tb = TooltipBuilder()
                    .addPropRow(translate("timePeriod"), periodText(vacation.startDate, vacation.endDate, startHidden(vacation)))
                    .addPropRow(
                        translate("vacation.replacement"),
                        vacationService.collectAllReplacements(vacation).joinToString { employeeCache.getUser(it)?.displayName ?: "???" })
                // The cached comment is kept raw (VacationDao.afterLoad): the comment is personal data, only visible
                // to the owner and to users with update access (HR/manager).
                if (absenceService.isCommentVisible(viewer, vacation)) {
                    tb.addPropRow(translate("comment"), vacation.comment, abbreviate = true)
                }
                event.setTooltip(title, tb)
                events.add(event)
            }
        }
    }

    companion object {
        /** "from - until", or "until ..." if the begin isn't told (an entry of somebody else shown from today on). */
        internal fun periodText(startDate: LocalDate?, endDate: LocalDate?, startHidden: Boolean): String {
            val end = PFDay.fromOrNull(endDate)?.format() ?: ""
            if (startHidden) {
                return translateMsg("absence.until", end)
            }
            val start = PFDay.fromOrNull(startDate)?.format() ?: ""
            return "$start - $end"
        }
    }
}
