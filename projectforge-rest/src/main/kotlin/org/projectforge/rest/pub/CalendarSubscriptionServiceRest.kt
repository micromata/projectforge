/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.Location
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.commons.lang3.StringUtils
import org.projectforge.SystemStatus
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.multitenancy.TenantRegistry
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.business.teamcal.TeamCalConfig
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.common.CalendarHelper
import org.projectforge.business.teamcal.event.TeamEventFilter
import org.projectforge.business.teamcal.event.TeamEventService
import org.projectforge.business.teamcal.event.ical.ICalGenerator
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.teamcal.model.CalendarFeedConst
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserService
import org.projectforge.business.vacation.VacationCache
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.calendar.Holidays.Companion.instance
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDay.Companion.now
import org.projectforge.framework.utils.NumberHelper.parseInteger
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import javax.servlet.http.HttpServletRequest


/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping("/export/ProjectForge.ics")
class CalendarSubscriptionServiceRest {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var accessChecker: AccessChecker

    private lateinit var springContext: WebApplicationContext

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var teamEventService: TeamEventService

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var systemStatus: SystemStatus

    @Autowired
    private lateinit var vacationCache: VacationCache

    @GetMapping
    fun exportCalendar(request: HttpServletRequest,
                       @RequestParam("user") userIdString: String?,
                       @RequestParam("q") q: String?)
            : ResponseEntity<Any> {
        // check if PF is running
        if (!systemStatus.upAndRunning) {
            log.error("System isn't up and running, CalendarFeed call denied. The system is may-be in start-up phase or in maintenance mode.")
            return ResponseEntity(HttpStatus.SERVICE_UNAVAILABLE)
        }
        var user: PFUserDO?
        var logMessage: String? = null
        try { // add logging stuff
            MDC.put("ip", request.remoteAddr)
            MDC.put("session", request.session.id)
            // read user
            if (userIdString.isNullOrBlank() || q.isNullOrBlank()) {
                log.error("Bad request, parameters user and q not given. Query string is: ${request.queryString}")
                return ResponseEntity(HttpStatus.BAD_REQUEST)
            }
            val userId = parseInteger(userIdString) ?: run {
                log.error("Bad request, parameter user is not an integer: ${request.queryString}")
                return ResponseEntity(HttpStatus.BAD_REQUEST)
            }
            // read params of request
            val decryptedParams = userService.decrypt(userId, q) ?: run {
                log.error("Bad request, can't decrypt parameter q (may-be the user's authentication token was changed): ${request.queryString}")
                return ResponseEntity(HttpStatus.BAD_REQUEST)
            }
            val params = StringHelper.getKeyValues(decryptedParams, "&")
            // validate user
            user = userService.getUserByAuthenticationToken(userId, params["token"]) ?: run {
                log.error("Bad request, user not found: ${request.queryString}")
                return ResponseEntity(HttpStatus.BAD_REQUEST)
            }
            ThreadLocalUserContext.setUser(getUserGroupCache(), user)
            MDC.put("user", user.username)
            // check timesheet user
            val timesheetUserParam = params[CalendarFeedConst.PARAM_NAME_TIMESHEET_USER]
            var timesheetUser: PFUserDO? = null
            if (timesheetUserParam != null) {
                timesheetUser = getTimesheetUser(userId, timesheetUserParam) ?: run {
                    log.error("Bad request, timesheet user not found: ${request.queryString}")
                    return ResponseEntity(HttpStatus.BAD_REQUEST)
                }
            }
            // create ical generator
            val generator = ICalGenerator.exportAllFields()
            generator.exportVEventAlarm("true" == params[PARAM_EXPORT_REMINDER])
            // read events
            val processCalendars = readEventsFromCalendars(generator, params)
            val processedTimesheetUser = readTimesheets(generator, timesheetUser)
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
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$safeFilename")
                    .body(resource)
        } finally {
            log.info("Finished request: $logMessage")
            ThreadLocalUserContext.setUser(getUserGroupCache(), null)
            MDC.remove("ip")
            MDC.remove("session")
            MDC.remove("user")
        }
    }

