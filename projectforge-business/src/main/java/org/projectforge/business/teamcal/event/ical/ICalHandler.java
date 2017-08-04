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
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ICalHandler.class);

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
    return this.readIcal(new InputStreamReader(iCalStream), method);
  }

  public boolean readICal(final String iCalString, final HandleMethod method)
  {
    return this.readIcal(new StringReader(iCalString), method);
  }

  public boolean readIcal(final Reader iCalReader, final HandleMethod handleMethod)
  {
    // parse ical
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
        }

        if (event.getRecurrenceRule() != null) {
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

  public boolean processAll()
  {
    boolean error = false;

    for (EventHandle eventHandle : singleEventHandles) {
      try {
        switch (eventHandle.getMethod()) {
          case ADD_UPDATE:
            this.saveOrUpdateEvent(eventHandle);
            break;
          case REMOVE:
            this.deleteEvent(eventHandle);
            break;
        }

        if (eventHandle.getErrors().isEmpty() == false) {
          error = true;
        }
      } catch (Exception e) {
        error = true;
        log.error(String.format("An error occurred while processing the event with uid '%s'", eventHandle.getEvent().getUid()), e);
      }
    }

    // TODO improve handling of recurring events!!!!
    for (RecurringEventHandle eventHandle : recurringHandles.values()) {
      if (eventHandle.getEvent() == null) {
        continue;
      }

      try {
        switch (eventHandle.getMethod()) {
          case ADD_UPDATE:
            this.saveOrUpdateEvent(eventHandle);
            for (EventHandle additionalEvent : eventHandle.getRelatedEvents()) {
              additionalEvent.getEvent().setUid(null);
              this.saveOrUpdateEvent(additionalEvent); // TODO send mail ?
            }
            break;
          case REMOVE:
            this.deleteEvent(eventHandle);
            //            for (EventHandle additionalEvent : eventHandle.getRelatedEvents()) {
            //              this.deleteEvent(additionalEvent); // TODO send mail
            //            }
            break;
        }

        if (eventHandle.getErrors().isEmpty() == false) {
          error = true;
        }
      } catch (Exception e) {
        error = true;
        log.error(String.format("An error occurred while processing the event with uid '%s'", eventHandle.getEvent().getUid()), e);
      }
    }

    return error == false;
  }

  private void saveOrUpdateEvent(final EventHandle eventHandle)
  {
    TeamEventDO event = eventHandle.getEvent();

    if (event == null) {
      return;
    }

    // try getting the event from database for uid
    final TeamEventDO eventDB;
    if (eventHandle.getCalendar() != null) {
      eventDB = eventService.findByUid(eventHandle.getCalendar().getId(), event.getUid(), false);
      event.setCalendar(eventHandle.getCalendar());
    } else {
      eventDB = null;
      eventHandle.addError(EventHandleError.CALANDER_NOT_SPECIFIED);
    }

    // fix attendees
    this.fixAttendees(eventHandle);

    if (eventDB != null) {
      // event exists, update metadata
      event.setId(eventDB.getPk());
      event.setCreated(eventDB.getCreated());
      event.setLastUpdate();
      event.setTenant(eventDB.getTenant());
      event.setCreator(eventDB.getCreator());

      final boolean isDeleted = eventDB.isDeleted();
      if (isDeleted) {
        // event is deleted, restore
        eventService.undelete(eventDB);
      }

      // update attendees & event
      eventService.updateAttendees(event, eventDB.getAttendees());
      eventService.update(event);

      // send notification mail
      if (isDeleted) {
        eventService.checkAndSendMail(event, TeamEventDiffType.NEW);
      } else {
        eventService.checkAndSendMail(event, eventDB);
      }
    } else {
      // save attendee list, because assignment later
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

  private void deleteEvent(final EventHandle eventHandle)
  {
    TeamEventDO event = eventService.findByUid(eventHandle.getCalendar().getId(), eventHandle.getEvent().getUid(), true);
    if (event != null) {
      eventService.markAsDeleted(event);
      eventService.checkAndSendMail(event, TeamEventDiffType.DELETED);
    } else {
      eventHandle.addError(EventHandleError.EVENT_NOT_FOUND);
    }
  }

  private HandleMethod readMethod(final HandleMethod expectedMethod)
  {
    if (expectedMethod != null) {
      return expectedMethod; // TODO
    }

    Method methodIcal = parser.getMethod();

    if (methodIcal == null) {
      log.warn("No method defined and ICal does not contain a method");
      return null;
    }

    final HandleMethod method;
    if (Method.CANCEL.equals(methodIcal)) {
      method = HandleMethod.REMOVE;
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
      log.warn(String.format("Unknown method in ICal: '%s'", methodIcal.getValue()));
      method = null;
    }
    // TODO

    return method;
  }

  public TeamEventDO getFirstResult()
  {
    if (this.isEmpty()) {
      return null;
    }

    if (this.singleEventHandles.isEmpty() == false) {
      return this.singleEventHandles.get(0).getEvent();
    } else {
      return this.recurringHandles.get(0).getEvent();
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
}
