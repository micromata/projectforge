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

package org.projectforge.business.tasktree;

import java.io.Serializable;

import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.task.TaskTree;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.stereotype.Service;

public class TaskTreeHelper implements Serializable
{
  private static final long serialVersionUID = 7331067728377855594L;

  public static TaskTree getTaskTree()
  {
    return getTaskTree(null);
  }

  public static TaskTree getTaskTree(final TenantDO tenant)
  {
    final TenantRegistry tenantRegistry = TenantRegistryMap.getInstance().getTenantRegistry(tenant);
    return tenantRegistry.getTaskTree();
  }

  public static TaskTree getTaskTree(final BaseDO<?> obj)
  {
    return getTaskTree(obj.getTenant());
  }
}
