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

package org.projectforge.framework.time;

import java.util.Locale;

import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Date formats. All the formats base on the given defaultDateFormat. Default date formats are e. g. "dd.MM.yyyy", "dd.MM.yy", "dd/MM/yyyy",
 * "dd/MM/yy", "MM/dd/yyyy", "MM/dd/yy".
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DateFormats
{
  public static final String COMPACT_DATE = "yyyyMMdd";

  public static final String ISO_DATE = "yyyy-MM-dd";

  public static final String ISO_TIMESTAMP_MINUTES = "yyyy-MM-dd HH:mm";

  public static final String ISO_TIMESTAMP_SECONDS = "yyyy-MM-dd HH:mm:ss";

  public static final String ISO_TIMESTAMP_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";

  public static final String EXCEL_ISO_DATE = "YYYY-MM-DD";

  public static final String LUCENE_TIMESTAMP_MINUTE = "yyyyMMddHHmm";

  /**
   * Check weather the given dateString has month or day first. If not analyzable then true is returned as default value.
   * @param dateString
   * @return true if month is used before day of month.
   */
  public static boolean isFormatMonthFirst(final String dateString)
  {
    if (dateString == null) {
      return true;
    }
    final int monthPos = dateString.indexOf('M');
    final int dayPos = dateString.indexOf('d');
    return monthPos <= dayPos; // '=': if none of both found, true is the default.
  }

  /**
   * Tries to get the separator char in dates ('/' is the default if nothing found). <br/>
   * Example: "dd.MM.yyyy ..." results in '.', "MM/dd/yyy ..." results in '/'. <br/>
   * Only '/', '-' and '.' are supported for now.
   * @param dateString
   * @return the separator char.
   */
  public static char getDateSeparatorChar(final String dateString)
  {
    if (dateString == null) {
      return '/';
    }
    if (dateString.indexOf('/') > 0) {
      return '/';
    } else if (dateString.indexOf('.') > 0) {
      return '.';
    } else if (dateString.indexOf('-') > 0) {
      return '-';
    }
    return '/';
  }

  /**
   * @param dateString
   * @return true if the dateString starts with "yyyy-MM-dd", otherwise false.
   */
  public static boolean isIsoFormat(final String dateString)
  {
    if (dateString == null) {
      return false;
    }
    return dateString.startsWith("yyyy-MM-dd");
  }

  /**
   * Uses default format of the logged-in user.
   */
  public static String[] getDateParseFormats()
  {
    return getDateParseFormats(ensureAndGetDefaultDateFormat());
  }

  /**
   * DefaultDateFormat with yyyy and yy and ISO format yyyy-MM-dd.
   * @param defaultDateFormat
   */
  public static String[] getDateParseFormats(final String defaultDateFormat)
  {
    // # Date/time formats (important: don't use spaces after separator char, e. g. correct is dd.MMM yyyy instead of dd. MMM yyyy):
    final String[] sa = new String[4];
    if (defaultDateFormat.contains("yyyy") == true) {
      sa[0] = defaultDateFormat.replace("yyyy", "yy"); // First, try "yy"
      sa[1] = defaultDateFormat;
    } else {
      sa[0] = defaultDateFormat;
      sa[1] = defaultDateFormat.replace("yy", "yyyy");
    }
    sa[2] = getFormatString(defaultDateFormat, null, DateFormatType.DATE_WITHOUT_YEAR);
    sa[3] = ISO_DATE;
    return sa;
  }

  /**
   * Gets the format string for the logged-in user. Uses the date format of the logged in user and if not given, it'll be set.
   * @param format
   * @see Configuration#getDateFormats()
   * @see PFUserDO#getExcelDateFormat()
   */
  public static String getFormatString(final DateFormatType format)
  {
    return getFormatString(ensureAndGetDefaultDateFormat(), ensureAndGetDefaultTimeNotation(), format);
  }

  /**
   * Ensures and gets the default date format of the logged-in user.
   * @return
   */
  private static String ensureAndGetDefaultDateFormat()
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    String defaultDateFormat = user != null ? user.getDateFormat() : null;
    if (defaultDateFormat == null) {
      defaultDateFormat = Configuration.getInstance().getDefaultDateFormat();
      if (user != null) {
        user.setDateFormat(defaultDateFormat);
      }
    }
    return defaultDateFormat;
  }

  /**
   * Ensures and gets the default time notation of the logged-in user.
   * @return
   */
  public static TimeNotation ensureAndGetDefaultTimeNotation()
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    TimeNotation defaultTimeNotation = user != null ? user.getTimeNotation() : null;
    if (defaultTimeNotation == null) {
      if (ConfigXml.getInstance().getDefaultTimeNotation() != null) {
        defaultTimeNotation = ConfigXml.getInstance().getDefaultTimeNotation();
      } else {
        final Locale locale = ThreadLocalUserContext.getLocale();
        if (locale != null && locale.toString().toLowerCase().startsWith("de") == true) {
          defaultTimeNotation = TimeNotation.H24;
        } else {
          defaultTimeNotation = TimeNotation.H12;
        }
      }
      if (user != null) {
        user.setTimeNotation(defaultTimeNotation);
      }
    }
    return defaultTimeNotation;
  }

  /**
   * Ensures and gets the default excel date format of the logged-in user.
   * @return
   */
  private static String ensureAndGetDefaultExcelDateFormat()
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    String defaultExcelDateFormat = user != null ? user.getExcelDateFormat() : null;
    if (defaultExcelDateFormat == null) {
      defaultExcelDateFormat = Configuration.getInstance().getDefaultExcelDateFormat();
      if (user != null) {
        user.setExcelDateFormat(defaultExcelDateFormat);
      }
    }
    return defaultExcelDateFormat;
  }

  /**
   * Gets the format string for the logged-in user. Uses the date format of the logged in user and if not given, a default format is
   * returned.
   * @param format
   * @see Configuration#getExcelDateFormats()
   * @see PFUserDO#getExcelDateFormat()
   */
  public static String getExcelFormatString(final DateFormatType format)
  {
    return getExcelFormatString(ensureAndGetDefaultExcelDateFormat(), format);
  }

  public static String getExcelFormatString(final String defaultExcelDateFormat, final DateFormatType format)
  {
    switch (format) {
      case DATE:
        return defaultExcelDateFormat;
      case DATE_TIME_MINUTES:
        return defaultExcelDateFormat + " hh:mm";
      case DATE_TIME_SECONDS:
        return defaultExcelDateFormat + " hh:mm:ss";
      case DATE_TIME_MILLIS:
        return defaultExcelDateFormat + " hh:mm:ss.fff";
      default:
        return defaultExcelDateFormat + " hh:mm:ss";
    }
  }

  public static String getFormatString(final String defaultDateFormat, final TimeNotation timeNotation, final DateFormatType format)
  {
    switch (format) {
      case DATE:
        return defaultDateFormat;
      case DATE_WITH_DAY_NAME:
        return "E, " + getFormatString(defaultDateFormat, timeNotation, DateFormatType.DATE);
      case DATE_WITHOUT_YEAR:
        String pattern;
        if (defaultDateFormat.contains("yyyy") == true) {
          pattern = defaultDateFormat.replace("yyyy", "");
        } else {
          pattern = defaultDateFormat.replace("yy", "");
        }
        if (pattern.endsWith("/") == true) {
          return pattern.substring(0, pattern.length() - 1);
        } else if (pattern.startsWith("-") == true) {
          return pattern.substring(1);
        } else {
          return pattern;
        }
      case DATE_SHORT:
        if (defaultDateFormat.contains("yyyy") == false) {
          return defaultDateFormat;
        }
        return defaultDateFormat.replace("yyyy", "yy");
      case ISO_DATE:
        return "yyyy-MM-dd";
      case ISO_TIMESTAMP_MINUTES:
        return "yyyy-MM-dd HH:mm";
      case ISO_TIMESTAMP_SECONDS:
        return "yyyy-MM-dd HH:mm:ss";
      case ISO_TIMESTAMP_MILLIS:
        return "yyyy-MM-dd HH:mm:ss.SSS";
      case DAY_OF_WEEK_SHORT:
        return "EE";
      case DATE_TIME_MINUTES:
        return getFormatString(defaultDateFormat, timeNotation, DateFormatType.DATE)
            + (timeNotation == TimeNotation.H24 ? " HH:mm" : " hh:mm aa");
      case DATE_TIME_SECONDS:
        return getFormatString(defaultDateFormat, timeNotation, DateFormatType.DATE)
            + (timeNotation == TimeNotation.H24 ? " HH:mm:ss" : " hh:mm:ss aa");
      case DATE_TIME_SHORT_MINUTES:
        return getFormatString(defaultDateFormat, timeNotation, DateFormatType.DATE_SHORT)
            + (timeNotation == TimeNotation.H24 ? " HH:mm" : " hh:mm aa");
      case DATE_TIME_SHORT_SECONDS:
        return getFormatString(defaultDateFormat, timeNotation, DateFormatType.DATE_SHORT)
            + (timeNotation == TimeNotation.H24 ? " HH:mm:ss" : " hh:mm:ss aa");
      case TIME_OF_DAY_MINUTES:
        return (timeNotation == TimeNotation.H24 ? " HH:mm" : " hh:mm aa");
      case TIME_OF_DAY_SECONDS:
        return (timeNotation == TimeNotation.H24 ? " HH:mm:ss" : " hh:mm:ss aa");
      default:
        return defaultDateFormat;
    }
  }
}
