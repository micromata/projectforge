package org.projectforge.business.teamcal.event;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.diff.TeamEventDiffType;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

public interface TeamEventService
{
  List<Integer> getAssignedAttendeeIds(TeamEventDO data);

  List<TeamEventAttendeeDO> getAddressesAndUserAsAttendee();

  TeamEventAttendeeDO getAttendee(Integer attendeeId);

  void assignAttendees(TeamEventDO data, Set<TeamEventAttendeeDO> itemsToAssign,
      Set<TeamEventAttendeeDO> itemsToUnassign);

  void updateAttendees(TeamEventDO event, Set<TeamEventAttendeeDO> attendeesOldState);

  boolean checkAndSendMail(final TeamEventDO event, final TeamEventDiffType diffType);

  boolean checkAndSendMail(final TeamEventDO eventNew, final TeamEventDO eventOld);

  TeamEventDO findByUid(String reqEventUid);

  TeamEventAttendeeDO findByAttendeeId(Integer attendeeId, boolean checkAccess);

  TeamEventAttendeeDO findByAttendeeId(Integer attendeeId);

  void update(TeamEventDO event);

  void update(TeamEventDO event, boolean checkAccess);

  List<TeamEvent> getEventList(TeamEventFilter filter, boolean calculateRecurrenceEvents);

  List<TeamEventDO> getTeamEventDOList(TeamEventFilter filter);

  TeamEventDO getById(Integer teamEventId);

  void saveOrUpdate(TeamEventDO teamEvent);

  void markAsDeleted(TeamEventDO teamEvent);

  void undelete(TeamEventDO teamEvent);

  void save(TeamEventDO newEvent);

  TeamEventDao getTeamEventDao();

  void updateAttendee(TeamEventAttendeeDO attendee, boolean accessCheck);

  List<Integer> getCalIdList(Collection<TeamCalDO> teamCals);
}
