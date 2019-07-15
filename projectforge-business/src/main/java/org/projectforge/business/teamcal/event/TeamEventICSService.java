package org.projectforge.business.teamcal.event;

import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.event.diff.TeamEventDiffType;
import org.projectforge.business.teamcal.event.model.TeamEventICSDO;
import org.projectforge.model.rest.CalendarEventObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class TeamEventICSService
{
  @Autowired
  private TeamEventICSDao teamEventICSDao;

  @Autowired
  private TeamCalCache teamCalCache;

  public TeamEventICSDao getTeamEventICSDao(){
    return teamEventICSDao;
  }

  public void saveOrUpdate(TeamEventICSDO teamEvent) {
    teamEventICSDao.saveOrUpdate(teamEvent);
  }

  public void markAsDeleted(TeamEventICSDO teamEvent) {
    teamEventICSDao.markAsDeleted(teamEvent);
  }

  public void undelete(TeamEventICSDO teamEvent) {
    teamEventICSDao.undelete(teamEvent);
  }

  public void save(TeamEventICSDO teamEvent) {
    teamEventICSDao.save(teamEvent);
  }

  public void update(TeamEventICSDO teamEvent) {
    teamEventICSDao.update(teamEvent);
  }

  public void update(TeamEventICSDO teamEvent, boolean checkAccess) {
    teamEventICSDao.internalUpdate(teamEvent, checkAccess);
  }



  public boolean checkAndSendMail(final TeamEventICSDO event, final TeamEventDiffType diffType) { return false; }

  public boolean checkAndSendMail(final TeamEventICSDO eventNew, final TeamEventICSDO eventOld) { return false; }



  public TeamEventICSDO findByUid(Integer calendarId, String reqEventUid, boolean excludeDeleted) {
    return teamEventICSDao.getByUid(calendarId, reqEventUid, excludeDeleted);
  }

  public List<TeamEventICSDO> getTeamEventICSDOList(TeamEventFilter filter) {
    return teamEventICSDao.getList(filter);
  }

  public TeamEventICSDO getTeamEventICSDO (CalendarEventObject calendarEventObject) {
    TeamEventICSDO teamEventICSDO = new TeamEventICSDO();
    teamEventICSDO.setCalendar(teamCalCache.getCalendar(calendarEventObject.getCalendarId()));
    teamEventICSDO.setStartDate((Timestamp) calendarEventObject.getStartDate());
    teamEventICSDO.setEndDate((Timestamp) calendarEventObject.getEndDate());
    teamEventICSDO.setLocation(calendarEventObject.getLocation());
    teamEventICSDO.setNote(calendarEventObject.getNote());
    teamEventICSDO.setSubject(calendarEventObject.getSubject());
    teamEventICSDO.setUid(calendarEventObject.getUid());
    teamEventICSDO.setIcsData(calendarEventObject.getIcsData());
    return teamEventICSDO;
  }
}
