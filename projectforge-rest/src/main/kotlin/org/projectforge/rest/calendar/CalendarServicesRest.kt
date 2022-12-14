/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.Constants
import org.projectforge.business.address.AddressDao
import org.projectforge.business.calendar.CalendarView
import org.projectforge.business.calendar.StyledTeamCalendar
import org.projectforge.business.calendar.TeamCalendar
import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DatePrecision
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.calendar.CalendarFilterServicesRest.Companion.getCurrentFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.RestButtonEvent
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

// private val log = KotlinLogging.logger {}

/**
 * Rest services for getting events.
 */
@RestController
@RequestMapping("${Rest.URL}/calendar")
class CalendarServicesRest {
  internal class CalendarData(
    val date: LocalDate,
    val alternateHoursBackground: Boolean?,
    @Suppress("unused") val events: List<FullCalendarEvent>,
  )

  class CalendarState(
    var date: String?,
    var view: String?,
    var timeZone: String?,
    var activeCalendars: Collection<StyledTeamCalendar?>?
  )

  private class DateTimeRange(
    var start: PFDateTime,
    var end: PFDateTime? = null
  )

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
  private lateinit var calendarFilterServicesRest: CalendarFilterServicesRest

  @Autowired
  private lateinit var calendarSettingsService: CalendarSettingsService

  @Autowired
  private lateinit var teamCalCache: TeamCalCache

  @Autowired
  private lateinit var teamEventExternalSubscriptionCache: TeamEventExternalSubscriptionCache

  @Autowired
  private lateinit var timesheetsProvider: TimesheetEventsProvider

  @Autowired
  private lateinit var userPrefService: UserPrefService

  @Autowired
  private lateinit var vacationProvider: VacationProvider

  @PostMapping("events")
  fun getEvents(@RequestBody filter: CalendarRestFilter): ResponseEntity<Any> {
    filter.afterDeserialization()
    if (filter.start == null) {
      return ResponseEntity("At least start date required for getting events.", HttpStatus.BAD_REQUEST)
    }
    val currentFilter = calendarFilterServicesRest.getCurrentFilter()
    filter.vacationGroupIds = currentFilter.vacationGroupIds?.toMutableSet()
    filter.vacationUserIds = currentFilter.vacationUserIds?.toMutableSet()
    return ResponseEntity(buildEvents(filter), HttpStatus.OK)
  }

  /**
   * Force to poll (refresh) external calendars (if currently displayed). If no external calendar is displayed, nothing
   * is done.
   */
  @GetMapping("refresh")
  fun refresh(): ResponseEntity<Any> {
    val filter = calendarFilterServicesRest.getCurrentFilter()
    val visibleCalendarIds = mutableListOf<Int>()
    filter.calendarIds.forEach {
      if (it != null && !filter.invisibleCalendars.contains(it)) {
        visibleCalendarIds.add(it)
      }
    }
    var reload = false
    visibleCalendarIds.forEach { calendarId ->
      val teamCalDO = teamCalCache.getCalendar(calendarId)
      if (teamCalDO != null && teamCalDO.externalSubscription) {
        // Force reload of given calendar.
        reload = true
        teamEventExternalSubscriptionCache.updateCache(teamCalDO, true)
      }
    }
    return ResponseEntity("{\"reload\": $reload}", HttpStatus.OK)
  }

