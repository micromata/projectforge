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

    internal class CalendarData(val date: LocalDate,
                                val viewType: CalendarViewType = CalendarViewType.MONTH,
                                val events: List<BigCalendarEvent>,
                                val specialDays: Map<String, HolidayAndWeekendProvider.SpecialDayInfo>)

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

    companion object {
        val OLD_USERPREF_KEY = "TeamCalendarPage.userPrefs";
        val USERPREF_KEY = "calendar.displaySettings";
    }

    @Autowired
    private lateinit var timesheetsProvider: TimesheetEventsProvider

    @Autowired
    private lateinit var teamCalEventsProvider: TeamCalEventsProvider

    @Autowired
    private lateinit var userPreferenceService: UserPreferencesService

    private val restHelper = RestHelper()

    @GET
    @Path("initial")
    @Produces(MediaType.APPLICATION_JSON)
    fun getInitialCalendar(): Response {
        return buildEvents();
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

    private fun buildEvents(startParam: PFDateTime? = null, endParam: PFDateTime? = null, viewParam: CalendarViewType? = null): Response {
        val events = mutableListOf<BigCalendarEvent>()
        val settings = getUsersSettings()
        val view =
                if (viewParam != null)
                    viewParam
                else if (settings.viewType != null)
                    settings.viewType
                else
                    CalendarViewType.MONTH
        val range: DateTimeRange
        if (startParam == null)
            range = DateTimeRange(PFDateTime.from(settings.startDate), endParam)
        else
            range = DateTimeRange(startParam, endParam)
        adjustRange(range, view)
        //if (filter.isShowTimesheets) {
        timesheetsProvider.addTimesheetEvents(range.start, range.end!!,
                ThreadLocalUserContext.getUserId(),
                events)
        // }
        val idx = settings.activeDisplayFilterIndex
        val active: CalendarsDisplayFilter?
        if (idx < settings.displayFilters?.list.size)
            active = settings.displayFilters.list[idx]
        else
            active = null
        if (active != null)
            teamCalEventsProvider.addEvents(range.start, range.end!!, events, active)
        val specialDays = HolidayAndWeekendProvider.getSpecialDayInfos(range.start, range.end!!)
        var counter = 0
        events.forEach {
            it.id = "e-${counter++}"
        }
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

    private fun getUsersSettings(): CalendarDisplaySettings {
        var settings = userPreferenceService.getEntry(CalendarDisplaySettings::class.java, "ignore")//USERPREF_KEY)
        if (settings == null) {
            // No current user pref entry available. Try the old one (from release 6.* / Wicket Calendarpage):
            val oldFilter = userPreferenceService.getEntry(TeamCalCalendarFilter::class.java, OLD_USERPREF_KEY)
            oldFilter.viewType
            settings = CalendarDisplaySettings()
            settings.copyFrom(oldFilter)
            userPreferenceService.putEntry(USERPREF_KEY, settings, true)
            settings.saveDisplayFilters(userPreferenceService)
        }
        if (settings.startDate == null)
            settings.startDate = LocalDate.now()
        if (settings.viewType == null)
            settings.viewType = CalendarViewType.MONTH
        return settings
    }
}

