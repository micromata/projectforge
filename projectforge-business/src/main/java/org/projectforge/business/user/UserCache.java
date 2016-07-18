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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The users will be cached with this class (without groups).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class UserCache extends AbstractCache
{
  private static final long serialVersionUID = -3292057087221854198L;

  private static Logger log = Logger.getLogger(UserCache.class);

  private Map<Integer, PFUserDO> userMap;

  @Autowired
  private UserService userService;

  public UserCache()
  {
    setExpireTimeInHours(1);
  }

  public PFUserDO getUser(final Integer userId)
  {
    if (userId == null) {
      return null;
    }
    // checkRefresh(); Done by getUserMap().
    return getUserMap() != null ? userMap.get(userId) : null; // Only null in maintenance mode (if t_user isn't readable).
  }

  public PFUserDO getUser(final String username)
  {
    if (StringUtils.isEmpty(username) == true) {
      return null;
    }
    for (final PFUserDO user : getUserMap().values()) {
      if (username.equals(user.getUsername()) == true) {
        return user;
      }
    }
    return null;
  }

  public PFUserDO getUserByFullname(final String fullname)
  {
    if (StringUtils.isEmpty(fullname) == true) {
      return null;
    }
    for (final PFUserDO user : getUserMap().values()) {
      if (fullname.equals(user.getFullname()) == true) {
        return user;
      }
    }
    return null;
  }

  /**
   * @return all users (also deleted users).
   */
  public Collection<PFUserDO> getAllUsers()
  {
    // checkRefresh(); Done by getUserMap().
    return getUserMap().values();
  }

  /**
   * Only for internal use.
   */
  public int internalGetNumberOfUsers()
  {
    if (userMap == null) {
      return 0;
    } else {
      // checkRefresh(); Done by getUserMap().
      return getUserMap().size();
    }
  }

  public String getUsername(final Integer userId)
  {
    // checkRefresh(); Done by getUserMap().
    final PFUserDO user = getUserMap().get(userId);
    if (user == null) {
      return String.valueOf(userId);
    }
    return user.getUsername();
  }

  /**
   * Should be called after user modifications.
   * 
   * @param user
   */
  void updateUser(final PFUserDO user)
  {
    getUserMap().put(user.getId(), user);
  }

  private Map<Integer, PFUserDO> getUserMap()
  {
    checkRefresh();
    return userMap;
  }

  /**
   * This method will be called by CacheHelper and is synchronized.
   */
  @Override
  protected void refresh()
  {
    log.info("Initializing UserCache ...");
    // This method must not be synchronized because it works with a new copy of maps.
    final Map<Integer, PFUserDO> uMap = new HashMap<Integer, PFUserDO>();
    // Could not autowire UserDao because of cyclic reference with AccessChecker.
    final List<PFUserDO> users = userService.getAllUsers();
    for (final PFUserDO user : users) {
      final PFUserDO copiedUser = PFUserDO.createCopyWithoutSecretFields(user);
      boolean withsecret = copiedUser.hasSecretFieldValues();
      uMap.put(user.getId(), copiedUser);
    }
    this.userMap = uMap;
    log.info("Initializing of UserCache done.");
  }
}
