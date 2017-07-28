package org.projectforge.business.teamcal.event.ical.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Transp;

public class TransparencyConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    return Transp.OPAQUE; // TODO
  }
}
