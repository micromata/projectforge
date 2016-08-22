package org.projectforge.business.teamcal.event;

import java.util.List;
import java.util.Set;

import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

public interface TeamEventService
{
  List<Integer> getAssignedAttendeeIds(TeamEventDO data);

  List<TeamEventAttendeeDO> getSortedAddressesAsAttendee();

  TeamEventAttendeeDO getAttendee(Integer attendeeId);

  void assignAttendees(TeamEventDO data, Set<TeamEventAttendeeDO> itemsToAssign,
      Set<TeamEventAttendeeDO> itemsToUnassign);

  boolean sendTeamEventToAttendees(TeamEventDO data, boolean isNew, boolean hasChanges,
      Set<TeamEventAttendeeDO> addedAttendees);

}
