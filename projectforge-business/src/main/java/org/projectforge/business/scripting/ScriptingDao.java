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

package org.projectforge.business.scripting;

import java.io.Serializable;
import java.util.List;

import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.QueryFilter;

public class ScriptingDao<O extends ExtendedBaseDO<Integer>>
{
  private BaseDao<O> __baseDao;

  @SuppressWarnings("unchecked")
  public ScriptingDao(BaseDao<?> baseDao)
  {
    this.__baseDao = (BaseDao<O>) baseDao;
  }

  /**
   * @see BaseDao#getList(BaseSearchFilter)
   */
  public List<O> getList(BaseSearchFilter filter)
  {
    return __baseDao.getList(filter);
  }

  /**
   * Show whole list of objects with select access (without deleted entries).
   * 
   * @return
   */
  public List<O> getList()
  {
    return __baseDao.getList(new QueryFilter());
  }

  /**
   * @see BaseDao#getList(QueryFilter)
   */
  public List<O> getList(QueryFilter filter)
  {
    return __baseDao.getList(filter);
  }

  /**
   * @see BaseDao#getById(Serializable)
   * @throws AccessException
   */
  public O getById(Serializable id) throws AccessException
  {
    return __baseDao.getById(id);
  }
}
