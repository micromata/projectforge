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

package org.projectforge.core;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;


public class BarDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 2366517053064935738L;

  private int number;
  
  private String testString;

  public BarDO()
  {
    number = -1;
  }

  public BarDO(int number, String testString)
  {
    this.number = number;
    this.testString = testString;
  }

  public String getTestString()
  {
    return testString;
  }

  public void setTestString(String testString)
  {
    this.testString = testString;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof BarDO) {
      BarDO other = (BarDO) o;
      return number == other.number;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(number);
    return hcb.toHashCode();
  }
}
