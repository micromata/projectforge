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

package org.projectforge.business.humanresources;

import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.time.LocalDatePeriod;
import org.projectforge.framework.time.TimePeriod;

import java.io.Serializable;
import java.time.LocalDate;

/**
 *
 * @author Mario Groß (m.gross@micromata.de)
 *
 */
public class HRPlanningFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -8570155150219604587L;

  private TimePeriod timePeriod;

  private Long projektId;

  private Long userId;

  private boolean longFormat;

  private boolean groupEntries;

  private boolean onlyMyProjects;

  public Long getProjektId()
  {
    return projektId;
  }

  public void setProjektId(Long projektId)
  {
    this.projektId = projektId;
  }

  public Long getUserId()
  {
    return userId;
  }

  public void setUserId(Long userId)
  {
    this.userId = userId;
  }

  /**
   * @return the startDay
   */
  public LocalDate getStartDay()
  {
    return getTimePeriod().getFromDay();
  }

  /**
   * @param startDay the startDay to set
   */
  public void setStartDay(LocalDate startDay)
  {
    getTimePeriod().setFromDay(startDay);
  }

  /**
   * @return the stopDay
   */
  public LocalDate getStopDay()
  {
    return getTimePeriod().getToDay();
  }

  /**
   * @param stopDay the stopDay to set
   */
  public void setStopDay(LocalDate stopDay)
  {
    getTimePeriod().setToDay(stopDay);
  }

  /**
   * Gets start and stop time from timePeriod.
   * @param timePeriod
   */
  public void setTimePeriod(final LocalDatePeriod timePeriod)
  {
    setStartDay(timePeriod.getBegin());
    setStopDay(timePeriod.getEnd());
  }

  private TimePeriod getTimePeriod()
  {
    if (timePeriod == null) {
      timePeriod = new TimePeriod();
    }
    return timePeriod;
  }

  /**
   * @return true if the whole description of an entry should be displayed or false if the description is abbreviated.
   */
  public boolean isLongFormat()
  {
    return longFormat;
  }

  public void setLongFormat(boolean longFormat)
  {
    this.longFormat = longFormat;
  }

  /**
   * If true then for each employee only one entry with the sum of planned hours is returned.
   */
  public boolean isGroupEntries()
  {
    return groupEntries;
  }

  public void setGroupEntries(boolean groupEntries)
  {
    this.groupEntries = groupEntries;
  }

  /**
   * If true then only entries will be returned which are assigned to at minimum one project of which the current user is member of the
   * project manager group.
   */
  public boolean isOnlyMyProjects()
  {
    return onlyMyProjects;
  }

  public void setOnlyMyProjects(boolean onlyMyProjects)
  {
    this.onlyMyProjects = onlyMyProjects;
  }
}
