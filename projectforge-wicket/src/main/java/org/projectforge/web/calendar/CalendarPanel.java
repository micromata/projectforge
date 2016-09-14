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

package org.projectforge.web.calendar;

import java.sql.Timestamp;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.projectforge.Const;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.humanresources.HRPlanningDao;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.address.AddressViewPage;
import org.projectforge.web.address.BirthdayEventsProvider;
import org.projectforge.web.humanresources.HRPlanningEventsProvider;
import org.projectforge.web.timesheet.TimesheetEditPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.components.DatePickerUtils;
import org.projectforge.web.wicket.components.JodaDatePanel;

import net.ftlines.wicket.fullcalendar.CalendarResponse;
import net.ftlines.wicket.fullcalendar.Event;
import net.ftlines.wicket.fullcalendar.EventProvider;
import net.ftlines.wicket.fullcalendar.EventSource;
import net.ftlines.wicket.fullcalendar.callback.CalendarDropMode;
import net.ftlines.wicket.fullcalendar.callback.ClickedEvent;
import net.ftlines.wicket.fullcalendar.callback.DroppedEvent;
import net.ftlines.wicket.fullcalendar.callback.ResizedEvent;
import net.ftlines.wicket.fullcalendar.callback.SelectedRange;
import net.ftlines.wicket.fullcalendar.callback.View;

