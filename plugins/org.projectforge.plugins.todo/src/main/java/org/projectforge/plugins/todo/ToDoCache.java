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

package org.projectforge.plugins.todo;

import org.projectforge.framework.cache.AbstractCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The number of open to-do entries is cached. Accessible via ToDoDao.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ToDoCache extends AbstractCache
{
  private static Logger log = LoggerFactory.getLogger(ToDoCache.class);

  /** The key is the user id and the value is the number of open to-do entries. */
  private Map<Integer, Integer> openEntriesMap;

  private ToDoDao toDoDao;

  /**
   * Refreshes every 30 minutes. After any modifications via MebDao this cache will be refreshed.
   */
  public ToDoCache(final ToDoDao toDoDao)
  {
    super(30 * TICKS_PER_MINUTE);
    this.toDoDao = toDoDao;
  }

  int getOpenToDoEntries(final Integer userId)
  {
    checkRefresh();
    final Integer result = openEntriesMap.get(userId);
    if (result != null) {
      return result;
    }
    final int counter = toDoDao.internalGetOpenEntries(userId);
    synchronized (openEntriesMap) {
      openEntriesMap.put(userId, counter);
    }
    return counter;
  }

  /**
   * This method will be called by CacheHelper and is synchronized via getData();
   */
  @Override
  protected void refresh()
  {
    log.info("Clearing cache with open to-do entries.");
    openEntriesMap = new HashMap<>();
  }
}
