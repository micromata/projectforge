package org.projectforge.rest.calendar

import com.google.gson.annotations.SerializedName
import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.converter.DateTimeFormat
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Component
@Path("calendar")
class CalendarServicesRest() {

    internal class SpecialCalendarDay(val title: String? = null, val bgColor: String?)

    internal class CalendarData(val date: LocalDate,
                                val viewType: CalendarViewType = CalendarViewType.MONTH,
                                val events: List<BigCalendarEvent>,
                                val specialDays: List<SpecialCalendarDay>)

    private class DateTimeRange(var start: PFDateTime,
                                var end: PFDateTime? = null)

    enum class CalendarViewType {
        @SerializedName("month")
        MONTH,
        @SerializedName("week")
        WEEK,
        @SerializedName("day")
        DAY,
        @SerializedName("agenda")
        AGENDA
    }

    private val log = org.slf4j.LoggerFactory.getLogger(CalendarServicesRest::class.java)

    companion object {
        val OLD_USERPREF_KEY = "TeamCalendarPage.userPrefs";
        val USERPREF_KEY = "calendar.displaySettings";
    }

    @Autowired
    private lateinit var timesheetsProvider: TimesheetEventsProvider

    @Autowired
    private lateinit var userPreferenceService: UserPreferencesService

    private val restHelper = RestHelper()

    @GET
    @Path("initial")
    @Produces(MediaType.APPLICATION_JSON)
    fun getInitialCalendar(): Response {
        var filter = userPreferenceService.getEntry(CalendarDisplaySettings::class.java, USERPREF_KEY)
        if (filter == null) {
            // No current user pref entry available. Try the old one (from release 6.* / Wicket Calendarpage):
            val oldFilter = userPreferenceService.getEntry(TeamCalCalendarFilter::class.java, OLD_USERPREF_KEY)
            oldFilter.viewType
            filter = CalendarDisplaySettings()
            filter.copyFrom(oldFilter)
            userPreferenceService.putEntry(USERPREF_KEY, filter, true)
            filter.saveDisplayFilters(userPreferenceService)
        }
        if (filter.startDate == null)
            filter.startDate = LocalDate.now()
        if (filter.viewType == null)
            filter.viewType = CalendarViewType.MONTH
        return buildEvents(startParam = PFDateTime.from(filter.startDate), view = filter.viewType);
    }

    @GET
    @Path("events")
    @Produces(MediaType.APPLICATION_JSON)
    fun getEvents(@QueryParam("start") startParam: String?, @QueryParam("end") endParam: String?, @QueryParam("view") viewParam: String?): Response {
        val start = restHelper.parseJSDateTime(startParam)
        if (start == null) {
            val msg = "Rest service 'events' must be called with at least one valid start date (of pattern '${DateTimeFormat.JS_DATE_TIME_MILLIS.pattern}')."
            return restHelper.buildResponseBadRequest(msg)
        }
        val end = restHelper.parseJSDateTime(endParam)
        val view = when (viewParam) {
            "week" -> CalendarViewType.WEEK
            "day" -> CalendarViewType.DAY
            "agenda" -> CalendarViewType.AGENDA
            else -> CalendarViewType.MONTH
        }
        return buildEvents(start, end, view)
    }

    private fun buildEvents(startParam: PFDateTime, endParam: PFDateTime? = null, view: CalendarViewType? = null): Response {
        val events = mutableListOf<BigCalendarEvent>()
        val range = DateTimeRange(startParam, endParam)
        adjustRange(range, view)
        //if (filter.isShowTimesheets) {
        timesheetsProvider.addTimesheetEvents(range.start, range.end!!,
                ThreadLocalUserContext.getUserId(),
                events)
        // }
        val specialDays = mutableListOf<SpecialCalendarDay>()
        //Holidays.getInstance().isWorkingDay()
        val result = CalendarData(range.start.dateTime.toLocalDate(), view!!, events, specialDays)
        return restHelper.buildResponse(result)
    }

    /**
     * Adjustes the range (start and end) if end is not given.
     */
    private fun adjustRange(range: DateTimeRange, view: CalendarViewType?) {
        if (range.end != null) {
            if (range.start.daysBetween(range.end!!) > 50)
                throw BadRequestException("Requested range for calendar to big. Max. number of days between start and end must not higher than 50.")
            return
        }
        val start = range.start
        when (view) {
            CalendarViewType.WEEK -> {
                range.start = start.getBeginOfWeek()
                range.end = range.start.plusDays(7)
            }
            CalendarViewType.DAY -> {
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

