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

package org.projectforge.web.teamcal.rest;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.TeamCalFilter;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.model.rest.CalendarObject;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.rest.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * REST interface for {@link TeamCalDao}.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Controller
@Path(RestPaths.TEAMCAL)
public class TeamCalDaoRest
{
  @Autowired
  private TeamCalDao teamCalDao;

  @Autowired
  private UserRightService userRights;

  /**
   * Rest-Call for {@link TeamCalDao#getList(org.projectforge.core.BaseSearchFilter)}
   */
  @GET
  @Path(RestPaths.LIST)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList(@QueryParam("fullAccess") final boolean fullAccess)
  {
    final TeamCalFilter filter = new TeamCalFilter();
    if (fullAccess == true) {
      filter.setFullAccess(true);
      filter.setMinimalAccess(false);
      filter.setReadonlyAccess(false);
    }
    final List<TeamCalDO> list = teamCalDao.getList(filter);
    final List<CalendarObject> result = new LinkedList<CalendarObject>();
    if (list != null && list.size() > 0) {
      for (final TeamCalDO cal : list) {
        result.add(TeamCalDOConverter.getCalendarObject(cal, userRights));
      }
    }
    final String json = JsonUtils.toJson(result);
    return Response.ok(json).build();
  }

}
