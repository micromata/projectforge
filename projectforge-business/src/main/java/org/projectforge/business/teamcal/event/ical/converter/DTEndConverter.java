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

import java.sql.Timestamp;
import java.util.Date;

import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.calendar.ICal4JUtils;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;

public class DTEndConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    net.fortuna.ical4j.model.Date date;

    if (event.isAllDay() == true) {
      final Date endUtc = CalendarUtils.getUTCMidnightDate(event.getEndDate());
      final org.joda.time.DateTime jodaTime = new org.joda.time.DateTime(endUtc);
      // TODO sn should not be done
      // requires plus 1 because one day will be omitted by calendar.
      final net.fortuna.ical4j.model.Date fortunaEndDate = new net.fortuna.ical4j.model.Date(jodaTime.plusDays(1).toDate());
      date = new net.fortuna.ical4j.model.Date(fortunaEndDate.getTime());
    } else {
      date = new DateTime(event.getEndDate());
      ((net.fortuna.ical4j.model.DateTime) date).setTimeZone(TIMEZONE_REGISTRY.getTimeZone(event.getTimeZone().getID()));
    }

    return new DtEnd(date);
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    final boolean isAllDay = this.isAllDay(vEvent);

    if (vEvent.getProperties().getProperties(Property.DTEND).isEmpty()) {
      return false;
    }

    if (isAllDay) {
      // TODO sn change behaviour to iCal standard
      final org.joda.time.DateTime jodaTime = new org.joda.time.DateTime(vEvent.getEndDate().getDate());
      final net.fortuna.ical4j.model.Date fortunaEndDate = new net.fortuna.ical4j.model.Date(jodaTime.plusDays(-1).toDate());
      event.setEndDate(new Timestamp(fortunaEndDate.getTime()));
    } else {
      event.setEndDate(ICal4JUtils.getSqlTimestamp(vEvent.getEndDate().getDate()));
    }

    return true;
  }
}
