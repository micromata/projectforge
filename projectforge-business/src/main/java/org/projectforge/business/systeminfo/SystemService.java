/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.io.FileUtils;
import org.projectforge.business.address.BirthdayCache;
import org.projectforge.business.fibu.AuftragsCache;
import org.projectforge.business.fibu.KontoCache;
import org.projectforge.business.fibu.RechnungCache;
import org.projectforge.business.fibu.kost.KostCache;
import org.projectforge.business.jsonRest.RestCallService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.database.SchemaExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides some system routines.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Florian Blumenstein
 */
@Service
public class SystemService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SystemService.class);

  @Autowired
  private UserGroupCache userGroupCache;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private TaskTree taskTree;

  @Autowired
  private SystemInfoCache systemInfoCache;

  @Autowired
  private AuftragsCache auftragsCache;

  @Autowired
  private RechnungCache rechnungCache;

  @Autowired
  private KontoCache kontoCache;

  @Autowired
  private KostCache kostCache;

  @Autowired
  private RestCallService restCallService;

  public String exportSchema() {
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
  public String checkSystemIntegrity() {
    final StringBuilder buf = new StringBuilder();
    buf.append("ProjectForge system integrity check.\n\n");
    buf.append("------------------------------------\n");
    buf.append("|                                  |\n");
    buf.append("| Task integrity (abandoned tasks) |\n");
    buf.append("|                                  |\n");
    buf.append("------------------------------------\n");
    final List<TaskDO> tasks = taskDao.selectAll(false);
    buf.append("Found " + tasks.size() + " tasks.\n");
    final Map<Long, TaskDO> taskMap = new HashMap<>();
    for (final TaskDO task : tasks) {
      taskMap.put(task.getId(), task);
    }
    boolean rootTask = false;
    boolean abandonedTasks = false;
    for (final TaskDO task : tasks) {
      if (task.getParentTask() == null) {
        if (rootTask) {
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
        if (!rootTaskFound) {
          buf.append("\n*** Error: Found abandoned task (cyclic tasks without path to root):\n " + task + "\n");
          abandonedTasks = true;
        } else {
          buf.append('.');
        }
      }
      taskMap.put(task.getId(), task);
    }
    if (!abandonedTasks) {
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
  public String refreshCaches() {
    userGroupCache.forceReload();
    taskTree.forceReload();
    kontoCache.forceReload();
    kostCache.forceReload();
    rechnungCache.forceReload();
    auftragsCache.forceReload();
    systemInfoCache.forceReload();
    BirthdayCache.getInstance().forceReload();
    return "UserGroupCache, TaskTree, KontoCache, KostCache, RechnungCache, AuftragsCache, SystemInfoCache, BirthdayCache";
  }
}
