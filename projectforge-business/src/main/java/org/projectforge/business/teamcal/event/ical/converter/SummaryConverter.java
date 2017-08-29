package org.projectforge.business.teamcal.event.ical.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Summary;

public class SummaryConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    if (event.getSubject() != null) {
      return new Summary(event.getSubject());
    }

    return null;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    if (vEvent.getSummary() != null) {
      event.setSubject(vEvent.getSummary().getValue());
      return true;
    }

    return false;
  }
}
