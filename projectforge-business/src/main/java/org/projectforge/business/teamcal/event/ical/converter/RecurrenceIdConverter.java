package org.projectforge.business.teamcal.event.ical.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.time.DateHelper;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RecurrenceId;

public class RecurrenceIdConverter extends PropertyConverter
{
  private SimpleDateFormat format;
  private SimpleDateFormat formatInclZ;

  public RecurrenceIdConverter()
  {
    format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    formatInclZ = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
  }

  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    // TODO
    return null;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    RecurrenceId recurrenceId = vEvent.getRecurrenceId();

    if (recurrenceId == null) {
      return false;
    }

    try {
      synchronized (format) {
        if (recurrenceId.getTimeZone() != null) {
          TimeZone timezone = TimeZone.getTimeZone(recurrenceId.getTimeZone().getID());
          format.setTimeZone(timezone != null ? timezone : DateHelper.UTC);
          formatInclZ.setTimeZone(timezone != null ? timezone : DateHelper.UTC);

          Date date = null;
          try {
            date = format.parse(recurrenceId.getValue());
          } catch (ParseException e) {
            date = formatInclZ.parse(recurrenceId.getValue());
          }

          if (date != null) {
            format.setTimeZone(DateHelper.UTC);
            event.setRecurrenceReferenceId(format.format(date));
          }
        } else {
          format.setTimeZone(DateHelper.UTC);
          event.setRecurrenceReferenceId(format.format(recurrenceId.getDate()));
        }
      }
    } catch (Exception e) {
      return false;
    }

    return true;
  }
}
