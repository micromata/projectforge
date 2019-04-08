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

package org.projectforge.business.user.service;

import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.CloneHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class UserPreferencesServiceImpl implements UserPreferencesService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserPreferencesServiceImpl.class);

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private UserXmlPreferencesCache userXmlPreferencesCache;

  /**
   * Stores the given value for the current user.
   *
   * @param key
   * @param value
   * @param persistent If true, the object will be persisted in the database.
   * @see UserXmlPreferencesCache#putEntry(Integer, String, Object, boolean)
   */
  @Override
  public void putEntry(final String key, final Object value, final boolean persistent) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null || value == null) {
      // Should only occur, if user is not logged in.
      return;
    }
    if (AccessChecker.isDemoUser(user) == true && value instanceof Serializable) {
      // Store user pref for demo user only in user's session.
      // Do nothing for demo user: MySession.get().setAttribute(key, (Serializable) value);
      return;
    }
    try {
      userXmlPreferencesCache.putEntry(user.getId(), key, value, persistent);
    } catch (final Exception ex) {
      log.error("Should only occur in maintenance mode: " + ex.getMessage(), ex);
    }
  }

  /**
   * Gets the stored user preference entry.
   *
   * @param key
   * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
   * existing, otherwise null;
   * @see UserXmlPreferencesCache#getEntry(Integer, String)
   */
  @Override
  public Object getEntry(final String key) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null) {
      // Should only occur, if user is not logged in.
      return null;
    }
    final Integer userId = user.getId();
    try {
      return userXmlPreferencesCache.getEntry(userId, key);
    } catch (final Exception ex) {
      log.error("Should only occur in maintenance mode: " + ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Gets the stored user preference entry.
   *
   * @param key
   * @param expectedType Checks the type of the user pref entry (if found) and returns only this object if the object is
   *                     from the expected type, otherwise null is returned.
   * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
   * existing, otherwise null;
   * @see UserXmlPreferencesCache#getEntry(Integer, String)
   */
  @Override
  public <T> T getEntry(Class<T> expectedType, String key) {
    final Object entry = getEntry(key);
    if (entry == null) {
      return null;
    }
    if (expectedType.isAssignableFrom(entry.getClass()) == true) {
      return (T)entry;
    }
    // Probably a new software release results in an incompability of old and new object format.
    log.info("Could not get user preference entry: (old) type "
            + entry.getClass().getName()
            + " is not assignable to (new) required type "
            + expectedType.getName()
            + " (OK, probably new software release).");
    return null;
  }

  /**
   * Removes the entry under the given key.
   *
   * @param key
   * @return The removed entry if found.
   */
  @Override
  public Object removeEntry(final String key) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null) {
      // Should only occur, if user is not logged in.
      return null;
    }
    if (AccessChecker.isDemoUser(user) == true) {
      return key; // Dummy return
    }
    return userXmlPreferencesCache.removeEntry(user.getId(), key);
  }
}
