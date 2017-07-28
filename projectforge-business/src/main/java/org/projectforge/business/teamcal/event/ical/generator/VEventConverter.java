package org.projectforge.business.teamcal.event.ical.generator;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.component.VEvent;

public interface VEventConverter
{
  public boolean convert(TeamEventDO event, VEvent vEvent);
}
