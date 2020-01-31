/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.ffp.repository;

import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Access to ffp event.
 *
 * @author Florian Blumenstein
 */
@Component
public class FFPDeptCounterCache extends AbstractCache {
  private static Logger log = LoggerFactory.getLogger(FFPDeptCounterCache.class);

  @Autowired
  private FFPDebtDao ffpDebtDao;

  private Map<Integer, Integer> openFromDebts = new HashMap<>();

  private Map<Integer, Integer> openToDebts = new HashMap<>();

  public Integer getOpenDebts(PFUserDO user)
  {
    return getOpenFromDebts(user) + getOpenToDebts(user);
  }

  public Integer getOpenFromDebts(PFUserDO user) {
    if (user == null)
      return 0;
    checkRefresh();
    Integer val = openFromDebts.get(user.getId());
    if (val == null) {
      val = ffpDebtDao.getOpenFromDebts(user);
      openFromDebts.put(user.getId(), val);
    }
    return val;
  }

  public Integer getOpenToDebts(PFUserDO user) {
    if (user == null)
      return 0;
    checkRefresh();
    Integer val = openToDebts.get(user.getId());
    if (val == null) {
      val = ffpDebtDao.getOpenToDebts(user);
      openToDebts.put(user.getId(), val);
    }
    return val;
  }

  /**
   * This method will be called by CacheHelper and is synchronized via getData();
   */
  @Override
  protected void refresh() {
    log.info("Initializing FFDeptCounterCache ...");
    openFromDebts.clear();
    openToDebts.clear();
    log.info("Initializing of FFDeptCounterCache done.");
  }
}
