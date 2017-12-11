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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
@DependsOn("entityManagerFactory")
public class UserXmlPreferencesCache extends AbstractCache
{
  private static final long serialVersionUID = 248972660689793455L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserXmlPreferencesCache.class);

  private final Map<Integer, UserXmlPreferencesMap> allPreferences = new HashMap<Integer, UserXmlPreferencesMap>();

  @Autowired
  private UserXmlPreferencesDao userXmlPreferencesDao;

  @Autowired
  private PfEmgrFactory emgrFactory;

  /**
   * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
   *
   * @see org.projectforge.business.user.UserXmlPreferencesMap#putEntry(String, Object, boolean)
   */
  public void putEntry(final Integer userId, final String key, final Object value, final boolean persistent)
  {
    final UserXmlPreferencesMap data = ensureAndGetUserPreferencesData(userId);
    data.putEntry(key, value, persistent);
    checkRefresh(); // Should be called at the end of this method for considering changes inside this method.
  }

  /**
   * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
   *
   * @see #ensureAndGetUserPreferencesData(Integer)
   */
  public Object getEntry(final Integer userId, final String key)
  {
    final UserXmlPreferencesMap data = ensureAndGetUserPreferencesData(userId);
    checkRefresh();
    return data.getEntry(key);
  }

  /**
   * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
   *
   * @see org.projectforge.business.user.UserXmlPreferencesMap#removeEntry(String)
   */
  public Object removeEntry(final Integer userId, final String key)
  {
    final UserXmlPreferencesMap data = getUserPreferencesData(userId);
    if (data == null) {
      // Should only occur for the pseudo-first-login-user setting up the system.
      return null;
    }
    if (data.getPersistentData().containsKey(key) == true) {
      userXmlPreferencesDao.remove(userId, key);
    } else if (data.getVolatileData().containsKey(key) == false) {
      log.warn("Oups, user preferences object with key '" + key + "' is wether persistent nor volatile!");
    }
    checkRefresh();
    return data.removeEntry(key);
  }

  /**
   * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
   *
   * @param userId
   * @return
   */
  public synchronized UserXmlPreferencesMap ensureAndGetUserPreferencesData(final Integer userId)
  {
    UserXmlPreferencesMap data = getUserPreferencesData(userId);
    if (data == null) {
      data = new UserXmlPreferencesMap();
      data.setUserId(userId);
      final List<UserXmlPreferencesDO> userPrefs = userXmlPreferencesDao.getUserPreferencesByUserId(userId);
      for (final UserXmlPreferencesDO userPref : userPrefs) {
        final Object value = userXmlPreferencesDao.deserialize(userId, userPref, true);
        data.putEntry(userPref.getKey(), value, true);
      }
      this.allPreferences.put(userId, data);
    }
    return data;
  }

  UserXmlPreferencesMap getUserPreferencesData(final Integer userId)
  {
    return this.allPreferences.get(userId);
  }

  void setUserPreferencesData(final Integer userId, final UserXmlPreferencesMap data)
  {
    this.allPreferences.put(userId, data);
  }

  /**
   * Flushes the user settings to the database (independent from the expire mechanism). Should be used after the user's
   * logout. If the user data isn't modified, then nothing will be done.
   */
  public void flushToDB(final Integer userId)
  {
    flushToDB(userId, true);
  }

  private synchronized void flushToDB(final Integer userId, final boolean checkAccess)
  {
    if (checkAccess == true) {
      if (userId.equals(ThreadLocalUserContext.getUserId()) == false) {
        log.error("User '" + ThreadLocalUserContext.getUserId()
            + "' has no access to write user preferences of other user '" + userId + "'.");
        // No access.
        return;
      }
    }
    PFUserDO user = emgrFactory.runInTrans(emgr -> {
      return emgr.selectByPk(PFUserDO.class, userId);
    });
    if (AccessChecker.isDemoUser(user) == true) {
      // Do nothing for demo user.
      return;
    }
    final UserXmlPreferencesMap data = allPreferences.get(userId);
    if (data == null || data.isModified() == false) {
      return;
    }
    userXmlPreferencesDao.saveOrUpdateUserEntries(userId, data, checkAccess);
  }

  /**
   * Stores the PersistentUserObjects in the database or on start up restores the persistent user objects from the
   * database.
   *
   * @see org.projectforge.framework.cache.AbstractCache#refresh()
   */
  @Override
  protected void refresh()
  {
    log.info("Flushing all user preferences to data-base....");
    for (final Integer userId : allPreferences.keySet()) {
      flushToDB(userId, false);
    }
    log.info("Flushing of user preferences to data-base done.");
  }

  /**
   * Clear all volatile data (after logout). Forces refreshing of volatile data after re-login.
   *
   * @param userId
   */
  public void clear(final Integer userId)
  {
    final UserXmlPreferencesMap data = allPreferences.get(userId);
    if (data == null) {
      return;
    }
    data.clear();
  }

  @Override
  public void setExpireTimeInMinutes(final long expireTime)
  {
    this.expireTime = 10 * TICKS_PER_MINUTE;
  }

  @PreDestroy
  public void preDestroy()
  {
    log.info("Syncing all user preferences to database.");
    this.forceReload();
  }
}
