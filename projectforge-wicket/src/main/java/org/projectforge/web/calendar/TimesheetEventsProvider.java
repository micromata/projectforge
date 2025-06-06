/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import net.ftlines.wicket.fullcalendar.Event;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.projectforge.Constants;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.fibu.OldKostFormatter;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.business.teamcal.CalendarHelper;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.business.timesheet.OrderDirection;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.time.TimePeriod;
import org.projectforge.web.teamcal.event.MyWicketEvent;

import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates events for FullCalendar.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TimesheetEventsProvider extends MyFullCalendarEventsProvider {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TimesheetEventsProvider.class);

  private static final long serialVersionUID = 2241430630558260146L;

  private final TimesheetDao timesheetDao;

  private final ICalendarFilter calFilter;

  private long totalDuration;

  private Month month;

  private PFDateTime firstDayOfMonth;

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
  public TimesheetEventsProvider(final TimesheetDao timesheetDao, final ICalendarFilter calFilter) {
    this.timesheetDao = timesheetDao;
    this.calFilter = calFilter;
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime,
   * org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end) {
    final PFDateTime startDate = PFDateTime.from(start.toDate()).getBeginOfDay();
    final PFDateTime endDate = PFDateTime.from(end.toDate()).getEndOfDay();
    totalDuration = 0;
    for (int i = 0; i < durationsPerDayOfMonth.length; i++) {
      durationsPerDayOfMonth[i] = 0;
    }
    for (int i = 0; i < durationsPerDayOfYear.length; i++) {
      durationsPerDayOfYear[i] = 0;
    }
    final Long userId = calFilter.getTimesheetUserId();
    if (userId == null) {
      return;
    }
    breaksMap = new HashMap<String, TimesheetDO>();
    int breaksCounter = 0;
    final TimesheetFilter filter = new TimesheetFilter();
    filter.setUserId(userId);
    filter.setStartTime(startDate.getUtilDate());
    filter.setStopTime(endDate.getUtilDate());
    filter.setOrderType(OrderDirection.ASC);
    timesheets = timesheetDao.select(filter);
    boolean longFormat = false;
    days = Days.daysBetween(start, end).getDays();
    if (days < 10) {
      // Week or day view:
      longFormat = true;
      month = null;
      firstDayOfMonth = null;
    } else {
      // Month view:
      final PFDateTime currentMonth = startDate.plusDays(10); // Now we're definitely in the right
      // month.
      month = currentMonth.getMonth();
      firstDayOfMonth = currentMonth.getBeginOfMonth();
    }
    if (CollectionUtils.isNotEmpty(timesheets)) {
      PFDateTime lastStopTime = null;
      for (final TimesheetDO timesheet : timesheets) {
        final PFDateTime startTime = PFDateTime.from(timesheet.getStartTime());
        final PFDateTime stopTime = PFDateTime.from(timesheet.getStopTime());
        if (stopTime.isBefore(startDate) || startTime.isAfter(endDate)) {
          // Time sheet doesn't match time period start - end.
          continue;
        }
        if (calFilter.isShowBreaks()) {
          if (lastStopTime != null
              && lastStopTime.isSameDay(stopTime)
              && startTime.getEpochMilli() - lastStopTime.getEpochMilli() > 60000) {
            // Show breaks between time sheets of one day (> 60s).
            final Event breakEvent = new Event();
            breakEvent.setEditable(false);
            final String breakId = String.valueOf(++breaksCounter);
            breakEvent.setClassName(Constants.BREAK_EVENT_CLASS_NAME).setId(breakId).setStart(convert(lastStopTime))
                .setEnd(convert(startTime))
                .setTitle(getString("timesheet.break"));
            breakEvent.setTextColor("#666666").setBackgroundColor("#F9F9F9").setColor("#F9F9F9");
            events.put(breakId, breakEvent);
            final TimesheetDO breakTimesheet = new TimesheetDO().setStartDate(lastStopTime.getUtilDate())
                .setStopDate(startTime.getUtilDate());
            breaksMap.put(breakId, breakTimesheet);
          }
          lastStopTime = stopTime;
        }
        final long duration = timesheet.getDuration();
        final MyWicketEvent event = new MyWicketEvent();
        final String id = "" + timesheet.getId();
        event.setClassName(Constants.EVENT_CLASS_NAME);
        event.setId(id);
        event.setStart(convert(startTime));
        event.setEnd(convert(stopTime));
        final String title = CalendarHelper.getTitle(timesheet);
        if (longFormat) {
          // Week or day view:
          event.setTitle(title + "\n" + CalendarHelper.getDescription(timesheet) + "\n" + formatDuration(duration, false));
        } else {
          // Month view:
          event.setTitle(title);
        }
        if (month != null && startTime.getMonth() != month && stopTime.getMonth() != month) {
          // Display time sheets of other month as grey blue:
          event.setTextColor("#222222").setBackgroundColor("#ACD9E8").setColor("#ACD9E8");
        }
        events.put(id, event);
        if (month == null || startTime.getMonth() == month) {
          totalDuration += duration;
          addDurationOfDay(startTime.getDayOfMonth(), duration);
        }
        final int dayOfYear = startTime.getDayOfYear();
        addDurationOfDayOfYear(dayOfYear, duration);
        event.setTooltip(
            getString("timesheet"),
            new String[][]{
                {title},
                {timesheet.getLocation(), getString("timesheet.location")},
                {OldKostFormatter.formatLong(timesheet.getKost2()), getString("fibu.kost2")},
                {WicketTaskFormatter.getTaskPath(timesheet.getTaskId(), true, OutputType.PLAIN),
                    getString("task")},
                {timesheet.getDescription(), getString("description")}});
      }
    }
    if (calFilter.isShowStatistics()) {
      // Show statistics: duration of every day is shown as all day event.
      DateTime day = start;
      final int numberOfDaysInYear = PFDay.from(start.toDate()).getNumberOfDaysInYear();
      int paranoiaCounter = 0;
      do {
        if (++paranoiaCounter > 1000) {
          log.error(
              "Paranoia counter exceeded! Dear developer, please have a look at the implementation of buildEvents.");
          break;
        }
        final int dayOfYear = day.getDayOfYear();
        final long duration = durationsPerDayOfYear[dayOfYear];
        final boolean firstDayOfWeek = day.getDayOfWeek() == ThreadLocalUserContext.getFirstDayOfWeekValue();
        if (!firstDayOfWeek && duration == 0) {
          day = day.plusDays(1);
          continue;
        }
        final Event event = new Event().setAllDay(true);
        final String id = "s-" + (dayOfYear);
        event.setId(id);
        event.setStart(day);
        final String durationString = formatDuration(duration, false);
        if (firstDayOfWeek) {
          // Show week of year at top of first day of week.
          long weekDuration = 0;
          for (short i = 0; i < 7; i++) {
            int d = dayOfYear + i;
            if (d > numberOfDaysInYear) {
              d -= numberOfDaysInYear;
            }
            weekDuration += durationsPerDayOfYear[d];
          }
          final StringBuilder sb = new StringBuilder();
          sb.append(getString("calendar.weekOfYearShortLabel")).append(DateHelper.getWeekOfYear(day));
          if (days > 1 && weekDuration > 0) {
            // Show total sum of durations over all time sheets of current week (only in week and month view).
            sb.append(": ").append(formatDuration(weekDuration, false));
          }
          if (duration > 0) {
            sb.append(", ").append(durationString);
          }
          event.setTitle(sb.toString());
        } else {
          event.setTitle(durationString);
        }
        event.setTextColor("#666666").setBackgroundColor("#F9F9F9").setColor("#F9F9F9");
        event.setEditable(false);
        events.put(id, event);
        day = day.plusDays(1);
      } while (!day.isAfter(end));
    }
  }

  public TimesheetDO getBreakTimesheet(final String id) {
    return breaksMap != null ? breaksMap.get(id) : null;
  }

  public TimesheetDO getLatestTimesheetOfDay(final DateTime date) {
    if (timesheets == null) {
      return null;
    }
    TimesheetDO latest = null;
    for (final TimesheetDO timesheet : timesheets) {
      if (DateHelper.isSameDay(timesheet.getStopTime(), date.toDate())) {
        if (latest == null) {
          latest = timesheet;
        } else if (latest.getStopTime().before(timesheet.getStopTime())) {
          latest = timesheet;
        }
      }
    }
    return latest;
  }

  public String formatDuration(final long millis) {
    return formatDuration(millis, firstDayOfMonth != null);
  }

  private String formatDuration(final long millis, final boolean showTimePeriod) {
    final int[] fields = TimePeriod.getDurationFields(millis, 8, 200);
    final StringBuilder sb = new StringBuilder();
    if (fields[0] > 0) {
      sb.append(fields[0]).append(ThreadLocalUserContext.getLocalizedString("calendar.unit.day")).append(" ");
    }
    sb.append(fields[1]).append(":").append(StringHelper.format2DigitNumber(fields[2]))
        .append(ThreadLocalUserContext.getLocalizedString("calendar.unit.hour"));
    if (showTimePeriod) {
      sb.append(" (").append(ThreadLocalUserContext.getLocalizedString("calendar.month")).append(")");
    }
    return sb.toString();
  }

  /**
   * @return the duration
   */
  public long getTotalDuration() {
    return totalDuration;
  }

  private void addDurationOfDay(final int dayOfMonth, final long duration) {
    durationsPerDayOfMonth[dayOfMonth] += duration;
  }

  private void addDurationOfDayOfYear(final int dayOfYear, final long duration) {
    durationsPerDayOfYear[dayOfYear] += duration;
  }

  private DateTime convert(final PFDateTime dateTime) {
    return new DateTime(dateTime.getEpochMilli(), ThreadLocalUserContext.getDateTimeZone());
  }
}
