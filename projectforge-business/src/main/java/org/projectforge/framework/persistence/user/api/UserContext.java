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

package org.projectforge.framework.persistence.user.api;

import java.io.Serializable;

import org.apache.commons.lang.Validate;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;

/**
 * User context for logged-in users. Contains the user and the current tenant (if any) etc.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class UserContext implements Serializable
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserContext.class);

  private static final long serialVersionUID = 4934701869144478233L;

  private PFUserDO user;

  private TenantDO currentTenant;

  private boolean stayLoggedIn;

  private UserGroupCache userGroupCache;

  public UserContext(UserGroupCache userGroupCache)
  {
    this.userGroupCache = userGroupCache;
  }

  /**
   * Don't use this method. It's used for creating an UserContext without copying a user.
   * 
   * @param user
   * @return The created UserContext.
   */
  public static UserContext __internalCreateWithSpecialUser(final PFUserDO user, UserGroupCache userGroupCache)
  {
    final UserContext userContext = new UserContext(userGroupCache);
    userContext.user = user;
    return userContext;
  }

  /**
   * Stores the given user in the context. If the user contains secret fields (such as password etc.) a copy without
   * such fields is stored.
   * 
   * @param user
   */
  public UserContext(final PFUserDO user, UserGroupCache userGroupCache)
  {
    Validate.notNull(user);
    this.userGroupCache = userGroupCache;
    if (user.hasSecretFieldValues() == true) {
      log.warn(
          "Should instantiate UserContext with user containing secret values (makes now a copy of the given user).");
      this.user = PFUserDO.createCopyWithoutSecretFields(user);
    } else {
      this.user = user;
    }
    this.currentTenant = user.getTenant();
  }

  /**
   * Clear all fields (user etc.).
   * 
   * @return this for chaining.
   */
  public UserContext logout()
  {
    this.user = null;
    this.currentTenant = null;
    this.stayLoggedIn = false;
    return this;
  }

  /**
   * Refreshes the user stored in the user group cache. Ignore fields such as stayLoggedInKey, password and
   * passwordSalt.
   * 
   * @return this for chaining.
   */
  public UserContext refreshUser()
  {
    final PFUserDO updatedUser = userGroupCache.getUser(user.getId());
    if (updatedUser == null) {
      log.warn("Couldn't update user from UserCache, should only occur in maintenance mode!");
      return this;
    }
    if (user.hasSecretFieldValues() == true) {
      log.warn(
          "Oups, userCache contains user (id=" + user.getId() + ") with secret values, please contact developers.");
      this.user = PFUserDO.createCopyWithoutSecretFields(updatedUser);
    } else {
      this.user = updatedUser;
    }
    return this;
  }

  /**
   * @return the user
   */
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @return the currentTenant
   */
  public TenantDO getCurrentTenant()
  {
    return currentTenant;
  }

  /**
   * @param tenant the currentTenant to set
   * @return this for chaining.
   */
  public UserContext setCurrentTenant(final TenantDO tenant)
  {
    if (tenant == null) {
      log.warn("Can't switch to current tenant=null!");
      return this;
    }
    if (tenant.getId() == null) {
      log.warn("Can't switch to current tenant with id=null!");
      return this;
    }
    this.currentTenant = tenant;
    return this;
  }

  /**
   * @return the stayLoggedIn
   */
  public boolean isStayLoggedIn()
  {
    return stayLoggedIn;
  }

  /**
   * @param stayLoggedIn the stayLoggedIn to set
   * @return this for chaining.
   */
  public UserContext setStayLoggedIn(final boolean stayLoggedIn)
  {
    this.stayLoggedIn = stayLoggedIn;
    return this;
  }

}
