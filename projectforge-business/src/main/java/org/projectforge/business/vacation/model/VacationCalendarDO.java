/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.vacation.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

/**
 * @author Florian Blumenstein
 */
@Entity
@Table(name = "t_employee_vacation_calendar")
public class VacationCalendarDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -1208123049212394757L;

  private VacationDO vacation;

  private TeamCalDO calendar;

  private TeamEventDO event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vacation_id", nullable = false)
  public VacationDO getVacation()
  {
    return vacation;
  }

  public void setVacation(VacationDO vacation)
  {
    this.vacation = vacation;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "calendar_id", nullable = false)
  public TeamCalDO getCalendar()
  {
    return calendar;
  }

  public void setCalendar(TeamCalDO calendar)
  {
    this.calendar = calendar;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  public TeamEventDO getEvent()
  {
    return event;
  }

  public void setEvent(TeamEventDO event)
  {
    this.event = event;
  }
}
