package org.projectforge.business.teamcal.event.ical.generator.converter;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Location;

public class LocationConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    if (StringUtils.isNotBlank(event.getLocation())) {
      return new Location(event.getLocation());
    }

    return null;
  }
}
