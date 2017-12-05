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

import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.springframework.stereotype.Component;

/**
 * Provides some system information in a cache.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@Component()
public class SystemInfoCache extends AbstractCache
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SystemInfoCache.class);

  private boolean cost2EntriesExists, projectEntriesExists, customerEntriesExists;

  protected long expireTimeHours = 1;

  /**
   * SystemInfoCache can be used either over Spring context or with this static method.
   * 
   * @return
   */
  public static SystemInfoCache instance()
  {
    return instance;
  }

  private static SystemInfoCache instance;

  /**
   * Only for internal usage on start-up of ProjectForge.
   * 
   * @param theInstance
   */
  public static void internalInitialize(final SystemInfoCache theInstance)
  {
    instance = theInstance;
  }

  public boolean isCustomerEntriesExists()
  {
    checkRefresh();
    return customerEntriesExists;
  }

  public boolean isProjectEntriesExists()
  {
    checkRefresh();
    return projectEntriesExists;
  }

  public boolean isCost2EntriesExists()
  {
    checkRefresh();
    return cost2EntriesExists;
  }

  @Override
  protected void refresh()
  {
    log.info("Refreshing SystemInfoCache...");

    customerEntriesExists = hasTableEntries(KundeDO.class);
    projectEntriesExists = hasTableEntries(ProjektDO.class);
    cost2EntriesExists = hasTableEntries(Kost2DO.class);
    log.info("Refreshing SystemInfoCache done.");
  }

  private boolean hasTableEntries(final Class<?> entity)
  {
    return PfEmgrFactory.get().runInTrans((emgr) -> {
      return emgr.createQuery(Long.class, "select count(e) from " + entity.getName() + " e").getSingleResult() != 0;
    });
  }

  @Override
  public void setExpireTimeInHours(final long expireTime)
  {
    this.expireTime = expireTimeHours * TICKS_PER_HOUR;
  }

}
