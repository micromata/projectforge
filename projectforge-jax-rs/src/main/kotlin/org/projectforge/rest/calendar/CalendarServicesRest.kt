package org.projectforge.rest.calendar

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

    /**
     * CalendarFilter to request calendar events as POST param. Dates are required as JavaScript ISO date time strings
     * (start and end).
     */
    class CalendarFilter(var start: Date? = null,
                         /** Optional, if view is given. */
                         var end: Date? = null,
                         /** Will be ignored if end is given. */
                         var view: String? = null,
                         var timesheetUserId: Int? = null,
                         /** The team calendarIds to display. */
                         var activeCalendarIds: List<Int>? = null,
                         /**
                          *  If true, then this filter settings updates the fields of the user's active filter.
                          *  If the user calls the calendar page next time, this properties are restored. */
                         var updateActiveDisplayFilter: Boolean? = false)

    private class DateTimeRange(var start: PFDateTime,
                                var end: PFDateTime? = null)

    @Autowired
    private lateinit var teamCalEventsProvider: TeamCalEventsProvider

    @Autowired
    private lateinit var timesheetsProvider: TimesheetEventsProvider

    @Autowired
    private lateinit var calendarConfigServicesRest: CalendarFilterServicesRest

    @PostMapping("events")
    fun getEvents(@RequestBody filter: CalendarFilter): ResponseEntity<Any> {
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
        val startDate = if (start != null) RestHelper.parseJSDateTime(start)?.toEpochSeconds() else null
        val endDate = if (end != null) RestHelper.parseJSDateTime(end)?.toEpochSeconds() else null

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

    private fun buildEvents(filter: CalendarFilter): CalendarData { //startParam: PFDateTime? = null, endParam: PFDateTime? = null, viewParam: CalendarViewType? = null): Response {
        val events = mutableListOf<BigCalendarEvent>()
        val view = CalendarView.from(filter.view)
        if (filter.updateActiveDisplayFilter == true) {
            calendarConfigServicesRest.updateCalendarFilterState(filter.start, view)
        }
        val range = DateTimeRange(PFDateTime.from(filter.start)!!, PFDateTime.from(filter.end))
        adjustRange(range, view)
        val timesheetUserId = filter.timesheetUserId
        if (timesheetUserId != null) {
            timesheetsProvider.addTimesheetEvents(range.start, range.end!!, timesheetUserId, events)
        }
        teamCalEventsProvider.addEvents(range.start, range.end!!, events, filter.activeCalendarIds, calendarConfigServicesRest.getStyleMap())
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
                range.start = start.getBeginOfWeek()
                range.end = range.start.plusDays(7)
            }
            CalendarView.DAY -> {
                range.start = start.getBeginOfDay()
                range.end = range.start.plusDays(1)
            }
            else -> {
                // Assuming month at default
                range.start = start.getBeginOfMonth()
                range.end = start.getEndOfMonth()
            }
        }
    }
}

