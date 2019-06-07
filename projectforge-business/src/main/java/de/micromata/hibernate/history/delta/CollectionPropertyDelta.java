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

package de.micromata.hibernate.history.delta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;

/**
 * Legacy used for XML persistence of DB.
 * 
 * @author Wolfgang Jung (w.jung@micromata.de)
 * 
 */
public class CollectionPropertyDelta extends PropertyDelta
{

  private transient Set<Object> additions = new HashSet<Object>();

  private transient Set<Object> removals = new HashSet<Object>();

  protected CollectionPropertyDelta()
  {
    // do nothing
  }

  public CollectionPropertyDelta(String propertyName, Class<?> propertyType, Collection<Object> oldValue,
      Collection<Object> newValue)
  {
    this.propertyName = propertyName;
    this.propertyType = ClassUtils.getShortClassName(propertyType);
    calculateAdditionsAndRemovals(oldValue, newValue);
    this.oldValue = StringUtils.join(removals.iterator(), ",");
    this.newValue = StringUtils.join(additions.iterator(), ",");
  }

  private void calculateAdditionsAndRemovals(Collection<Object> oldValue, Collection<Object> newValue)
  {
    // //////////////////////////////////////////////////////////////////////
    // First, determine additions
    if (newValue != null) {
      additions.addAll(newValue);
    }
    if (oldValue != null) {
      additions.removeAll(oldValue);
    }

    // //////////////////////////////////////////////////////////////////////
    // Then, determine removals
    if (oldValue != null) {
      removals.addAll(oldValue);
    }
    if (newValue != null) {
      removals.removeAll(newValue);
    }
  }

  @Override
  public String toString()
  {
    return "changes of " + propertyName + " new=" + newValue + " old=" + oldValue;
  }

  @Override
  public Object getNewObjectValue(final Session session)
  {
    return splitElements(getNewValue(), session);
  }

  private List<Object> splitElements(final String keyList, final Session session)
  {
    List<Object> entityList = new ArrayList<Object>();
    if (StringUtils.isEmpty(keyList) == false) {
      for (String key : keyList.split(",")) {
        if (StringUtils.isEmpty(key)) {
          continue;
        }
        entityList.add(loadItem(propertyType, key, session));
      }
    }
    return entityList;
  }

  @Override
  public Object getOldObjectValue(final Session session)
  {
    return splitElements(getOldValue(), session);
  }
}
