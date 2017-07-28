package org.projectforge.business.teamcal.event.ical.generator.converter;

import java.util.Date;

import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.CalendarUtils;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.DtStart;

public class DTStartConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    if (event.isAllDay() == true) {
      final Date startUtc = CalendarUtils.getUTCMidnightDate(event.getStartDate());
      net.fortuna.ical4j.model.Date date = new net.fortuna.ical4j.model.Date(startUtc);
      return new DtStart(date);
    } else {
      DateTime date = new DateTime(event.getStartDate());
      date.setTimeZone(registry.getTimeZone(event.getTimeZone().getID()));
      return new DtStart(date);
    }
  }
}
