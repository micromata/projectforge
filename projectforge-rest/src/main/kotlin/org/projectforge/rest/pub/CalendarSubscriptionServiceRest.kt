/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.pub

import de.micromata.merlin.utils.ReplaceUtils
import mu.KotlinLogging
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.Location
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.teamcal.TeamCalConfig
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.CalendarHelper
import org.projectforge.business.teamcal.event.TeamEventFilter
import org.projectforge.business.teamcal.event.TeamEventService
import org.projectforge.business.teamcal.event.ical.ICalGenerator
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.teamcal.model.CalendarFeedConst
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.vacation.VacationCache
import org.projectforge.common.StringHelper
import org.projectforge.framework.calendar.Holidays.Companion.instance
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDay.Companion.now
import org.projectforge.framework.utils.NumberHelper.parseLong
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.projectforge.security.SecurityLogging.logSecurityWarn
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping(Rest.CALENDAR_EXPORT_BASE_URI)
class CalendarSubscriptionServiceRest {
  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var teamEventService: TeamEventService

  @Autowired
  private lateinit var teamCalDao: TeamCalDao

  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  @Autowired
  private lateinit var vacationCache: VacationCache

  @GetMapping
  fun exportCalendar(request: HttpServletRequest): ResponseEntity<*> {
    var logMessage: String? = null
    try {
      val userId = ThreadLocalUserContext.loggedInUserId ?: run {
        log.error("Internal errror: shouldn't occur: can't get context user! Should be denied by filter!!!")
        return ResponseEntity<Any>(HttpStatus.BAD_REQUEST)
      }
      val params = decryptRequestParams(request, userId, userAuthenticationsService)
      if (params.isNullOrEmpty()) {
        return ResponseEntity<Any>(HttpStatus.BAD_REQUEST)
      }
      // check timesheet user
      val timesheetUserParam = params[CalendarFeedConst.PARAM_NAME_TIMESHEET_USER]
      var timesheetUser: PFUserDO? = null
      if (timesheetUserParam != null) {
        timesheetUser = getTimesheetUser(userId, timesheetUserParam) ?: run {
          log.error("Bad request, timesheet user not found: ${request.queryString}")
          return ResponseEntity<Any>(HttpStatus.BAD_REQUEST)
        }
      }
      // create ical generator
      val generator = ICalGenerator.exportAllFields()
      generator.exportVEventAlarm("true" == params[PARAM_EXPORT_REMINDER])
      // read events
      val processCalendars = readEventsFromCalendars(generator, params)
      readTimesheets(generator, timesheetUser)
      val holidaysProecessed = readHolidays(generator, params)
      val weeksOfYearProcessed = readWeeksOfYear(generator, params)
      // setup event is needed for empty calendars
      if (generator.isEmpty) {
        generator.addEvent(VEvent(Date(0), TeamCalConfig.SETUP_EVENT))
      }
      logMessage = params.filter { it.key != "token" }.map { "${it.key}=${it.value}" }.joinToString(", ")
      log.info("Read calendar entries for: $logMessage")
      val baos = ByteArrayOutputStream()
      generator.writeCalendarToOutputStream(baos)

      val resource = ByteArrayResource(baos.toByteArray())
      val sb = StringBuilder()
      sb.append(processCalendars?.joinToString { StringUtils.abbreviate(it.title, 25) }
        ?: "")
      timesheetUser?.let { sb.append(translate("timesheet.timesheets")).append("-").append(it.username) }
      if (holidaysProecessed) {
        sb.append(translate("holidays"))
      }
      if (weeksOfYearProcessed) {
        sb.append(translate("weekOfYear"))
      }
      val safeFilename = "projectforge-${ReplaceUtils.encodeFilename(sb.toString(), false)}.ics"
      return RestUtils.downloadFile(safeFilename, resource)
    } finally {
      log.info("Finished request: $logMessage")
      ThreadLocalUserContext.setUser(null)
      MDC.remove("ip")
      MDC.remove("session")
      MDC.remove("user")
    }
  }

