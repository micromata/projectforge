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
  private Collection<BarDO> managedChildren;

  private Collection<BarDO> unmanagedChildren1;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = false)
  private Collection<BarDO> unmanagedChildren2;

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

  public Collection<BarDO> getManagedChildren()
  {
    return managedChildren;
  }

  public void setManagedChildren(Collection<BarDO> managedChildren)
  {
    this.managedChildren = managedChildren;
  }

  public Collection<BarDO> getUnmanagedChildren1()
  {
    return unmanagedChildren1;
  }

  public void setUnmanagedChildren1(Collection<BarDO> unmanagedChildren1)
  {
    this.unmanagedChildren1 = unmanagedChildren1;
  }

  public Collection<BarDO> getUnmanagedChildren2()
  {
    return unmanagedChildren2;
  }

  public void setUnmanagedChildren2(Collection<BarDO> unmanagedChildren2)
  {
    this.unmanagedChildren2 = unmanagedChildren2;
  }
}
