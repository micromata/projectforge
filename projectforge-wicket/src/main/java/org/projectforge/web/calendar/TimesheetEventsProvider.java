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

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.projectforge.Const;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.business.teamcal.common.CalendarHelper;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.business.timesheet.OrderDirection;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.TimePeriod;
import org.projectforge.web.teamcal.event.MyWicketEvent;

import net.ftlines.wicket.fullcalendar.Event;

/**
 * Creates events for FullCalendar.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TimesheetEventsProvider extends MyFullCalendarEventsProvider
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TimesheetEventsProvider.class);

  private static final long serialVersionUID = 2241430630558260146L;

  private final TimesheetDao timesheetDao;

  private final ICalendarFilter calFilter;

  private long totalDuration;

  private Integer month;

  private DateTime firstDayOfMonth;

  private int days;

  // duration by day of month.
  private final long[] durationsPerDayOfMonth = new long[32];

  private final long[] durationsPerDayOfYear = new long[380];

  private Map<String, TimesheetDO> breaksMap;

  private List<TimesheetDO> timesheets;

  /**
   * @param timesheetDao
   * @param calFilter
   */
  public TimesheetEventsProvider(final TimesheetDao timesheetDao, final ICalendarFilter calFilter)
  {
    this.timesheetDao = timesheetDao;
    this.calFilter = calFilter;
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime,
   * org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end)
  {
    totalDuration = 0;
    for (int i = 0; i < durationsPerDayOfMonth.length; i++) {
      durationsPerDayOfMonth[i] = 0;
    }
    for (int i = 0; i < durationsPerDayOfYear.length; i++) {
      durationsPerDayOfYear[i] = 0;
    }
    final Integer userId = calFilter.getTimesheetUserId();
    if (userId == null) {
      return;
    }
    breaksMap = new HashMap<String, TimesheetDO>();
    int breaksCounter = 0;
    final TimesheetFilter filter = new TimesheetFilter();
    filter.setUserId(userId);
    filter.setStartTime(start.toDate());
    filter.setStopTime(end.toDate());
    filter.setOrderType(OrderDirection.ASC);
    timesheets = timesheetDao.getList(filter);
    boolean longFormat = false;
    days = Days.daysBetween(start, end).getDays();
    if (days < 10) {
      // Week or day view:
      longFormat = true;
      month = null;
      firstDayOfMonth = null;
    } else {
      // Month view:
      final DateTime currentMonth = new DateTime(start.plusDays(10), ThreadLocalUserContext.getDateTimeZone()); // Now we're definitely in the right
      // month.
      month = currentMonth.getMonthOfYear();
      firstDayOfMonth = currentMonth.withDayOfMonth(1);
    }
    if (CollectionUtils.isEmpty(timesheets) == false) {
      DateTime lastStopTime = null;
      for (final TimesheetDO timesheet : timesheets) {
        final DateTime startTime = new DateTime(timesheet.getStartTime(), ThreadLocalUserContext.getDateTimeZone());
        final DateTime stopTime = new DateTime(timesheet.getStopTime(), ThreadLocalUserContext.getDateTimeZone());
        if (stopTime.isBefore(start) == true || startTime.isAfter(end) == true) {
          // Time sheet doesn't match time period start - end.
          continue;
        }
        if (calFilter.isShowBreaks() == true) {
          if (lastStopTime != null
              && DateHelper.isSameDay(stopTime, lastStopTime) == true
              && startTime.getMillis() - lastStopTime.getMillis() > 60000) {
            // Show breaks between time sheets of one day (> 60s).
            final Event breakEvent = new Event();
            breakEvent.setEditable(false);
            final String breakId = String.valueOf(++breaksCounter);
            breakEvent.setClassName(Const.BREAK_EVENT_CLASS_NAME).setId(breakId).setStart(lastStopTime)
                .setEnd(startTime)
                .setTitle(getString("timesheet.break"));
            breakEvent.setTextColor("#666666").setBackgroundColor("#F9F9F9").setColor("#F9F9F9");
            events.put(breakId, breakEvent);
            final TimesheetDO breakTimesheet = new TimesheetDO().setStartDate(lastStopTime.toDate())
                .setStopTime(startTime.getMillis());
            breaksMap.put(breakId, breakTimesheet);
          }
          lastStopTime = stopTime;
        }
        final long duration = timesheet.getDuration();
        final MyWicketEvent event = new MyWicketEvent();
        final String id = "" + timesheet.getId();
        event.setClassName(Const.EVENT_CLASS_NAME);
        event.setId(id);
        event.setStart(startTime);
        event.setEnd(stopTime);
        final String title = CalendarHelper.getTitle(timesheet);
        if (longFormat == true) {
          // Week or day view:
          event.setTitle(title + "\n" + getToolTip(timesheet) + "\n" + formatDuration(duration, false));
        } else {
          // Month view:
          event.setTitle(title);
        }
        if (month != null && startTime.getMonthOfYear() != month && stopTime.getMonthOfYear() != month) {
          // Display time sheets of other month as grey blue:
          event.setTextColor("#222222").setBackgroundColor("#ACD9E8").setColor("#ACD9E8");
        }
        events.put(id, event);
        if (month == null || startTime.getMonthOfYear() == month) {
          totalDuration += duration;
          addDurationOfDay(startTime.getDayOfMonth(), duration);
        }
        final int dayOfYear = startTime.getDayOfYear();
        addDurationOfDayOfYear(dayOfYear, duration);
        event.setTooltip(
            getString("timesheet"),
            new String[][] {
                { title },
                { timesheet.getLocation(), getString("timesheet.location") },
                { KostFormatter.formatLong(timesheet.getKost2()), getString("fibu.kost2") },
                { WicketTaskFormatter.getTaskPath(timesheet.getTaskId(), true, OutputType.PLAIN),
                    getString("task") },
                { timesheet.getDescription(), getString("description") } });
      }
    }
    if (calFilter.isShowStatistics() == true) {
      // Show statistics: duration of every day is shown as all day event.
      DateTime day = start;
      final Calendar cal = DateHelper.getCalendar();
      cal.setTime(start.toDate());
      final int numberOfDaysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
      int paranoiaCounter = 0;
      do {
        if (++paranoiaCounter > 1000) {
          log.error(
              "Paranoia counter exceeded! Dear developer, please have a look at the implementation of buildEvents.");
          break;
        }
        final int dayOfYear = day.getDayOfYear();
        final long duration = durationsPerDayOfYear[dayOfYear];
        final boolean firstDayOfWeek = day.getDayOfWeek() == ThreadLocalUserContext.getJodaFirstDayOfWeek();
        if (firstDayOfWeek == false && duration == 0) {
          day = day.plusDays(1);
          continue;
        }
        final Event event = new Event().setAllDay(true);
        final String id = "s-" + (dayOfYear);
        event.setId(id);
        event.setStart(day);
        final String durationString = formatDuration(duration, false);
        if (firstDayOfWeek == true) {
          // Show week of year at top of first day of week.
          long weekDuration = 0;
          for (short i = 0; i < 7; i++) {
            int d = dayOfYear + i;
            if (d > numberOfDaysInYear) {
              d -= numberOfDaysInYear;
            }
            weekDuration += durationsPerDayOfYear[d];
          }
          final StringBuffer buf = new StringBuffer();
          buf.append(getString("calendar.weekOfYearShortLabel")).append(DateHelper.getWeekOfYear(day));
          if (days > 1 && weekDuration > 0) {
            // Show total sum of durations over all time sheets of current week (only in week and month view).
            buf.append(": ").append(formatDuration(weekDuration, false));
          }
          if (duration > 0) {
            buf.append(", ").append(durationString);
          }
          event.setTitle(buf.toString());
        } else {
          event.setTitle(durationString);
        }
        event.setTextColor("#666666").setBackgroundColor("#F9F9F9").setColor("#F9F9F9");
        event.setEditable(false);
        events.put(id, event);
        day = day.plusDays(1);
      } while (day.isAfter(end) == false);
    }
  }

  public TimesheetDO getBreakTimesheet(final String id)
  {
    return breaksMap != null ? breaksMap.get(id) : null;
  }

  public TimesheetDO getLatestTimesheetOfDay(final DateTime date)
  {
    if (timesheets == null) {
      return null;
    }
    TimesheetDO latest = null;
    for (final TimesheetDO timesheet : timesheets) {
      if (DateHelper.isSameDay(timesheet.getStopTime(), date.toDate()) == true) {
        if (latest == null) {
          latest = timesheet;
        } else if (latest.getStopTime().before(timesheet.getStopTime()) == true) {
          latest = timesheet;
        }
      }
    }
    return latest;
  }

  public String formatDuration(final long millis)
  {
    return formatDuration(millis, firstDayOfMonth != null);
  }

  private String formatDuration(final long millis, final boolean showTimePeriod)
  {
    final int[] fields = TimePeriod.getDurationFields(millis, 8, 200);
    final StringBuffer buf = new StringBuffer();
    if (fields[0] > 0) {
      buf.append(fields[0]).append(ThreadLocalUserContext.getLocalizedString("calendar.unit.day")).append(" ");
    }
    buf.append(fields[1]).append(":").append(StringHelper.format2DigitNumber(fields[2]))
        .append(ThreadLocalUserContext.getLocalizedString("calendar.unit.hour"));
    if (showTimePeriod == true) {
      buf.append(" (").append(ThreadLocalUserContext.getLocalizedString("calendar.month")).append(")");
    }
    return buf.toString();
  }

  public static String getToolTip(final TimesheetDO timesheet)
  {
    final String location = timesheet.getLocation();
    final String description = timesheet.getShortDescription();
    final TaskDO task = timesheet.getTask();
    final StringBuffer buf = new StringBuffer();
    if (StringUtils.isNotBlank(location) == true) {
      buf.append(location);
      if (StringUtils.isNotBlank(description) == true) {
        buf.append(": ");
      }
    }
    buf.append(description);
    if (timesheet.getKost2() == null) {
      buf.append("; \n").append(task.getTitle());
    }
    return buf.toString();
  }

  /**
   * @return the duration
   */
  public long getTotalDuration()
  {
    return totalDuration;
  }

  private void addDurationOfDay(final int dayOfMonth, final long duration)
  {
    durationsPerDayOfMonth[dayOfMonth] += duration;
  }

  private void addDurationOfDayOfYear(final int dayOfYear, final long duration)
  {
    durationsPerDayOfYear[dayOfYear] += duration;
  }
}
