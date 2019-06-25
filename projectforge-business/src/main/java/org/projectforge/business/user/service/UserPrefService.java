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

package org.projectforge.business.user.service;

import org.projectforge.business.user.UserPrefCache;
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
    userPrefCache.putEntry(area, name, value, persistent);
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
    return userPrefCache.getEntry(area, name, expectedType);
  }

  public Object getEntry(String area, String name) {
    return userPrefCache.getEntry(area, name);
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
