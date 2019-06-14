/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.calendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;

/**
 * Some date and calender helper methods for test cases.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CalendarTestUtils
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CalendarTestUtils.class);

  public static java.util.Date createDate(final String isoDateString, final TimeZone timeZone)
  {
    if (isoDateString == null) {
      return null;
    }
    DateFormat df;
    short colonsCount = 0;
    for (short i = 0; i < isoDateString.length(); i++) {
      if (isoDateString.charAt(i) == ':') {
        colonsCount++;
      }
    }
    if (colonsCount == 0) {
      // yyyy-MM-dd
      df = new SimpleDateFormat(DateFormats.ISO_DATE);
    } else if (colonsCount == 1) {
      // yyyy-MM-dd HH:mm
      df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
    } else if (isoDateString.contains(".") == false) {
      // yyyy-MM-dd HH:mm:ss
      df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_SECONDS);
    } else {
      // yyyy-MM-dd HH:mm:ss.SSS
      df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MILLIS);
    }
    df.setTimeZone(timeZone);
    try {
      final java.util.Date date = df.parse(isoDateString);
      return date;
    } catch (final ParseException ex) {
      log.error("Error while parsing date '" + isoDateString + "' with format " + df + ": " + ex.getMessage(), ex);
      return null;
    }
  }

  public static String formatUTCIsoDate(final java.util.Date date) {
    if (date == null) {
      return null;
    }
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MILLIS);
    df.setTimeZone(DateHelper.UTC);
    return df.format(date);
  }

  public static java.util.Date createDate(final int year, final int month, final int dayOfMonth, final TimeZone timeZone)
  {
    return createDate(year, month, dayOfMonth, 0, 0, 0, 0, timeZone);
  }

  public static java.util.Date createDate(final int year, final int month, final int dayOfMonth, final int hourOfDay, final int minute,
      final TimeZone timeZone)
  {
    return createDate(year, month, dayOfMonth, hourOfDay, minute, 0, 0, timeZone);
  }

  public static java.util.Date createDate(final int year, final int month, final int dayOfMonth, final int hourOfDay, final int minute,
      final int second, final TimeZone timeZone)
  {
    return createDate(year, month, dayOfMonth, hourOfDay, minute, second, 0, timeZone);
  }

  public static java.util.Date createDate(final int year, final int month, final int dayOfMonth, final int hourOfDay, final int minute,
      final int second, final int millisecond, final TimeZone timeZone)
  {
    final Calendar cal = createCalendar(year, month, dayOfMonth, hourOfDay, minute, second, millisecond, timeZone);
    return cal.getTime();
  }

  public static Calendar createCalendar(final int year, final int month, final int dayOfMonth, final TimeZone timeZone)
  {
    return createCalendar(year, month, dayOfMonth, 0, 0, timeZone);
  }

  public static Calendar createCalendar(final int year, final int month, final int dayOfMonth, final int hourOfDay, final int minute,
      final TimeZone timeZone)
  {
    return createCalendar(year, month, dayOfMonth, hourOfDay, minute, 0, timeZone);
  }

  public static Calendar createCalendar(final int year, final int month, final int dayOfMonth, final int hourOfDay, final int minute,
      final int second, final TimeZone timeZone)
  {
    return createCalendar(year, month, dayOfMonth, hourOfDay, minute, second, 0, timeZone);
  }

  public static Calendar createCalendar(final int year, final int month, final int dayOfMonth, final int hourOfDay, final int minute,
      final int second, final int millisecond, final TimeZone timeZone)
  {
    final Calendar cal = Calendar.getInstance(timeZone);
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
    cal.set(Calendar.MINUTE, minute);
    cal.set(Calendar.SECOND, second);
    cal.set(Calendar.MILLISECOND, millisecond);
    return cal;
  }
}
