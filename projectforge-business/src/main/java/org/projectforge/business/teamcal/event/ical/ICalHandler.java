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

package org.projectforge.business.teamcal.event.ical;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.diff.TeamEventDiffType;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.property.Method;

public class ICalHandler
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ICalHandler.class);

  private TeamEventService eventService;
  private ICalParser parser;
  private TeamCalDO defaultCalendar;

  private List<EventHandle> singleEventHandles;
  private Map<String, RecurringEventHandle> recurringHandles;
  private List<TeamEventAttendeeDO> attendeesFromDbList;

  public ICalHandler(final TeamEventService eventService, final TeamCalDO defaultCalendar)
  {
    this.eventService = eventService;
    this.parser = ICalParser.parseAllFields();
    this.defaultCalendar = defaultCalendar;

    this.singleEventHandles = new ArrayList<>();
    this.recurringHandles = new HashMap<>();
    this.attendeesFromDbList = null;
  }

  public boolean readICal(final InputStream iCalStream, final HandleMethod method)
  {
    return this.readICal(new InputStreamReader(iCalStream), method);
  }

  public boolean readICal(final String iCalString, final HandleMethod method)
  {
    return this.readICal(new StringReader(iCalString), method);
  }

  public boolean readICal(final Reader iCalReader, final HandleMethod handleMethod)
  {
    // parse iCal
    boolean result = parser.parse(iCalReader);

    if (result == false) {
      log.warn("ICal file could not be parsed");
      return false;
    }

    // check if ical contains vEvents
    List<TeamEventDO> parsedEvents = parser.getExtractedEvents();
    if (parsedEvents.isEmpty()) {
      return true;
    }

    // handle method
    final HandleMethod method = this.readMethod(handleMethod);

    // handle files
    for (TeamEventDO event : parsedEvents) {
      if (event.getRecurrenceRule() == null && event.getRecurrenceReferenceId() == null) {
        // single event
        this.singleEventHandles.add(new EventHandle(event, this.defaultCalendar, method));
      } else {
        // recurring event
        RecurringEventHandle handle = this.recurringHandles.get(event.getUid());

        if (handle == null) {
          handle = new RecurringEventHandle(null, this.defaultCalendar, method);
          this.recurringHandles.put(event.getUid(), handle);
        }

        if (event.getRecurrenceReferenceId() == null) {
          // set master event
          handle.setEvent(event);
        } else {
          // add additional event
          handle.getRelatedEvents().add(new EventHandle(event, this.defaultCalendar, method));
        }
      }
    }

    return true;
  }

  public boolean validate()
  {
    boolean error = false;

    for (EventHandle eventHandle : singleEventHandles) {
      this.validate(eventHandle);
      error = error || (eventHandle.getErrors().isEmpty() == false);
    }

    // TODO improve handling of recurring events!!!!
    for (RecurringEventHandle eventHandle : recurringHandles.values()) {
      this.validate(eventHandle);
      error = error || (eventHandle.getErrors().isEmpty() == false);

      // TODO additional events should be exdates or similar of the main event!
      for (EventHandle additionalEvent : eventHandle.getRelatedEvents()) {
        additionalEvent.getEvent().setUid(null); // TODO remove this line when uid constraint is fixed
        this.validate(additionalEvent);
      }
    }

    return error == false;
  }

  public void persist(final boolean ignoreWarnings)
  {
    for (EventHandle eventHandle : singleEventHandles) {
      this.persist(eventHandle, ignoreWarnings);
    }

    // TODO improve handling of recurring events!!!!
    for (RecurringEventHandle eventHandle : recurringHandles.values()) {
      TeamEventDO recurringEvent = eventHandle.getEvent();

      for (EventHandle additionalEvent : eventHandle.getRelatedEvents()) {
        this.persist(additionalEvent, ignoreWarnings);

        // TODO remove this after handling of recurring is fixed!
        if (recurringEvent != null) {
          String exDates = recurringEvent.getRecurrenceExDate();

          if (exDates != null && exDates.length() > 4) {
            exDates += ",";
          } else {
            exDates = "";
          }

          recurringEvent.setRecurrenceExDate(exDates + additionalEvent.getEvent().getRecurrenceReferenceId());
        }
      }

      // check if main recurring event is present
      if (recurringEvent != null) {
        this.persist(eventHandle, ignoreWarnings);
      }
    }
  }

  private void validate(final EventHandle eventHandle)
  {
    final TeamEventDO event = eventHandle.getEvent();
    final TeamCalDO calendar = eventHandle.getCalendar();

    // clear errors & warnings
    eventHandle.getErrors().clear();
    eventHandle.getWarnings().clear();

    // check calender
    if (calendar == null) {
      eventHandle.setEventInDB(null);
      eventHandle.addError(EventHandleError.CALANDER_NOT_SPECIFIED);
      return;
    }

    // check method
    if (eventHandle.getMethod() == null) {
      eventHandle.addError(EventHandleError.NO_METHOD_SELECTED);
      return;
    }

    // check event
    if (event == null) {
      eventHandle.addWarning(EventHandleError.WARN_MAIN_RECURRING_EVENT_MISSING);
      return;
    }

    // check db
    TeamEventDO eventInDB;
    switch (eventHandle.getMethod()) {
      case ADD_UPDATE:
        eventInDB = eventService.findByUid(calendar.getId(), event.getUid(), false);
        eventHandle.setEventInDB(eventInDB);
        if (eventInDB != null && eventInDB.getDtStamp() != null && eventInDB.getDtStamp().after(event.getDtStamp())) {
          eventHandle.addWarning(EventHandleError.WARN_OUTDATED);
        }
        break;

      case CANCEL:
        eventInDB = eventService.findByUid(calendar.getId(), event.getUid(), true);
        eventHandle.setEventInDB(eventInDB);
        if (eventInDB == null) {
          eventHandle.addWarning(EventHandleError.WARN_EVENT_TO_DELETE_NOT_FOUND);
        }
        break;
    }
  }

  private void persist(final EventHandle eventHandle, final boolean ignoreWarnings)
  {
    // persist is not possible if errors exists
    if (eventHandle.isValid(ignoreWarnings) == false) {
      return;
    }

    try {
      switch (eventHandle.getMethod()) {
        case ADD_UPDATE:
          this.saveOrUpdate(eventHandle);
          break;
        case CANCEL:
          this.delete(eventHandle);
          break;
      }
    } catch (Exception e) {
      log.error(String.format("An error occurred while persist event with uid '%s'", eventHandle.getEvent().getUid()), e);
    }
  }

  private void saveOrUpdate(final EventHandle eventHandle)
  {
    final TeamEventDO event = eventHandle.getEvent();
    final TeamEventDO eventInDB = eventHandle.getEventInDB();

    // set calendar
    event.setCalendar(eventHandle.getCalendar());

    // fix attendees
    this.fixAttendees(eventHandle);

    if (eventInDB != null) {
      // event exists, update metadata
      event.setId(eventInDB.getPk());
      event.setCreated(eventInDB.getCreated());
      event.setLastUpdate();
      event.setTenant(eventInDB.getTenant());
      event.setCreator(eventInDB.getCreator());

      final boolean isDeleted = eventInDB.isDeleted();
      if (isDeleted) {
        // event is deleted, restore
        eventService.undelete(eventInDB);
      }

      // update attendees & event
      eventService.updateAttendees(event, eventInDB.getAttendees());
      eventService.update(event);

      // send notification mail
      if (isDeleted) {
        eventService.checkAndSendMail(event, TeamEventDiffType.NEW);
      } else {
        eventService.checkAndSendMail(event, eventInDB);
      }
    } else {
      // save attendee list, assign after saving the event
      Set<TeamEventAttendeeDO> attendees = new HashSet<>();
      event.getAttendees().forEach(att -> attendees.add(att.clone()));
      event.setAttendees(null);

      // save event & set attendees
      eventService.save(event);
      eventService.assignAttendees(event, attendees, null);

      // send notification mail
      eventService.checkAndSendMail(event, TeamEventDiffType.NEW);
    }
  }

  private void delete(final EventHandle eventHandle)
  {
    final TeamEventDO event = eventHandle.getEventInDB();

    if (event == null)
      return;

    eventService.markAsDeleted(event);
    eventService.checkAndSendMail(event, TeamEventDiffType.DELETED);
  }

  private void fixAttendees(final EventHandle eventHandle)
  {
    if (eventHandle.getEvent() == null) {
      return;
    }

    //    if (attendeesFromDbList == null) {
    //      attendeesFromDbList = eventService.getAddressesAndUserAsAttendee();
    //    }

    eventService.fixAttendees(eventHandle.getEvent());
  }

  private HandleMethod readMethod(final HandleMethod expectedMethod)
  {
    Method methodIcal = parser.getMethod();

    if (methodIcal == null && expectedMethod == null) {
      log.warn("No method defined and ICal does not contain a method");
      return null;
    }

    final HandleMethod method;
    if (Method.CANCEL.equals(methodIcal)) {
      method = HandleMethod.CANCEL;
    } else if (Method.REQUEST.equals(methodIcal)) {
      method = HandleMethod.ADD_UPDATE;
    } else if (Method.REFRESH.equals(methodIcal)) {
      method = null;
    } else if (Method.ADD.equals(methodIcal)) {
      method = HandleMethod.ADD_UPDATE;
    } else if (Method.COUNTER.equals(methodIcal)) {
      method = null;
    } else if (Method.DECLINE_COUNTER.equals(methodIcal)) {
      method = null;
    } else if (Method.PUBLISH.equals(methodIcal)) {
      method = HandleMethod.ADD_UPDATE;
    } else if (Method.REPLY.equals(methodIcal)) {
      method = null;
    } else {
      if (methodIcal != null) {
        log.warn(String.format("Unknown method in ICal: '%s'", methodIcal));
      }
      method = null;
    }

    if (expectedMethod != null && method != null && expectedMethod != method) {
      log.warn(String.format("Expected method '%s' is overridden by method from iCal '%s'", expectedMethod.name(), methodIcal.getValue()));
    }

    return method == null ? expectedMethod : method;
  }

  public TeamEventDO getFirstResult()
  {
    if (this.isEmpty()) {
      return null;
    }

    if (this.singleEventHandles.isEmpty() == false) {
      return this.singleEventHandles.get(0).getEvent();
    } else {
      return this.recurringHandles.values().iterator().next().getEvent();
    }
  }

  public boolean isEmpty()
  {
    return this.singleEventHandles.isEmpty() && this.recurringHandles.isEmpty();
  }

  public int eventCount()
  {
    return this.singleEventHandles.size() + this.recurringHandles.size();
  }

  public TeamCalDO getDefaultCalendar()
  {
    return defaultCalendar;
  }

  public void setDefaultCalendar(final TeamCalDO defaultCalendar)
  {
    this.defaultCalendar = defaultCalendar;
  }

  public List<EventHandle> getSingleEventHandles()
  {
    return singleEventHandles;
  }

  public Map<String, RecurringEventHandle> getRecurringHandles()
  {
    return recurringHandles;
  }

}