  private fun getTimesheetUser(userId: Long, timesheetUserParam: String): PFUserDO? {
    var timesheetUser: PFUserDO? = null
    if (StringUtils.isNotBlank(timesheetUserParam)) {
      val timesheetUserId = parseLong(timesheetUserParam)
      if (timesheetUserId != null) {
        if (timesheetUserId != userId) {
          log.error("Not yet allowed: all users are only allowed to download their own time-sheets.")
          return null
        }
        timesheetUser = userGroupCache.getUser(timesheetUserId)
        if (timesheetUser == null) {
          log.error("Time-sheet user with id '$timesheetUserParam' not found.")
          return null
        }
      }
    }
    //    if (loggedInUser.getId().equals(timesheetUser.getId()) == false && isOtherUsersAllowed() == false) {
//      // Only project managers, controllers and administrative staff is allowed to subscribe time-sheets of other users.
//      log.warn("User tried to get time-sheets of other user: " + timesheetUser);
//      timesheetUser = loggedInUser;
//    }
    return timesheetUser
  }

  /**
   * @return processed team cals (for creating filename of export) or null/empty if no calendars were processed.
   */
  private fun readEventsFromCalendars(generator: ICalGenerator, params: Map<String, String>): List<TeamCalDO>? {
    val teamCals = params["teamCals"] ?: return null
    val teamCalIds = StringUtils.split(teamCals, ";") ?: return null
    val eventFilter = TeamEventFilter()
    val eventDateFromLimit = now().minusYears(1)
    val eventDateUntilLimit = now().plusYears(2)
    eventFilter.isDeleted = false
    eventFilter.startDate = eventDateFromLimit.utilDate
    val vacationEvents = mutableSetOf<Long>() // For avoiding multiple entries of vacation days. Ids of vacation event.
    val processedTeamCals = mutableListOf<TeamCalDO>()
    for (teamCalIdString in teamCalIds) {
      val calId = teamCalIdString.toLong()
      eventFilter.teamCalId = calId
      val teamEvents = teamEventService.getEventList(eventFilter, false)
      teamEvents?.forEach { teamEventObject ->
        if (teamEventObject !is TeamEventDO) {
          log.warn("Oups, shouldn't occur, please contact the developer: teamEvent isn't of type TeamEventDO: $teamEventObject")
        } else {
          generator.addEvent(teamEventObject)
        }
      }

      teamCalDao.find(calId, checkAccess = false)?.let { cal ->
        processedTeamCals.add(cal)
        if (!cal.includeLeaveDaysForGroups.isNullOrBlank() || !cal.includeLeaveDaysForUsers.isNullOrBlank()) {
          val userIds = User.toLongArray(cal.includeLeaveDaysForUsers)?.toSet()
          val groupIds = Group.toLongArray(cal.includeLeaveDaysForGroups)?.toSet()

          val vacations = vacationCache.getVacationForPeriodAndUsers(
            eventDateFromLimit.localDate,
            eventDateUntilLimit.localDate,
            groupIds,
            userIds
          )
          vacations.forEach { vacation ->
            val title = "${translate("vacation")}: ${vacation.employee?.user?.getFullname()}"
            if (!vacationEvents.contains(vacation.id) && vacation.startDate != null && vacation.endDate != null) {
              vacationEvents.add(vacation.id!!)
              // Event doesn't yet exist:
              generator.addEvent(
                PFDay.from(vacation.startDate!!).utilDate,
                PFDay.from(vacation.endDate!!).utilDate,
                true,
                title,
                "vacation-${vacation.id}"
              )
            }
          }
        }
      }
    }
    return processedTeamCals
  }

