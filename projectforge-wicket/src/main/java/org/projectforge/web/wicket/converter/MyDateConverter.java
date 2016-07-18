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

import java.util.Date;
import java.util.Locale;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class MyDateConverter extends MyAbstractDateConverter
{
  private static final long serialVersionUID = 7807860441331835759L;

  /**
   * Date style to use. See {@link DateTimeFormat#forStyle(String)}.
   */
  private final String dateStyle;

  /**
   * From Wicket StyleDateConverter: Construct. The dateStyle 'S-' (which is the same as {@link DateTimeFormat#shortDate()}) will be used
   * for constructing the date format for the current locale. </p> When applyTimeZoneDifference is true, the current time is applied on the
   * parsed date, and the date will be corrected for the time zone difference between the server and the client. For instance, if I'm in
   * Seattle and the server I'm working on is in Amsterdam, the server is 9 hours ahead. So, if I'm inputting say 12/24 at a couple of hours
   * before midnight, at the server it is already 12/25. If this boolean is true, it will be transformed to 12/25, while the client sees
   * 12/24. </p>
   */
  public MyDateConverter()
  {
    this("S-");
  }

  /**
   * From Wicket StyleDateConverter: Construct. The provided pattern will be used as the base format (but they will be localized for the
   * current locale) and if null, {@link DateTimeFormat#shortDate()} will be used. </p> When applyTimeZoneDifference is true, the current
   * time is applied on the parsed date, and the date will be corrected for the time zone difference between the server and the client. For
   * instance, if I'm in Seattle and the server I'm working on is in Amsterdam, the server is 9 hours ahead. So, if I'm inputting say 12/24
   * at a couple of hours before midnight, at the server it is already 12/25. If this boolean is true, it will be transformed to 12/25,
   * while the client sees 12/24. </p>
   * 
   * @param dateStyle Date style to use. The first character is the date style, and the second character is the time style. Specify a
   *          character of 'S' for short style, 'M' for medium, 'L' for long, and 'F' for full. A date or time may be ommitted by specifying
   *          a style character '-'. See {@link DateTimeFormat#forStyle(String)}.
   * @throws IllegalArgumentException in case dateStyle is null
   */
  public MyDateConverter(final String dateStyle)
  {
    this(Date.class, dateStyle);
  }

  public MyDateConverter(final Class< ? extends Date> targetType, final String dateStyle)
  {
    super(targetType);
    if (dateStyle == null) {
      throw new IllegalArgumentException("dateStyle must be not null");
    }
    this.dateStyle = dateStyle;
  }

  @Override
  public String getDatePattern(final Locale locale)
  {
    return getPattern();
  }

  @Override
  protected DateTimeFormatter getFormat(final Locale locale)
  {
    final DateTimeFormatter dtf = DateTimeFormat.forPattern(getDatePattern(locale)).withLocale(locale).withPivotYear(2000);
    return dtf;
  }

}
