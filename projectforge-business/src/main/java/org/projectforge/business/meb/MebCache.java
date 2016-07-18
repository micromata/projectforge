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

package org.projectforge.business.meb;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.projectforge.framework.cache.AbstractCache;

/**
 * The number of recent MEB entries is cached. Accessible via MebDao.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MebCache extends AbstractCache
{
  private static Logger log = Logger.getLogger(MebCache.class);

  /** The key is the user id and the value is the number of recent MEB entries. */
  private Map<Integer, Integer> recentEntriesMap;

  private MebDao mebDao;

  /**
   * Refreshes every 30 minutes. After any modifications via MebDao this cache will be refreshed.
   */
  public MebCache(final MebDao mebDao)
  {
    super(30 * TICKS_PER_MINUTE);
    this.mebDao = mebDao;
  }

  int getRecentMEBEntries(final Integer userId)
  {
    checkRefresh();
    final Integer result = recentEntriesMap.get(userId);
    if (result != null) {
      return result;
    }
    final int counter = mebDao.internalGetRecentMEBEntries(userId);
    synchronized (recentEntriesMap) {
      recentEntriesMap.put(userId, counter);
    }
    return counter;
  }

  /**
   * This method will be called by CacheHelper and is synchronized via getData();
   */
  protected void refresh()
  {
    log.info("Clearing MebCache.");
    recentEntriesMap = new HashMap<Integer, Integer>();
  }
}
