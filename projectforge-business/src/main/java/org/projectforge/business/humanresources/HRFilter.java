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

package org.projectforge.business.humanresources;

import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.time.TimePeriod;

import java.time.LocalDate;


/**
 * Is not synchronized.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class HRFilter extends BaseSearchFilter
{
  private static final long serialVersionUID = -7378185662383701224L;

  private TimePeriod timePeriod;

  private boolean onlyMyProjects;

  private boolean otherProjectsGroupedByCustomer;

  private boolean allProjectsGroupedByCustomer;

  private boolean showPlanning = true;

  private boolean showBookedTimesheets;

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
   * Gets start and stop day from timePeriod.
   * @param timePeriod
   */
  public void setTimePeriod(final TimePeriod timePeriod)
  {
    setStartDay(timePeriod.getFromDay());
    setStopDay(timePeriod.getToDay());
  }

  private TimePeriod getTimePeriod()
  {
    if (timePeriod == null) {
      timePeriod = new TimePeriod();
    }
    return timePeriod;
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

  /**
   * Show all projects only grouped by customer (show the total sum of all projects for each customer).
   */
  public boolean isAllProjectsGroupedByCustomer()
  {
    return allProjectsGroupedByCustomer;
  }

  public void setAllProjectsGroupedByCustomer(boolean allProjectsGroupedByCustomer)
  {
    this.allProjectsGroupedByCustomer = allProjectsGroupedByCustomer;
  }

  /**
   * Show other projects (the projects the current logged in user is not assigned as project manager) as total sum by customer.
   */
  public boolean isOtherProjectsGroupedByCustomer()
  {
    return otherProjectsGroupedByCustomer;
  }

  public void setOtherProjectsGroupedByCustomer(boolean otherProjectsGroupedByCustomer)
  {
    this.otherProjectsGroupedByCustomer = otherProjectsGroupedByCustomer;
  }

  /**
   * Show planning entries.
   */
  public boolean isShowPlanning()
  {
    return showPlanning;
  }

  public void setShowPlanning(boolean showPlanning)
  {
    this.showPlanning = showPlanning;
  }

  /**
   * Show the total times of booked time sheets.
   */
  public boolean isShowBookedTimesheets()
  {
    return showBookedTimesheets;
  }

  public void setShowBookedTimesheets(boolean showBookedTimesheets)
  {
    this.showBookedTimesheets = showBookedTimesheets;
  }
}
