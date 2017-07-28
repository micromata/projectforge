package org.projectforge.business.teamcal.event.ical.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Summary;

public class SummaryConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    if (event.getSubject() != null) {
      return new Summary(event.getSubject());
    }

    return null;
  }
}
