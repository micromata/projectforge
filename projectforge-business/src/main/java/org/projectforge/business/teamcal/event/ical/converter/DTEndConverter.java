package org.projectforge.business.teamcal.event.ical.converter;

import static org.projectforge.business.teamcal.event.ical.ICalConverterStore.TIMEZONE_REGISTRY;

import java.util.Date;

import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.CalendarUtils;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.DtEnd;

public class DTEndConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    net.fortuna.ical4j.model.Date date;

    if (event.isAllDay() == true) {
      final Date startUtc = CalendarUtils.getUTCMidnightDate(event.getEndDate());
      date = new net.fortuna.ical4j.model.Date(startUtc);
    } else {
      date = new DateTime(event.getEndDate());
      ((net.fortuna.ical4j.model.DateTime) date).setTimeZone(TIMEZONE_REGISTRY.getTimeZone(event.getTimeZone().getID()));
    }

    return new DtEnd(date);
  }
}
