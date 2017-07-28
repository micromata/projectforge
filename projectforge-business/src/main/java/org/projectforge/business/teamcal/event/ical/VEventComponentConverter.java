package org.projectforge.business.teamcal.event.ical;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.component.VEvent;

public interface VEventComponentConverter
{
  public boolean toVEvent(TeamEventDO event, VEvent vEvent);

  public boolean fromVEvent(TeamEventDO event, VEvent vEvent);
}
