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

package org.projectforge.web.wicket.converter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.util.convert.ConversionException;
import org.projectforge.common.DateFormatType;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.DayHolder;

/**
 * Concepts and implementation based on Stripes DateTypeConverter implementation.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class MyAbstractDateConverter extends DateConverter
{
  private static final long serialVersionUID = 8019686218512218256L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MyAbstractDateConverter.class);

  private final Class<? extends Date> targetType;

  private TimeZone timeZone;

  private String userDateFormat;

  public MyAbstractDateConverter(final Class<? extends Date> targetType)
  {
    super(true);
    this.targetType = targetType;
  }

  public String getPattern()
  {
    if (this.userDateFormat == null) {
      this.userDateFormat = DateFormats.getFormatString(DateFormatType.DATE);
    }
    return this.userDateFormat;
  }

  /**
   * @param locale ignored, locale of ThreadLocalUserContext is used instead.
   * @see org.apache.wicket.datetime.DateConverter#convertToString(java.lang.Object, java.util.Locale)
   * @see DateTimeFormatter#getFormattedDate(Object)
   */
  @Override
  public String convertToString(final Date value, final Locale locale)
  {
    if (value == null) {
      return null;
    }

    return DateTimeFormatter.instance()
        .getFormattedDate(value, ThreadLocalUserContext.getLocale(), this.timeZone == null ? ThreadLocalUserContext.getTimeZone() : this.timeZone);
  }

  /**
   * Returns an array of format strings that will be used, in order, to try and parse the date. This method can be overridden to make
   * DateTypeConverter use a different set of format Strings. Given that pre-processing converts most common separator characters into
   * spaces, patterns should be expressed with spaces as separators, not slashes, hyphens etc.
   */
  protected String[] getFormatStrings(final Locale locale)
  {
    return DateFormats.getDateParseFormats();
  }

  /**
   * Attempts to convert a String to a Date object. Pre-processes the input by invoking the method preProcessInput(), then uses an ordered
   * list of DateFormat objects (supplied by getDateFormats()) to try and parse the String into a Date.
   */
  @Override
  public Date convertToObject(final String value, final Locale locale)
  {
    if (StringUtils.isBlank(value) == true) {
      return null;
    }
    final String[] formatStrings = getFormatStrings(locale);
    final SimpleDateFormat[] dateFormats = new SimpleDateFormat[formatStrings.length];

    for (int i = 0; i < formatStrings.length; i++) {
      dateFormats[i] = new SimpleDateFormat(formatStrings[i], locale);
      dateFormats[i].setLenient(false);
      if (ClassUtils.isAssignable(targetType, java.sql.Date.class) == false) {
        // Set time zone not for java.sql.Date, because e. g. for Europe/Berlin the date 1970-11-21 will
        // result in 1970-11-20 23:00:00 UTC and therefore 1970-11-20!
        dateFormats[i].setTimeZone(this.timeZone == null ? ThreadLocalUserContext.getTimeZone() : this.timeZone);
      }
    }

    // Step 1: pre-process the input to make it more palatable
    final String parseable = preProcessInput(value, locale);

    // Step 2: try really hard to parse the input
    Date date = null;
    for (final DateFormat format : dateFormats) {
      try {
        date = format.parse(parseable);
        break;
      } catch (final ParseException pe) { /* Do nothing, we'll get lots of these. */
      }
    }
    // Step 3: If we successfully parsed, return a date, otherwise send back an error
    if (date != null) {
      if (ClassUtils.isAssignable(targetType, java.sql.Date.class) == true) {
        final DayHolder day = new DayHolder(date);
        return day.getSQLDate();
      }
      return date;
    } else {
      log.info("Unparseable date string: " + value);
      throw new ConversionException("validation.error.general"); // Message key will not be used (dummy).
    }
  }

  /**
   * Removes unnecessary white spaces and appends current year, if not given.
   *
   * @param dateString
   * @param locale
   * @return
   */
  protected String preProcessInput(final String dateString, final Locale locale)
  {
    final StringBuffer buf = new StringBuffer(dateString.length());
    boolean whitespace = true; // Ignore leading white spaces.
    final int size = dateString.length();
    final char separatorChar;
    if (dateString.indexOf('.') > 0) {
      separatorChar = '.';
    } else if (dateString.indexOf('-') > 0) {
      separatorChar = '-';
    } else {
      separatorChar = '/';
    }
    for (int i = 0; i < size; i++) {
      final char ch = dateString.charAt(i);
      if (whitespace == true) {
        if (Character.isWhitespace(ch) == true) {
          continue; // Ignore following white spaces.
        }
        if (ch != separatorChar) {
          whitespace = false;
        }
        buf.append(ch);
      } else {
        if (Character.isWhitespace(ch) == true) {
          while (i < size - 1) {
            final char nextCh = dateString.charAt(i + 1);
            if (nextCh == separatorChar) {
              buf.append(nextCh);
              i++;
              break;
            }
            if (Character.isWhitespace(nextCh) == false) {
              buf.append(ch);
              break;
            }
            i++;
          }
          whitespace = true;
        } else {
          if (ch == separatorChar) {
            whitespace = true; // Ignore white spaces after separator char.
          }
          buf.append(ch);
        }
      }
    }
    final String str = buf.toString();
    final int count = buf.toString().split("[\\./\\s]").length;
    // Looks like we probably only have a day and month component, that won't work!
    if (count == 2) {
      if (str.charAt(str.length() - 1) != separatorChar) {
        buf.append(separatorChar);
      }
      buf.append(Calendar.getInstance(locale).get(Calendar.YEAR));
      return buf.toString();
    }
    return str;
  }

  public MyAbstractDateConverter setTimeZone(TimeZone timeZone)
  {
    this.timeZone = timeZone;
    return this;
  }
}
