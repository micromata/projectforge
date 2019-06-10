/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.calendar.CalendarFilter
import org.projectforge.business.calendar.CalendarView
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.RestHelper
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import javax.ws.rs.BadRequestException

/**
 * Rest services for getting events.
 */
@RestController
@RequestMapping("${Rest.URL}/calendar")
class CalendarServicesRest {
    enum class ACCESS { OWNER, FULL, READ, MINIMAL, NONE }

    internal class CalendarData(val date: LocalDate,
                                @Suppress("unused") val events: List<BigCalendarEvent>,
                                @Suppress("unused") val specialDays: Map<String, HolidayAndWeekendProvider.SpecialDayInfo>)

    private class DateTimeRange(var start: PFDateTime,
                                var end: PFDateTime? = null)

    @Autowired
    private lateinit var teamCalEventsProvider: TeamCalEventsProvider

    @Autowired
    private lateinit var timesheetsProvider: TimesheetEventsProvider

    @Autowired
    private lateinit var calendarConfigServicesRest: CalendarFilterServicesRest

    @Autowired
    private lateinit var userPreferenceService: UserPreferencesService

    @PostMapping("events")
    fun getEvents(@RequestBody filter: CalendarRestFilter): ResponseEntity<Any> {
        filter.afterDeserialization()
        if (filter.start == null) {
            return ResponseEntity("At least start date required for getting events.", HttpStatus.BAD_REQUEST)
        }
        val userId = ThreadLocalUserContext.getUserId()
        filter.timesheetUserId = userId // TODO: get from client
        return ResponseEntity(buildEvents(filter), HttpStatus.OK)
    }

    /**
     * The users selected a slot in the calendar.
     */
    @GetMapping("action")
    fun action(@RequestParam("action") action: String?,
               @RequestParam("start") start: String?,
               @RequestParam("end") end: String?,
               /**
                * The default calendar name or null (default) for creating time sheets.
                */
               @RequestParam("calendar") calendar: String?)
            : ResponseEntity<Any> {
        if (action != null && action != "select")
            return ResponseEntity("Action '$action' not supported. Supported action is only 'select'.", HttpStatus.BAD_REQUEST)
        val startDate = if (start != null) RestHelper.parseJSDateTime(start)?.epochSeconds else null
        val endDate = if (end != null) RestHelper.parseJSDateTime(end)?.epochSeconds else null

        val category: String;
        if (calendar.isNullOrBlank()) {
            category = "timesheet"
        } else {
            category = "teamEvent"
        }
        val responseAction = ResponseAction("$category/edit?start=$startDate&end=$endDate")
                .addVariable("category", category)
                .addVariable("startDate", startDate)
                .addVariable("endDate", endDate)
        return ResponseEntity(responseAction, HttpStatus.OK)
    }

    private fun buildEvents(filter: CalendarRestFilter): CalendarData { //startParam: PFDateTime? = null, endParam: PFDateTime? = null, viewParam: CalendarViewType? = null): Response {
        val events = mutableListOf<BigCalendarEvent>()
        val view = CalendarView.from(filter.view)
        if (filter.updateState == true) {
            calendarConfigServicesRest.updateCalendarFilter(filter.start, view, filter.activeCalendarIds)
        }
        val range = DateTimeRange(PFDateTime.from(filter.start)!!, PFDateTime.from(filter.end))
        adjustRange(range, view)
        val timesheetUserId = filter.timesheetUserId
        if (timesheetUserId != null) {
            timesheetsProvider.addTimesheetEvents(range.start, range.end!!, timesheetUserId, events)
        }
        var visibleCalendarIds = filter.activeCalendarIds
        if (filter.useVisibilityState == true && !visibleCalendarIds.isNullOrEmpty()) {
            val currentFilter = userPreferenceService.getEntry(CalendarFilter::class.java, CalendarFilterServicesRest.PREF_KEY_CURRENT_FAV)
            if (currentFilter != null) {
                val set = mutableSetOf<Int>()
                visibleCalendarIds.forEach {
                    if (currentFilter.isVisible(it))
                        set.add(it) // Add only visible calendars.
                }
                visibleCalendarIds = set
            }

        }
        teamCalEventsProvider.addEvents(range.start, range.end!!, events, visibleCalendarIds, calendarConfigServicesRest.getStyleMap())
        val specialDays = HolidayAndWeekendProvider.getSpecialDayInfos(range.start, range.end!!)
        var counter = 0
        events.forEach {
            it.key = "e-${counter++}"
        }
        return CalendarData(range.start.dateTime.toLocalDate(), events, specialDays)
    }

    /**
     * Adjustes the range (start and end) if end is not given.
     */
    private fun adjustRange(range: DateTimeRange, view: CalendarView?) {
        if (range.end != null) {
            if (range.start.daysBetween(range.end!!) > 50)
                throw BadRequestException("Requested range for calendar to big. Max. number of days between start and end must not higher than 50.")
            return
        }
        val start = range.start
        when (view) {
            CalendarView.WEEK -> {
                range.start = start.beginOfWeek
                range.end = range.start.plusDays(7)
            }
            CalendarView.DAY -> {
                range.start = start.beginOfDay
                range.end = range.start.plusDays(1)
            }
            else -> {
                // Assuming month at default
                range.start = start.beginOfMonth
                range.end = start.endOfMonth
            }
        }
    }
}