public class CalendarPanel extends Panel
{
  private static final long serialVersionUID = -8491059902148238143L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CalendarPanel.class);

  @SpringBean
  protected transient AccessChecker accessChecker;

  @SpringBean
  private transient AddressDao addressDao;

  @SpringBean
  private transient HRPlanningDao hrPlanningDao;

  @SpringBean
  private transient TimesheetDao timesheetDao;

  private MyFullCalendar calendar;

  private BirthdayEventsProvider birthdayEventsProvider;

  private HolidayEventsProvider holidayEventsProvider;

  private HRPlanningEventsProvider hrPlanningEventsProvider;

  private TimesheetEventsProvider timesheetEventsProvider;

  protected ICalendarFilter filter;

  private boolean refresh;

  private final JodaDatePanel currentDatePanel;

  public CalendarPanel(final String id, final JodaDatePanel currentDatePanel)
  {
    super(id);
    this.currentDatePanel = currentDatePanel;
  }

  /**
   * At default the filter is equals to the customized filter. For the team plug-in the filter is a different filter.
   * 
   * @param filter
   * @param filter
   */
  @SuppressWarnings("serial")
  public void init(final ICalendarFilter filter)
  {
    this.filter = filter;
    final MyFullCalendarConfig config = new MyFullCalendarConfig(this);
    config.setSelectable(true);
    config.setSelectHelper(true);
    config.setLoading("function(bool) { if (bool) $(\"#loading\").show(); else $(\"#loading\").hide(); }");
    // config.setMinTime(new LocalTime(6, 30));
    // config.setMaxTime(new LocalTime(17, 30));
    config.setAllDaySlot(true);
    calendar = new MyFullCalendar("cal", config)
    {
      @Override
      protected void onDateRangeSelected(final SelectedRange range, final CalendarResponse response)
      {
        final String selectedCalendar = filter.getSelectedCalendar();
        if (selectedCalendar == null || Const.EVENT_CLASS_NAME.equals(selectedCalendar) == true) {
          if (log.isDebugEnabled() == true) {
            log.debug(
                "Selected region: " + range.getStart() + " - " + range.getEnd() + " / allDay: " + range.isAllDay());
          }
          if (accessChecker.isRestrictedUser() == true) {
            return;
          }
          final TimesheetDO timesheet = new TimesheetDO();
          if (range.isAllDay() == true) {
            // Start with the first hour displayed (combo-box) or if any time sheets already exists for this date with a time sheet starts
            // with the stop date of the last time sheet of the current day.
            final TimesheetDO latest = timesheetEventsProvider.getLatestTimesheetOfDay(range.getEnd());
            if (latest != null) {
              timesheet.setStartDate(latest.getStopTime()).setStopTime(latest.getStopTime());
            } else {
              final Integer firstHourOfDay = filter.getFirstHour();
              final DateTime start = range.getStart().withHourOfDay(firstHourOfDay != null ? firstHourOfDay : 8);
              final long millis = DateHelper.getDateTimeAsMillis(start);
              timesheet.setStartDate(millis).setStopTime(millis);
            }
          } else {
            timesheet.setStartDate(DateHelper.getDateTimeAsMillis(range.getStart()))//
                .setStopTime(DateHelper.getDateTimeAsMillis(range.getEnd()));
          }
          if (filter.getTimesheetUserId() != null) {
            timesheetDao.setUser(timesheet, filter.getTimesheetUserId());
          }
          final TimesheetEditPage timesheetEditPage = new TimesheetEditPage(timesheet);
          timesheetEditPage.setReturnToPage((WebPage) getPage());
          setResponsePage(timesheetEditPage);
        } else {
          onDateRangeSelectedHook(filter.getSelectedCalendar(), range, response);
        }
      }

      /**
       * Event was moved, a new start time was chosen.
       * 
       * @see net.ftlines.wicket.fullcalendar.FullCalendar#onEventDropped(net.ftlines.wicket.fullcalendar.callback.DroppedEvent,
       *      net.ftlines.wicket.fullcalendar.CalendarResponse)
       */
      @Override
      protected boolean onEventDropped(final DroppedEvent event, final CalendarResponse response)
      {
        // default mode is move and edit
        CalendarDropMode dropMode = CalendarDropMode.MOVE_EDIT;
        final StringValue parameterValue = RequestCycle.get().getRequest().getQueryParameters()
            .getParameterValue("which");
        if (parameterValue != null) {
          try {
            dropMode = CalendarDropMode.fromAjaxTarget(parameterValue.toString());
          } catch (final Exception ex) {
            log.warn("Unable to get calendar drop mode for given value, using default mode. Given mode: "
                + parameterValue.toString());
          }
        }
        if (log.isDebugEnabled() == true) {
          log.debug("Event drop. eventId: "
              + event.getEvent().getId()
              + " eventClass"
              + event.getEvent().getClassName()
              + " sourceId: "
              + event.getSource().getUuid()
              + " dayDelta: "
              + event.getDaysDelta()
              + " minuteDelta: "
              + event.getMinutesDelta()
              + " allDay: "
              + event.isAllDay());
          log.debug("Original start time: " + event.getEvent().getStart() + ", original end time: "
              + event.getEvent().getEnd());
          log.debug("New start time: " + event.getNewStartTime() + ", new end time: " + event.getNewEndTime());
        }
        modifyEvent(event.getEvent(), event.getNewStartTime(), event.getNewEndTime(), dropMode, response);
        return false;
      }

      @Override
      protected boolean onEventResized(final ResizedEvent event, final CalendarResponse response)
      {
        if (log.isDebugEnabled() == true) {
          log.debug("Event resized. eventId: "
              + event.getEvent().getId()
              + " eventClass"
              + event.getEvent().getClassName()
              + " sourceId: "
              + event.getSource().getUuid()
              + " dayDelta: "
              + event.getDaysDelta()
              + " minuteDelta: "
              + event.getMinutesDelta());
        }
        modifyEvent(event.getEvent(), null, event.getNewEndTime(), CalendarDropMode.MOVE_EDIT, response);
        return false;
      }

      @Override
      protected void onEventClicked(final ClickedEvent clickedEvent, final CalendarResponse response)
      {
        final Event event = clickedEvent.getEvent();
        final String eventId = event != null ? event.getId() : null;
        final String eventClassName = event != null ? event.getClassName() : null;
        if (log.isDebugEnabled() == true) {
          log.debug("Event clicked. eventId: "
              + eventId
              + " eventClass"
              + event.getClassName()
              + ", sourceId: "
              + clickedEvent.getSource().getUuid());
        }
        if (eventId != null && eventClassName != null) {
          if (Const.EVENT_CLASS_NAME.startsWith(eventClassName) == true) {
            // User clicked on a time sheet, show the time sheet:
            final Integer id = NumberHelper.parseInteger(eventId);
            final PageParameters parameters = new PageParameters();
            parameters.add(AbstractEditPage.PARAMETER_KEY_ID, id);
            final TimesheetEditPage timesheetEditPage = new TimesheetEditPage(parameters);
            timesheetEditPage.setReturnToPage((WebPage) getPage());
            setResponsePage(timesheetEditPage);
            return;
          } else if (Const.BREAK_EVENT_CLASS_NAME.startsWith(eventClassName) == true) {
            // User clicked on a break (between time sheets), create new time sheet with times of the break:
            final TimesheetDO breaksTimesheet = timesheetEventsProvider.getBreakTimesheet(eventId);
            final TimesheetEditPage timesheetEditPage = new TimesheetEditPage(breaksTimesheet);
            timesheetEditPage.setReturnToPage((WebPage) getPage());
            setResponsePage(timesheetEditPage);
            return;
          } else if (BirthdayEventsProvider.EVENT_CLASS_NAME.startsWith(eventClassName) == true) {
            // User clicked on birthday, show the address:
            final Integer id = NumberHelper.parseInteger(eventId);
            final PageParameters parameters = new PageParameters();
            parameters.add(AbstractEditPage.PARAMETER_KEY_ID, id);
            final AddressViewPage addressViewPage = new AddressViewPage(parameters);
            setResponsePage(addressViewPage);
            addressViewPage.setReturnToPage((WebPage) getPage());
            return;
          }
          onEventClickedHook(clickedEvent, response, event, eventId, eventClassName);
        }
        response.refetchEvents();
      }

      @Override
      protected void onViewDisplayed(final View view, final CalendarResponse response)
      {
        if (log.isDebugEnabled() == true) {
          log.debug("View displayed. viewType: " + view.getType().name() + ", start: " + view.getStart() + ", end: "
              + view.getEnd());
        }
        response.refetchEvents();
        setStartDate(view.getStart());
        filter.setViewType(view.getType());
        // Need calling getEvents for getting correct duration label, it's not predictable what will be called first: onViewDisplayed or
        // getEvents.
        timesheetEventsProvider.getEvents(view.getVisibleStart().toDateTime(), view.getVisibleEnd().toDateTime());
        onCallGetEventsHook(view, response);
        if (currentDatePanel != null) {
          currentDatePanel.getDateField().modelChanged();
          response.getTarget().add(currentDatePanel.getDateField());
          response.getTarget().appendJavaScript(
              DatePickerUtils.getDatePickerInitJavaScript(currentDatePanel.getDateField().getMarkupId(), true));
        }
        // Set interval on refresh the timeline.
        response.getTarget().appendJavaScript(
            "if(first){ first = false; window.setInterval(setTimeline, 60000); } try { setTimeline(); } catch(err) { }");
        response.getTarget().add(((CalendarPage) getPage()).getForm().getDurationLabel());
      }
    };
    calendar.setMarkupId("calendar");
    add(calendar);
    setConfig();

    // Time sheets
    EventSource eventSource = new EventSource();
    timesheetEventsProvider = new TimesheetEventsProvider(timesheetDao, filter);
    eventSource.setEventsProvider(timesheetEventsProvider);
    eventSource.setEditable(true);
    config.add(eventSource);
    // Holidays:
    eventSource = new EventSource();
    holidayEventsProvider = new HolidayEventsProvider();
    eventSource.setEventsProvider(holidayEventsProvider);
    eventSource.setEditable(false);
    config.add(eventSource);
    // HR planning:
    eventSource = new EventSource();
    hrPlanningEventsProvider = new HRPlanningEventsProvider(filter, hrPlanningDao);
    eventSource.setEventsProvider(hrPlanningEventsProvider);
    eventSource.setEditable(false);
    eventSource.setBackgroundColor("#0080FF");
    eventSource.setColor("#0080FF");
    config.add(eventSource);
    // Birthdays:
    eventSource = new EventSource();
    birthdayEventsProvider = new BirthdayEventsProvider(filter, addressDao,
        accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP) == false);
    eventSource.setEventsProvider(birthdayEventsProvider);
    eventSource.setEditable(false);
    // The default color of birthdays (not favorites), should be gray, see BirthdayEventsProvider for colors of birthdays of favorites.
    eventSource.setBackgroundColor("#EEEEEE");
    eventSource.setColor("#EEEEEE");
    eventSource.setTextColor("#222222");
    config.add(eventSource);
    onRegisterEventSourceHook(config, filter);
  }

  /**
   * Hook method for overwriting children, which is called, when an date range event occurs which can not be handled
   * through this page.
   * 
   * @param selectedCalendar
   * @param range
   * @param response
   */
  protected void onDateRangeSelectedHook(final String selectedCalendar, final SelectedRange range,
      final CalendarResponse response)
  {
    // by default nothing happens here
  }

  /**
   * Hook method for overwriting children, which is called, when an event source should be registered
   * 
   * @param config
   * @param filter
   */
  protected void onRegisterEventSourceHook(final MyFullCalendarConfig config, final ICalendarFilter filter)
  {
    // by default nothing happens here
  }

  /**
   * Hook method for overwriting children, which is called, when an event should be modifies which could not be handled
   * through this page.<br/>
   * This could occur, when {@link Event} belongs to an {@link EventProvider} of a child implementation.
   * 
   * @param event
   * @param newStartTime
   * @param newEndTime
   * @param dropMode
   * @param response
   */
  protected void onModifyEventHook(final Event event, final DateTime newStartTime, final DateTime newEndTime,
      final CalendarDropMode dropMode, final CalendarResponse response)
  {
    // by default nothing happens here
  }

  /**
   * Hook method for overwriting children, which is called, when the {@link EventProvider} should be refreshed.<br/>
   * Please call forceReload on your provider.
   */
  protected void onRefreshEventProvider()
  {
    // by default nothing happens here
  }

  /**
   * Hook method for overwriting children, which is called, when the pre initialization of the calendars are made.<br/>
   * Please call getEvents on you {@link EventProvider}.
   * 
   * @param response
   * @param view
   * 
   */
  protected void onCallGetEventsHook(final View view, final CalendarResponse response)
  {
    // by default nothing happens here
  }

  /**
   * Hook method for overwriting children, which is called, when something in the calendar was clicked
   * 
   * @param clickedEvent
   * @param response
   * @param event
   * @param eventId
   * @param eventClassName
   */
  protected void onEventClickedHook(final ClickedEvent clickedEvent, final CalendarResponse response, final Event event,
      final String eventId, final String eventClassName)
  {
    // by default nothing happens here
  }

  private void modifyEvent(final Event event, final DateTime newStartTime, final DateTime newEndTime,
      final CalendarDropMode dropMode,
      final CalendarResponse response)
  {
    final String eventId = event != null ? event.getId() : null;
    final String eventClassName = event != null ? event.getClassName() : null;
    if (eventId != null && Const.EVENT_CLASS_NAME.equals(eventClassName) == true) {
      // User clicked on a time sheet, show the time sheet:
      final Integer id = NumberHelper.parseInteger(eventId);
      final TimesheetDO dbTimesheet = timesheetDao.internalGetById(id);
      if (dbTimesheet == null) {
        return;
      }
      final TimesheetDO timesheet = new TimesheetDO();
      timesheet.copyValuesFrom(dbTimesheet);
      final Long newStartTimeMillis = newStartTime != null ? DateHelper.getDateTimeAsMillis(newStartTime) : null;
      final Long newEndTimeMillis = newEndTime != null ? DateHelper.getDateTimeAsMillis(newEndTime) : null;
      if (newStartTimeMillis != null) {
        timesheet.setStartDate(newStartTimeMillis);
      }
      if (newEndTimeMillis != null) {
        timesheet.setStopTime(new Timestamp(newEndTimeMillis));
      }
      final PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
      if (CalendarDropMode.MOVE_SAVE.equals(dropMode) == true || CalendarDropMode.MOVE_EDIT.equals(dropMode) == true) {
        if (timesheetDao.hasUpdateAccess(loggedInUser, timesheet, dbTimesheet, false) == false) {
          // User has no update access, therefore ignore this request...
          return;
        }
        if (CalendarDropMode.MOVE_SAVE.equals(dropMode) == true) {
          try {
            timesheetDao.update(timesheet);
            setResponsePage(getPage());
          } catch (IllegalArgumentException ex) {
            if (ex.getMessage().equals("Kost2Id of time sheet is not available in the task's kost2 list!")
                || ex.getMessage().equals("Kost2Id can't be given for task without any kost2 entries!")) {
              TimesheetEditPage timesheetEditPage = new TimesheetEditPage(timesheet);
              timesheetEditPage.error(getString("timesheet.error.copyNoMatchingKost2"));
              setResponsePage(timesheetEditPage.setReturnToPage((WebPage) getPage()));
              return;
            } else {
              throw ex;
            }
          }
        } else {
          setResponsePage(new TimesheetEditPage(timesheet).setReturnToPage((WebPage) getPage()));
        }
        return;
      }
      // Copy this time sheet:
      timesheet.setId(null);
      timesheet.setDeleted(false);
      timesheetDao.setUser(timesheet, loggedInUser.getId()); // Copy for own user.
      if (CalendarDropMode.COPY_SAVE.equals(dropMode) == true) {
        if (timesheetDao.hasInsertAccess(loggedInUser, timesheet, false) == false) {
          // User has no insert access, therefore ignore this request...
          this.error(getString("timesheet.error.taskNotBookable.taskClosedForBooking"));
          return;
        }
        try {
          timesheetDao.save(timesheet);
        } catch (IllegalArgumentException ex) {
          if (ex.getMessage().equals("Kost2Id of time sheet is not available in the task's kost2 list!")
              || ex.getMessage().equals("Kost2Id can't be given for task without any kost2 entries!")) {
            TimesheetEditPage timesheetEditPage = new TimesheetEditPage(timesheet);
            timesheetEditPage.error(getString("timesheet.error.copyNoMatchingKost2"));
            setResponsePage(timesheetEditPage.setReturnToPage((WebPage) getPage()));
            return;
          } else {
            throw ex;
          }
        }
        setResponsePage(getPage());
        return;
      } else if (CalendarDropMode.COPY_EDIT.equals(dropMode) == true) {
        setResponsePage(new TimesheetEditPage(timesheet).setReturnToPage((WebPage) getPage()));
      } else {
        // CANCEL -> should be handled through javascript now
        setResponsePage(getPage());
      }
    } else {
      onModifyEventHook(event, newStartTime, newEndTime, dropMode, response);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    // Restore current date (e. g. on reload or on coming back from callee page).
    final MyFullCalendarConfig config = calendar.getConfig();
    final DateMidnight startDate = filter.getStartDate();
    if (startDate != null) {
      config.setYear(startDate.getYear());
      config.setMonth(startDate.getMonthOfYear() - 1);
      config.setDate(startDate.getDayOfMonth());
    }
    config.setDefaultView(filter.getViewType().getCode());
    if (refresh == true) {
      refresh = false;
      timesheetEventsProvider.forceReload();
      birthdayEventsProvider.forceReload();
      hrPlanningEventsProvider.forceReload();
      setConfig();
      onRefreshEventProvider();
    }
  }

  private void setConfig()
  {
    final MyFullCalendarConfig config = calendar.getConfig();
    if (filter.isSlot30() == true) {
      config.setSlotMinutes(30);
    } else {
      config.setSlotMinutes(15);
    }
    if (filter.getFirstHour() != null) {
      config.setFirstHour(filter.getFirstHour());
    }
  }

  /**
   * @param startDate the startDate to set
   * @return this for chaining.
   */
  public CalendarPanel setStartDate(final DateMidnight startDate)
  {
    filter.setStartDate(startDate);
    return this;
  }

  /**
   * @return the startDate
   */
  public DateMidnight getStartDate()
  {
    return filter.getStartDate();
  }

  /**
   * Forces to reloaded time sheets in onBeforeRender().
   * 
   * @param refresh the refresh to set
   * @return this for chaining.
   */
  public CalendarPanel forceReload()
  {
    this.refresh = true;
    return this;
  }

  public String getTotalTimesheetDuration()
  {
    return timesheetEventsProvider.formatDuration(timesheetEventsProvider.getTotalDuration());
  }
}
