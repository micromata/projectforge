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

package org.projectforge.plugins.poll.event;

import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.plugins.poll.PollDO;
import org.projectforge.plugins.poll.PollPluginUserRightId;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Repository
public class PollEventDao extends BaseDao<PollEventDO> {
  protected PollEventDao() {
    super(PollEventDO.class);
    userRightId = PollPluginUserRightId.PLUGIN_POLL_EVENT;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#newInstance()
   */
  @Override
  public PollEventDO newInstance() {
    return new PollEventDO();
  }

  public List<PollEventDO> getListByPoll(PollDO poll) {
    QueryFilter qFilter = new QueryFilter();
    qFilter.add(QueryFilter.and(QueryFilter.eq("poll", poll), QueryFilter.eq("deleted", false)));
    return getList(qFilter);
  }
}
