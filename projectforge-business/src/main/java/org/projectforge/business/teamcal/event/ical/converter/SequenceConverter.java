package org.projectforge.business.teamcal.event.ical.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Sequence;

public class SequenceConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    return new Sequence(event.getSequence() != null ? event.getSequence() : 0);
  }
}
