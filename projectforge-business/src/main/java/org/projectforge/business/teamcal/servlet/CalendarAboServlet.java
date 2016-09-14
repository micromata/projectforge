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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.MDC;
import org.joda.time.DateTime;
import org.projectforge.ProjectForgeApp;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.common.CalendarHelper;
import org.projectforge.business.teamcal.model.CalendarFeedConst;
import org.projectforge.business.teamcal.service.TeamCalCalendarFeedHook;
import org.projectforge.business.teamcal.service.TeamCalService;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.service.UserService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;

/**
 * Feed Servlet, which generates a 'text/calendar' output of the last four mounts. Currently relevant informations are
 * date, start- and stop time and last but not least the location of an event.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@WebServlet("/export/ProjectForge.ics")
public class CalendarAboServlet extends HttpServlet
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CalendarAboServlet.class);

  private static final long serialVersionUID = 1480433876190009435L;

  /**
   * setup event is needed for empty calendars
   */
  public static final String SETUP_EVENT = "SETUP EVENT";

  @Autowired
  private TimesheetDao timesheetDao;

  @Autowired
  private AccessChecker accessChecker;

  private WebApplicationContext springContext;

  @Autowired
  private UserService userService;

  @Autowired
  private TeamCalService teamCalService;

  @Autowired
  private TeamCalCalendarFeedHook teamCalCalendarFeedHook;

  @Override
  public void init(final ServletConfig config) throws ServletException
  {
    super.init(config);
    springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
    final AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
    beanFactory.autowireBean(this);
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException
  {
    if (ProjectForgeApp.getInstance().isUpAndRunning() == false) {
      log.error(
          "System isn't up and running, CalendarFeed call denied. The system is may-be in start-up phase or in maintenance mode.");
      resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }
    PFUserDO user = null;
    String logMessage = null;
    try {
      MDC.put("ip", req.getRemoteAddr());
      MDC.put("session", req.getSession().getId());
      if (StringUtils.isBlank(req.getParameter("user")) || StringUtils.isBlank(req.getParameter("q"))) {
        resp.sendError(HttpStatus.SC_BAD_REQUEST);
        log.error("Bad request, parameters user and q not given. Query string is: " + req.getQueryString());
        return;
      }
      final String encryptedParams = req.getParameter("q");
      final Integer userId = NumberHelper.parseInteger(req.getParameter("user"));
      if (userId == null) {
        log.error("Bad request, parameter user is not an integer: " + req.getQueryString());
        return;
      }
      user = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache().getUser(userId);
      if (user == null) {
        log.error("Bad request, user not found: " + req.getQueryString());
        return;
      }
      ThreadLocalUserContext.setUser(getUserGroupCache(), user);
      MDC.put("user", user.getUsername());
      final String decryptedParams = userService.decrypt(userId, encryptedParams);
      if (decryptedParams == null) {
        log.error("Bad request, can't decrypt parameter q (may-be the user's authentication token was changed): "
            + req.getQueryString());
        return;
      }
      final Map<String, String> params = StringHelper.getKeyValues(decryptedParams, "&");
      final Calendar calendar = createCal(params, userId, params.get("token"),
          params.get(CalendarFeedConst.PARAM_NAME_TIMESHEET_USER));
      final StringBuffer buf = new StringBuffer();
      boolean first = true;
      for (final Map.Entry<String, String> entry : params.entrySet()) {
        if ("token".equals(entry.getKey()) == true) {
          continue;
        }
        first = StringHelper.append(buf, first, entry.getKey(), ", ");
        buf.append("=").append(entry.getValue());
      }
      logMessage = buf.toString();
      log.info("Getting calendar entries for: " + logMessage);

      if (calendar == null) {
        resp.sendError(HttpStatus.SC_BAD_REQUEST);
        log.error("Bad request, can't find calendar.");
        return;
      }

      resp.setContentType("text/calendar");
      final CalendarOutputter output = new CalendarOutputter(false);
      try {
        output.output(calendar, resp.getOutputStream());
      } catch (final ValidationException ex) {
        ex.printStackTrace();
      }
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

  /**
   * creates a calendar for the user, identified by his name and authentication key.
   * 
   * @param params
   * 
   * @param userName
   * @param userKey
   * @return a calendar, null if authentication fails
   */
  private Calendar createCal(final Map<String, String> params, final Integer userId, final String authKey,
      final String timesheetUserParam)
  {
    final PFUserDO loggedInUser = userService.getUserByAuthenticationToken(userId, authKey);

    if (loggedInUser == null) {
      return null;
    }
    PFUserDO timesheetUser = null;
    if (StringUtils.isNotBlank(timesheetUserParam) == true) {
      final Integer timesheetUserId = NumberHelper.parseInteger(timesheetUserParam);
      if (timesheetUserId != null) {
        if (timesheetUserId.equals(loggedInUser.getId()) == false) {
          log.error("Not yet allowed: all users are only allowed to download their own time-sheets.");
          return null;
        }
        timesheetUser = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache()
            .getUser(timesheetUserId);
        if (timesheetUser == null) {
          log.error("Time-sheet user with id '" + timesheetUserParam + "' not found.");
          return null;
        }
      }
    }
    // creating a new calendar
    final Calendar calendar = new Calendar();
    final Locale locale = ThreadLocalUserContext.getLocale();
    calendar.getProperties().add(
        new ProdId("-//" + loggedInUser.getDisplayUsername() + "//ProjectForge//" + locale.toString().toUpperCase()));
    calendar.getProperties().add(Version.VERSION_2_0);
    calendar.getProperties().add(CalScale.GREGORIAN);

    // setup event is needed for empty calendars
    calendar.getComponents().add(new VEvent(new net.fortuna.ical4j.model.Date(0), SETUP_EVENT));

    // adding events
    for (final VEvent event : getEvents(params, timesheetUser)) {
      calendar.getComponents().add(event);
    }
    return calendar;
  }

  /**
   * builds the list of events
   * 
   * @return
   */
  private List<VEvent> getEvents(final Map<String, String> params, PFUserDO timesheetUser)
  {
    final PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
    if (loggedInUser == null) {
      throw new AccessException("No logged-in-user found!");
    }
    final List<VEvent> events = new ArrayList<VEvent>();
    final TimeZone timezone = ICal4JUtils.getUserTimeZone();
    final java.util.Calendar cal = java.util.Calendar.getInstance(ThreadLocalUserContext.getTimeZone());

    boolean eventsExist = false;
    final List<VEvent> list = teamCalCalendarFeedHook.getEvents(params, timezone);
    if (list != null && list.size() > 0) {
      events.addAll(list);
      eventsExist = true;
    }

    if (timesheetUser != null) {
      if (loggedInUser.getId().equals(timesheetUser.getId()) == false && isOtherUsersAllowed() == false) {
        // Only project managers, controllers and administrative staff is allowed to subscribe time-sheets of other users.
        log.warn("User tried to get time-sheets of other user: " + timesheetUser);
        timesheetUser = loggedInUser;
      }
      // initializes timesheet filter
      final TimesheetFilter filter = new TimesheetFilter();
      filter.setUserId(timesheetUser.getId());
      filter.setDeleted(false);
      filter.setStopTime(cal.getTime());
      // calculates the offset of the calendar
      final int offset = cal.get(java.util.Calendar.MONTH) - CalendarFeedConst.PERIOD_IN_MONTHS;
      if (offset < 0) {
        setCalDate(cal, cal.get(java.util.Calendar.YEAR) - 1, 12 + offset);
      } else {
        setCalDate(cal, cal.get(java.util.Calendar.YEAR), offset);
      }
      filter.setStartTime(cal.getTime());

      final List<TimesheetDO> timesheetList = timesheetDao.getList(filter);

      // iterate over all timesheets and adds each event to the calendar
      for (final TimesheetDO timesheet : timesheetList) {

        final String uid = TeamCalConfig.get().createTimesheetUid(timesheet.getId());
        String summary;
        if (eventsExist == true) {
          summary = CalendarHelper.getTitle(timesheet) + " (ts)";
        } else {
          summary = CalendarHelper.getTitle(timesheet);
        }
        final VEvent vEvent = ICal4JUtils.createVEvent(timesheet.getStartTime(), timesheet.getStopTime(), uid, summary);
        if (StringUtils.isNotBlank(timesheet.getDescription()) == true) {
          vEvent.getProperties().add(new Description(timesheet.getDescription()));
        }
        if (StringUtils.isNotBlank(timesheet.getLocation()) == true) {
          vEvent.getProperties().add(new Location(timesheet.getLocation()));
        }
        events.add(vEvent);
      }
    }
    final String holidays = params.get(CalendarFeedConst.PARAM_NAME_HOLIDAYS);
    if ("true".equals(holidays) == true) {
      DateTime holidaysFrom = new DateTime(ThreadLocalUserContext.getDateTimeZone());
      holidaysFrom = holidaysFrom.dayOfYear().withMinimumValue().millisOfDay().withMinimumValue().minusYears(2);
      final DateTime holidayTo = holidaysFrom.plusYears(6);
      events.addAll(teamCalService.getConfiguredHolidaysAsVEvent(holidaysFrom, holidayTo));
    }
    final String weeksOfYear = params.get(CalendarFeedConst.PARAM_NAME_WEEK_OF_YEARS);
    if ("true".equals(weeksOfYear) == true) {
      final DayHolder from = new DayHolder();
      from.setBeginOfYear().add(java.util.Calendar.YEAR, -2).setBeginOfWeek();
      final DayHolder to = new DayHolder(from);
      to.add(java.util.Calendar.YEAR, 6);
      final DayHolder current = new DayHolder(from);
      int paranoiaCounter = 0;
      do {
        final VEvent vEvent = ICal4JUtils.createVEvent(current.getDate(), current.getDate(), "pf-weekOfYear"
            + current.getYear()
            + "-"
            + paranoiaCounter,
            ThreadLocalUserContext.getLocalizedString("calendar.weekOfYearShortLabel") + " " + current.getWeekOfYear(),
            true);
        events.add(vEvent);
        current.add(java.util.Calendar.WEEK_OF_YEAR, 1);
        if (++paranoiaCounter > 500) {
          log.warn(
              "Dear developer, please have a look here, paranoiaCounter exceeded! Aborting calculation of weeks of year.");
        }
      } while (current.before(to) == true);
    }
    // Integer hrPlanningUserId = NumberHelper.parseInteger(params.get(PARAM_NAME_HR_PLANNING));
    // if (hrPlanningUserId != null) {
    // if (loggedInUser.getId().equals(hrPlanningUserId) == false && isOtherUsersAllowed() == false) {
    // // Only project managers, controllers and administrative staff is allowed to subscribe time-sheets of other users.
    // log.warn("User tried to get time-sheets of other user: " + timesheetUser);
    // hrPlanningUserId = loggedInUser.getId();
    // }
    // final HRPlanningDao hrPlanningDao = Registry.instance().getDao(HRPlanningDao.class);
    // final HRPlanningEventsProvider hrPlanningEventsProvider = new HRPlanningEventsProvider(new CalendarFilter().setShowPlanning(true)
    // .setTimesheetUserId(hrPlanningUserId), hrPlanningDao);
    // DateTime planningFrom = new DateTime(ThreadLocalUserContext.getDateTimeZone());
    // planningFrom = planningFrom.dayOfYear().withMinimumValue().millisOfDay().withMinimumValue().minusYears(1);
    // final DateTime planningTo = planningFrom.plusYears(4);
    // for (final Event event : hrPlanningEventsProvider.getEvents(planningFrom, planningTo)) {
    // final Date fromDate = event.getStart().toDate();
    // final Date toDate = event.getEnd() != null ? event.getEnd().toDate() : fromDate;
    // final VEvent vEvent = ICal4JUtils.createVEvent(fromDate, toDate, "pf-hr-planning" + event.getId(), event.getTitle(), true);
    // events.add(vEvent);
    // }
    // }
    return events;
  }

  /**
   * sets the calendar to a special date. Used to calculate the year offset of an negative time period. When the time
   * period is set to 4 month and the current month is at the begin of a year, the year-number must be decremented by
   * one
   * 
   * @param cal
   * @param year
   * @param mounth
   */
  private void setCalDate(final java.util.Calendar cal, final int year, final int mounth)
  {
    cal.clear();
    cal.set(java.util.Calendar.YEAR, year);
    cal.set(java.util.Calendar.MONTH, mounth);
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
