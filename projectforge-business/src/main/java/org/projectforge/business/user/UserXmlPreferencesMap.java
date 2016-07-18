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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * User preferences contains a Map used by UserXmlPreferencesCache for storing user data application wide. Also
 * persistent user preferences in the database are supported.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XStreamAlias("userPreferences")
public class UserXmlPreferencesMap
{
  @XStreamOmitField
  private Integer userId;

  @XStreamOmitField
  private transient Set<String> modifiedObjects;

  private Map<String, Object> persistentData;

  @XStreamOmitField
  private transient Map<String, Object> volatileData;

  protected Map<String, Object> getPersistentData()
  {
    synchronized (this) {
      if (persistentData == null) {
        persistentData = new HashMap<String, Object>();
      }
    }
    return persistentData;
  }

  protected Map<String, Object> getVolatileData()
  {
    synchronized (this) {
      if (volatileData == null) {
        volatileData = new HashMap<String, Object>();
      }
    }
    return volatileData;
  }

  protected Set<String> getModifiedObjects()
  {
    synchronized (this) {
      if (modifiedObjects == null) {
        modifiedObjects = new HashSet<String>();
      }
    }
    return modifiedObjects;
  }

  /**
   * @param key
   * @param value
   * @param persistent If true, the object will be persisted in the database.
   */
  public void putEntry(String key, Object value, boolean persistent)
  {
    if (persistent == true) {
      setModified(key, true);
      getPersistentData().put(key, value);
    } else {
      getVolatileData().put(key, value);
    }
  }

  /**
   * Gets the stored user preference entry.
   * 
   * @param key
   * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
   *         existing, otherwise null;
   */
  public Object getEntry(String key)
  {
    Object value = getPersistentData().get(key);
    if (value != null) {
      // Assuming modification after use-age:
      setModified(key, true);
      return value;
    }
    return getVolatileData().get(key);
  }

  /**
   * Removes the entry from persistent and volatile storage if exist. Does not remove the entry from the data base!
   * 
   * @param key
   * @return the removed value if found.
   */
  public Object removeEntry(String key)
  {
    Object value = getPersistentData().remove(key);
    if (value == null) {
      value = getVolatileData().remove(key);
    } else {
      getVolatileData().remove(key);
    }
    return value;
  }

  public Integer getUserId()
  {
    return userId;
  }

  public void setUserId(Integer userId)
  {
    this.userId = userId;
  }

  public boolean isModified()
  {
    return getModifiedObjects().isEmpty() == false;
  }

  protected void setModified(String key, boolean isModified)
  {
    if (isModified == true) {
      getModifiedObjects().add(key);
    } else {
      getModifiedObjects().remove(key);
    }
  }

  protected boolean isModified(String key)
  {
    return getModifiedObjects().contains(key);
  }

  /**
   * Clear all volatile data (after logout). Forces refreshing of volatile data after re-login.
   */
  public void clear()
  {
    if (volatileData != null) {
      volatileData.clear();
    }
  }
}
