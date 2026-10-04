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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamEventServiceImpl implements TeamEventService {
    @Autowired
    private TeamEventDao teamEventDao;

    @Override
    public TeamEventDO findByUid(Long calendarId, String reqEventUid, boolean excludeDeleted) {
        return teamEventDao.getByUid(calendarId, reqEventUid, excludeDeleted);
    }

    @Override
    public void update(TeamEventDO event) {
        update(event, true);
    }

    @Override
    public void update(TeamEventDO event, boolean checkAccess) {
        teamEventDao.update(event, checkAccess);
    }

    @Override
    public List<ICalendarEvent> getEventList(TeamEventFilter filter, boolean calculateRecurrenceEvents) {
        return teamEventDao.getEventList(filter, calculateRecurrenceEvents);
    }

    @Override
    public List<TeamEventDO> getTeamEventDOList(TeamEventFilter filter) {
        return teamEventDao.select(filter);
    }

    @Override
    public TeamEventDO getById(Long teamEventId) {
        return teamEventDao.find(teamEventId);
    }

    @Override
    public void saveOrUpdate(TeamEventDO teamEvent) {
        teamEventDao.insertOrUpdate(teamEvent);
    }

    @Override
    public void markAsDeleted(TeamEventDO teamEvent) {
        teamEventDao.markAsDeleted(teamEvent);
    }

    @Override
    public void undelete(TeamEventDO teamEvent) {
        teamEventDao.undelete(teamEvent);
    }

    @Override
    public void save(TeamEventDO newEvent) {
        teamEventDao.insert(newEvent);
    }

    @Override
    public TeamEventDao getTeamEventDao() {
        return teamEventDao;
    }
}
