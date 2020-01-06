/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.service;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.vacation.model.VacationCalendarDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.repository.VacationDao;
import org.projectforge.framework.time.PFDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Calendarservices for vacations.
 *
 * @author Florian Blumenstein
 */
@Service
public class VacationCalendarService {

  @Autowired
  private VacationDao vacationDao;

  @Autowired
  private TeamEventDao teamEventDao;

  public List<TeamCalDO> getCalendarsForVacation(final VacationDO vacation) {
    return vacationDao.getCalendarsForVacation(vacation);
  }


  public void saveOrUpdateVacationCalendars(final VacationDO vacation, final Collection<TeamCalDO> calendars) {
    if (calendars != null) {
      for (final TeamCalDO teamCalDO : calendars) {
        vacationDao.saveVacationCalendar(getOrCreateVacationCalendarDO(vacation, teamCalDO));
      }
    }
    final List<VacationCalendarDO> vacationCalendars = vacationDao.getVacationCalendarDOs(vacation);
    for (VacationCalendarDO vacationCalendar : vacationCalendars) {
      if (!calendars.contains(vacationCalendar.getCalendar())) {
        vacationDao.markAsDeleted(vacationCalendar);
      } else {
        vacationDao.markAsUndeleted(vacationCalendar);
      }
    }
  }

  public void markTeamEventsOfVacationAsDeleted(final VacationDO vacation, boolean deleteIncludingVacationCalendarDO) {
    final List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (final VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (vacationCalendarDO.getEvent() != null) {
        if (deleteIncludingVacationCalendarDO) {
          vacationDao.markAsDeleted(vacationCalendarDO);
        }
        teamEventDao.internalMarkAsDeleted(teamEventDao.internalGetById((vacationCalendarDO.getEvent().getId())));
      }
    }
  }

  public void undeleteTeamEventsOfVacation(final VacationDO vacation) {
    final List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (final VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (vacationCalendarDO.isDeleted()) {
        vacationDao.markAsUndeleted(vacationCalendarDO);
      }
    }
  }

  public void markAsUnDeleteEventsForVacationCalendars(final VacationDO vacation) {
    List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (vacationCalendarDO.getEvent() != null) {
        teamEventDao.internalUndelete(teamEventDao.internalGetById(vacationCalendarDO.getEvent().getId()));
      }
    }
  }

  public void createEventsForVacationCalendars(final VacationDO vacation) {
    final List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (!vacationCalendarDO.isDeleted()) {
        vacationCalendarDO.setEvent(getAndUpdateOrCreateTeamEventDO(vacationCalendarDO));
        vacationDao.saveVacationCalendar(vacationCalendarDO);
      }
    }
  }

  private VacationCalendarDO getOrCreateVacationCalendarDO(final VacationDO vacation, final TeamCalDO teamCalDO) {
    final List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (final VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (vacationCalendarDO.getCalendar().equals(teamCalDO)) {
        vacationCalendarDO.setDeleted(false);
        return vacationCalendarDO;
      }
    }
    final VacationCalendarDO vacationCalendarDO = new VacationCalendarDO();
    vacationCalendarDO.setCalendar(teamCalDO);
    vacationCalendarDO.setVacation(vacation);
    return vacationCalendarDO;
  }

  private TeamEventDO getAndUpdateOrCreateTeamEventDO(final VacationCalendarDO vacationCalendarDO) {
    final PFDateTime startTimestamp = PFDateTime.from(vacationCalendarDO.getVacation().getStartDate());
    final PFDateTime endTimestamp = PFDateTime.from(vacationCalendarDO.getVacation().getEndDate());

    if (vacationCalendarDO.getEvent() != null) {
      final TeamEventDO vacationTeamEvent = teamEventDao.internalGetById(vacationCalendarDO.getEvent().getId());
      if (vacationTeamEvent != null) {
        if (vacationTeamEvent.isDeleted()) {
          teamEventDao.internalUndelete(vacationTeamEvent);
        }

        if (!vacationTeamEvent.getStartDate().equals(startTimestamp) || !vacationTeamEvent.getEndDate().equals(endTimestamp)) {
          vacationTeamEvent.setStartDate(startTimestamp.getSqlTimestamp());
          vacationTeamEvent.setEndDate(endTimestamp.getSqlTimestamp());
          teamEventDao.internalSaveOrUpdate(vacationTeamEvent);
        }
      }
      return vacationTeamEvent;
    } else {
      final TeamEventDO newCalEventDO = new TeamEventDO();
      newCalEventDO.setAllDay(true);
      newCalEventDO.setStartDate(startTimestamp.getSqlTimestamp());
      newCalEventDO.setEndDate(endTimestamp.getSqlTimestamp());
      newCalEventDO.setSubject(vacationCalendarDO.getVacation().getEmployee().getUser().getFullname());
      newCalEventDO.setCalendar(vacationCalendarDO.getCalendar());
      teamEventDao.internalSave(newCalEventDO);
      return newCalEventDO;
    }
  }
}
