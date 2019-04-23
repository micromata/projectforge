package org.projectforge.rest.calendar

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.core.RestHelper
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Rest services for getting events.
 */
@Component
@Path("calendar")
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
                         var activeCalendarIds: List<Int>? = null)

    private class DateTimeRange(var start: PFDateTime,
                                var end: PFDateTime? = null)

    @Autowired
    private lateinit var teamCalEventsProvider: TeamCalEventsProvider

    @Autowired
    private lateinit var timesheetsProvider: TimesheetEventsProvider

    @Autowired
    private lateinit var calendarConfigServicesRest: CalendarConfigServicesRest

    private val restHelper = RestHelper()

    @POST
    @Path("events")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun getEvents(filter: CalendarFilter): Response {
        if (filter.start == null) {
            return restHelper.buildResponseBadRequest("At least start date required for getting events.")
        }
        val userId = ThreadLocalUserContext.getUserId()
        filter.timesheetUserId = userId // TODO: get from client
        return buildEvents(filter)
    }

    /**
     * The users selected a slot in the calendar.
     */
    @GET
    @Path("action")
    @Produces(MediaType.APPLICATION_JSON)
    fun action(@QueryParam("action") action: String?,
               @QueryParam("start") start: String?,
               @QueryParam("end") end: String?,
               /**
                * The default calendar name or null (default) for creating time sheets.
                */
               @QueryParam("calendar") calendar: String?): Response {
        if (action != null && action != "select")
            return restHelper.buildResponseBadRequest("Action '$action' not supported. Supported action is only 'select'.")
        val startDate = if (start != null) restHelper.parseJSDateTime(start)?.toEpochSeconds() else null
        val endDate = if (end != null) restHelper.parseJSDateTime(end)?.toEpochSeconds() else null
        return if (calendar.isNullOrBlank())
            restHelper.buildResponseAction(ResponseAction("timesheet/edit?start=$startDate&end=$endDate"))
        else
            restHelper.buildResponseAction(ResponseAction("teamEvent/edit?start=$startDate&end=$endDate"))
    }

    private fun buildEvents(filter: CalendarFilter): Response { //startParam: PFDateTime? = null, endParam: PFDateTime? = null, viewParam: CalendarViewType? = null): Response {
        val events = mutableListOf<BigCalendarEvent>()
        // val settings = getUsersSettings()
        val range = DateTimeRange(PFDateTime.from(filter.start)!!, PFDateTime.from(filter.end))
        adjustRange(range, CalendarView.from(filter.view))
        val timesheetUserId = filter.timesheetUserId
        if (timesheetUserId != null) {
            timesheetsProvider.addTimesheetEvents(range.start, range.end!!, timesheetUserId, events)
        }
        teamCalEventsProvider.addEvents(range.start, range.end!!, events, filter.activeCalendarIds, calendarConfigServicesRest.getStyleMap())
        val specialDays = HolidayAndWeekendProvider.getSpecialDayInfos(range.start, range.end!!)
        var counter = 0
        events.forEach {
            it.id = "e-${counter++}"
        }
        val result = CalendarData(range.start.dateTime.toLocalDate(), events, specialDays)
        return restHelper.buildResponse(result)
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

