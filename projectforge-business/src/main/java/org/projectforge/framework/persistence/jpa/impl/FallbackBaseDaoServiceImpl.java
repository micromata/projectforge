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

package org.projectforge.framework.persistence.jpa.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.FallbackBaseDaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import de.micromata.genome.logging.GenomeLogCategory;
import de.micromata.genome.logging.LogLevel;
import de.micromata.genome.logging.LoggedRuntimeException;

/**
 * Implementation of FallbackBaseDaoService.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Service
public class FallbackBaseDaoServiceImpl implements FallbackBaseDaoService, Serializable
{
  private static final long serialVersionUID = 6896926326176928446L;

  @Autowired
  transient ApplicationContext applicationContext;
  /**
   * Cached map of registered BaseDaos.
   */
  private Map<String, BaseDao> baseDaos;
  private Map<Class<?>, BaseDao> entToBaseDao;

  private Map<String, BaseDao> getBaseDaos()
  {
    if (baseDaos != null) {
      return baseDaos;
    }
    baseDaos = applicationContext.getBeansOfType(BaseDao.class);

    return baseDaos;
  }

  private Map<Class<?>, BaseDao> getEntToBaseDao()
  {
    if (entToBaseDao != null) {
      return entToBaseDao;
    }
    Map<String, BaseDao> bda = getBaseDaos();
    Map<Class<?>, BaseDao> tentToBaseDao = new HashMap<>();
    for (BaseDao bd : bda.values()) {
      Class entclas = bd.getEntityClass();
      if (tentToBaseDao.containsKey(entclas) == true) {
        throw new LoggedRuntimeException(LogLevel.Fatal, GenomeLogCategory.Coding, "Multiple BaseDaos for DO: "
            + entclas.getName() + "; " + tentToBaseDao.get(entclas).getClass() + ", " + bd.getClass());
      }
      tentToBaseDao.put(bd.getEntityClass(), bd);

    }
    return entToBaseDao = tentToBaseDao;
  }

  @Override
  public <ENT extends ExtendedBaseDO<Integer>> BaseDao<ENT> getBaseDaoForEntity(Class<ENT> ent)
  {
    BaseDao<ENT> ret = getEntToBaseDao().get(ent);
    if (ret != null) {
      return ret;
    }
    throw new IllegalArgumentException("Cannot find BaseDao for entity: " + ent.getName());
  }

}
