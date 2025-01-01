/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.teamcal.event;

import org.projectforge.business.calendar.event.model.ICalendarEvent;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.diff.TeamEventDiffType;
import org.projectforge.business.teamcal.event.ical.ICalHandler;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TeamEventService
{
  List<TeamEventAttendeeDO> getAddressesAndUserAsAttendee();

  TeamEventAttendeeDO getAttendee(Long attendeeId);

  void assignAttendees(TeamEventDO data, Set<TeamEventAttendeeDO> itemsToAssign,
      Set<TeamEventAttendeeDO> itemsToUnassign);

  void updateAttendees(TeamEventDO event, Set<TeamEventAttendeeDO> attendeesOldState);

  TeamEventDO findByUid(Long calendarId, String reqEventUid, boolean excludeDeleted);

  TeamEventAttendeeDO findByAttendeeId(Long attendeeId, boolean checkAccess);

  void update(TeamEventDO event);

  void update(TeamEventDO event, boolean checkAccess);

  List<ICalendarEvent> getEventList(TeamEventFilter filter, boolean calculateRecurrenceEvents);

  List<TeamEventDO> getTeamEventDOList(TeamEventFilter filter);

  TeamEventDO getById(Long teamEventId);

  void saveOrUpdate(TeamEventDO teamEvent);

  void markAsDeleted(TeamEventDO teamEvent);

  void undelete(TeamEventDO teamEvent);

  void save(TeamEventDO newEvent);

  TeamEventDao getTeamEventDao();

  void updateAttendee(TeamEventAttendeeDO attendee, boolean accessCheck);

  ICalHandler getEventHandler(final TeamCalDO defaultCalendar);

  /**
   * This method should be moved to ICalHandler after a rework of import ical in web ui!
   *
   * @param event
   */
  @Deprecated
  void fixAttendees(final TeamEventDO event);
}
