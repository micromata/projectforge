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

package org.projectforge.business.user.service;

import org.projectforge.business.user.UserPrefCache;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Uses {@link UserPrefCache}.
 */
@Service
public class UserPrefService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserPrefService.class);

  @Autowired
  private UserPrefCache userPrefCache;

  /**
   * Needed for migrations.
   */
  @SuppressWarnings("deprecation")
  @Autowired
  private UserXmlPreferencesService userXmlPreferencesService;

  /**
   * Should only be used for migration issues.
   */
  public UserXmlPreferencesService getUserXmlPreferencesService() {
    return userXmlPreferencesService;
  }

  /**
   * Stores the given value for the current user as persistent value.
   *
   * @param name
   * @param value
   */
  public void putEntry(final String area, final String name, final Object value) {
    putEntry(area, name, value, true);
  }

  /**
   * Stores the given value for the current user.
   *
   * @param name
   * @param value
   * @param persistent If true, the object will be persisted in the database.
   */
  public void putEntry(final String area, final String name, final Object value, final boolean persistent) {
    putEntry(area, name, value, persistent, null);
  }

  /**
   * Stores the given value for the current user.
   *
   * @param name
   * @param value
   * @param persistent If true, the object will be persisted in the database.
   * @param userId     Optional userId. If not given, {@link ThreadLocalUserContext#getLoggedInUserId()} is used.
   */
  public void putEntry(final String area, final String name, final Object value, final boolean persistent, Long userId) {
    userPrefCache.putEntry(area, name, value, persistent, userId);
  }

  /**
   * Gets the stored user preference entry.
   *
   * @param area
   * @param name
   * @param expectedType Checks the type of the user pref entry (if found) and returns only this object if the object is
   *                     from the expected type, otherwise null is returned.
   * @return Return a persistent object with this name, if existing, or if not a volatile object with this name, if
   * existing, otherwise null;
   */
  public <T> T getEntry(String area, String name, Class<T> expectedType) {
    return getEntry(area, name, expectedType, null);
  }

  /**
   * Gets the stored user preference entry.
   *
   * @param area
   * @param name
   * @param expectedType Checks the type of the user pref entry (if found) and returns only this object if the object is
   *                     from the expected type, otherwise null is returned.
   * @param userId       User id to user. If null, ThreadLocalUserContext.getUserId() is used.
   * @return Return a persistent object with this name, if existing, or if not a volatile object with this name, if
   * existing, otherwise null;
   */
  public <T> T getEntry(String area, String name, Class<T> expectedType, Long userId) {
    return userPrefCache.getEntry(area, name, expectedType, userId);
  }


  public Object getEntry(String area, String name) {
    return userPrefCache.getEntry(area, name);
  }

  public <T> T ensureEntry(String area, String name, T defaultValue) {
    return ensureEntry(area, name, defaultValue, true);
  }

  /**
   * Gets the entry if exist, if not, defaultValue will be returned an the default entry will be stored.
   *
   * @param area
   * @param name
   * @param defaultValue
   * @param persistent
   * @return
   */
  public <T> T ensureEntry(String area, String name, T defaultValue, boolean persistent) {
    T value = (T) getEntry(area, name, defaultValue.getClass());
    if (value == null) {
      value = defaultValue;
      putEntry(area, name, value, persistent);
    }
    return value;
  }

  /**
   * Gets the entry if exist, if not, defaultValue will be returned an the default entry will be stored.
   *
   * @param area
   * @param name
   * @param defaultValue
   * @param persistent
   * @param userId       User to use. Uses ThreadLocalUserContext.getUserId() if null.
   * @return
   */
  public <T> T ensureEntry(String area, String name, T defaultValue, boolean persistent, Long userId) {
    T value = (T) getEntry(area, name, defaultValue.getClass(), userId);
    if (value == null) {
      value = defaultValue;
      putEntry(area, name, value, persistent, userId);
    }
    return value;
  }

  /**
   * Removes the entry under the given name.
   *
   * @param name
   * @return The removed entry if found.
   */
  public void removeEntry(final String area, final String name) {
    userPrefCache.removeEntry(area, name);
  }
}
