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

@file:Suppress("DEPRECATION")

package org.projectforge.rest.calendar

import org.projectforge.business.address.AddressDao
import org.projectforge.business.calendar.CalendarView
import org.projectforge.business.calendar.TeamCalendar
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.RestHelper
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.time.LocalDate
import java.util.*
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

    @Value("\${calendar.useNewCalendarEvents}")
    private var useNewCalendarEvents: Boolean = false

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var teamCalEventsProvider: TeamCalEventsProvider

    @Autowired
    private lateinit var calendarEventsProvider: CalEventsProvider

    @Autowired
    private lateinit var timesheetsProvider: TimesheetEventsProvider

    @Autowired
    private lateinit var calendarFilterServicesRest: CalendarFilterServicesRest

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @PostMapping("events")
    fun getEvents(@RequestBody filter: CalendarRestFilter): ResponseEntity<Any> {
        filter.afterDeserialization()
        if (filter.start == null) {
            return ResponseEntity("At least start date required for getting events.", HttpStatus.BAD_REQUEST)
        }
        return ResponseEntity(buildEvents(filter), HttpStatus.OK)
    }

    /**
     * The users selected a slot in the calendar.
     * @param action slotSelected, resize, drop
     * @param startDate startDate timestamp of event (after resize or drag&drop)
     * @param endDate endDate timestamp of event (after resize or drag&drop)
     * @param allDay
     * @param category calEvent, teamEvent or timesheet.
     * @param dbId Data base id of timesheet or team event.
     * @param uid Uid of event, if given.
     * @param origStartDate For resizing or moving events of series, the origin startDate date is required.
     * @param origEndDate For resizing or moving events of series, the origin endDate date is required.
     */
    @GetMapping("action")
    fun action(@RequestParam("action") action: String?,
               @RequestParam("startDate") startDateParam: String?,
               @RequestParam("endDate") endDateParam: String?,
               @RequestParam("allDay") allDay: String?,
               @RequestParam("category") categoryParam: String?,
               @RequestParam("dbId") dbIdParam: String?,
               @RequestParam("uid") uidParam: String?,
               @RequestParam("origStartDate") origStartDateParam: String?,
               @RequestParam("origEndDate") origEndDateParam: String?)
            : ResponseEntity<Any> {
        if (action.isNullOrBlank()) {
            return ResponseEntity("Action not given, 'slotSelected', 'resize' or 'dragAndDrop' expected.", HttpStatus.BAD_REQUEST)
        }
        val startDate = if (startDateParam != null) RestHelper.parseJSDateTime(startDateParam)?.epochSeconds else null
        val endDate = if (endDateParam != null) RestHelper.parseJSDateTime(endDateParam)?.epochSeconds else null
        var url: String
        var category: String? = categoryParam
        if (action == "slotSelected") {
            val defaultCalendarId = calendarFilterServicesRest.getCurrentFilter().defaultCalendarId
            category = if (defaultCalendarId != null && defaultCalendarId > 0) {
                if (useNewCalendarEvents) "calEvent" else "teamEvent"
            } else {
                "timesheet"
            }
            url = "$category/edit?startDate=$startDate&endDate=$endDate"
            if (defaultCalendarId != null && defaultCalendarId > 0) {
                url = "$url&calendar=$defaultCalendarId"
            }
        } else if (action == "resize" || action == "dragAndDrop") {
            val origStartDate = if (startDate != null) RestHelper.parseJSDateTime(origStartDateParam)?.epochSeconds else null
            val origEndDate = if (endDate != null) RestHelper.parseJSDateTime(origEndDateParam)?.epochSeconds else null
            val dbId = NumberHelper.parseInteger(dbIdParam)
            val dbIdString = if (dbId != null && dbId >= 0) "$dbId" else ""
            val uidString = if (uidParam.isNullOrBlank()) "" else URLEncoder.encode(uidParam, "UTF-8")
            url = "$category/edit/$dbIdString$uidString?startDate=$startDate&endDate=$endDate"
            if (category != "timesheet" && origStartDate != null) {
                url = "$url&origStartDate=$origStartDate&origEndDate=$origEndDate"
            }
        } else {
            return ResponseEntity("Action '$action' not supported. Supported actions are: 'slotSelected', 'resize' and 'dragAndDrop'.", HttpStatus.BAD_REQUEST)
        }
        val responseAction = ResponseAction(url)
        return ResponseEntity(responseAction, HttpStatus.OK)
    }

    private fun buildEvents(filter: CalendarRestFilter): CalendarData { //startParam: PFDateTime? = null, endParam: PFDateTime? = null, viewParam: CalendarViewType? = null): Response {
        val events = mutableListOf<BigCalendarEvent>()
        val view = CalendarView.from(filter.view)
        // Workaround for BigCalendar, if the browser's timezone differs from user's timezone in ThreadLocalUserContext.
        // ZoneInfo.getTimeZone returns null, if timeZone not known. TimeZone.getTimeZone returns GMT on failure!
        val timeZone = if (filter.timeZone != null) TimeZone.getTimeZone(filter.timeZone) else null
        if (filter.updateState == true) {
            calendarFilterServicesRest.updateCalendarFilter(filter.start, view, filter.activeCalendarIds)
        }
        val range = DateTimeRange(PFDateTime.from(filter.start, timeZone = timeZone)!!,
                PFDateTime.from(filter.end, timeZone = timeZone))
        adjustRange(range, view)
        timesheetsProvider.addTimesheetEvents(range.start, range.end!!, filter.timesheetUserId, events)
        var visibleCalendarIds = filter.activeCalendarIds
        if (filter.useVisibilityState == true && !visibleCalendarIds.isNullOrEmpty()) {
            val currentFilter = CalendarFilterServicesRest.getCurrentFilter(userPrefService)
            if (currentFilter != null) {
                val set = mutableSetOf<Int>()
                visibleCalendarIds.forEach {
                    if (currentFilter.isVisible(it))
                        set.add(it) // Add only visible calendars.
                }
                visibleCalendarIds = set
            }

        }
        val visibleTeamCalendarIds = visibleCalendarIds?.filter { it >= 0 } // calendars with id < 0 are pseudo calendars (such as birthdays etc.)
        if (useNewCalendarEvents) {
            calendarEventsProvider.addEvents(range.start, range.end!!, events, visibleTeamCalendarIds, calendarFilterServicesRest.getStyleMap())
        } else {
            teamCalEventsProvider.addEvents(range.start, range.end!!, events, visibleTeamCalendarIds, calendarFilterServicesRest.getStyleMap())
        }

        val showFavoritesBirthdays = visibleCalendarIds?.contains(TeamCalendar.BIRTHDAYS_FAVS_CAL_ID) ?: false
        val showAllBirthdays = visibleCalendarIds?.contains(TeamCalendar.BIRTHDAYS_ALL_CAL_ID) ?: false
        if (showAllBirthdays || showFavoritesBirthdays) {
            BirthdaysProvider.addEvents(addressDao, range.start, range.end!!, events, calendarFilterServicesRest.getStyleMap(),
                    showFavoritesBirthdays,
                    showAllBirthdays,
                    !accessChecker.isLoggedInUserMemberOfGroup(
                            ProjectForgeGroup.FINANCE_GROUP,
                            ProjectForgeGroup.HR_GROUP,
                            ProjectForgeGroup.ORGA_TEAM))
        }

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
            CalendarView.WORK_WEEK -> {
                range.start = start.beginOfWeek
                range.end = range.start.plusDays(5)
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

