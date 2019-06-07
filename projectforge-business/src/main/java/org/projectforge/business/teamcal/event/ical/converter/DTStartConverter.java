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

package org.projectforge.business.teamcal.event.ical.converter;

import static org.projectforge.business.teamcal.event.ical.ICalConverterStore.TIMEZONE_REGISTRY;

import java.util.Date;

import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.calendar.ICal4JUtils;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;

public class DTStartConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    if (event.isAllDay() == true) {
      final Date startUtc = CalendarUtils.getUTCMidnightDate(event.getStartDate());
      net.fortuna.ical4j.model.Date date = new net.fortuna.ical4j.model.Date(startUtc);
      return new DtStart(date);
    } else {
      DateTime date = new DateTime(event.getStartDate());
      date.setTimeZone(TIMEZONE_REGISTRY.getTimeZone(event.getTimeZone().getID()));
      return new DtStart(date);
    }
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    final DtStart dtStart = vEvent.getStartDate();

    if (dtStart == null) {
      return false;
    }

    event.setAllDay(this.isAllDay(vEvent));
    event.setStartDate(ICal4JUtils.getSqlTimestamp(dtStart.getDate()));

    return true;
  }
}
