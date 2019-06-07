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

package org.projectforge.framework.xstream.converter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.xstream.XmlConstants;

public class DateConverter extends AbstractValueConverter<Date>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DateConverter.class);

  protected static final String FORMAT_ISO_DATE = "yyyy-MM-dd";

  protected static final String FORMAT_ISO_TIMESTAMP_MINUTES = "yyyy-MM-dd HH:mm";

  protected static final String FORMAT_ISO_TIMESTAMP_SECONDS = "yyyy-MM-dd HH:mm:ss";

  protected static final String FORMAT_ISO_TIMESTAMP_MILLIS = "yyyy-MM-dd HH:mm:ss.S";

  private TimeZone timeZone;

  public DateConverter()
  {
  }

  /**
   * Use the given time zone instead of the time zone of the logged in user (or, if not given the default time zone of the system).
   * @param timeZone
   */
  public DateConverter(final TimeZone timeZone)
  {
    this.timeZone = timeZone;
  }

  @Override
  public String toString(Object obj)
  {
    if (obj == null) {
      return null;
    }
    return String.valueOf(((Date) obj).getTime());
  }

  @Override
  public Date fromString(String str)
  {
    if (StringUtils.isEmpty(str) == true || XmlConstants.NULL_IDENTIFIER.equals(str) == true) {
      return null;
    }
    String format = null;
    if (str.indexOf('-') > 0) {
      final int length = str.length();
      if (length == FORMAT_ISO_DATE.length()) {
        format = FORMAT_ISO_DATE;
      } else if (length == FORMAT_ISO_TIMESTAMP_MINUTES.length()) {
        format = FORMAT_ISO_TIMESTAMP_MINUTES;
      } else if (length == FORMAT_ISO_TIMESTAMP_SECONDS.length()) {
        format = FORMAT_ISO_TIMESTAMP_SECONDS;
      } else if (length == FORMAT_ISO_TIMESTAMP_MILLIS.length()) {
        format = FORMAT_ISO_TIMESTAMP_MILLIS;
      }
    }
    if (format != null) {
      final DateFormat dateFormat = new SimpleDateFormat(format);
      dateFormat.setTimeZone(getTimeZone());
      try {
        return dateFormat.parse(str);
      } catch (ParseException ex) {
        log.warn("Can't parse date string '" + str + "'.");
        return null;
      }
    }
    try {
      final long timeInMillis = new Long(str);
      return new Date(timeInMillis);
    } catch (final NumberFormatException ex) {
      log.warn("Can't convert value '" + str + "' to time in millis (long value).");
      return null;
    }
  }
  
  /**
   * The time zone of this object (if given) or the time zone of the user if found in the ThreadLocalUserContext, otherwise {@link TimeZone#getDefault()}.
   * @see ThreadLocalUserContext#getUser()
   * @see ThreadLocalUserContext#getTimeZone()
   */
  protected TimeZone getTimeZone() {
    if (timeZone != null) {
      return timeZone;
    }
    return ThreadLocalUserContext.getTimeZone();
  }
}
