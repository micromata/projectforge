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

package org.projectforge.business.humanresources;

import java.io.Serializable;
import java.util.Date;

import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.time.TimePeriod;


/**
 * 
 * @author Mario Groß (m.gross@micromata.de)
 * 
 */
public class HRPlanningFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -8570155150219604587L;

  private TimePeriod timePeriod;

  private Integer projektId;

  private Integer userId;

  private boolean longFormat;

  private boolean groupEntries;

  private boolean onlyMyProjects;

  public Integer getProjektId()
  {
    return projektId;
  }

  public void setProjektId(Integer projektId)
  {
    this.projektId = projektId;
  }

  public Integer getUserId()
  {
    return userId;
  }

  public void setUserId(Integer userId)
  {
    this.userId = userId;
  }

  /**
   * @return the startTime
   */
  public Date getStartTime()
  {
    return getTimePeriod().getFromDate();
  }

  /**
   * @param startTime the startTime to set
   */
  public void setStartTime(Date startTime)
  {
    getTimePeriod().setFromDate(startTime);
  }

  /**
   * @return the stopTime
   */
  public Date getStopTime()
  {
    return getTimePeriod().getToDate();
  }

  /**
   * @param stopTime the stopTime to set
   */
  public void setStopTime(Date stopTime)
  {
    getTimePeriod().setToDate(stopTime);
  }

  /**
   * Gets start and stop time from timePeriod.
   * @param timePeriod
   */
  public void setTimePeriod(final TimePeriod timePeriod)
  {
    setStartTime(timePeriod.getFromDate());
    setStopTime(timePeriod.getToDate());
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
