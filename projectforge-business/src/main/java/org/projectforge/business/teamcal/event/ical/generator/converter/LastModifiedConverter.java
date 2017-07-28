package org.projectforge.business.teamcal.event.ical.generator.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.LastModified;

public class LastModifiedConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    DateTime lastModified = new DateTime(event.getDtStamp() != null ? event.getDtStamp() : event.getCreated());
    lastModified.setUtc(true);
    return new LastModified(lastModified);
  }
}
