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
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.utils.ReflectionToString;
import org.projectforge.framework.time.PFDateTime;

import java.io.Serializable;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Represents a recurrence event (created by a master TeamEventDO with recurrence rules).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class TeamRecurrenceEvent implements ICalendarEvent, Serializable
{
  private static final long serialVersionUID = -7523583666714303142L;

  private final TeamEventDO master;

  private final Date startDate, endDate;

  /**
   * @param master
   * @param startDay day of event (startDate and endDate will be calculated based on this day and the master).
   */
  public TeamRecurrenceEvent(final TeamEventDO master, final PFDateTime startDate)
  {
    this.master = master;
    this.startDate = startDate.getUtilDate();
    final long duration = master.getEndDate().getTime() - master.getStartDate().getTime();
    PFDateTime endDate = startDate.plus((int) (duration / 60000), ChronoUnit.MINUTES);
    this.endDate = endDate.getUtilDate();
  }

  /**
   * @return the master
   */
  public TeamEventDO getMaster()
  {
    return master;
  }

  /**
   * @see ICalendarEvent#getUid()
   */
  @Override
  public String getUid()
  {
    return master.getUid();
  }

  /**
   * @see ICalendarEvent#getSubject()
   */
  @Override
  public String getSubject()
  {
    return master.getSubject();
  }

  /**
   * @see ICalendarEvent#getLocation()
   */
  @Override
  public String getLocation()
  {
    return master.getLocation();
  }

  /**
   * @see ICalendarEvent#getAllDay()
   */
  @Override
  public boolean getAllDay()
  {
    return master.getAllDay();
  }

  /**
   * @see ICalendarEvent#getStartDate()
   */
  @Override
  public Date getStartDate()
  {
    return startDate;
  }

  /**
   * @see ICalendarEvent#getEndDate()
   */
  @Override
  public Date getEndDate()
  {
    return endDate;
  }

  /**
   * @see ICalendarEvent#getNote()
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
