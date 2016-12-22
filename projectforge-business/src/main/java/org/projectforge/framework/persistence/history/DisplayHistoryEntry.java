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

package org.projectforge.framework.persistence.history;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.hibernate.Session;
import org.jfree.util.Log;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberHelper;

import de.micromata.genome.db.jpa.history.api.DiffEntry;
import de.micromata.genome.db.jpa.history.api.HistProp;
import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.genome.db.jpa.history.entities.EntityOpType;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * For storing the hibernate history entries in flat format.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Roger Kommer, Florian Blumenstein
 */
public class DisplayHistoryEntry implements Serializable
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DisplayHistoryEntry.class);

  private static final long serialVersionUID = 3900345445639438747L;

  private PFUserDO user;

  private final EntityOpType entryType;

  private String propertyName;

  private String propertyType;

  private String oldValue;

  private String newValue;

  private final Date timestamp;

  public DisplayHistoryEntry(final UserGroupCache userGroupCache, final HistoryEntry entry)
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

  private PFUserDO getUser(final UserGroupCache userGroupCache, final String userId)
  {
    if (StringUtils.isBlank(userId) == true) {
      return null;
    }
    final Integer id = NumberHelper.parseInteger(userId);
    if (id == null) {
      return null;
    }
    return userGroupCache.getUser(id);
  }

  public DisplayHistoryEntry(final UserGroupCache userGroupCache, final HistoryEntry entry, final DiffEntry prop,
      final Session session)
  {
    this(userGroupCache, entry);
    if (prop.getNewProp() != null) {
      this.propertyType = prop.getNewProp().getType();
    }
    if (prop.getOldProp() != null) {
      this.propertyType = prop.getOldProp().getType();
    }
    this.newValue = prop.getNewValue();
    this.oldValue = prop.getOldValue();
    Object oldObjectValue = null;
    Object newObjectValue = null;

    try {
      oldObjectValue = getObjectValue(userGroupCache, session, prop.getOldProp());
    } catch (final Exception ex) {
      oldObjectValue = "???";
      log.warn("Error while try to parse old object value '"
          + prop.getOldValue()
          + "' of prop-type '"
          + prop.getClass().getName()
          + "': "
          + ex.getMessage(), ex);
    }

    try {
      newObjectValue = getObjectValue(userGroupCache, session, prop.getNewProp());
    } catch (final Exception ex) {
      newObjectValue = "???";
      log.warn("Error while try to parse new object value '"
          + prop.getNewValue()
          + "' of prop-type '"
          + prop.getClass().getName()
          + "': "
          + ex.getMessage(), ex);
    }

    if (oldObjectValue != null) {
      this.oldValue = objectValueToDisplay(oldObjectValue);
    }
    if (newObjectValue != null) {
      this.newValue = objectValueToDisplay(newObjectValue);
    }

    this.propertyName = prop.getPropertyName();
  }

  private String objectValueToDisplay(Object value)
  {
    if (value instanceof Date || value instanceof java.sql.Date || value instanceof Timestamp) {
      return formatDate(value);
    }

    return String.valueOf(toShortNameOfList(value));
  }

  private Object getObjectValue(UserGroupCache userGroupCache, Session session, HistProp prop)
  {
    if (prop == null) {
      return null;
    }
    if (StringUtils.isBlank(prop.getValue()) == true) {
      return prop.getValue();
    }
    String type = prop.getType();
    if (String.class.getName().equals(type) == true) {
      return prop.getValue();
    }
    if (PFUserDO.class.getName().equals(type) == true) {
      PFUserDO user = getUser(userGroupCache, prop.getValue());
      if (user != null) {
        return user;
      }
    }
    if (EmployeeDO.class.getName().equals(type) == true) {
      StringBuffer sb = new StringBuffer();
      getDBObjects(session, prop).forEach(emp -> {
        if (emp instanceof EmployeeDO) {
          EmployeeDO employee = (EmployeeDO) emp;
          sb.append(employee.getUser().getFullname() + ";");
        }
      });
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
    }

    return getDBObjects(session, prop);
  }

  private List<Object> getDBObjects(Session session, HistProp prop)
  {
    List<Object> ret = new ArrayList<>();
    EntityMetadata emd = PfEmgrFactory.get().getMetadataRepository().findEntityMetadata(prop.getType());
    if (emd == null) {
      ret.add(prop.getValue());
      return ret;
    }
    String[] sa = StringUtils.split(prop.getValue(), ", ");
    if (sa == null || sa.length == 0) {
      return Collections.emptyList();
    }
    for (String pks : sa) {
      try {
        int pk = Integer.parseInt(pks);
        Object ent = session.get(emd.getJavaType(), pk);
        if (ent != null) {
          ret.add(ent);
        }
      } catch (NumberFormatException ex) {
        Log.warn("Cannot parse pk: " + prop);
      }
    }
    return ret;
  }

  private String formatDate(final Object objectValue)
  {
    if (objectValue == null) {
      return "";
    }
    if (objectValue instanceof java.sql.Date) {
      return DateHelper.formatIsoDate((Date) objectValue);
    } else if (objectValue instanceof Date) {
      return DateHelper.formatIsoTimestamp((Date) objectValue);
    }
    return String.valueOf(objectValue);
  }

  private Object toShortNameOfList(final Object value)
  {
    if (value instanceof Collection<?>) {
      return CollectionUtils.collect((Collection<?>) value, new Transformer()
      {
        @Override
        public Object transform(final Object input)
        {
          return toShortName(input);
        }
      });
    }

    return toShortName(value);
  }

  String toShortName(final Object object)
  {
    return String.valueOf(
        object instanceof ShortDisplayNameCapable ? ((ShortDisplayNameCapable) object).getShortDisplayName() : object);
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
   * @param newValue the newValue to set
   * @return this for chaining.
   */
  public void setNewValue(final String newValue)
  {
    this.newValue = newValue;
  }

  /**
   * @return the oldValue
   */
  public String getOldValue()
  {
    return oldValue;
  }

  /**
   * @param oldValue the oldValue to set
   * @return this for chaining.
   */
  public void setOldValue(final String oldValue)
  {
    this.oldValue = oldValue;
  }

  /**
   * @return the propertyName
   */
  public String getPropertyName()
  {
    return propertyName;
  }

  /**
   * Use-full for prepending id of childs (e. g. entries in a collection displayed in the history table of the parent
   * object). Example: AuftragDO -> AuftragsPositionDO.
   *
   * @param propertyName
   */
  public void setPropertyName(final String propertyName)
  {
    this.propertyName = propertyName;
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
