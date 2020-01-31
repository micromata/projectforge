/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.time;

import org.joda.time.DateTime;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.Date;
import java.util.TimeZone;

/**
 * Parse and formats dates.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DateHelper implements Serializable {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DateHelper.class);

  private static final long serialVersionUID = -94010735614402146L;

  /**
   * Number of milliseconds of one minute. DO NOT USE FOR exact date calculations (summer and winter time etc.)!
   */
  public static final long MILLIS_MINUTE = 60 * 1000;

  /**
   * Number of milliseconds of one hour. DO NOT USE FOR exact date calculations (summer and winter time etc.)!
   */
  public static final long MILLIS_HOUR = 60 * MILLIS_MINUTE;

  /**
   * Number of milliseconds of one day. DO NOT USE FOR exact date calculations (summer and winter time etc.)!
   */
  public static final long MILLIS_DAY = 24 * MILLIS_HOUR;

  /**
   * Europe/Berlin
   */
  public final static TimeZone EUROPE_BERLIN = PFDateTimeUtils.TIMEZONE_EUROPE_BERLIN;

  public static final BigDecimal MILLIS_PER_HOUR = new BigDecimal(MILLIS_HOUR);

  public static final BigDecimal HOURS_PER_WORKING_DAY = new BigDecimal(DateTimeFormatter.DEFAULT_HOURS_OF_DAY);

  public static final BigDecimal MILLIS_PER_WORKING_DAY = new BigDecimal(
          MILLIS_HOUR * DateTimeFormatter.DEFAULT_HOURS_OF_DAY);

  public static final BigDecimal SECONDS_PER_WORKING_DAY = new BigDecimal(
          60 * 60 * DateTimeFormatter.DEFAULT_HOURS_OF_DAY);

  /**
   * UTC
   */
  public final static TimeZone UTC = PFDateTimeUtils.TIMEZONE_UTC;

  private static final DateFormat FORMAT_ISO_DATE = new SimpleDateFormat(DateFormats.ISO_DATE);

  private static final DateFormat FORMAT_ISO_TIMESTAMP = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MILLIS);

  private static final DateFormat FILENAME_FORMAT_TIMESTAMP = new SimpleDateFormat(DateFormats.ISO_DATE + "_HH-mm");

  private static final DateFormat FILENAME_FORMAT_DATE = new SimpleDateFormat(DateFormats.ISO_DATE);

  /**
   * thread safe
   *
   * @param timezone
   */
  public static DateFormat getIsoDateFormat(final TimeZone timezone) {
    final DateFormat df = (DateFormat) FORMAT_ISO_DATE.clone();
    if (timezone != null) {
      df.setTimeZone(timezone);
    }
    return df;
  }

  /**
   * thread safe
   *
   * @param timezone If null then time zone is ignored.
   */
  public static DateFormat getIsoTimestampFormat(final TimeZone timezone) {
    final DateFormat df = (DateFormat) FORMAT_ISO_TIMESTAMP.clone();
    if (timezone != null) {
      df.setTimeZone(timezone);
    }
    return df;
  }

  /**
   * thread safe
   *
   * @param timezone
   */
  public static DateFormat getFilenameFormatTimestamp(final TimeZone timezone) {
    final DateFormat df = (DateFormat) FILENAME_FORMAT_TIMESTAMP.clone();
    if (timezone != null) {
      df.setTimeZone(timezone);
    }
    return df;
  }

  /**
   * thread safe
   *
   * @param timezone
   */
  public static DateFormat getFilenameFormatDate(final TimeZone timezone) {
    final DateFormat df = (DateFormat) FILENAME_FORMAT_DATE.clone();
    if (timezone != null) {
      df.setTimeZone(timezone);
    }
    return df;
  }

  /**
   * yyyy-MM-dd HH:mm:ss.S in UTC. Thread safe usage: FOR_TESTCASE_OUTPUT_FORMATTER.get().format(date)
   */
  public static final ThreadLocal<DateFormat> FOR_TESTCASE_OUTPUT_FORMATTER = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MILLIS);
      df.setTimeZone(UTC);
      return df;
    }
  };

  /**
   * Thread safe usage: FOR_TESTCASE_OUTPUT_FORMATTER.get().format(date)
   */
  public static final ThreadLocal<DateFormat> TECHNICAL_ISO_UTC = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      final DateFormat dateFormat = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MILLIS + " z");
      dateFormat.setTimeZone(UTC);
      return dateFormat;
    }
  };

  /**
   * @return Short name of day represented by the giving day. The context user's locale and time zone is considered.
   */
  public static final String formatShortNameOfDay(final Date date) {
    final DateFormat df = new SimpleDateFormat("EE", ThreadLocalUserContext.getLocale());
    df.setTimeZone(ThreadLocalUserContext.getTimeZone());
    return df.format(date);
  }

  /**
   * Formats the given date as UTC date in ISO format attached TimeZone (UTC).
   *
   * @param date
   * @return
   */
  public static final String formatAsUTC(final Date date) {
    if (date == null) {
      return "";
    }
    return UTC_ISO_DATE.get().format(date);
  }

  /**
   * Thread safe usage: UTC_ISO_DATE.get().format(date)
   */
  public static final ThreadLocal<DateFormat> UTC_ISO_DATE = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MILLIS + " Z");
      df.setTimeZone(UTC);
      return df;
    }
  };

  /**
   * Takes time zone of context user if exist.
   *
   * @param date
   */
  public static String formatIsoDate(final Date date) {
    return getIsoDateFormat(ThreadLocalUserContext.getTimeZone()).format(date);
  }

  /**
   * Takes time zone of context user if exist.
   *
   * @param date
   */
  public static String formatIsoDate(final Date date, final TimeZone timeZone) {
    return getIsoDateFormat(timeZone).format(date);
  }

  /**
   * logError = true
   *
   * @param str
   * @return
   * @see #parseMillis(String, boolean)
   */
  public static Date parseMillis(final String str) {
    return parseMillis(str, true);
  }

  /**
   * @param str
   * @param logError If true, any ParseException error will be logged if occured.
   * @return The parsed date or null, if not parseable.
   */
  public static Date parseMillis(final String str, final boolean logError) {
    Date date = null;
    try {
      final long millis = Long.parseLong(str);
      date = new Date(millis);
    } catch (final NumberFormatException ex) {
      if (logError) {
        log.error("Could not parse date string (millis expected): " + str, ex);
      }
    }
    return date;
  }

  public static String formatIsoTimestamp(final Date date) {
    return formatIsoTimestamp(date, ThreadLocalUserContext.getTimeZone());
  }

  public static String formatIsoTimestamp(final Date date, final TimeZone timeZone) {
    return getIsoTimestampFormat(timeZone).format(date);
  }

  /**
   * Format yyyy-mm-dd
   *
   * @param isoDateString
   * @return Parsed date or null if a parse error occurs.
   */
  public static Date parseIsoDate(final String isoDateString, final TimeZone timeZone) {
    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    df.setTimeZone(timeZone);
    Date date;
    try {
      date = df.parse(isoDateString);
    } catch (final ParseException ex) {
      return null;
    }
    return date;
  }

  /**
   * Format: {@link DateFormats#ISO_TIMESTAMP_MILLIS}
   *
   * @param isoDateString
   * @return Parsed date or null if a parse error occurs.
   */
  public static Date parseIsoTimestamp(final String isoDateString, final TimeZone timeZone) {
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MILLIS);
    df.setTimeZone(timeZone);
    Date date;
    try {
      date = df.parse(isoDateString);
    } catch (final ParseException ex) {
      return null;
    }
    return date;
  }

  /**
   * Output via FOR_TESTCASE_OUTPUT_FORMATTER for test cases.
   *
   * @param date
   * @return
   */
  public static final String getForTestCase(final Date date) {
    return FOR_TESTCASE_OUTPUT_FORMATTER.get().format(date);
  }

  public static final String getTimestampAsFilenameSuffix(final Date date) {
    if (date == null) {
      return "--";
    }
    return getFilenameFormatTimestamp(ThreadLocalUserContext.getTimeZone()).format(date);
  }

  public static final String getDateAsFilenameSuffix(final Date date) {
    if (date == null) {
      return "--";
    }
    return getFilenameFormatDate(ThreadLocalUserContext.getTimeZone()).format(date);
  }

  /**
   * @param year
   * @param month
   * @return "yyyy-mm"
   */
  public static String formatMonth(final int year, final Month month) {
    return formatMonth(year, PFDayUtils.getMonthValue(month));
  }

  /**
   * @param year
   * @param month 0-11
   * @return "yyyy-mm"
   */
  public static String formatMonth(final int year, final Integer month) {
    final StringBuilder buf = new StringBuilder();
    buf.append(year);
    if (month != null) {
      buf.append('-');
      if (month == null) {
        buf.append("??");
      } else {
        final int m = month.intValue();
        if (m <= 9) {
          buf.append('0');
        }
        buf.append(m);
      }
    }
    return buf.toString();
  }

  /**
   * Should be used application wide for getting and/or displaying the week of year!
   *
   * @param date
   * @return Return the week of year. The week of year depends on the Locale set in the Configuration (config.xml). If
   * given date is null then -1 is returned. For "de" the first week of year is the first week with a minimum of
   * 4 days in the new year. For "en" the first week of the year is the first week with a minimum of 1 days in
   * the new year.
   */
  public static int getWeekOfYear(final Date date) {
    if (date == null) {
      return -1;
    }
    return PFDay.from(date).getWeekOfYear();
  }

  /**
   * Should be used application wide for getting and/or displaying the week of year!
   *
   * @param date
   * @return Return the week of year. The week of year depends on the Locale set in the Configuration (config.xml). If
   * given date is null then -1 is returned. For "de" the first week of year is the first week with a minimum of
   * 4 days in the new year. For "en" the first week of the year is the first week with a minimum of 1 days in
   * the new year.
   */
  @Deprecated
  public static int getWeekOfYear(final DateTime date) {
    if (date == null) {
      return -1;
    }
    return getWeekOfYear(date.toDate());
  }

  /**
   * @param d1
   * @param d2
   * @return True if the dates are both null or both represents the same day (year, month, day) independent of the
   * hours, minutes etc.
   */
  public static boolean isSameDay(final Date d1, final Date d2) {
    if (d1 == null || d2 == null) {
      return false;
    }
    return isSameDay(PFDateTime.fromOrNull(d1), PFDateTime.fromOrNull(d2));
  }

  /**
   * @param d1
   * @param d2
   * @return True if the dates are both null or both represents the same day (year, month, day) independent of the
   * hours, minutes etc.
   * @see DateHolder#isSameDay(Date)
   */
  public static boolean isSameDay(final PFDateTime d1, final PFDateTime d2) {
    if (d1 == null) {
      return d2 == null;
    } else if (d2 == null) {
      return false;
    }
    return d1.getYear() == d2.getYear() && d1.getDayOfYear() == d2.getDayOfYear();
  }

  /**
   * @param d1
   * @param d2
   * @return True if the dates are both null or both represents the same day (year, month, day) independent of the
   * hours, minutes etc.
   * @see DateHolder#isSameDay(Date)
   */
  @Deprecated
  public static boolean isSameDay(final DateTime d1, final DateTime d2) {
    if (d1 == null) {
      return d2 == null;
    } else if (d2 == null) {
      return false;
    }
    return d1.getYear() == d2.getYear() && d1.getDayOfYear() == d2.getDayOfYear();
  }

  public static boolean dateOfYearBetween(final int month, final int dayOfMonth, final int fromMonth,
                                          final int fromDayOfMonth,
                                          final int toMonth, final int toDayOfMonth) {
    if (fromMonth == toMonth) {
      if (month != fromMonth) {
        return false;
      }
      return dayOfMonth >= fromDayOfMonth && dayOfMonth <= toDayOfMonth;
    } else if (fromMonth < toMonth) {
      // e. g. APR - JUN
      if (month < fromMonth || month > toMonth) {
        // e. g. FEB or JUL
        return false;
      } else if (month == fromMonth && dayOfMonth < fromDayOfMonth) {
        return false;
      } else return month != toMonth || dayOfMonth <= toDayOfMonth;
    } else {
      // e. g. NOV - FEB
      if (month > toMonth && month < fromMonth) {
        // e. g. MAR
        return false;
      } else if (month == fromMonth && dayOfMonth < fromDayOfMonth) {
        return false;
      } else return month != toMonth || dayOfMonth <= toDayOfMonth;
    }
  }

  /**
   * Sets given calendar (UTC) as local time, meaning e. g. 08:00 UTC will be 08:00 local time.
   *
   * @param dateTime
   * @return
   * @see DateTime#toString(String)
   * @see DateHelper#parseIsoDate(String, TimeZone)
   */
  @Deprecated
  public static long getDateTimeAsMillis(final DateTime dateTime) {
    final String isoDateString = dateTime.toString(DateFormats.ISO_TIMESTAMP_MILLIS);
    final Date date = DateHelper.parseIsoTimestamp(isoDateString, ThreadLocalUserContext.getTimeZone());
    return date.getTime();
  }
}
