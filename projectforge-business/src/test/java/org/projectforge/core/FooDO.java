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

package org.projectforge.core;

import java.util.Collection;

import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;


public class FooDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 2439234268569121526L;

  private boolean testBoolean;
  
  private String testString;
  
  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  private Collection<BarDO> managedChilds;

  private Collection<BarDO> unmanagedChilds1;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = false)
  private Collection<BarDO> unmanagedChilds2;

  public boolean isTestBoolean()
  {
    return testBoolean;
  }

  public void setTestBoolean(boolean testBoolean)
  {
    this.testBoolean = testBoolean;
  }

  public String getTestString()
  {
    return testString;
  }

  public void setTestString(String testString)
  {
    this.testString = testString;
  }

  public Collection<BarDO> getManagedChilds()
  {
    return managedChilds;
  }

  public void setManagedChilds(Collection<BarDO> managedChilds)
  {
    this.managedChilds = managedChilds;
  }

  public Collection<BarDO> getUnmanagedChilds1()
  {
    return unmanagedChilds1;
  }

  public void setUnmanagedChilds1(Collection<BarDO> unmanagedChilds1)
  {
    this.unmanagedChilds1 = unmanagedChilds1;
  }

  public Collection<BarDO> getUnmanagedChilds2()
  {
    return unmanagedChilds2;
  }

  public void setUnmanagedChilds2(Collection<BarDO> unmanagedChilds2)
  {
    this.unmanagedChilds2 = unmanagedChilds2;
  }
}
