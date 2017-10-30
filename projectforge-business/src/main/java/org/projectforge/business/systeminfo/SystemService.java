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

package org.projectforge.business.systeminfo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.projectforge.AppVersion;
import org.projectforge.business.fibu.KontoCache;
import org.projectforge.business.fibu.RechnungCache;
import org.projectforge.business.fibu.kost.KostCache;
import org.projectforge.business.jsonRest.RestCallService;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.framework.persistence.database.SchemaExport;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.model.rest.VersionCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides some system routines.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Florian Blumenstein
 */
@Service
@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
public class SystemService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SystemService.class);

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private SystemInfoCache systemInfoCache;

  @Autowired
  RechnungCache rechnungCache;

  @Autowired
  KontoCache kontoCache;

  @Autowired
  KostCache kostCache;

  @Autowired
  private RestCallService restCallService;

  public VersionCheck getVersionCheckInformations()
  {
    VersionCheck versionCheck = new VersionCheck(AppVersion.VERSION.toString(), ThreadLocalUserContext.getLocale(), ThreadLocalUserContext.getTimeZone());
    String url = "http://localhost:8080/publicRest/versionCheck";
    versionCheck = restCallService.callRestInterfaceForUrl(url, HttpMethod.POST, VersionCheck.class, versionCheck);
    return versionCheck;
  }

  public String exportSchema()
  {
    final SchemaExport exp = new SchemaExport();
    File file;
    try {
      file = File.createTempFile("projectforge-schema", ".sql");
    } catch (final IOException ex) {
      log.error(ex.getMessage(), ex);
      return ex.getMessage();
    }
    exp.exportSchema(file.getPath());
    String result;
    try {
      result = FileUtils.readFileToString(file, "UTF-8");
    } catch (final IOException ex) {
      log.error(ex.getMessage(), ex);
      return ex.getMessage();
    }
    file.delete();
    return result;
  }

  /**
   * Search for abandoned tasks (task outside the task hierarchy, unaccessible and unavailable for the users).
   *
   * @return
   */
  public String checkSystemIntegrity()
  {
    final StringBuffer buf = new StringBuffer();
    buf.append("ProjectForge system integrity check.\n\n");
    buf.append("------------------------------------\n");
    buf.append("|                                  |\n");
    buf.append("| Task integrity (abandoned tasks) |\n");
    buf.append("|                                  |\n");
    buf.append("------------------------------------\n");
    final List<TaskDO> tasks = taskDao.internalLoadAll();
    buf.append("Found " + tasks.size() + " tasks.\n");
    final Map<Integer, TaskDO> taskMap = new HashMap<Integer, TaskDO>();
    for (final TaskDO task : tasks) {
      taskMap.put(task.getId(), task);
    }
    boolean rootTask = false;
    boolean abandonedTasks = false;
    for (final TaskDO task : tasks) {
      if (task.getParentTask() == null) {
        if (rootTask == true) {
          buf.append("\n*** Error: Found another root task:\n " + task + "\n");
        } else {
          buf.append("\nFound root task:\n " + task + "\n");
          rootTask = true;
        }
      } else {
        TaskDO ancestor = taskMap.get(task.getParentTaskId());
        boolean rootTaskFound = false;
        for (int i = 0; i < 50; i++) { // Max. depth of 50, otherwise cyclic task!
          if (ancestor == null) {
            break;
          }
          if (ancestor.getParentTaskId() == null) {
            // Root task found, OK.
            rootTaskFound = true;
            break;
          }
          ancestor = taskMap.get(ancestor.getParentTaskId());
        }
        if (rootTaskFound == false) {
          buf.append("\n*** Error: Found abandoned task (cyclic tasks without path to root):\n " + task + "\n");
          abandonedTasks = true;
        } else {
          buf.append('.');
        }
      }
      taskMap.put(task.getId(), task);
    }
    if (abandonedTasks == false) {
      buf.append("\n\nTest OK, no abandoned tasks detected.");
    } else {
      buf.append("\n\n*** Test FAILED, abandoned tasks detected.");
    }
    return buf.toString();
  }

  /**
   * Refreshes the caches: TaskTree, userGroupCache and kost2.
   *
   * @return the name of the refreshed caches.
   */
  public String refreshCaches()
  {
    final TenantRegistry tenantRegistry = TenantRegistryMap.getInstance().getTenantRegistry();
    tenantRegistry.getUserGroupCache().forceReload();
    tenantRegistry.getTaskTree().forceReload();
    kontoCache.forceReload();
    kostCache.forceReload();
    rechnungCache.forceReload();
    systemInfoCache.forceReload();
    return "UserGroupCache, TaskTree, KontoCache, KostCache, RechnungCache, SystemInfoCache";
  }

}
