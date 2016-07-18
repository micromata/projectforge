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

package org.projectforge.business.user;

import java.io.Serializable;

import org.projectforge.framework.persistence.api.BaseSearchFilter;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class PFUserFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = 5603571926905349386L;

  private Boolean isAdminUser, deactivatedUser, restrictedUser, localUser, hrPlanning;

  public PFUserFilter()
  {
  }

  public PFUserFilter(final BaseSearchFilter filter)
  {
    super(filter);
    isAdminUser = deactivatedUser = restrictedUser = localUser = null;
  }

  /**
   * @return the isAdminUser
   */
  public Boolean getIsAdminUser()
  {
    return isAdminUser;
  }

  /**
   * @param isAdminUser the isAdminUser to set
   * @return this for chaining.
   */
  public PFUserFilter setIsAdminUser(final Boolean isAdminUser)
  {
    this.isAdminUser = isAdminUser;
    return this;
  }

  /**
   * @return the deactivated
   */
  public Boolean getDeactivatedUser()
  {
    return deactivatedUser;
  }

  /**
   * @param deactivated the deactivated to set
   * @return this for chaining.
   */
  public PFUserFilter setDeactivatedUser(final Boolean deactivatedUser)
  {
    this.deactivatedUser = deactivatedUser;
    return this;
  }

  /**
   * @return the restricted
   */
  public Boolean getRestrictedUser()
  {
    return restrictedUser;
  }

  /**
   * @param restricted the restricted to set
   * @return this for chaining.
   */
  public PFUserFilter setRestrictedUser(final Boolean restrictedUser)
  {
    this.restrictedUser = restrictedUser;
    return this;
  }

  /**
   * @return the localUser
   */
  public Boolean getLocalUser()
  {
    return localUser;
  }

  /**
   * @param localUser the localUser to set
   * @return this for chaining.
   */
  public PFUserFilter setLocalUser(final Boolean localUser)
  {
    this.localUser = localUser;
    return this;
  }

  /**
   * @return the hrPlanning
   */
  public Boolean getHrPlanning()
  {
    return hrPlanning;
  }

  /**
   * @param hrPlanning the hrPlanning to set
   * @return this for chaining.
   */
  public PFUserFilter setHrPlanning(final Boolean hrPlanning)
  {
    this.hrPlanning = hrPlanning;
    return this;
  }
}
