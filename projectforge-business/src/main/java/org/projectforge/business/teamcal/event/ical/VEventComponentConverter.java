package org.projectforge.business.teamcal.event.ical;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.component.VEvent;

public interface VEventComponentConverter
{
  public boolean convert(TeamEventDO event, VEvent vEvent);
}
