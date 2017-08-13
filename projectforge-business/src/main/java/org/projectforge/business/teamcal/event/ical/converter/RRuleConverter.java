package org.projectforge.business.teamcal.event.ical.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RRule;

public class RRuleConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    if (event.hasRecurrence()) {
      return event.getRecurrenceRuleObject();
    }

    return null;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    final RRule rule = (RRule) vEvent.getProperty(Property.RRULE);
    event.setRecurrence(rule);

    return true;
  }
}
