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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.service.TeamCalServiceImpl;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.model.rest.CalendarEventObject;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.rest.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.CalendarComponent;
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
  private static final String CREATED = "created";
  private static final String UPDATED = "updated";

  @Autowired
  private TeamCalServiceImpl teamCalService;

  @Autowired
  private TeamEventService teamEventService;

  @Autowired
  private TeamCalCache teamCalCache;

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
        list.forEach(event -> result.add(teamCalService.getEventObject(event, true, true)));
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
            result.add(teamCalService.getEventObject(event, true, true));
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
  @Path(RestPaths.SAVE)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response saveTeamEvent(final CalendarEventObject calendarEvent)
  {

    try {
      //Getting the calender at which the event will created/updated
      TeamCalDO teamCalDO = teamCalCache.getCalendar(calendarEvent.getCalendarId());
      //ICal4J CalendarBuilder for building calendar events from ics
      final CalendarBuilder builder = new CalendarBuilder();
      //Build the event from ics
      final net.fortuna.ical4j.model.Calendar calendar = builder.build(new ByteArrayInputStream(Base64.decodeBase64(calendarEvent.getIcsData())));
      //Getting the VEvent from ics
      final VEvent event = (VEvent) calendar.getComponent(Component.VEVENT);
      return saveVEvent(event, teamCalDO);
    } catch (Exception e) {
      log.error("Exception while creating team event", e);
      return Response.serverError().build();
    }
  }

  private Response saveVEvent(VEvent event, TeamCalDO teamCalDO)
  {
    //The result for returning
    CalendarEventObject result = null;
    //Building TeamEventDO from VEvent
    final TeamEventDO teamEvent = teamCalService.createTeamEventDO(event,
        TimeZone.getTimeZone(teamCalDO.getOwner().getTimeZone()), false);
    //Setting the calendar
    teamEvent.setCalendar(teamCalDO);
    //Save attendee list, because assignment later
    Set<TeamEventAttendeeDO> attendees = new HashSet<>();
    teamEvent.getAttendees().forEach(att -> {
      attendees.add(att.clone());
    });
    teamEvent.setAttendees(null);
    //Save or update the generated event
    teamEventService.save(teamEvent);
    //Update attendees
    teamEventService.assignAttendees(teamEvent, attendees, null);

    if (attendees.size() > 0) {
      teamEventService.sendTeamEventToAttendees(teamEvent, true, false, false, null);
    }
    result = teamCalService.getEventObject(teamEvent, true, true);
    log.info("Team event: " + teamEvent.getSubject() + " for calendar #" + teamCalDO.getId() + " successfully created.");
    if (result != null) {
      final String json = JsonUtils.toJson(result);
      return Response.ok(json).build();
    } else {
      log.error("Something went wrong while creating team event");
      return Response.serverError().build();
    }
  }

  @PUT
  @Path(RestPaths.UPDATE)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateTeamEvent(final CalendarEventObject calendarEvent)
  {
    //The result for returning
    CalendarEventObject result = null;
    try {
      //Getting the calender at which the event will created/updated
      TeamCalDO teamCalDO = teamCalCache.getCalendar(calendarEvent.getCalendarId());
      //ICal4J CalendarBuilder for building calendar events from ics
      final CalendarBuilder builder = new CalendarBuilder();
      //Build the event from ics
      final net.fortuna.ical4j.model.Calendar calendar = builder.build(new ByteArrayInputStream(Base64.decodeBase64(calendarEvent.getIcsData())));
      //Getting the VEvent from ics
      final ComponentList<CalendarComponent> vevents = calendar.getComponents(Component.VEVENT);
      final VEvent event = (VEvent) vevents.get(0);
      //Geting the event uid
      Uid eventUid = event.getUid();
      //Building TeamEventDO from VEvent
      final TeamEventDO teamEvent = teamCalService.createTeamEventDO(event,
          TimeZone.getTimeZone(teamCalDO.getOwner().getTimeZone()));
      if (vevents.size() > 1) {
        VEvent event2 = (VEvent) vevents.get(1);
        if (event.getUid().equals(event2.getUid())) {
          //Set ExDate in event1
          teamEvent.addRecurrenceExDate(event2.getRecurrenceId().getDate(), teamEvent.getTimeZone());
          //Create new Event from event2
          saveVEvent(event2, teamCalDO);
        }
      }
      //Getting the origin team event from database by uid if exist
      TeamEventDO teamEventOrigin = teamEventService.findByUid(eventUid.getValue());
      //Check if db event exists
      if (teamEventOrigin == null) {
        log.error("No team event found with uid " + eventUid.getValue());
        log.info("Creating new team event!");
        return saveTeamEvent(calendarEvent);
      }
      //Set for origin attendees from db event
      Set<TeamEventAttendeeDO> originAttendees = new HashSet<>();
      //Setting the existing DB id, created timestamp, tenant
      teamEvent.setId(teamEventOrigin.getPk());
      teamEvent.setCreated(teamEventOrigin.getCreated());
      teamEvent.setTenant(teamEventOrigin.getTenant());
      //Save existing attendees from the db event
      originAttendees = teamEventOrigin.getAttendees();
      //Setting the calendar
      teamEvent.setCalendar(teamCalDO);
      //Setting uid
      teamEvent.setUid(eventUid.getValue());
      //Decide which attendees are new, which has to be deleted, which has to be updated
      Set<TeamEventAttendeeDO> attendeesToAssignMap = new HashSet<>();
      Set<TeamEventAttendeeDO> attendeesToUnassignMap = new HashSet<>();
      if (teamEvent.getAttendees() != null && teamEvent.getAttendees().size() > 0) {
        attendeesToAssignMap = getAttendeesToAssign(teamEvent, teamEventOrigin);
        attendeesToUnassignMap = getAttendeesToUnassign(teamEvent, teamEventOrigin);
        teamEvent.setAttendees(originAttendees);
      }
      //Save or update the generated event
      teamEventService.update(teamEvent);

      TeamEventDO teamEventAfterSaveOrUpdate = teamEventService.getById(teamEvent.getPk());
      ModificationStatus modificationStatus = ModificationStatus.NONE;
      modificationStatus = TeamEventDO.copyValues(teamEventOrigin, teamEventAfterSaveOrUpdate, "attendees");
      TeamEventDO teamEventAfterModificationTest = teamEventService.getById(teamEvent.getPk());
      //Update attendees
      teamEventService.assignAttendees(teamEventAfterModificationTest, attendeesToAssignMap, attendeesToUnassignMap);

      TeamEventDO teamEventAfterAssignAttendees = teamEventService.getById(teamEventAfterSaveOrUpdate.getPk());
      if ((attendeesToAssignMap != null && attendeesToAssignMap.size() > 0) || (modificationStatus != null && modificationStatus != ModificationStatus.NONE)) {
        teamEventService.sendTeamEventToAttendees(teamEventAfterAssignAttendees, false,
            true && modificationStatus != ModificationStatus.NONE, false, attendeesToAssignMap);
      }
      result = teamCalService.getEventObject(teamEventAfterAssignAttendees, true, true);
      log.info("Team event: " + teamEventAfterAssignAttendees.getSubject() + " for calendar #" + teamCalDO.getId() + " successfully updated.");
    } catch (Exception e) {
      log.error("Exception while updating team event", e);
      return Response.serverError().build();
    }
    if (result != null) {
      final String json = JsonUtils.toJson(result);
      return Response.ok(json).build();
    } else {
      log.error("Something went wrong while updating team event");
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
        teamEventService.sendTeamEventToAttendees(teamEventOrigin, false, false, true, null);
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
      Set<String> newEmailAdresses = new HashSet<>();
      newTeamEvent.getAttendees().forEach(att -> newEmailAdresses.add(att.getAddress() != null ? att.getAddress().getEmail() : att.getUrl()));
      Map<String, TeamEventAttendeeDO> originEmailAttendeeMap = new HashMap<>();
      originTeamEvent.getAttendees().forEach(att -> originEmailAttendeeMap.put(att.getAddress() != null ? att.getAddress().getEmail() : att.getUrl(), att));
      Set<TeamEventAttendeeDO> attendeesToUnassign = new HashSet<>();
      for (String originAttendeeEmail : originEmailAttendeeMap.keySet()) {
        if (newEmailAdresses.contains(originAttendeeEmail) == false) {
          result.add(originEmailAttendeeMap.get(originAttendeeEmail));
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
      Set<String> originEmailAdresses = new HashSet<>();
      originTeamEvent.getAttendees().forEach(att -> originEmailAdresses.add(att.getAddress() != null ? att.getAddress().getEmail() : att.getUrl()));
      Map<String, TeamEventAttendeeDO> newEmailAttendeeMap = new HashMap<>();
      newTeamEvent.getAttendees().forEach(att -> newEmailAttendeeMap.put(att.getAddress() != null ? att.getAddress().getEmail() : att.getUrl(), att));
      Set<TeamEventAttendeeDO> attendeesToUnassign = new HashSet<>();
      for (String newAttendeeEmail : newEmailAttendeeMap.keySet()) {
        if (originEmailAdresses.contains(newAttendeeEmail) == false) {
          TeamEventAttendeeDO newTeamEventAttendeeDO = newEmailAttendeeMap.get(newAttendeeEmail);
          newTeamEventAttendeeDO.setPk(null);
          result.add(newTeamEventAttendeeDO);
        }
      }
      return result;
    }
  }

}
