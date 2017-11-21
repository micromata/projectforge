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

package org.projectforge.framework.utils;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Simply an holder for a key value property.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class KeyValueBean<KeyType, ValueType extends Comparable<ValueType>> implements Comparable<KeyValueBean<KeyType, ValueType>>, Serializable
{
  private static final long serialVersionUID = 413658277016185385L;

  protected KeyType key = null;

  protected ValueType value = null;

  public KeyValueBean()
  {
  }

  public KeyValueBean(final KeyType key)
  {
    this.key = key;
  }

  public KeyValueBean(final KeyType key, final ValueType value)
  {
    this.key = key;
    this.value = value;
  }

  public KeyType getKey()
  {
    return this.key;
  }

  public ValueType getValue()
  {
    return this.value;
  }
  
  public String toString() {
    ToStringBuilder sb = new ToStringBuilder(this);
    sb.append("key", this.key);
    sb.append("value", this.value);
    return sb.toString();
  }
  
  /*
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(KeyValueBean<KeyType, ValueType> o)
  {
    return this.value.compareTo(o.value);
  }
}
