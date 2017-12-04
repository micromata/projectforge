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

package org.projectforge.framework.persistence.database;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.registry.Registry;
import org.projectforge.registry.RegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Stores the number of entities in the different tables (used by SearchPage).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class StatisticsCache extends AbstractCache
{
  private static Logger log = LoggerFactory.getLogger(StatisticsCache.class);

  /** The key is the entity class and the value the number of entries in the table. */
  private Map<Class<? extends BaseDO<?>>, Integer> numberOfEntitiesMap;

  @Autowired
  private DataSource dataSource;

  protected long expireTimeHours = 12;

  public Integer getNumberOfEntities(final Class<? extends BaseDO<?>> clazz)
  {
    checkRefresh();
    return numberOfEntitiesMap.get(clazz);
  }

  /**
   * This method will be called by CacheHelper and is synchronized via getData();
   */
  @Override
  protected void refresh()
  {
    log.info("Initializing StatisticsCache ...");
    numberOfEntitiesMap = new HashMap<Class<? extends BaseDO<?>>, Integer>();
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    for (final RegistryEntry registryEntry : Registry.getInstance().getOrderedList()) {
      try {
        final int number = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + HibernateUtils.getDBTableName(registryEntry.getDOClass()), Integer.class);
        numberOfEntitiesMap.put(registryEntry.getDOClass(), number);
      } catch (final Exception ex) {
        log.error(ex.getMessage(), ex);
        continue;
      }
    }
    log.info("Initializing of StatisticsCache done.");
  }

  @Override
  public void setExpireTimeInHours(final long expireTime)
  {
    this.expireTime = expireTimeHours * TICKS_PER_HOUR;
  }
}
