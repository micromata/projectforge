/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.service;

import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Access to ffp event.
 *
 * @author Florian Blumenstein
 */
@Component
public class VacationMenuCounterCache extends AbstractCache {
  private static Logger log = LoggerFactory.getLogger(VacationMenuCounterCache.class);

  @Autowired
  private VacationService vacationService;

  // Key is the user id. Concurrent, as the menus of all users are built in parallel request threads. Expired by
  // VacationDao on every vacation change, so a badge doesn't count already handled applications.
  private final Map<Long, Integer> openMenuCounter = new ConcurrentHashMap<>();

  public Integer getOpenLeaveApplicationsForUser(PFUserDO employeeUser) {
    if (employeeUser == null)
      return 0;
    checkRefresh();
    return openMenuCounter.computeIfAbsent(employeeUser.getId(),
        id -> vacationService.getOpenLeaveApplicationsForUser(employeeUser));
  }

  /**
   * This method will be called by CacheHelper and is synchronized via getData();
   */
  @Override
  protected void refresh() {
    log.debug("VacationMenuCounterCache set as expired.");
    openMenuCounter.clear();
  }
}
