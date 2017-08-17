package org.projectforge.business.teamcal.event.ical.converter;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;

public class DescriptionConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    if (StringUtils.isNotBlank(event.getNote())) {
      return new Description(event.getNote());
    }

    return null;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    if (vEvent.getDescription() != null) {
      event.setNote(vEvent.getDescription().getValue());
      return true;
    }

    return false;
  }
}
