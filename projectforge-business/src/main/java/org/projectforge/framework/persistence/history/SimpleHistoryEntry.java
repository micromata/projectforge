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

package org.projectforge.framework.persistence.history;

import java.util.Date;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;

import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.genome.db.jpa.history.entities.EntityOpType;
import de.micromata.hibernate.history.delta.PropertyDelta;

/**
 * For storing the hibernate history entries in flat format.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class SimpleHistoryEntry
{
  private PFUserDO user;

  private final EntityOpType entryType;

  private String propertyName;

  private String propertyType;

  private String oldValue;

  private String newValue;

  private final Date timestamp;

  public SimpleHistoryEntry(final UserGroupCache userGroupCache, final HistoryEntry entry)
  {
    this.timestamp = entry.getModifiedAt();
    final Integer userId = NumberHelper.parseInteger(entry.getUserName());
    if (userId != null) {
      this.user = userGroupCache.getUser(userId);
    }
    // entry.getClassName();
    // entry.getComment();
    this.entryType = entry.getEntityOpType();
    // entry.getEntityId();
  }

  public SimpleHistoryEntry(final UserGroupCache userGroupCache, final HistoryEntry entry, final PropertyDelta prop)
  {
    this(userGroupCache, entry);
    this.propertyType = prop.getPropertyType();
    this.oldValue = prop.getOldValue();
    this.newValue = prop.getNewValue();
    this.propertyName = prop.getPropertyName();
  }

  /**
   * @return the entryType
   */
  public EntityOpType getEntryType()
  {
    return entryType;
  }

  /**
   * @return the newValue
   */
  public String getNewValue()
  {
    return newValue;
  }

  /**
   * @return the oldValue
   */
  public String getOldValue()
  {
    return oldValue;
  }

  /**
   * @return the propertyName
   */
  public String getPropertyName()
  {
    return propertyName;
  }

  /**
   * @return the propertyType
   */
  public String getPropertyType()
  {
    return propertyType;
  }

  public PFUserDO getUser()
  {
    return user;
  }

  public Date getTimestamp()
  {
    return timestamp;
  }

  /**
   * Returns string containing all fields (except the password, via ReflectionToStringBuilder).
   * 
   * @return
   */
  @Override
  public String toString()
  {
    return new ReflectionToStringBuilder(this).toString();
  }
}
