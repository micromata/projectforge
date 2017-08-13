package org.projectforge.business.teamcal.event.ical.converter;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Uid;

public class UidConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    if (event.getUid() != null) {
      return new Uid(event.getUid());
    }

    return null;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    if (vEvent.getUid() != null && StringUtils.isEmpty(vEvent.getUid().getValue()) == false) {
      event.setUid(vEvent.getUid().getValue());
      return true;
    }

    return false;
  }
}
