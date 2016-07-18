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

package org.projectforge.business.orga;

import java.util.List;

import org.hibernate.criterion.Order;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PostausgangDao extends BaseDao<PostausgangDO>
{
  public static final UserRightId USER_RIGHT_ID = UserRightId.ORGA_OUTGOING_MAIL;

  protected PostausgangDao()
  {
    super(PostausgangDO.class);
    userRightId = USER_RIGHT_ID;
  }

  /**
   * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  public int[] getYears()
  {
    final List<Object[]> list = getSession().createQuery("select min(datum), max(datum) from PostausgangDO t").list();
    return SQLHelper.getYears(list);
  }

  @Override
  public List<PostausgangDO> getList(final BaseSearchFilter filter)
  {
    final PostFilter myFilter;
    if (filter instanceof PostFilter) {
      myFilter = (PostFilter) filter;
    } else {
      myFilter = new PostFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(filter);
    queryFilter.setYearAndMonth("datum", myFilter.getYear(), myFilter.getMonth());
    queryFilter.addOrder(Order.desc("datum"));
    queryFilter.addOrder(Order.asc("empfaenger"));
    final List<PostausgangDO> list = getList(queryFilter);
    return list;
  }

  @Override
  public PostausgangDO newInstance()
  {
    return new PostausgangDO();
  }
}
