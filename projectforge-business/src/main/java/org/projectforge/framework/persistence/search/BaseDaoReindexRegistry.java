/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.search;

import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.springframework.cglib.proxy.Enhancer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Hotfix: Hibernate-search does not update index of dependent objects.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class BaseDaoReindexRegistry
{
  private static BaseDaoReindexRegistry instance = new BaseDaoReindexRegistry();

  public static BaseDaoReindexRegistry getSingleton()
  {
    return instance;
  }

  protected Map<Class< ? extends BaseDO< ? >>, Set<BaseDao< ? >>> registeredDependents = new HashMap<>();

  /**
   * Register dao to be called after updating an object of type clazz for updating search index of dependent objects managed by the given
   * dao.
   * @param clazz Type of modified object
   * @param dao Dao to notify.
   */
  public void registerDependent(Class< ? extends BaseDO< ? >> clazz, BaseDao< ? > dao)
  {
    if (Enhancer.isEnhanced(dao.getClass())) {
      return;
    }
    Set<BaseDao< ? >> set = this.registeredDependents.get(clazz);
    if (set == null) {
      set = new HashSet<>();
      this.registeredDependents.put(clazz, set);
    }
    set.add(dao);
  }

  /**
   * Update all objects which are dependent from the given object. Is called by the dao managing the given object after updating the given
   * object.
   * @param obj Updated object.
   */
  public Set<BaseDao< ? >> getRegisteredDependents(final BaseDO< ? > obj)
  {
    return this.registeredDependents.get(obj.getClass());
  }
}
