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

package org.projectforge.business.teamcal.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class TeamEventFilter extends BaseSearchFilter implements Serializable, Cloneable
{
  private static final long serialVersionUID = 2554610661216573080L;

  private PFUserDO user;

  private Integer teamCalId;

  private Collection<Integer> teamCals;

  private Date startDate;

  private Date endDate;

  private boolean onlyRecurrence;

  /**
   * @param filter
   */
  public TeamEventFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public TeamEventFilter()
  {
  }

  /**
   * @return the user
   */
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @param user the user to set
   * @return this for chaining.
   */
  public TeamEventFilter setUser(final PFUserDO user)
  {
    this.user = user;
    return this;
  }

  /**
   * @return the startDate
   */
  public Date getStartDate()
  {
    return startDate;
  }

  /**
   * @param startDate the startDate to set
   * @return this for chaining.
   */
  public TeamEventFilter setStartDate(final Date startDate)
  {
    this.startDate = startDate;
    return this;
  }

  /**
   * @return the endDate
   */
  public Date getEndDate()
  {
    return endDate;
  }

  /**
   * @param date the endDate to set
   * @return this for chaining.
   */
  public TeamEventFilter setEndDate(final Date date)
  {
    this.endDate = date;
    return this;
  }

  /**
   * @return the teamCalId
   */
  public Integer getTeamCalId()
  {
    return teamCalId;
  }

  /**
   * @param teamCalId the teamCalId to set
   * @return this for chaining.
   */
  public TeamEventFilter setTeamCalId(final Integer teamCalId)
  {
    this.teamCalId = teamCalId;
    return this;
  }

  /**
   * @return the teamCals
   */
  public Collection<Integer> getTeamCals()
  {
    return teamCals;
  }

  /**
   * @param teamCals the teamCals to set
   * @return this for chaining.
   */
  public TeamEventFilter setTeamCals(final Collection<Integer> teamCals)
  {
    this.teamCals = teamCals;
    return this;
  }

  /**
   * Only for internal purposes.
   * @return the onlyRecurrence
   */
  public boolean isOnlyRecurrence()
  {
    return onlyRecurrence;
  }

  /**
   * Only for internal purposes.
   * @param onlyRecurrence the onlyRecurrence to set
   * @return this for chaining.
   */
  public TeamEventFilter setOnlyRecurrence(final boolean onlyRecurrence)
  {
    this.onlyRecurrence = onlyRecurrence;
    return this;
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public TeamEventFilter clone()
  {
    final TeamEventFilter clone = new TeamEventFilter(this);
    if (this.startDate != null) {
      clone.startDate = (Date) this.startDate.clone();
    }
    if (this.endDate != null) {
      clone.endDate = (Date) this.endDate.clone();
    }
    clone.teamCalId = this.teamCalId;
    clone.teamCals = this.teamCals;
    clone.user = this.user;
    return clone;
  }
}
