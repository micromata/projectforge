package org.projectforge.business.teamcal.event.ical.generator.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;

public class RRuleConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    if (event.hasRecurrence()) {
      return event.getRecurrenceRuleObject();
    }

    return null;
  }
}
