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

package org.projectforge.plugins.ffp.repository;

import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.wicket.FFPEventFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Access to ffp events.
 *
 * @author Florian Blumenstein
 */
@Repository
public class FFPEventDao extends BaseDao<FFPEventDO> {
  @Autowired
  private PfEmgrFactory emgrFactory;

  public FFPEventDao() {
    super(FFPEventDO.class);
  }

  @Override
  public FFPEventDO newInstance() {
    return new FFPEventDO();
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final FFPEventDO obj, final FFPEventDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    return true;
  }

  @Override
  public List<FFPEventDO> getList(BaseSearchFilter filter) {
    final FFPEventFilter myFilter;
    if (filter instanceof FFPEventFilter) {
      myFilter = (FFPEventFilter) filter;
    } else {
      myFilter = new FFPEventFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(filter);
    if (myFilter.isShowOnlyActiveEntries()) {
      queryFilter.add(QueryFilter.eq("finished", false));
    }
    return getList(queryFilter);
  }
}