  /**
   * The users selected a slot in the calendar.
   *
   * Supports different date formats: long number of epoch seconds
   * or iso date time including any time zone offset.
   *
   * @param action slotSelected, resize, drop
   * @param startDateParam startDate timestamp of event (after resize or drag&drop)
   * @param endDateParam endDate timestamp of event (after resize or drag&drop)
   * @param categoryParam calEvent, teamEvent or timesheet.
   * @param dbIdParam Data base id of timesheet or team event.
   * @param uidParam Uid of event, if given.
   * @param origStartDateParam For resizing or moving events of series, the origin startDate date is required.
   * @param origEndDateParam For resizing or moving events of series, the origin endDate date is required.
   *
   * @see PFDateTimeUtils.parse for supported date formats.
   */
  @GetMapping("action")
  fun action(
    @RequestParam("action") action: String?,
    @RequestParam("startDate") startDateParam: String?,
    @RequestParam("endDate") endDateParam: String?,
    // @RequestParam("allDay") allDay: String?,
    @RequestParam("category") categoryParam: String?,
    @RequestParam("dbId") dbIdParam: String?,
    @RequestParam("uid") uidParam: String?,
    @RequestParam("origStartDate") origStartDateParam: String?,
    @RequestParam("origEndDate") origEndDateParam: String?,
    @RequestParam("firstHour") firstHour: Int?,
  )
      : ResponseEntity<Any> {
    if (action.isNullOrBlank()) {
      return ResponseEntity(
        "Action not given, 'slotSelected', 'create', 'resize' or 'dragAndDrop' expected.",
        HttpStatus.BAD_REQUEST
      )
    }
    var startDateTime = if (!startDateParam.isNullOrBlank()) RestHelper.parseJSDateTime(startDateParam) else null
    var endDateTime = if (!endDateParam.isNullOrBlank()) RestHelper.parseJSDateTime(endDateParam) else null
    if (action == "create" && startDateTime != null && endDateTime == null) {
      // User wants to create new entry (but no slot was selected, only '+' button), so predefine hour of day:
      startDateTime = startDateTime.withHour(8).withPrecision(DatePrecision.HOUR_OF_DAY)
      endDateTime = startDateTime
    }
    val startDate = startDateTime?.javaScriptString
    val endDate = endDateTime?.javaScriptString
    var url: String
    var category: String? = categoryParam
    if (action == "slotSelected" || action == "create") {
      val currentFilter = calendarFilterServicesRest.getCurrentFilter()
      val defaultCalendarId = currentFilter.defaultCalendarId
      category = if (defaultCalendarId != null && defaultCalendarId > 0) {
        if (useNewCalendarEvents) "calEvent" else "teamEvent"
      } else {
        "timesheet"
      }
      url = "/$category/edit?startDate=$startDate&endDate=$endDate"
      url = if (defaultCalendarId != null && defaultCalendarId > 0) {
        "$url&calendar=$defaultCalendarId"
      } else {
        "$url&userId=${currentFilter.timesheetUserId ?: ThreadLocalUserContext.userId}&firstHour=$firstHour"
      }
    } else if (action == "resize" || action == "dragAndDrop") {
      val origStartDate =
        if (startDate != null) RestHelper.parseJSDateTime(origStartDateParam)?.javaScriptString else null
      val origEndDate = if (endDate != null) RestHelper.parseJSDateTime(origEndDateParam)?.javaScriptString else null
      val dbId = NumberHelper.parseInteger(dbIdParam)
      val dbIdString = if (dbId != null && dbId >= 0) "$dbId" else ""
      val uidString = if (uidParam.isNullOrBlank()) "" else URLEncoder.encode(uidParam, "UTF-8")
      url = "/$category/edit/$dbIdString$uidString?startDate=$startDate&endDate=$endDate"
      if (category != "timesheet" && origStartDate != null) {
        url = "$url&origStartDate=$origStartDate&origEndDate=$origEndDate"
      }
    } else {
      return ResponseEntity(
        "Action '$action' not supported. Supported actions are: 'slotSelected', 'create', 'resize' and 'dragAndDrop'.",
        HttpStatus.BAD_REQUEST
      )
    }
    val responseAction = ResponseAction(url)
    return ResponseEntity(responseAction, HttpStatus.OK)
  }

  @PostMapping("storeState")
  fun storeState(@RequestBody state: CalendarState): ResponseEntity<Any> {
    val dateString = state.date
    val viewString = state.view
    val filterState = calendarFilterServicesRest.getFilterState()
    if (!dateString.isNullOrBlank()) {
      val date = PFDayUtils.parseDate(dateString)
      if (date != null && !viewString.isNullOrBlank()) {
        val view = CalendarView.from(viewString)
        filterState.startDate = date
        filterState.view = view
      }
    }
    state.activeCalendars?.let { activeCalendars ->
      getCurrentFilter(userPrefService)?.let { currentFilter ->
        currentFilter.calendarIds = activeCalendars.mapNotNull { it?.id }.toMutableSet()
        // currentFilter.showVacations = restFilter.showVacations
        // currentFilter.vacationGroupIds = restFilter.vacationGroupIds
        // currentFilter.vacationUserIds = restFilter.vacationUserIds
      }
    }
    return ResponseEntity("{}", HttpStatus.OK)
  }

