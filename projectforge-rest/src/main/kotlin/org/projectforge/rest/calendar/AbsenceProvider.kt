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

import org.projectforge.business.availability.AbsenceKind
import org.projectforge.business.availability.AbsenceService
import org.projectforge.business.calendar.CalendarStyle
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * The shared view of the absences of employees and users for the calendar and the team calendars, as [AbsenceService]
 * tells them (as far as the logged-in user may see them and the given kinds ask for them): their vacations
 * ([VacationProvider]) and their availability entries ([AvailabilityProvider]). An absent entry (not available) looks
 * like a vacation, the others in the availability color.
 */
@Component
open class AbsenceProvider {
    @Autowired
    private lateinit var absenceService: AbsenceService

    @Autowired
    private lateinit var availabilityProvider: AvailabilityProvider

    @Autowired
    private lateinit var vacationProvider: VacationProvider

    /**
     * @param groupIds Null items should only occur on (de)serialization issues.
     * @param userIds Null items should only occur on (de)serialization issues.
     * @param style The style of a team calendar, which shows its absences in its own color; the calendar's colors
     * for vacations and availability entries otherwise.
     */
    open fun addEvents(
        start: PFDateTime,
        end: PFDateTime,
        events: MutableList<FullCalendarEvent>,
        groupIds: Set<Long?>?,
        userIds: Set<Long?>?,
        kinds: Set<AbsenceKind>,
        settings: CalendarSettings,
        style: CalendarStyle? = null,
    ) {
        if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
            return // Nothing to do
        }
        val absences = absenceService.getAbsences(start.beginOfDay.localDate, end.localDate, kinds, groupIds, userIds)
        val absentStyle = style ?: CalendarStyle(settings.vacationsColorOrDefault)
        vacationProvider.addEvents(absences.vacations, events, settings, absentStyle)
        availabilityProvider.addEvents(
            absences.availabilities, events, settings,
            absentStyle = absentStyle,
            otherStyle = style ?: settings.availabilityStyle,
        )
    }
}
