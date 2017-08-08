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
