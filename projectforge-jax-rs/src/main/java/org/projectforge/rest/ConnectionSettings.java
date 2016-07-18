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

package org.projectforge.rest;

import java.util.Locale;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.projectforge.rest.converter.DateTimeFormat;

/**
 * ConnectionSettings is used for configuring single rest calls. This class also stores a connection settings object in
 * ThreadLocal.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ConnectionSettings
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ConnectionSettings.class);

  private static final DateTimeFormat DEFAULT_DATE_TIME_FORMAT = DateTimeFormat.ISO_DATE_TIME_MILLIS;

  private static ThreadLocal<ConnectionSettings> context = new ThreadLocal<ConnectionSettings>();

  private DateTimeFormat dateTimeFormat = DEFAULT_DATE_TIME_FORMAT;

  private Locale locale = Locale.US;

  /**
   * settings.dateTimeFormat
   */
  public static final String DATE_TIME_FORMAT = "settings.dateTimeFormat";

  /**
   * @return the connection settings stored in ThreadLocal. If not given in ThreadLocal a new instance is returned.
   */
  public final static ConnectionSettings get()
  {
    ConnectionSettings settings = context.get();
    if (settings == null) {
      settings = new ConnectionSettings();
    }
    return settings;
  }

  /**
   * Stores connection settings in ThreadLocal (don't forget to remove them in a finally block!). Otherwise these
   * settings may shared with other users!
   * 
   * @param settings
   */
  public final static void set(final ConnectionSettings settings)
  {
    if (log.isDebugEnabled() == true) {
      log.debug("set connection settings: " + settings);
    }
    context.set(settings);
  }

  /**
   * @return the dateTimeFormat
   */
  public DateTimeFormat getDateTimeFormat()
  {
    return dateTimeFormat;
  }

  /**
   * @param dateTimeFormat the dateTimeFormat to set
   * @return this for chaining.
   */
  public ConnectionSettings setDateTimeFormat(final DateTimeFormat dateTimeFormat)
  {
    if (dateTimeFormat == null) {
      this.dateTimeFormat = DateTimeFormat.ISO_DATE_TIME_MILLIS;
    } else {
      this.dateTimeFormat = dateTimeFormat;
    }
    return this;
  }

  public boolean isDefaultDateTimeFormat()
  {
    return dateTimeFormat == DEFAULT_DATE_TIME_FORMAT;
  }

  /**
   * @return the locale
   */
  public Locale getLocale()
  {
    return locale;
  }

  /**
   * @param locale the locale to set
   * @return this for chaining.
   */
  public ConnectionSettings setLocale(final Locale locale)
  {
    if (locale == null) {
      this.locale = Locale.getDefault();
    }
    this.locale = locale;
    return this;
  }

  @Override
  public String toString()
  {
    return new ReflectionToStringBuilder(this).toString();
  }
}
