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
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.calendar.CalendarStyle
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Builds the calendar events of availability entries of users (employees and freelancers), as [AbsenceProvider] got
 * them ([AbsenceService]).
 */
@Component
open class AvailabilityProvider {
    @Autowired
    private lateinit var absenceService: AbsenceService

    /**
     * Adds an event for each of the given entries, but not twice for the same title and period (e.g. of two team
     * calendars).
     */
    open fun addEvents(
        availabilities: List<AvailabilityDO>,
        events: MutableList<FullCalendarEvent>,
        settings: CalendarSettings,
        /** The style of absent entries ([AvailabilityStatus.absent]), drawn like vacations. */
        absentStyle: CalendarStyle = settings.availabilityStyle,
        otherStyle: CalendarStyle = settings.availabilityStyle,
        /** An entry shown from its start date on only, its real begin isn't told ([AbsenceService.Absences.isStartHidden]). */
        startHidden: (AvailabilityDO) -> Boolean = { false },
    ) {
        val viewer = ThreadLocalUserContext.requiredLoggedInUser
        availabilities.forEach { availability ->
            val status = availability.status ?: return@forEach
            val user = absenceService.userOf(availability)
            // The type only for the user and HR staff: it may be personal (e.g. sickness).
            val title = "${absenceService.labelFor(viewer, availability)}: ${user?.getFullname()}"
            if (!events.any {
                    it.title == title && FullCalendarEvent.samePeriod(it, availability.startDate, availability.endDate)
                }) {
                val event = FullCalendarEvent.createAllDayEvent(
                    id = availability.id,
                    category = FullCalendarEvent.Category.AVAILABILITY,
                    title = title,
                    start = availability.startDate!!,
                    end = availability.endDate!!,
                    dbId = availability.id,
                    style = if (status.absent) absentStyle else otherStyle,
                    classNames = if (status.absent) "availability-event availability-absent" else "availability-event",
                    calendarSettings = settings,
                )
                val tb = TooltipBuilder().addPropRow(
                    translate("timePeriod"),
                    VacationProvider.periodText(availability.startDate, availability.endDate, startHidden(availability)),
                )
                tb.addPropRow(translate("availability.status"), translate(status.i18nKey))
                availability.location?.let {
                    tb.addPropRow(translate("availability.location"), translate(it.i18nKey))
                }
                if (absenceService.isCommentVisible(viewer, availability)) {
                    tb.addPropRow(translate("availability.comment"), availability.comment, abbreviate = true)
                }
                // The cached internal comment is kept raw (AvailabilityDao.afterLoad): it's personal data, only
                // visible to the user and to HR staff.
                if (absenceService.isInternalCommentVisible(viewer, availability)) {
                    tb.addPropRow(translate("availability.internalComment"), availability.internalComment, abbreviate = true)
                }
                event.setTooltip(title, tb)
                events.add(event)
            }
        }
    }
}
