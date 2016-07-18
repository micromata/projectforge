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

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.rest.JsonUtils;
import org.projectforge.rest.RestPaths;
import org.projectforge.rest.objects.CalendarEventObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * REST interface for {@link TeamEventDao}
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Controller
@Path(RestPaths.TEAMEVENTS)
public class TeamEventDaoRest
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventDaoRest.class);

  @Autowired
  private TeamEventDao teamEventDao;

  @Autowired
  TeamCalCache teamCalCache;

  /**
   * Rest call for {@link TeamEventDao#getEventList(TeamEventFilter, boolean)}
   * 
   * @param calendarIds The id's of the calendars to search for events (comma separated). If not given, all calendars
   *          owned by the context user are assumed.
   * @param daysInFuture Get events from today until daysInFuture (default is 30). Maximum allowed value is 90.
   */
  @GET
  @Path(RestPaths.LIST)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getReminderList(@QueryParam("calendarIds") final String calendarIds,
      @QueryParam("modifiedSince") final Integer daysInFuture)
  {
    final DayHolder day = new DayHolder();
    int days = daysInFuture != null ? daysInFuture : 30;
    if (days <= 0 || days > 90) {
      days = 90;
    }
    day.add(Calendar.DAY_OF_YEAR, days);
    final Collection<Integer> cals = new LinkedList<Integer>();
    if (StringUtils.isBlank(calendarIds) == true) {
      final Collection<TeamCalDO> ownCals = teamCalCache.getAllOwnCalendars();
      if (ownCals != null && ownCals.size() > 0) {
        for (final TeamCalDO cal : ownCals) {
          cals.add(cal.getId());
        }
      }
    } else {
      final Integer[] ids = StringHelper.splitToIntegers(calendarIds, ",;:");
      if (ids != null && ids.length > 0) {
        for (final Integer id : ids) {
          if (id != null) {
            cals.add(id);
          }
        }
      }
    }
    final List<CalendarEventObject> result = new LinkedList<CalendarEventObject>();
    if (cals.size() > 0) {
      final Date now = new Date();
      final TeamEventFilter filter = new TeamEventFilter().setStartDate(now).setEndDate(day.getDate())
          .setTeamCals(cals);
      final List<TeamEvent> list = teamEventDao.getEventList(filter, true);
      if (list != null && list.size() > 0) {
        for (final TeamEvent event : list) {
          if (event.getStartDate().after(now) == true) {
            result.add(TeamEventDOConverter.getEventObject(event));
          } else {
            log.info("Start date not in future:" + event.getStartDate() + ", " + event.getSubject());
          }
        }
      }
    } else {
      log.warn("No calendar ids are given, so can't find any events.");
    }
    final String json = JsonUtils.toJson(result);
    return Response.ok(json).build();
  }
}
