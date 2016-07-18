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

package org.projectforge.framework.xstream.converter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.projectforge.framework.time.DateHolder;

public class ISODateConverter extends DateConverter
{
  @Override
  public String toString(Object obj)
  {
    if (obj == null) {
      return null;
    }
    final DateHolder dh = new DateHolder((Date) obj);
    final String format;
    if (dh.getMilliSecond() == 0) {
      if (dh.getSecond() == 0) {
        if (dh.getMinute() == 0 && dh.getHourOfDay() == 0) {
          format = FORMAT_ISO_DATE;
        } else {
          format = FORMAT_ISO_TIMESTAMP_MINUTES;
        }
      } else {
        format = FORMAT_ISO_TIMESTAMP_SECONDS;
      }
    } else {
      format = FORMAT_ISO_TIMESTAMP_MILLIS;
    }
    final DateFormat dateFormat = new SimpleDateFormat(format);
    dateFormat.setTimeZone(getTimeZone());
    return dateFormat.format((Date) obj);
  }
}
