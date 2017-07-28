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
      final Date startUtc = CalendarUtils.getUTCMidnightDate(event.getEndDate());
      date = new net.fortuna.ical4j.model.Date(startUtc);
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
