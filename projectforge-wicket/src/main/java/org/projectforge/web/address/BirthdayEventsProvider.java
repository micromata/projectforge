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

package org.projectforge.web.address;

import java.util.Calendar;
import java.util.Set;

import org.apache.wicket.Component;
import org.joda.time.DateTime;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.BirthdayAddress;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.common.DateFormatType;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.calendar.MyFullCalendarEventsProvider;

import net.ftlines.wicket.fullcalendar.Event;

/**
 * Creates events for FullCalendar.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class BirthdayEventsProvider extends MyFullCalendarEventsProvider
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BirthdayEventsProvider.class);

  private static final long serialVersionUID = 2241430630558260146L;

  private final AddressDao addressDao;

  private final boolean dataProtection;

  private final ICalendarFilter filter;

  /**
   * the name of the event class.
   */
  public static final String EVENT_CLASS_NAME = "birth";

  /**
   * @param parent         For i18n.
   * @param addressDao
   * @param dataProtection If true (default) then no ages will be shown, only the names.
   * @see Component#getString(String)
   */
  public BirthdayEventsProvider(final ICalendarFilter filter, final AddressDao addressDao, final boolean dataProtection)
  {
    this.filter = filter;
    this.addressDao = addressDao;
    this.dataProtection = dataProtection;
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime, org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end)
  {
    if (filter.isShowBirthdays() == false) {
      // Don't show birthdays.
      return;
    }
    DateTime from = start;
    if (start.getMonthOfYear() == Calendar.MARCH && start.getDayOfMonth() == 1) {
      from = start.minusDays(1);
    }
    final Set<BirthdayAddress> set = addressDao.getBirthdays(from.toDate(), end.toDate(), 1000, true);
    for (final BirthdayAddress birthdayAddress : set) {
      final AddressDO address = birthdayAddress.getAddress();
      final int month = birthdayAddress.getMonth() + 1;
      final int dayOfMonth = birthdayAddress.getDayOfMonth();
      DateTime date = getDate(from, end, month, dayOfMonth);
      // February, 29th fix:
      if (date == null && month == Calendar.FEBRUARY + 1 && dayOfMonth == 29) {
        date = getDate(from, end, month + 1, 1);
      }
      if (date == null && WebConfiguration.isDevelopmentMode() == true) {
        log.info("Date "
            + birthdayAddress.getDayOfMonth()
            + "/"
            + (birthdayAddress.getMonth() + 1)
            + " not found between "
            + from
            + " and "
            + end);
        continue;
      } else {
        if (dataProtection == false && date != null) {
          birthdayAddress.setAge(date.toDate());
        }
      }
      final Event event = new Event().setAllDay(true);
      event.setClassName(EVENT_CLASS_NAME);
      final String id = "" + address.getId();
      event.setId(id);
      event.setStart(date);
      final StringBuffer buf = new StringBuffer();
      if (dataProtection == false) {
        // Birthday is not visible for all users (age == 0).
        buf.append(
            DateTimeFormatter.instance().getFormattedDate(address.getBirthday(), DateFormats.getFormatString(DateFormatType.DATE_SHORT)))
            .append(" ");
      }
      buf.append(address.getFirstName()).append(" ").append(address.getName());
      if (dataProtection == false && birthdayAddress.getAge() > 0) {
        // Birthday is not visible for all users (age == 0).
        buf.append(" (").append(birthdayAddress.getAge()).append(" ").append(ThreadLocalUserContext.getLocalizedString("address.age.short")).append(")");
      }
      event.setTitle(buf.toString());
      if (birthdayAddress.isFavorite() == true) {
        // Colors of events of birthdays of favorites (for default color see CalendarPanel):
        event.setBackgroundColor("#06790E");
        event.setBorderColor("#06790E");
        event.setTextColor("#FFFFFF");
      }
      events.put(id, event);
    }
  }

  private DateTime getDate(final DateTime start, final DateTime end, final int month, final int dayOfMonth)
  {
    DateTime day = start;
    int paranoiaCounter = 0;
    do {
      if (day.getMonthOfYear() == month && day.getDayOfMonth() == dayOfMonth) {
        return day;
      }
      day = day.plusDays(1);
      if (++paranoiaCounter > 1000) {
        log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of getDate.");
        break;
      }
    } while (day.isAfter(end) == false);
    return null;
  }
}
