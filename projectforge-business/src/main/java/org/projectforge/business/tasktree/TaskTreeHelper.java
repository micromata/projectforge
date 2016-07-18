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
