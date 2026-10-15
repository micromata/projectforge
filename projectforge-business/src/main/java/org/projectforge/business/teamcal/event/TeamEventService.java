/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import java.util.List;

public interface TeamEventService
{
  TeamEventDO findByUid(Long calendarId, String reqEventUid, boolean excludeDeleted);

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
}
