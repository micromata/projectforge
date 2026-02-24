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

import org.projectforge.business.availability.AvailabilityCache
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Provides the availability entries of employees for the calendar view. You may filter by ProjectForge groups.
 */
@Component
open class AvailabilityProvider {
    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var availabilityCache: AvailabilityCache

    /**
     * @param groupIds Null items should only occur on (de)serialization issues.
     * @param userIds Null items should only occur on (de)serialization issues.
     */
    open fun addEvents(
        start: PFDateTime,
        end: PFDateTime,
        events: MutableList<FullCalendarEvent>,
        groupIds: Set<Long?>?,
        userIds: Set<Long?>?,
        settings: CalendarSettings,
    ) {
        if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
            return
        }
        val availabilities =
            availabilityCache.getAvailabilityForPeriodAndUsers(start.beginOfDay.localDate, end.localDate, groupIds, userIds)
        availabilities.forEach { availability ->
            val employeeUser = employeeCache.getUser(availability.employee)
            val typeLabel = availability.type ?: translate("availability.title")
            val title = "$typeLabel: ${employeeUser?.getFullname()}"
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
                    style = settings.availabilityStyle,
                    classNames = "availability-event",
                    calendarSettings = settings,
                )
                val startDate = PFDay.fromOrNull(availability.startDate)?.format() ?: ""
                val endDate = PFDay.fromOrNull(availability.endDate)?.format() ?: ""
                val tb = TooltipBuilder()
                    .addPropRow(translate("timePeriod"), "$startDate - $endDate")
                availability.status?.let {
                    tb.addPropRow(translate("availability.status"), translate(it.i18nKey))
                }
                availability.location?.let {
                    tb.addPropRow(translate("availability.location"), translate(it.i18nKey))
                }
                if (!availability.description.isNullOrBlank()) {
                    tb.addPropRow(translate("availability.description"), availability.description, abbreviate = true)
                }
                event.setTooltip(title, tb)
                events.add(event)
            }
        }
    }
}
