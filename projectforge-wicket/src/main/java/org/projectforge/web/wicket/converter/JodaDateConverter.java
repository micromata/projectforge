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

import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateFormatType;
import org.projectforge.framework.time.DateFormats;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class JodaDateConverter implements IConverter<DateMidnight>
{
  public static final int PIVOT_YEAR = 1950;

  private static final long serialVersionUID = 8665928904693938793L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(JodaDateConverter.class);

  private String userDateFormat;

  private final DateTimeZone timeZone;

  private final int currentYear;

  public JodaDateConverter()
  {
    this.userDateFormat = getPattern();
    this.timeZone = ThreadLocalUserContext.getDateTimeZone();
    this.currentYear = new DateMidnight(timeZone).getYear();
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
  public String convertToString(final DateMidnight value, final Locale locale)
  {
    if (value == null) {
      return null;
    }
    final DateTimeFormatter formatter = getDateTimeFormatter(userDateFormat, locale);
    return formatter.print(value);
  }

  protected DateTimeFormatter getDateTimeFormatter(final String pattern, final Locale locale)
  {
    return DateTimeFormat.forPattern(pattern).withLocale(locale).withZone(timeZone).withPivotYear(PIVOT_YEAR).withDefaultYear(currentYear);
  }

  /**
   * Attempts to convert a String to a Date object. Pre-processes the input by invoking the method preProcessInput(), then uses an ordered
   * list of DateFormat objects (supplied by getDateFormats()) to try and parse the String into a Date.
   */
  @Override
  public DateMidnight convertToObject(final String value, final Locale locale)
  {
    if (StringUtils.isBlank(value) == true) {
      return null;
    }
    final String[] formatStrings = getFormatStrings(locale);
    final DateTimeFormatter[] dateFormats = new DateTimeFormatter[formatStrings.length];

    for (int i = 0; i < formatStrings.length; i++) {
      dateFormats[i] = getDateTimeFormatter(formatStrings[i], locale);
    }
    DateMidnight date = null;
    for (final DateTimeFormatter formatter : dateFormats) {
      try {
        date = DateMidnight.parse(value, formatter);
        break;
      } catch (final Exception ex) { /* Do nothing, we'll get lots of these. */
        if (log.isDebugEnabled() == true) {
          log.debug(ex.getMessage(), ex);
        }
      }
    }
    // If we successfully parsed, return a date, otherwise send back an error
    if (date != null) {
      return date;
    } else {
      log.info("Unparseable date string (user's input): " + value + " for locale " + locale);
      throw new ConversionException("validation.error.general"); // Message key will not be used (dummy).
    }
  }

  /**
   * Returns an array of format strings that will be used, in order, to try and parse the date. This method can be overridden to make
   * DateTypeConverter use a different set of format Strings. Given that pre-processing converts most common separator characters into
   * spaces, patterns should be expressed with spaces as separators, not slashes, hyphens etc.
   */
  protected String[] getFormatStrings(final Locale locale)
  {
    return DateFormats.getDateParseFormats(this.userDateFormat);
  }
}
