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

import net.ftlines.wicket.fullcalendar.Event;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DayHolder;

/**
 * Creates holiday events for FullCalendar.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HolidayEventsProvider extends MyFullCalendarEventsProvider
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HolidayEventsProvider.class);

  private static final long serialVersionUID = 6514836533889643685L;

  /**
   */
  public HolidayEventsProvider()
  {
    super();
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime, org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end)
  {
    DateMidnight day = new DateMidnight(start);
    int idCounter = 0;
    int paranoiaCounter = 0;
    do {
      if (++paranoiaCounter > 4000) {
        log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of buildEvents.");
        break;
      }
      final DayHolder dh = new DayHolder(day.toDate());
      String backgroundColor, color, textColor;
      if (dh.isHoliday() == true) {
        if (dh.isWorkingDay() == true) {
          backgroundColor = "#FFF0F0";
          color = "#EEEEEE";
          textColor = "#222222";
        } else {
          backgroundColor = "#f9dfde";
          color = "#EEEEEE";
          textColor = "#FF2222";
        }
      } else {
        day = day.plusDays(1);
        continue;
      }

      final Event event = new Event().setAllDay(true);
      final String id = "h-" + (++idCounter);
      event.setId(id);
      event.setStart(day.toDateTime());
      String title;
      final String holidayInfo = dh.getHolidayInfo();
      if (holidayInfo != null && holidayInfo.startsWith("calendar.holiday.") == true) {
        title = ThreadLocalUserContext.getLocalizedString(holidayInfo);
      } else {
        title = holidayInfo;
      }
      event.setTitle(title);
      event.setBackgroundColor(backgroundColor);
      event.setColor(color);
      event.setTextColor(textColor);
      events.put(id, event);
      day = day.plusDays(1);
    } while (day.isAfter(end) == false);
  }
}