    private fun getTimesheetUser(userId: Int, timesheetUserParam: String): PFUserDO? {
        var timesheetUser: PFUserDO? = null
        if (StringUtils.isNotBlank(timesheetUserParam)) {
            val timesheetUserId = parseInteger(timesheetUserParam)
            if (timesheetUserId != null) {
                if (timesheetUserId != userId) {
                    log.error("Not yet allowed: all users are only allowed to download their own time-sheets.")
                    return null
                }
                timesheetUser = TenantRegistryMap.getInstance().tenantRegistry.userGroupCache.getUser(timesheetUserId)
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
        val vacationEvents = mutableSetOf<Int>() // For avoiding multiple entries of vacation days. Ids of vacation event.
        var processedTeamCals = mutableListOf<TeamCalDO>()
        for (teamCalIdString in teamCalIds) {
            val calId = Integer.valueOf(teamCalIdString)
            eventFilter.teamCalId = calId
            val teamEvents = teamEventService.getEventList(eventFilter, false)
            teamEvents?.forEach { teamEventObject ->
                if (teamEventObject !is TeamEventDO) {
                    log.warn("Oups, shouldn't occur, please contact the developer: teamEvent isn't of type TeamEventDO: $teamEventObject")
                } else {
                    generator.addEvent(teamEventObject)
                }
            }

            teamCalDao.internalGetById(calId)?.let { cal ->
                processedTeamCals.add(cal)
                if (!cal.includeLeaveDaysForGroups.isNullOrBlank() || !cal.includeLeaveDaysForUsers.isNullOrBlank()) {
                    val userIds = User.toIntArray(cal.includeLeaveDaysForUsers)?.toSet()
                    val groupIds = Group.toIntArray(cal.includeLeaveDaysForGroups)?.toSet()

                    val vacations = vacationCache.getVacationForPeriodAndUsers(eventDateFromLimit.localDate, eventDateUntilLimit.localDate, groupIds, userIds)
                    vacations.forEach { vacation ->
                        val title = "${translate("vacation")}: ${vacation.employee?.user?.getFullname()}"
                        if (!vacationEvents.contains(vacation.id)) {
                            vacationEvents.add(vacation.id)
                            // Event doesn't yet exist:
                            generator.addEvent(PFDay.from(vacation.startDate)!!.utilDate, PFDay.from(vacation.endDate)!!.utilDate, true, title, "vacation-${vacation.id}")
                        }
                    }
                }
            }
        }
        return processedTeamCals
    }

    private data class VactionEvent(val employee: String, val startDate: LocalDate, val endDate: LocalDate)

    /**
     * @return processed time sheet user (for creating filename of export) or null if no time sheet user was given.
     */
    private fun readTimesheets(generator: ICalGenerator, timesheetUser: PFUserDO?): PFUserDO? {
        if (timesheetUser == null) {
            return null
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
        val timesheetList = timesheetDao.getList(filter)
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
        return timesheetUser
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
            generator.addEvent(to.utilDate, to.utilDate, true,
                    ThreadLocalUserContext.getLocalizedString("calendar.weekOfYearShortLabel") + " " + to.weekOfYear,
                    "pf-weekOfYear" + to.year + "-" + paranoiaCounter)
            to.plusWeeks(1)
            if (++paranoiaCounter > 500) {
                log.warn("Dear developer, please have a look here, paranoiaCounter exceeded! Aborting calculation of weeks of year.")
            }
        } while (to.isBefore(to))
        return true
    }

    private fun isOtherUsersAllowed(): Boolean {
        return accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
                ProjectForgeGroup.CONTROLLING_GROUP,
                ProjectForgeGroup.PROJECT_MANAGER)
    }

    private fun getTenantRegistry(): TenantRegistry {
        return TenantRegistryMap.getInstance().tenantRegistry
    }

    private fun getUserGroupCache(): UserGroupCache? {
        return getTenantRegistry().userGroupCache
    }

    companion object {
        private val log = LoggerFactory.getLogger(CalendarSubscriptionServiceRest::class.java)

        const val PARAM_EXPORT_REMINDER = "exportReminders"
        const val PARAM_EXPORT_ATTENDEES = "exportAttendees"
    }
}
