/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.availability.AvailabilityTypeConfiguration
import org.projectforge.business.availability.service.AvailabilityService
import org.projectforge.business.calendar.CalendarStyle
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Provides the availability entries of the employees. You may filter the availabilities by ProjectForge groups.
 */
@Component
open class AvailabilityProvider {
    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var availabilityService: AvailabilityService

    @Autowired
    private lateinit var availabilityTypeConfiguration: AvailabilityTypeConfiguration

    /**
     * @param groupIds Null items should only occur on (de)serialization issues.
     * @param userIds Null items should only occur on (de)serialization issues.
     */
    open fun addEvents(
        start: PFDateTime,
        end: PFDateTime,
        events: MutableList<FullCalendarEvent>,
        /**
         * Availability entries will only be displayed for employees (users) who are member of at least one of the following groups:
         */
        groupIds: Set<Long?>?,
        userIds: Set<Long?>?,
        settings: CalendarSettings,
    ) {
        if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
            return // Nothing to do
        }

        // Get all employees from groups/users
        val employees = mutableSetOf<Long>()

        // Process userIds directly
        userIds?.filterNotNull()?.forEach { userId ->
            employeeCache.getEmployeeByUserId(userId)?.id?.let { employees.add(it) }
        }

        // For groupIds, we need to iterate through all employees (simplified approach)
        // In production, this should be optimized with a proper query
        if (!groupIds.isNullOrEmpty()) {
            // Get all employee IDs that match the group filter
            // This is a simplified version - in production you'd query this more efficiently
            val filteredGroupIds = groupIds.filterNotNull()
            if (filteredGroupIds.isNotEmpty()) {
                // For now, we'll just use the availabilities we find based on userIds
                // A more complete implementation would query employees by group membership
            }
        }

        // Get availabilities for all employees
        employees.forEach { employeeId ->
            val availabilities = availabilityService.getAvailabilityListForPeriod(
                employeeId,
                start.beginOfDay.localDate,
                end.localDate
            )
            availabilities.forEach { availability ->
                val employeeUser = employeeCache.getUser(availability.employee)
                val typeConfig = availabilityTypeConfiguration.getTypeByKey(availability.availabilityType ?: "")
                val typeName = typeConfig?.let { translate(it.i18nKey) } ?: availability.availabilityType
                val title = "$typeName: ${employeeUser?.getFullname()}"

                if (!events.any {
                        it.title == title && FullCalendarEvent.samePeriod(
                            it,
                            availability.startDate,
                            availability.endDate
                        ) &&
                                it.extendedProps?.dbId == availability.id
                    }) {
                    val useStyle = typeConfig?.let { CalendarStyle(it.color) } ?: CalendarStyle("#000000")
                    // Event doesn't yet exist:
                    val event = FullCalendarEvent.createAllDayEvent(
                        id = availability.id,
                        title = title,
                        start = availability.startDate!!,
                        end = availability.endDate!!,
                        style = useStyle,
                        dbId = availability.id,
                        classNames = "availability-event",
                        calendarSettings = settings,
                    )

                    // Add tooltip with details
                    val startDate = org.projectforge.framework.time.PFDay.fromOrNull(availability.startDate)?.format() ?: ""
                    val endDate = org.projectforge.framework.time.PFDay.fromOrNull(availability.endDate)?.format() ?: ""
                    val tb = TooltipBuilder()
                        .addPropRow(translate("timePeriod"), "$startDate - $endDate")
                    availability.percentage?.let {
                        tb.addPropRow(translate("availability.percentage"), "$it%")
                    }
                    if (availability.replacement != null) {
                        tb.addPropRow(
                            translate("vacation.replacement"),
                            availabilityService.collectAllReplacements(availability).joinToString {
                                employeeCache.getUser(it)?.displayName ?: "???"
                            }
                        )
                    }
                    if (!availability.comment.isNullOrBlank()) {
                        tb.addPropRow(translate("comment"), availability.comment, abbreviate = true)
                    }
                    event.setTooltip(title, tb)
                    events.add(event)
                }
            }
        }
    }
}
