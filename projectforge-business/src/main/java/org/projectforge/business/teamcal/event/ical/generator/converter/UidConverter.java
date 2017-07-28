package org.projectforge.business.teamcal.event.ical.generator.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Uid;

public class UidConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    if (event.getUid() != null) {
      return new Uid(event.getUid());
    }

    return null;
  }
}