  private fun buildEvents(filter: CalendarRestFilter): CalendarData { //startParam: PFDateTime? = null, endParam: PFDateTime? = null, viewParam: CalendarViewType? = null): Response {
    val events = mutableListOf<FullCalendarEvent>()
    val range = DateTimeRange(
      PFDateTime.fromOrNow(filter.start),
      PFDateTime.fromOrNull(filter.end)
    )
    adjustRange(range)
    val settings = calendarSettingsService.getSettings()
    timesheetsProvider.addTimesheetEvents(
      range.start,
      range.end!!,
      filter.timesheetUserId,
      events,
      settings,
      showBreaks = filter.showBreaks
    )
    var visibleCalendarIds = filter.activeCalendarIds
    if (filter.useVisibilityState == true && !visibleCalendarIds.isNullOrEmpty()) {
      val currentFilter = getCurrentFilter(userPrefService)
      if (currentFilter != null) {
        val set = mutableSetOf<Int?>()
        visibleCalendarIds.forEach {
          if (it != null && !currentFilter.isInvisible(it))
            set.add(it) // Add only visible calendars.
        }
        visibleCalendarIds = set
      }

    }
    val visibleTeamCalendarIds =
      visibleCalendarIds?.filter { it != null && it >= 0 } // calendars with id < 0 are pseudo calendars (such as birthdays etc.)
    val calendarSettings = calendarSettingsService.getSettings()
    if (useNewCalendarEvents) {
      calendarEventsProvider.addEvents(
        calendarSettings,
        range.start,
        range.end!!,
        events,
        visibleTeamCalendarIds,
        calendarFilterServicesRest.getStyleMap()
      )
    } else {
      teamCalEventsProvider.addEvents(
        range.start,
        range.end!!,
        events,
        visibleTeamCalendarIds,
        calendarFilterServicesRest.getStyleMap(),
        settings,
      )
    }

    val showFavoritesBirthdays = visibleCalendarIds?.contains(TeamCalendar.BIRTHDAYS_FAVS_CAL_ID) ?: false
    val showAllBirthdays = visibleCalendarIds?.contains(TeamCalendar.BIRTHDAYS_ALL_CAL_ID) ?: false
    if (showAllBirthdays || showFavoritesBirthdays) {
      BirthdaysProvider.addEvents(
        addressDao, range.start, range.end!!, events, calendarFilterServicesRest.getStyleMap(),
        calendarSettings,
        showFavoritesBirthdays,
        showAllBirthdays,
        !accessChecker.isLoggedInUserMemberOfGroup(
          ProjectForgeGroup.FINANCE_GROUP,
          ProjectForgeGroup.HR_GROUP,
          ProjectForgeGroup.ORGA_TEAM
        )
      )
    }
    vacationProvider.addEvents(
      range.start,
      range.end!!,
      events,
      filter.vacationGroupIds,
      filter.vacationUserIds,
      settings,
    )

    val specialDays = HolidayAndWeekendProvider.getSpecialDayInfos(range.start, range.end!!)
    specialDays.forEach { specialDay ->
      if (specialDay.holidayTitle.isNotBlank()) {
        // Show allday entry with title:
        events.add(
          FullCalendarEvent.createAllDayEvent(
            title = specialDay.holidayTitle,
            calendarSettings = calendarSettings,
            start = specialDay.date,
            classNames = "fc-holiday-weekend",
          ).withTextColor("red")
        )
      }
      if (!specialDay.workingDay) {
        events.add(
          FullCalendarEvent.createBackgroundEvent(
            start = specialDay.date,
            classNames = "fc-holiday-weekend",
          )
        )
      }
    }
    return CalendarData(range.start.localDate, calendarSettingsService.getSettings().alternateHoursBackground, events)
  }

  /**
   * Adjustes the range (start and end) to max 50 days and sets end date to start date + 1 day if not given.
   */
  private fun adjustRange(range: DateTimeRange) {
    if (range.end != null) {
      if (range.start.daysBetween(range.end!!) > 50)
        throw BadRequestException("Requested range for calendar to big. Max. number of days between start and end must not higher than 50.")
      return
    } else {
      range.end = range.start.plusDays(1)
    }
  }

  companion object {
    fun redirectToCalendarWithDate(date: Date?, event: RestButtonEvent): ResponseAction {
      if (date != null && (event == RestButtonEvent.SAVE || event == RestButtonEvent.UPDATE)) {
        // Time sheet was modified, so reload page and goto date of timesheet (if modified):
        val hash = NumberHelper.getSecureRandomAlphanumeric(4)
        val gotoDate = PFDateTime.fromOrNow(date).localDate
        return ResponseAction("/${Constants.REACT_APP_PATH}calendar?gotoDate=$gotoDate&hash=$hash")
      }
      return ResponseAction("/${Constants.REACT_APP_PATH}calendar")
    }
  }
}

