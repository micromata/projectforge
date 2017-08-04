package org.projectforge.business.teamcal.event.ical.converter;

import java.text.SimpleDateFormat;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RecurrenceId;

public class RecurrenceIdConverter extends PropertyConverter
{
  private SimpleDateFormat format;

  public RecurrenceIdConverter()
  {
    format = new SimpleDateFormat("");
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

    if (recurrenceId != null) {
      try {
        event.setRecurrenceReferenceId(format.format(recurrenceId.getDate()));
      } catch (Exception e) {
        return false;
      }

      return true;
    }
    return false;
  }
}
