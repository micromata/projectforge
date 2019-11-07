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

package org.projectforge.business.systeminfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * Provides some system routines.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Florian Blumenstein
 */
@Service
public class SystemService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SystemService.class);

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private SystemInfoCache systemInfoCache;

  @Autowired
  private RechnungCache rechnungCache;

  @Autowired
  private KontoCache kontoCache;

  @Autowired
  private KostCache kostCache;

  @Value("${projectforge.versioncheck.enable:true}")
  private boolean enableVersionCheck;

  private LocalDate lastVersionCheckDate;

  @Value("${projectforge.versioncheck.url:https://projectforge.micromata.de/publicRest/versionCheck}")
  private String versionCheckUrl;

  private Boolean newPFVersionAvailable;

  @Autowired
  private RestCallService restCallService;

  public VersionCheck getVersionCheckInformations() {
    Locale locale = ThreadLocalUserContext.getUser() != null && ThreadLocalUserContext.getUser().getLocale() != null ?
            ThreadLocalUserContext.getUser().getLocale() :
            ThreadLocalUserContext.getLocale();
    TimeZone timeZone = ThreadLocalUserContext.getUser() != null && ThreadLocalUserContext.getUser().getTimeZone() != null ?
            TimeZone.getTimeZone(ThreadLocalUserContext.getUser().getTimeZone()) :
            ThreadLocalUserContext.getTimeZone();
    VersionCheck versionCheck = new VersionCheck(AppVersion.VERSION.toString(), locale, timeZone);
    try {
      versionCheck = restCallService.callRestInterfaceForUrl(versionCheckUrl, HttpMethod.POST, VersionCheck.class, versionCheck);
    } catch (Exception ex) {
      // System offline?
    }
    return versionCheck;
  }

  public boolean isNewPFVersionAvailable() {
    LocalDate now = LocalDate.now();
    if (lastVersionCheckDate == null) {
      lastVersionCheckDate = LocalDate.now().minusDays(1);
    }
    if (enableVersionCheck && (now.isAfter(lastVersionCheckDate) || newPFVersionAvailable == null)) {
      lastVersionCheckDate = LocalDate.now();
      newPFVersionAvailable = Boolean.FALSE;
      try {
        VersionCheck versionCheckInformations = getVersionCheckInformations();
        if (versionCheckInformations != null && StringUtils.isNotEmpty(versionCheckInformations.getSourceVersion()) && StringUtils
                .isNotEmpty(versionCheckInformations.getTargetVersion())) {
          String[] sourceVersionPartsWithoutMinus = versionCheckInformations.getSourceVersion().split("-");
          String[] targetVersionPartsWithoutMinus = versionCheckInformations.getTargetVersion().split("-");
          if (sourceVersionPartsWithoutMinus.length > 0 && targetVersionPartsWithoutMinus.length > 0) {
            int[] sourceVersionPartsInteger = getIntegerVersionArray(sourceVersionPartsWithoutMinus[0].split("\\."));
            int[] targetVersionPartsInteger = getIntegerVersionArray(targetVersionPartsWithoutMinus[0].split("\\."));
            for (int i = 0; i < 4; i++) {
              if (sourceVersionPartsInteger[i] < targetVersionPartsInteger[i]) {
                newPFVersionAvailable = Boolean.TRUE;
                return newPFVersionAvailable;
              }
              if (sourceVersionPartsInteger[i] > targetVersionPartsInteger[i]) {
                newPFVersionAvailable = Boolean.FALSE;
                return newPFVersionAvailable;
              }
            }
          }
        }
      } catch (Exception e) {
        log.error("An exception occured while checkin PF version: " + e.getMessage(), e);
        return Boolean.FALSE;
      }
    }
    if (newPFVersionAvailable == null) {
      newPFVersionAvailable = Boolean.FALSE;
    }
    return newPFVersionAvailable;
  }

  private int[] getIntegerVersionArray(final String[] sourceVersionParts) {
    int[] result = new int[4];
    for (int i = 0; i < 4; i++) {
      try {
        result[i] = Integer.parseInt(sourceVersionParts[i]);
      } catch (Exception e) {
        result[i] = 0;
      }
    }
    return result;
  }

  public void setLastVersionCheckDate(LocalDate newDateValue) {
    lastVersionCheckDate = newDateValue;
  }

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
    final List<TaskDO> tasks = taskDao.internalLoadAll();
    buf.append("Found " + tasks.size() + " tasks.\n");
    final Map<Integer, TaskDO> taskMap = new HashMap<>();
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
    final TenantRegistry tenantRegistry = TenantRegistryMap.getInstance().getTenantRegistry();
    tenantRegistry.getUserGroupCache().forceReload();
    tenantRegistry.getTaskTree().forceReload();
    kontoCache.forceReload();
    kostCache.forceReload();
    rechnungCache.forceReload();
    systemInfoCache.forceReload();
    return "UserGroupCache, TaskTree, KontoCache, KostCache, RechnungCache, SystemInfoCache";
  }

  public void setEnableVersionCheck(final boolean enableVersionCheck) {
    this.enableVersionCheck = enableVersionCheck;
  }
}
