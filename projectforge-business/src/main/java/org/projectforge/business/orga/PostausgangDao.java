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

package org.projectforge.business.orga;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.criterion.Order;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

  private static final String[] ENABLED_AUTOCOMPLETION_PROPERTIES = {"empfaenger", "person", "inhalt"};

  @Override
  public boolean isAutocompletionPropertyEnabled(String property) {
    return ArrayUtils.contains(ENABLED_AUTOCOMPLETION_PROPERTIES, property);
  }

  /**
   * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
   */
  public int[] getYears()
  {
    final Object[] minMaxDate = getSession().createNamedQuery(PostausgangDO.SELECT_MIN_MAX_DATE, Object[].class)
            .getSingleResult();
    return SQLHelper.getYears((java.sql.Date)minMaxDate[0], (java.sql.Date)minMaxDate[1]);
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
