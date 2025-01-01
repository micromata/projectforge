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

package org.projectforge.web.humanresources;

import net.ftlines.wicket.fullcalendar.Event;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.projectforge.business.humanresources.HRPlanningDO;
import org.projectforge.business.humanresources.HRPlanningDao;
import org.projectforge.business.humanresources.HRPlanningEntryDO;
import org.projectforge.business.humanresources.HRPlanningFilter;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.calendar.MyFullCalendarEventsProvider;

import java.math.BigDecimal;
import java.util.List;

/**
 * Creates events corresponding to the hr planning entries.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class HRPlanningEventsProvider extends MyFullCalendarEventsProvider
{
  private static final long serialVersionUID = -8614136730204759894L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRPlanningEventsProvider.class);

  private final HRPlanningDao hrPlanningDao;

  private final ICalendarFilter calendarFilter;

  /**
   * the name of the event class.
   */
  public static final String EVENT_CLASS_NAME = "hrPlanning";

  /**
   * @param calendarFilter
   * @param hrPlanningDao
   */
  public HRPlanningEventsProvider(final ICalendarFilter calendarFilter, final HRPlanningDao hrPlanningDao)
  {
    this.calendarFilter = calendarFilter;
    this.hrPlanningDao = hrPlanningDao;
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime, org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end)
  {
    if (calendarFilter.isShowPlanning() == false) {
      // Don't show plannings.
      return;
    }
    final HRPlanningFilter filter = new HRPlanningFilter();
    Long timesheetUserId = calendarFilter.getTimesheetUserId();
    if (timesheetUserId == null) {
      timesheetUserId = ThreadLocalUserContext.getLoggedInUserId();
    }
    filter.setUserId(timesheetUserId);

    PFDay startDay = PFDay.fromOrNow(start.toDate());
    PFDay endDay = PFDay.fromOrNow(end.toDate());

    filter.setStartDay(startDay.getLocalDate());
    filter.setStopDay(endDay.getLocalDate());
    final List<HRPlanningDO> list = hrPlanningDao.select(filter);
    if (list == null) {
      return;
    }
    for (final HRPlanningDO planning : list) {
      if (planning.getEntries() == null) {
        continue;
      }
      final DateTime week = new DateTime(PFDateTime.from(planning.getWeek()).getUtilDate(), ThreadLocalUserContext.getDateTimeZone());
      for (final HRPlanningEntryDO entry : planning.getEntries()) {
        if (entry.getDeleted() == true) {
          continue;
        }
        putEvent(entry, week, "week", 6, entry.getUnassignedHours());
        putEvent(entry, week, "mo", 0, entry.getMondayHours());
        putEvent(entry, week.plusDays(1), "tu", 0, entry.getTuesdayHours());
        putEvent(entry, week.plusDays(2), "we", 0, entry.getWednesdayHours());
        putEvent(entry, week.plusDays(3), "th", 0, entry.getThursdayHours());
        putEvent(entry, week.plusDays(4), "fr", 0, entry.getFridayHours());
        putEvent(entry, week.plusDays(5), "we", 1, entry.getWeekendHours());
      }
    }
  }

  private void putEvent(final HRPlanningEntryDO entry, final DateTime start, final String suffix, final int durationDays,
      final BigDecimal hours)
  {
    if (NumberHelper.isGreaterZero(hours) == false) {
      return;
    }
    if (log.isDebugEnabled() == true) {
      log.debug("Date: " + start + ", hours=" + hours + ", duration: " + durationDays);
    }
    final Event event = new Event().setAllDay(true);
    event.setClassName(EVENT_CLASS_NAME);
    final String id = "" + entry.getId() + "-" + suffix;
    event.setId(id);
    event.setStart(start);
    if (durationDays > 0) {
      event.setEnd(start.plusDays(durationDays));
    } else {
      event.setEnd(start);
    }
    final StringBuilder buf = new StringBuilder();
    buf.append(NumberHelper.formatFraction2(hours)).append(getString("calendar.unit.hour")).append(" ")
        .append(entry.getProjektNameOrStatus());
    if (StringUtils.isNotBlank(entry.getDescription()) == true) {
      buf.append(": ");
      if (durationDays > 2) {
        buf.append(StringUtils.abbreviate(entry.getDescription(), 100));
      } else if (durationDays > 1) {
        buf.append(StringUtils.abbreviate(entry.getDescription(), 50));
      } else {
        buf.append(StringUtils.abbreviate(entry.getDescription(), 20));
      }
    }
    event.setTitle(buf.toString());
    events.put(id, event);
  }
}
