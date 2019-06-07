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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.utils.ReflectionToString;

/**
 * Represents a recurrence event (created by a master TeamEventDO with recurrence rules).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TeamRecurrenceEvent implements TeamEvent, Serializable
{
  private static final long serialVersionUID = -7523583666714303142L;

  private final TeamEventDO master;

  private final Date startDate, endDate;

  /**
   * @param master
   * @param startDay day of event (startDate and endDate will be calculated based on this day and the master).
   */
  public TeamRecurrenceEvent(final TeamEventDO master, final Calendar startDate)
  {
    this.master = master;
    final Calendar cal = (Calendar) startDate.clone();
    this.startDate = cal.getTime();
    final long duration = master.getEndDate().getTime() - master.getStartDate().getTime();
    cal.add(Calendar.MINUTE, (int) (duration / 60000));
    this.endDate = cal.getTime();
  }

  /**
   * @return the master
   */
  public TeamEventDO getMaster()
  {
    return master;
  }

  /**
   * @see org.projectforge.business.teamcal.event.model.TeamEvent#getUid()
   */
  @Override
  public String getUid()
  {
    return master.getUid();
  }

  /**
   * @see org.projectforge.business.teamcal.event.model.TeamEvent#getSubject()
   */
  @Override
  public String getSubject()
  {
    return master.getSubject();
  }

  /**
   * @see org.projectforge.business.teamcal.event.model.TeamEvent#getLocation()
   */
  @Override
  public String getLocation()
  {
    return master.getLocation();
  }

  /**
   * @see org.projectforge.business.teamcal.event.model.TeamEvent#isAllDay()
   */
  @Override
  public boolean isAllDay()
  {
    return master.isAllDay();
  }

  /**
   * @see org.projectforge.business.teamcal.event.model.TeamEvent#getStartDate()
   */
  @Override
  public Date getStartDate()
  {
    return startDate;
  }

  /**
   * @see org.projectforge.business.teamcal.event.model.TeamEvent#getEndDate()
   */
  @Override
  public Date getEndDate()
  {
    return endDate;
  }

  /**
   * @see org.projectforge.business.teamcal.event.model.TeamEvent#getNote()
   */
  @Override
  public String getNote()
  {
    return master.getNote();
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return (new ReflectionToString(this)).toString();
  }
}
