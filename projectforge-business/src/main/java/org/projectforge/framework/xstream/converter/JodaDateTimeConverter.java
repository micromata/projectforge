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

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream converter that converts Joda date times.
 * 
 * @author Kai Reinhard
 * 
 */
public class JodaDateTimeConverter implements Converter
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JodaDateTimeConverter.class);

  protected static final String ISO_FORMAT = "yyyy-MM-dd HH:mm:ss";

  @Override
  public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context)
  {
    writer.setValue(toString(source));
  }

  String toString(final Object source)
  {
    final DateTime data = (DateTime) source;
    final DateTimeFormatter printFormat = DateTimeFormat.forPattern(ISO_FORMAT);
    final String date = printFormat.withZone(DateTimeZone.UTC).print(data);
    return date;
  }

  @Override
  public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context)
  {
    final String data = reader.getValue();
    return parse(data);
  }

  Object parse(final String data)
  {
    try {
      final DateTimeFormatter parseFormat = DateTimeFormat.forPattern(ISO_FORMAT);
      final DateTime date = parseFormat.withZone(DateTimeZone.UTC).parseDateTime(data);
      final DateTimeZone dateTimeZone = ThreadLocalUserContext.getDateTimeZone();
      return new DateTime(date, dateTimeZone);
    } catch (final Exception ex) {
      log.error("Can't parse DateMidnight: " + data);
      return new DateMidnight();
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public boolean canConvert(final Class clazz)
  {
    if (DateTime.class.isAssignableFrom(clazz) == true) {
      return true;
    }
    return false;
  }
}
