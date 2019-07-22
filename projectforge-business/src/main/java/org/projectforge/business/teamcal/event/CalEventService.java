/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.event.diff.TeamEventDiffType;
import org.projectforge.business.teamcal.event.model.CalEventDO;
import org.projectforge.model.rest.CalendarEventObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class CalEventService
{
  @Autowired
  private CalEventDao calEventDao;

  @Autowired
  private TeamCalCache teamCalCache;

  public CalEventDao getTeamEventICSDao(){
    return calEventDao;
  }

  public void saveOrUpdate(CalEventDO teamEvent) {
    calEventDao.saveOrUpdate(teamEvent);
  }

  public void markAsDeleted(CalEventDO teamEvent) {
    calEventDao.markAsDeleted(teamEvent);
  }

  public void undelete(CalEventDO teamEvent) {
    calEventDao.undelete(teamEvent);
  }

  public void save(CalEventDO teamEvent) {
    calEventDao.save(teamEvent);
  }

  public void update(CalEventDO teamEvent) {
    calEventDao.update(teamEvent);
  }

  public void update(CalEventDO teamEvent, boolean checkAccess) {
    calEventDao.internalUpdate(teamEvent, checkAccess);
  }

  public boolean checkAndSendMail(final CalEventDO event, final TeamEventDiffType diffType) { return false; }

  public boolean checkAndSendMail(final CalEventDO eventNew, final CalEventDO eventOld) { return false; }

  public CalEventDO findByUid(Integer calendarId, String reqEventUid, boolean excludeDeleted) {
    return calEventDao.getByUid(calendarId, reqEventUid, excludeDeleted);
  }

  public List<CalEventDO> getTeamEventICSDOList(TeamEventFilter filter) {
    return calEventDao.getList(filter);
  }

  public CalEventDO getTeamEventICSDO (CalendarEventObject calendarEventObject) {
    CalEventDO calEventDO = new CalEventDO();
    calEventDO.setCalendar(teamCalCache.getCalendar(calendarEventObject.getCalendarId()));
    calEventDO.setStartDate((Timestamp) calendarEventObject.getStartDate());
    calEventDO.setEndDate((Timestamp) calendarEventObject.getEndDate());
    calEventDO.setLocation(calendarEventObject.getLocation());
    calEventDO.setNote(calendarEventObject.getNote());
    calEventDO.setSubject(calendarEventObject.getSubject());
    calEventDO.setUid(calendarEventObject.getUid());
    calEventDO.setIcsData(calendarEventObject.getIcsData());
    return calEventDO;
  }
}
