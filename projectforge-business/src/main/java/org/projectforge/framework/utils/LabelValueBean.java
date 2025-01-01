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

package org.projectforge.framework.utils;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * Simply an holder for a key value property.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LabelValueBean<L extends Comparable<L>, V> implements Comparable<LabelValueBean<L, V>>, ILabelValueBean<L, V>, Serializable
{
  private static final long serialVersionUID = -5085397483556387710L;

  protected V value = null;

  protected L label = null;

  public LabelValueBean()
  {
  }

  public LabelValueBean(final V value)
  {
    this.value = value;
  }

  public LabelValueBean(final L label, final V value)
  {
    this.label = label;
    this.value = value;
  }

  @Override
  public V getValue()
  {
    return this.value;
  }

  public void setValue(V value)
  {
    this.value = value;
  }

  @Override
  public L getLabel()
  {
    return this.label;
  }
  
  public String toString() {
    ToStringBuilder sb = new ToStringBuilder(this);
    sb.append("key", this.value);
    sb.append("value", this.label);
    return sb.toString();
  }
  
  /*
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(LabelValueBean<L, V> o)
  {
    return this.label.compareTo(o.label);
  }
}
