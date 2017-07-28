package org.projectforge.business.teamcal.event.ical.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

public class TransparencyConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    // currently not implemented
    return null;
    //    return Transp.OPAQUE;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    // currently not implemented
    return false;
  }
}
