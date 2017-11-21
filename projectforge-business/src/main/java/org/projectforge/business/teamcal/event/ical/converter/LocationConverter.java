package org.projectforge.business.teamcal.event.ical.converter;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Location;

public class LocationConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    if (StringUtils.isNotBlank(event.getLocation())) {
      return new Location(event.getLocation());
    }

    return null;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    if (vEvent.getLocation() != null) {
      event.setLocation(vEvent.getLocation().getValue());
      return true;
    }
    return false;
  }
}