  /**
   * @return processed time sheet user (for creating filename of export) or null if no time sheet user was given.
   */
  private fun readTimesheets(generator: ICalGenerator, timesheetUser: PFUserDO?) {
    if (timesheetUser == null) {
      return
    }
    val dt = PFDateTime.now()
    // initializes timesheet filter
    val filter = TimesheetFilter()
    filter.userId = timesheetUser.id
    filter.isDeleted = false
    val stopTime = dt.plusMonths(CalendarFeedConst.PERIOD_IN_MONTHS.toLong())
    filter.stopTime = stopTime.utilDate
    val startTime = dt.minusMonths(2 * CalendarFeedConst.PERIOD_IN_MONTHS.toLong())
    filter.startTime = startTime.utilDate
    val timesheetList = timesheetDao.select(filter) ?: return
    // iterate over all timesheets and adds each event to the calendar
    for (timesheet in timesheetList) {
      val uid = TeamCalConfig.get().createTimesheetUid(timesheet.id)
      val summary = CalendarHelper.getTitle(timesheet) + " (ts)"
      val vEvent = generator.convertVEvent(timesheet.startTime, timesheet.stopTime, false, summary, uid)
      if (StringUtils.isNotBlank(timesheet.description)) {
        vEvent.properties.add(Description(timesheet.description))
      }
      if (StringUtils.isNotBlank(timesheet.location)) {
        vEvent.properties.add(Location(timesheet.location))
      }
      generator.addEvent(vEvent)
    }
  }

  /**
   * @return true, if holidays where exported, otherwise false.
   */
  private fun readHolidays(generator: ICalGenerator, params: Map<String, String>): Boolean {
    if ("true" != params[CalendarFeedConst.PARAM_NAME_HOLIDAYS]) {
      return false
    }
    val holidaysFrom = now().beginOfYear.plusYears(-2)
    val holidayTo = holidaysFrom.plusYears(6)
    var day = holidaysFrom
    val holidays = instance
    var idCounter = 0
    var paranoiaCounter = 0
    do {
      if (++paranoiaCounter > 4000) {
        log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of buildEvents.")
        break
      }
      if (!holidays.isHoliday(day)) {
        day = day.plusDays(1)
        continue
      }
      val title: String?
      val holidayInfo = holidays.getHolidayInfo(day)
      title = if (holidayInfo.startsWith("calendar.holiday.")) {
        translate(holidayInfo)
      } else {
        holidayInfo
      }
      generator.addEvent(holidaysFrom.utilDate, holidayTo.utilDate, true, title, "pf-holiday" + ++idCounter)
      day = day.plusDays(1)
    } while (!day.isAfter(holidayTo))
    return true
  }

  private fun readWeeksOfYear(generator: ICalGenerator, params: Map<String, String>): Boolean {
    val weeksOfYear = params[CalendarFeedConst.PARAM_NAME_WEEK_OF_YEARS]
    if ("true" != weeksOfYear) {
      return false
    }
    var from = PFDateTime.now()
    from = from.beginOfYear.minusYears(2).beginOfWeek
    val to = from
    to.plusYears(6)
    var paranoiaCounter = 0
    do {
      generator.addEvent(
        to.utilDate, to.utilDate, true,
        ThreadLocalUserContext.getLocalizedString("calendar.weekOfYearShortLabel") + " " + to.weekOfYear,
        "pf-weekOfYear" + to.year + "-" + paranoiaCounter
      )
      to.plusWeeks(1)
      if (++paranoiaCounter > 500) {
        log.warn("Dear developer, please have a look here, paranoiaCounter exceeded! Aborting calculation of weeks of year.")
      }
    } while (to.isBefore(to))
    return true
  }

  companion object {
    const val PARAM_EXPORT_REMINDER = "exportReminders"

    fun decryptRequestParams(
      request: HttpServletRequest,
      userId: Long,
      userAuthenticationsService: UserAuthenticationsService
    )
        : Map<String, String>? {
      val q = request.getParameter("q")
      if (q.isNullOrBlank()) {
        log.info("Parameter 'q' with encrypted credentials not found in request parameters. Rest call denied.")
        return null
      }
      // Parameters of q are encrypted by user's token for calendar subscriptions:
      val decryptedParams = userAuthenticationsService.decrypt(userId, UserTokenType.CALENDAR_REST, q)
        ?: run {
          val msg =
            "Bad request, can't decrypt parameter q (may-be the user's authentication token was changed): ${request.queryString}"
          log.error(msg)
          logSecurityWarn(this::class.java, "${UserTokenType.CALENDAR_REST.name} AUTHENTICATION FAILED", msg)
          return null
        }
      return StringHelper.getKeyValues(decryptedParams, "&")
    }
  }
}
