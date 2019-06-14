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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Wolfgang Jung (w.jung@micromata.de)
 * 
 */
public class DeltaSet implements java.io.Serializable
{
  /**
   * 
   */
  private static final long serialVersionUID = 3887433882538488773L;

  private List<PropertyDelta> deltas = new ArrayList<PropertyDelta>();

  private Class<?> entity;

  private Serializable id;

  private transient Set<String> deltaPropertyNames = new HashSet<String>();

  public void addDelta(PropertyDelta delta)
  {
    if (delta.anyChangeDetected()) {
      deltas.add(delta);
      deltaPropertyNames.add(delta.getPropertyName());
    }
  }

  public List<PropertyDelta> getDeltas()
  {
    return Collections.unmodifiableList(deltas);
  }

  public boolean wasDelta(String propertyName)
  {
    return deltaPropertyNames.contains(propertyName);
  }

  public Class<?> getEntity()
  {
    return entity;
  }

  public Serializable getId()
  {
    return id;
  }

  public void setEntity(Class<?> entity)
  {
    this.entity = entity;
  }

  public void setId(Serializable id)
  {
    this.id = id;
  }

  void clear()
  {
    deltas.clear();
  }

  @Override
  public String toString()
  {
    return "deltas: " + deltas;
  }
}
