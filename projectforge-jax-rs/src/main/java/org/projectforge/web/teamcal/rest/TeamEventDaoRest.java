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

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventConverter;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.model.rest.CalendarEventObject;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.rest.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Uid;

/**
 * REST interface for {@link TeamEventDao}
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Controller
@Path(RestPaths.TEAMEVENTS)
public class TeamEventDaoRest
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventDaoRest.class);

  @Autowired
  private TeamEventService teamEventService;

  @Autowired
  private TeamCalCache teamCalCache;

  @Autowired
  private TeamEventConverter teamEventConverter;

  /**
   * @param calendarIds  The id's of the calendars to search for events (comma separated). If not given, all calendars
   *                     owned by the context user are assumed.
   * @param daysInFuture Get events from today until daysInFuture (default is 30). Maximum allowed value is 90.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getReminderList(@QueryParam("calendarIds") final String calendarIds, @QueryParam("daysInPast") final Integer daysInPast,
      @QueryParam("daysInFuture") final Integer daysInFuture)
  {
    Calendar start = null;
    if (daysInPast != null && daysInPast > 0) {
      start = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
      start.add(Calendar.DAY_OF_MONTH, -daysInPast);
    }
    Calendar end = null;
    if (daysInFuture != null && daysInFuture > 0) {
      end = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
      end.add(Calendar.DAY_OF_MONTH, daysInFuture);
    }

    final Collection<Integer> cals = getCalendarIds(calendarIds);
    final List<CalendarEventObject> result = new LinkedList<CalendarEventObject>();
    if (cals.size() > 0) {
      final TeamEventFilter filter = new TeamEventFilter().setTeamCals(cals);
      if (start != null) {
        filter.setStartDate(start.getTime());
      }
      if (end != null) {
        filter.setEndDate(end.getTime());
      }
      final List<TeamEventDO> list = teamEventService.getTeamEventDOList(filter);
      if (list != null && list.size() > 0) {
        list.forEach(event -> result.add(teamEventConverter.getEventObject(event, true)));
      }
    } else {
      log.warn("No calendar ids are given, so can't find any events.");
    }
    final String json = JsonUtils.toJson(result);
    return Response.ok(json).build();
  }

  /**
   * Rest call for {@link TeamEventDao#getEventList(TeamEventFilter, boolean)}
   *
   * @param calendarIds  The id's of the calendars to search for events (comma separated). If not given, all calendars
   *                     owned by the context user are assumed.
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
    final Collection<Integer> cals = getCalendarIds(calendarIds);
    final List<CalendarEventObject> result = new LinkedList<CalendarEventObject>();
    if (cals.size() > 0) {
      final Date now = new Date();
      final TeamEventFilter filter = new TeamEventFilter().setStartDate(now).setEndDate(day.getDate())
          .setTeamCals(cals);
      final List<TeamEvent> list = teamEventService.getEventList(filter, true);
      if (list != null && list.size() > 0) {
        for (final TeamEvent event : list) {
          if (event.getStartDate().after(now) == true) {
            result.add(teamEventConverter.getEventObject(event, true));
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

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response saveOrUpdateTeamEvent(final CalendarEventObject calendarEvent)
  {
    CalendarEventObject result = null;
    try {
      String createUpdate = "created";
      TeamCalDO teamCalDO = teamCalCache.getCalendar(calendarEvent.getCalendarId());
      final CalendarBuilder builder = new CalendarBuilder();
      final net.fortuna.ical4j.model.Calendar calendar = builder.build(new ByteArrayInputStream(Base64.decodeBase64(calendarEvent.getIcsData())));
      final VEvent event = (VEvent) calendar.getComponent(Component.VEVENT);
      Uid eventUid = event.getUid();
      final TeamEventDO teamEvent = teamEventConverter.createTeamEventDO(event,
          TimeZone.getTimeZone(teamCalDO.getOwner().getTimeZone()));
      TeamEventDO teamEventOrigin = teamEventService.findByUid(eventUid.getValue());
      Set<TeamEventAttendeeDO> originAttendees = new HashSet<>();
      if (teamEventOrigin != null) {
        createUpdate = "updated";
        teamEvent.setId(teamEventOrigin.getPk());
        teamEvent.setCreated(teamEventOrigin.getCreated());
        teamEvent.setTenant(teamEventOrigin.getTenant());
        originAttendees = teamEventOrigin.getAttendees();
      }
      teamEvent.setCalendar(teamCalDO);
      teamEvent.setUid(eventUid.getValue());
      Set<TeamEventAttendeeDO> attendeesToAssignMap = new HashSet<>();
      Set<TeamEventAttendeeDO> attendeesToUnassignMap = new HashSet<>();
      if (teamEvent.getAttendees() != null && teamEvent.getAttendees().size() > 0) {
        attendeesToAssignMap = getAttendeesToAssign(teamEvent, teamEventOrigin);
        attendeesToUnassignMap = getAttendeesToUnassign(teamEvent, teamEventOrigin);
        teamEvent.setAttendees(originAttendees);
      }
      teamEventService.saveOrUpdate(teamEvent);
      teamEventService.assignAttendees(teamEvent, attendeesToAssignMap, attendeesToUnassignMap);
      if (attendeesToAssignMap != null && attendeesToAssignMap.size() > 0) {
        teamEventService.sendTeamEventToAttendees(teamEvent, teamEvent.getPk() == null, teamEvent.getPk() != null, false, attendeesToAssignMap);
      }
      result = teamEventConverter.getEventObject(teamEvent, true);
      log.info("Team event: " + teamEvent.getSubject() + " for calendar #" + teamCalDO.getId() + " successfully " + createUpdate + ".");
    } catch (Exception e) {
      log.error("Exception while creating/updating team event", e);
      return Response.serverError().build();
    }
    if (result != null) {
      final String json = JsonUtils.toJson(result);
      return Response.ok(json).build();
    } else {
      log.error("Something went wrong while creating/updating team event");
      return Response.serverError().build();
    }
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteTeamEvent(final CalendarEventObject calendarEvent)
  {
    try {
      final CalendarBuilder builder = new CalendarBuilder();
      final net.fortuna.ical4j.model.Calendar calendar = builder.build(new ByteArrayInputStream(Base64.decodeBase64(calendarEvent.getIcsData())));
      final VEvent event = (VEvent) calendar.getComponent(Component.VEVENT);
      Uid eventUid = event.getUid();
      TeamEventDO teamEventOrigin = teamEventService.findByUid(eventUid.getValue());
      if (teamEventOrigin != null) {
        teamEventService.markAsDeleted(teamEventOrigin);
        log.info("Team event with the id: " + eventUid.getValue() + " for calendar #" + calendarEvent.getCalendarId() + " successfully marked as deleted.");
      } else {
        log.warn("Team event with uid: " + eventUid.getValue() + " not found");
        return Response.serverError().build();
      }
    } catch (Exception e) {
      log.error("Exception while deleting team event", e);
      return Response.serverError().build();
    }
    return Response.ok().build();
  }

  private Collection<Integer> getCalendarIds(String calendarIds)
  {
    final Collection<Integer> cals = new LinkedList<>();
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
    return cals;
  }

  private Set<TeamEventAttendeeDO> getAttendeesToUnassign(TeamEventDO newTeamEvent, TeamEventDO originTeamEvent)
  {
    Set<TeamEventAttendeeDO> result = new HashSet<>();
    if (originTeamEvent == null || originTeamEvent.getAttendees() == null || originTeamEvent.getAttendees().size() < 1) {
      return result;
    } else {
      Set<TeamEventAttendeeDO> attendeesToUnassign = new HashSet<>();
      for (TeamEventAttendeeDO originAttendee : originTeamEvent.getAttendees()) {
        int originPk = originAttendee.getPk();
        originAttendee.setPk(null);
        newTeamEvent.getAttendees().forEach(att -> att.setPk(null));
        if (newTeamEvent.getAttendees().contains(originAttendee) == false) {
          originAttendee.setPk(originPk);
          result.add(originAttendee);
        }
      }
    }
    return result;
  }

  private Set<TeamEventAttendeeDO> getAttendeesToAssign(TeamEventDO newTeamEvent, TeamEventDO originTeamEvent)
  {
    Set<TeamEventAttendeeDO> result = new HashSet<>();
    if (originTeamEvent == null || originTeamEvent.getAttendees() == null || originTeamEvent.getAttendees().size() < 1) {
      for (TeamEventAttendeeDO newAttendee : newTeamEvent.getAttendees()) {
        newAttendee.setPk(null);
        result.add(newAttendee);
      }
      return result;
    } else {
      Set<TeamEventAttendeeDO> attendeesToAssign = new HashSet<>();
      for (TeamEventAttendeeDO newAttendee : newTeamEvent.getAttendees()) {
        newAttendee.setPk(null);
        boolean hasToBeAssigned = false;
        for (TeamEventAttendeeDO originAttendee : originTeamEvent.getAttendees()) {
          if (originAttendee.equals(newAttendee) == false) {
            hasToBeAssigned = true;
          }
        }
        if (hasToBeAssigned) {
          result.add(newAttendee);
        }
      }
      return result;
    }
  }

}
