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

package org.projectforge.web.user;

import java.io.Serializable;

import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.CloneHelper;
import org.projectforge.web.session.MySession;

public class UserPreferencesHelper
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserPreferencesHelper.class);

  /**
   * Stores the given value for the current user.
   * 
   * @param key
   * @param value
   * @param persistent If true, the object will be persisted in the database.
   * @see UserXmlPreferencesCache#putEntry(Integer, String, Object, boolean)
   */
  public static void putEntry(final String key, final Object value, final boolean persistent)
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null || value == null) {
      // Should only occur, if user is not logged in.
      return;
    }
    if (AccessChecker.isDemoUser(user) == true && value instanceof Serializable) {
      // Store user pref for demo user only in user's session.
      MySession.get().setAttribute(key, (Serializable) value);
      return;
    }
    try {
      getUserXmlPreferencesCache().putEntry(user.getId(), key, value, persistent);
    } catch (final Exception ex) {
      log.error("Should only occur in maintenance mode: " + ex.getMessage(), ex);
    }
  }

  /**
   * Gets the stored user preference entry.
   * 
   * @param key
   * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
   *         existing, otherwise null;
   * @see UserXmlPreferencesCache#getEntry(Integer, String)
   */
  public static Object getEntry(final String key)
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null) {
      // Should only occur, if user is not logged in.
      return null;
    }
    final Integer userId = user.getId();
    if (AccessChecker.isDemoUser(user) == true) {
      // Store user pref for demo user only in user's session.
      Object value = MySession.get().getAttribute(key);
      if (value != null) {
        return value;
      }
      value = getUserXmlPreferencesCache().getEntry(userId, key);
      if (value == null || value instanceof Serializable == false) {
        return null;
      }
      value = CloneHelper.cloneBySerialization(value);
      MySession.get().setAttribute(key, (Serializable) value);
      return value;
    }
    try {
      return getUserXmlPreferencesCache().getEntry(userId, key);
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
   *          from the expected type, otherwise null is returned.
   * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
   *         existing, otherwise null;
   * @see UserXmlPreferencesCache#getEntry(Integer, String)
   */
  public static Object getEntry(final Class<?> expectedType, final String key)
  {
    final Object entry = getEntry(key);
    if (entry == null) {
      return null;
    }
    if (expectedType.isAssignableFrom(entry.getClass()) == true) {
      return entry;
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
  public static Object removeEntry(final String key)
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null) {
      // Should only occur, if user is not logged in.
      return null;
    }
    if (AccessChecker.isDemoUser(user) == true) {
      MySession.get().removeAttribute(key);
    }
    return getUserXmlPreferencesCache().removeEntry(user.getId(), key);
  }

  private static UserXmlPreferencesCache getUserXmlPreferencesCache()
  {
    return ApplicationContextProvider.getApplicationContext().getBean(UserXmlPreferencesCache.class);
  }
}
