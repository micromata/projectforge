package org.projectforge.business.teamcal.event.ical.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Created;

public class CreatedConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    DateTime created = new DateTime(event.getCreated());
    created.setUtc(true);
    return new Created(created);
  }
}
