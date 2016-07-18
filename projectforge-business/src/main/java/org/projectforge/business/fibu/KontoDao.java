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

package org.projectforge.business.fibu;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class KontoDao extends BaseDao<KontoDO>
{
  public static final UserRightId USER_RIGHT_ID = UserRightId.FIBU_ACCOUNTS;

  @Autowired
  KontoCache kontoCache;

  public KontoDao()
  {
    super(KontoDO.class);
    userRightId = USER_RIGHT_ID;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterSaveOrModify(final KontoDO obj)
  {
    getKontoCache().refresh();
  }

  @SuppressWarnings("unchecked")
  public KontoDO getKonto(final Integer kontonummer)
  {
    if (kontonummer == null) {
      return null;
    }
    final List<KontoDO> list = (List<KontoDO>) getHibernateTemplate().find("from KontoDO u where u.nummer = ?",
        kontonummer);
    if (CollectionUtils.isEmpty(list) == true) {
      return null;
    }
    return list.get(0);
  }

  @Override
  public KontoDO newInstance()
  {
    return new KontoDO();
  }

  /**
   * @return the kontoCache
   */
  public KontoCache getKontoCache()
  {
    return kontoCache;
  }
}
