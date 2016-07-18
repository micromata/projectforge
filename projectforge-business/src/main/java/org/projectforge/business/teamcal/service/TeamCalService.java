package org.projectforge.business.teamcal.service;

import java.util.Collection;
import java.util.List;

import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;

public interface TeamCalService
{

  List<Integer> getCalIdList(Collection<TeamCalDO> teamCals);

  List<TeamCalDO> getCalList(TeamCalCache teamCalCache, Collection<Integer> teamCalIds);

  List<String> getCalendarNames(String calIds);

  Collection<TeamCalDO> getSortedCalendars(String calIds);

  String getCalendarIds(Collection<TeamCalDO> calendars);

  Collection<TeamCalDO> getSortedCalenders();

}
