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

package org.projectforge.business.multitenancy;

import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.kost.KostCache;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.context.ApplicationContext;

/**
 * Holds caches of a single tenant. After the configured time to live (TTL) this registry is detached from
 * {@link TenantRegistryMap}.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TenantRegistry
{
  /**
   * Default time to live for an holder.
   */
  private static final long TIME_TO_LIVE_MS = 60 * 60 * 1000;

  private long lastUsage;

  private final long timeToLive;

  private Configuration configuration;

  private ApplicationContext applicationContext;

  private TaskTree taskTree;

  private UserGroupCache userGroupCache;

  private final TenantDO tenant;

  private ConfigurationService configurationService;

  private AccessDao accessDao;

  private AuftragDao auftragDao;

  private ProjektDao projektDao;

  private TaskDao taskDao;

  private TimesheetDao timesheetDao;

  private KostCache kostCache;

  public TenantRegistry(final TenantDO tenant, ApplicationContext applicationContext)
  {
    this.tenant = tenant;
    this.lastUsage = System.currentTimeMillis();
    this.timeToLive = TIME_TO_LIVE_MS;

    this.applicationContext = applicationContext;
    this.configurationService = applicationContext.getBean(ConfigurationService.class);
    this.accessDao = applicationContext.getBean(AccessDao.class);
    this.auftragDao = applicationContext.getBean(AuftragDao.class);
    this.projektDao = applicationContext.getBean(ProjektDao.class);
    this.taskDao = applicationContext.getBean(TaskDao.class);
    this.timesheetDao = applicationContext.getBean(TimesheetDao.class);

    this.kostCache = applicationContext.getBean(KostCache.class);
  }

  /**
   * @return true if the last usage of this holder more ms in the past than the TTL.
   */
  public boolean isOutdated()
  {
    return System.currentTimeMillis() - lastUsage > 0;
  }

  /**
   * @return the configuration
   */
  public Configuration getConfiguration()
  {
    if (configuration == null) {
      configuration = new Configuration(configurationService, tenant);
    }
    return configuration;
  }

  /**
   * @return the taskTree
   */
  public TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = new TaskTree();
      taskTree.setTenant(tenant);
      taskTree.setAccessDao(accessDao);
      taskTree.setAuftragDao(auftragDao);
      taskTree.setKostCache(kostCache);
      taskTree.setProjektDao(projektDao);
      taskTree.setTaskDao(taskDao);
      taskTree.setTimesheetDao(timesheetDao);
      taskTree.forceReload();
    }
    updateUsageTime();
    return taskTree;
  }

  /**
   * @return the userGroupCache
   */
  public UserGroupCache getUserGroupCache()
  {
    if (userGroupCache == null) {
      userGroupCache = new UserGroupCache(tenant, applicationContext);
    }
    return userGroupCache;
  }

  /**
   * @return the tenant
   */
  public TenantDO getTenant()
  {
    return tenant;
  }

  private void updateUsageTime()
  {
    this.lastUsage = System.currentTimeMillis();
  }

  /**
   * @return the lastUsage time in ms.
   */
  public long getLastUsage()
  {
    return lastUsage;
  }

  /**
   * @return the timeToLive of this holder in ms.
   */
  public long getTimeToLive()
  {
    return timeToLive;
  }
}
