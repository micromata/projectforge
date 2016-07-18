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

package org.projectforge.framework.calendar;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class CalendarUtils
{
  /**
   * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
   * @param date
   * @return
   */
  public static Date getUTCMidnightDate(final Date date)
  {
    final Calendar utcCal = getUTCMidnightCalendar(date);
    return utcCal.getTime();
  }

  /**
   * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
   * @param date
   * @return
   */
  public static Timestamp getUTCMidnightTimestamp(final Date date)
  {
    final Calendar cal = getUTCMidnightCalendar(date);
    return new Timestamp(cal.getTimeInMillis());
  }

  /**
   * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
   * @param date
   * @return
   */
  public static Calendar getUTCMidnightCalendar(final Date date)
  {
    return getUTCMidnightCalendar(date, ThreadLocalUserContext.getTimeZone());
  }

  /**
   * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
   * @param date
   * @param timeZone
   * @return
   */
  public static Calendar getUTCMidnightCalendar(final Date date, final TimeZone timeZone)
  {
    final Calendar usersCal = Calendar.getInstance(timeZone);
    usersCal.setTime(date);
    final Calendar utcCal = DateHelper.getUTCCalendar();
    copyCalendarDay(usersCal, utcCal);
    return utcCal;
  }

  /**
   * Converts a given date (in UTC) to midnight of user's timeZone.
   * @param date
   * @return
   */
  public static Timestamp getMidnightTimestampFromUTC(final Date date)
  {
    final Calendar cal = getMidnightCalendarFromUTC(date);
    return new Timestamp(cal.getTimeInMillis());
  }

  /**
   * Converts a given date (in UTC) to midnight of user's timeZone.
   * @param date
   * @param timeZone
   * @return
   */
  public static Timestamp getMidnightTimestampFromUTC(final Date date, final TimeZone timeZone)
  {
    final Calendar cal = getMidnightCalendarFromUTC(date, timeZone);
    return new Timestamp(cal.getTimeInMillis());
  }

  /**
   * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
   * @param date
   * @return
   */
  public static Calendar getMidnightCalendarFromUTC(final Date date)
  {
    return getMidnightCalendarFromUTC(date, ThreadLocalUserContext.getTimeZone());
  }

  /**
   * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
   * @param date
   * @param timeZone
   * @return
   */
  public static Calendar getMidnightCalendarFromUTC(final Date date, final TimeZone timeZone)
  {
    final Calendar utcCal = DateHelper.getUTCCalendar();
    utcCal.setTime(date);
    final Calendar usersCal = Calendar.getInstance(timeZone);
    copyCalendarDay(utcCal, usersCal);
    return usersCal;
  }

  public static Date getEndOfDay(final Date date, final TimeZone timeZone)
  {
    final Calendar cal = Calendar.getInstance(timeZone);
    cal.setTime(date);
    cal.set(Calendar.HOUR_OF_DAY, 23);
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.SECOND, 59);
    cal.set(Calendar.MILLISECOND, 999);
    return cal.getTime();
  }

  public static int daysBetween(final Calendar cal1, final Calendar cal2)
  {
    final Calendar from, to;
    boolean positive = true;
    if (cal1.getTimeInMillis() < cal2.getTimeInMillis()) {
      from = cal1;
      to = cal2;
    } else {
      from = cal2;
      to = cal1;
      positive = false;
    }
    int result = 0;
    final int toYear = to.get(Calendar.YEAR);
    final Calendar cal = (Calendar) from.clone();

    int endlessLoopProtection = 0;
    while (cal.get(Calendar.YEAR) < toYear) {
      final int fromDay = cal.get(Calendar.DAY_OF_YEAR);
      cal.set(Calendar.MONTH, Calendar.DECEMBER);
      cal.set(Calendar.DAY_OF_MONTH, 31);
      result += cal.get(Calendar.DAY_OF_YEAR) - fromDay + 1;
      cal.add(Calendar.DAY_OF_MONTH, 1);
      if (++endlessLoopProtection > 5000) {
        throw new IllegalArgumentException("Days between doesn's support more than 5000 years");
      }
    }
    result += to.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR);
    if (positive == true) {
      return result;
    } else {
      return -result;
    }
  }

  public static boolean isSameDay(final Calendar cal1, final Calendar cal2)
  {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
  }

  private static void copyCalendarDay(final Calendar src, final Calendar dest)
  {
    copyCalField(src, dest, Calendar.YEAR);
    copyCalField(src, dest, Calendar.MONTH);
    copyCalField(src, dest, Calendar.DAY_OF_MONTH);
    dest.set(Calendar.HOUR_OF_DAY, 0);
    dest.set(Calendar.MINUTE, 0);
    dest.set(Calendar.SECOND, 0);
    dest.set(Calendar.MILLISECOND, 0);
  }

  private static void copyCalField(final Calendar src, final Calendar dest, final int field)
  {
    dest.set(field, src.get(field));
  }
}
