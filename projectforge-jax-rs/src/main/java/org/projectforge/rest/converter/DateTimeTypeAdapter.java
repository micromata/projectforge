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

package org.projectforge.rest.converter;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.rest.ConnectionSettings;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Serialization and deserialization for dates in ISO format and UTC time-zone.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DateTimeTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
  private final DateFormat dateTimeFormatter;

  private final DateTimeFormat dateTimeFormat;

  public DateTimeTypeAdapter() {
    this(TimeZone.getTimeZone("UTC"));
  }

  public DateTimeTypeAdapter(TimeZone timeZone) {
    this(timeZone, null);
  }

  public DateTimeTypeAdapter(TimeZone timeZone, DateTimeFormat dateTimeFormat) {
    final ConnectionSettings settings = ConnectionSettings.get();
    if (dateTimeFormat != null) {
      this.dateTimeFormat = dateTimeFormat;
    } else {
      this.dateTimeFormat = settings.getDateTimeFormat();
    }
    if (this.dateTimeFormat != null && this.dateTimeFormat.getPattern() != null) {
      dateTimeFormatter = new SimpleDateFormat(this.dateTimeFormat.getPattern(), settings.getLocale());
      dateTimeFormatter.setTimeZone(timeZone);
    } else {
      dateTimeFormatter = null;
    }
  }

  @Override
  public synchronized JsonElement serialize(final Date date, final Type type, final JsonSerializationContext jsonSerializationContext) {
    synchronized (dateTimeFormat) {
      if (dateTimeFormatter != null) {
        final String dateFormatAsString = dateTimeFormatter.format(date);
        return new JsonPrimitive(dateFormatAsString);
      } else {
        return new JsonPrimitive(String.valueOf(date.getTime()));
      }
    }
  }

  @Override
  public synchronized Date deserialize(final JsonElement jsonElement, final Type type,
                                       final JsonDeserializationContext jsonDeserializationContext) {
    try {
      synchronized (dateTimeFormat) {
        final String element = jsonElement.getAsString();
        if (element == null) {
          return null;
        }
        if (StringUtils.isNumeric(element) == true) {
          final Date date = new Date(Long.parseLong(element));
          return date;
        }
        return dateTimeFormatter.parse(element);
      }
    } catch (final ParseException e) {
      throw new JsonSyntaxException(jsonElement.getAsString(), e);
    }
  }
}
