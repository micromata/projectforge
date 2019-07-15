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

package org.projectforge.business.teamcal.event;

import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.teamcal.event.model.TeamEventICSDO;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TeamEventICSDao extends BaseDao<TeamEventICSDO>
{

  @Autowired
  private PfEmgrFactory emgrFac;

  @Autowired
  private TenantService tenantService;

  public TeamEventICSDao() {
    super(TeamEventICSDO.class);
    userRightId = UserRightId.PLUGIN_CALENDAR_EVENT;
  }

  public TeamEventICSDO getByUid(Integer calendarId, final String uid)
  {
    return this.getByUid(calendarId, uid, true);
  }

  public TeamEventICSDO getByUid(Integer calendarId, final String uid, final boolean excludeDeleted) {
    if (uid == null) {
      return null;
    }

    final StringBuilder sqlQuery = new StringBuilder();
    final List<Object> params = new ArrayList<>();

    sqlQuery.append("select e from TeamEventICSDO e where e.uid = :uid AND e.tenant = :tenant");

    params.add("uid");
    params.add(uid);
    params.add("tenant");
    params.add(ThreadLocalUserContext.getUser() != null ? ThreadLocalUserContext.getUser().getTenant() : tenantService.getDefaultTenant());

    if (excludeDeleted) {
      sqlQuery.append(" AND e.deleted = :deleted");
      params.add("deleted");
      params.add(false);
    }

    // workaround to still handle old requests
    if (calendarId != null) {
      sqlQuery.append(" AND e.calendar.id = :calendarId");
      params.add("calendarId");
      params.add(calendarId);
    }

    try {
      return emgrFac.runRoTrans(emgr -> emgr.selectSingleAttached(TeamEventICSDO.class, sqlQuery.toString(), params.toArray()));
    } catch (NoResultException | NonUniqueResultException e) {
      return null;
    }
  }

  @Override
  public TeamEventICSDO newInstance()
  {
    return null;
  }
}
