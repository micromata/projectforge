/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.teamcal.servlet;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.MDC;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.projectforge.ProjectForgeApp;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.common.CalendarHelper;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.ical.ICalGenerator;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.model.CalendarFeedConst;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.service.UserService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;

/**
 * Feed Servlet, which generates a 'text/calendar' output of the last four mounts. Currently relevant information is
 * date, start- and stop time and last but not least the location of an event.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@WebServlet("/export/ProjectForge.ics")
public class CalendarAboServlet extends HttpServlet
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CalendarAboServlet.class);

  private static final long serialVersionUID = 1480433876190009435L;

  public static final String PARAM_EXPORT_REMINDER = "exportReminders";
  public static final String PARAM_EXPORT_ATTENDEES = "exportAttendees";

  @Autowired
  private TimesheetDao timesheetDao;

  @Autowired
  private AccessChecker accessChecker;

  private WebApplicationContext springContext;

  @Autowired
  private UserService userService;

  @Autowired
  private TeamEventService teamEventService;

  @Override
  public void init(final ServletConfig config) throws ServletException
  {
    super.init(config);
    springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
    final AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
    beanFactory.autowireBean(this);
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException
  {
    // check if PF is running
    if (ProjectForgeApp.getInstance().isUpAndRunning() == false) {
      log.error("System isn't up and running, CalendarFeed call denied. The system is may-be in start-up phase or in maintenance mode.");
      resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }

    PFUserDO user = null;
    String logMessage = null;

    try {
      // add logging stuff
      MDC.put("ip", (Object) req.getRemoteAddr());
      MDC.put("session", (Object) req.getSession().getId());

      // read user
      if (StringUtils.isBlank(req.getParameter("user")) || StringUtils.isBlank(req.getParameter("q"))) {
        resp.sendError(HttpStatus.SC_BAD_REQUEST);
        log.error("Bad request, parameters user and q not given. Query string is: " + req.getQueryString());
        return;
      }
      final Integer userId = NumberHelper.parseInteger(req.getParameter("user"));
      if (userId == null) {
        log.error("Bad request, parameter user is not an integer: " + req.getQueryString());
        resp.sendError(HttpStatus.SC_BAD_REQUEST);
        return;
      }

      // read params of request
      final String encryptedParams = req.getParameter("q");
      final String decryptedParams = userService.decrypt(userId, encryptedParams);
      if (decryptedParams == null) {
        log.error("Bad request, can't decrypt parameter q (may-be the user's authentication token was changed): "
            + req.getQueryString());
        return;
      }
      final Map<String, String> params = StringHelper.getKeyValues(decryptedParams, "&");

      // validate user
      user = userService.getUserByAuthenticationToken(userId, params.get("token"));
      if (user == null) {
        log.error("Bad request, user not found: " + req.getQueryString());
        resp.sendError(HttpStatus.SC_BAD_REQUEST);
        return;
      }
      ThreadLocalUserContext.setUser(getUserGroupCache(), user);
      MDC.put("user", (Object) user.getUsername());

      // check timesheet user
      String timesheetUserParam = params.get(CalendarFeedConst.PARAM_NAME_TIMESHEET_USER);
      PFUserDO timesheetUser = null;
      if (timesheetUserParam != null) {
        timesheetUser = this.getTimesheetUser(userId, timesheetUserParam);

        if (timesheetUser == null) {
          resp.sendError(HttpStatus.SC_BAD_REQUEST);
          return;
        }
      }

      // create ical generator
      ICalGenerator generator = ICalGenerator.exportAllFields();
      generator.exportVEventAlarm("true".equals(params.get(PARAM_EXPORT_REMINDER)));

      // read events
      readEventsFromCalendars(generator, params);
      readTimesheets(generator, timesheetUser);
      readHolidays(generator, params);
      readWeeksOfYear(generator, params);

      // setup event is needed for empty calendars
      if (generator.isEmpty()) {
        generator.addEvent(new VEvent(new net.fortuna.ical4j.model.Date(0), TeamCalConfig.SETUP_EVENT));
      }

      final StringBuffer buf = new StringBuffer();

      // create log message
      for (final Map.Entry<String, String> entry : params.entrySet()) {
        if ("token".equals(entry.getKey())) {
          continue;
        }

        buf.append(entry.getKey());
        buf.append("=");
        buf.append(entry.getValue());
        buf.append(", ");
      }
      logMessage = buf.toString();
      log.info("Read calendar entries for: " + logMessage);

      resp.setContentType("text/calendar");
      generator.writeCalendarToOutputStream(resp.getOutputStream());

    } finally {
      log.info("Finished request: " + logMessage);
      ThreadLocalUserContext.setUser(getUserGroupCache(), null);
      MDC.remove("ip");
      MDC.remove("session");
      if (user != null) {
        MDC.remove("user");
      }
    }
  }

  private PFUserDO getTimesheetUser(final Integer userId, final String timesheetUserParam)
  {
    PFUserDO timesheetUser = null;

    if (StringUtils.isNotBlank(timesheetUserParam) == true) {
      final Integer timesheetUserId = NumberHelper.parseInteger(timesheetUserParam);
      if (timesheetUserId != null) {
        if (timesheetUserId.equals(userId) == false) {
          log.error("Not yet allowed: all users are only allowed to download their own time-sheets.");
          return null;
        }
        timesheetUser = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache().getUser(timesheetUserId);

        if (timesheetUser == null) {
          log.error("Time-sheet user with id '" + timesheetUserParam + "' not found.");
          return null;
        }
      }
    }

    //    if (loggedInUser.getId().equals(timesheetUser.getId()) == false && isOtherUsersAllowed() == false) {
    //      // Only project managers, controllers and administrative staff is allowed to subscribe time-sheets of other users.
    //      log.warn("User tried to get time-sheets of other user: " + timesheetUser);
    //      timesheetUser = loggedInUser;
    //    }

    return timesheetUser;
  }

  private void readEventsFromCalendars(final ICalGenerator generator, final Map<String, String> params)
  {
    final String teamCals = params.get("teamCals");
    if (teamCals == null) {
      return;
    }
    final String[] teamCalIds = StringUtils.split(teamCals, ";");
    if (teamCalIds == null) {
      return;
    }

    final TeamEventFilter eventFilter = new TeamEventFilter();
    final DateTime now = DateTime.now();
    final Date eventDateLimit = now.minusYears(1).toDate();
    eventFilter.setDeleted(false);
    eventFilter.setStartDate(eventDateLimit);

    for (final String teamCalId : teamCalIds) {
      final Integer id = Integer.valueOf(teamCalId);
      eventFilter.setTeamCalId(id);
      final List<TeamEvent> teamEvents = teamEventService.getEventList(eventFilter, false);

      if (teamEvents == null || teamEvents.isEmpty()) {
        continue;
      }

      for (final TeamEvent teamEventObject : teamEvents) {
        if (teamEventObject instanceof TeamEventDO == false) {
          log.warn("Oups, shouldn't occur, please contact the developer: teamEvent isn't of type TeamEventDO: " + teamEventObject);
          continue;
        }

        generator.addEvent((TeamEventDO) teamEventObject);
      }
    }
  }

  private void readTimesheets(final ICalGenerator generator, final PFUserDO timesheetUser)
  {
    if (timesheetUser == null) {
      return;
    }

    final java.util.Calendar cal = java.util.Calendar.getInstance(ThreadLocalUserContext.getTimeZone());

    // initializes timesheet filter
    final TimesheetFilter filter = new TimesheetFilter();
    filter.setUserId(timesheetUser.getId());
    filter.setDeleted(false);
    cal.add(java.util.Calendar.MONTH, CalendarFeedConst.PERIOD_IN_MONTHS);
    filter.setStopTime(cal.getTime());
    cal.add(java.util.Calendar.MONTH, -2 * CalendarFeedConst.PERIOD_IN_MONTHS);
    filter.setStartTime(cal.getTime());

    final List<TimesheetDO> timesheetList = timesheetDao.getList(filter);

    // iterate over all timesheets and adds each event to the calendar
    for (final TimesheetDO timesheet : timesheetList) {
      final String uid = TeamCalConfig.get().createTimesheetUid(timesheet.getId());
      final String summary = CalendarHelper.getTitle(timesheet) + " (ts)";

      final VEvent vEvent = generator.convertVEvent(timesheet.getStartTime(), timesheet.getStopTime(), false, uid, summary);

      if (StringUtils.isNotBlank(timesheet.getDescription()) == true) {
        vEvent.getProperties().add(new Description(timesheet.getDescription()));
      }
      if (StringUtils.isNotBlank(timesheet.getLocation()) == true) {
        vEvent.getProperties().add(new Location(timesheet.getLocation()));
      }

      generator.addEvent(vEvent);
    }
  }

  private void readHolidays(final ICalGenerator generator, final Map<String, String> params)
  {
    if ("true".equals(params.get(CalendarFeedConst.PARAM_NAME_HOLIDAYS)) == false) {
      return;
    }

    DateTime holidaysFrom = new DateTime(ThreadLocalUserContext.getDateTimeZone());
    holidaysFrom = holidaysFrom.dayOfYear().withMinimumValue().millisOfDay().withMinimumValue().minusYears(2);
    final DateTime holidayTo = holidaysFrom.plusYears(6);
    DateMidnight day = new DateMidnight(holidaysFrom);
    int idCounter = 0;
    int paranoiaCounter = 0;

    do {
      if (++paranoiaCounter > 4000) {
        log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of buildEvents.");
        break;
      }
      final Date date = day.toDate();
      final TimeZone timeZone = day.getZone().toTimeZone();
      final DayHolder dh = new DayHolder(date, timeZone, null);
      if (dh.isHoliday() == false) {
        day = day.plusDays(1);
        continue;
      }

      final String title;
      final String holidayInfo = dh.getHolidayInfo();
      if (holidayInfo != null && holidayInfo.startsWith("calendar.holiday.") == true) {
        title = ThreadLocalUserContext.getLocalizedString(holidayInfo);
      } else {
        title = holidayInfo;
      }

      generator.addEvent(holidaysFrom.toDate(), holidayTo.toDate(), true, title, "pf-holiday" + (++idCounter));

      day = day.plusDays(1);
    } while (day.isAfter(holidayTo) == false);
  }

  private void readWeeksOfYear(final ICalGenerator generator, final Map<String, String> params)
  {
    final String weeksOfYear = params.get(CalendarFeedConst.PARAM_NAME_WEEK_OF_YEARS);
    if ("true".equals(weeksOfYear) == false) {
      return;
    }

    final DayHolder from = new DayHolder();
    from.setBeginOfYear().add(java.util.Calendar.YEAR, -2).setBeginOfWeek();
    final DayHolder to = new DayHolder(from);
    to.add(java.util.Calendar.YEAR, 6);
    final DayHolder current = new DayHolder(from);
    int paranoiaCounter = 0;
    do {
      generator.addEvent(current.getDate(), current.getDate(), true,
          ThreadLocalUserContext.getLocalizedString("calendar.weekOfYearShortLabel") + " " + current.getWeekOfYear(),
          "pf-weekOfYear" + current.getYear() + "-" + paranoiaCounter);

      current.add(java.util.Calendar.WEEK_OF_YEAR, 1);
      if (++paranoiaCounter > 500) {
        log.warn("Dear developer, please have a look here, paranoiaCounter exceeded! Aborting calculation of weeks of year.");
      }
    } while (current.before(to) == true);
  }

  private boolean isOtherUsersAllowed()
  {
    return accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP,
        ProjectForgeGroup.PROJECT_MANAGER);
  }

  private TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  private UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }

}
