package org.projectforge.business.teamcal.event.ical;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

public class ICalHandler
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ICalHandler.class);

  private List<EventHandle> eventHandles;
  private List<EventHandle> recurringHandles;
  private ICalParser parser;
  private TeamEventService eventService;

  public ICalHandler(final TeamEventService eventService)
  {
    this.eventHandles = new ArrayList<>();
    this.parser = ICalParser.parseAllFields();
    this.eventService = eventService;
  }

  public boolean handle(final String icalString)
  {
    // add ical to parser
    boolean result = parser.parse(icalString);

    if (result == false) {
      log.warn("ICal file could not be parsed");
      return false;
    }

    List<TeamEventDO> parsedEvents = parser.getExtractedEvents();
    if (parsedEvents.isEmpty()) {
      return true;
    }

    // handle files
    for (TeamEventDO event : parsedEvents) {
      final EventHandle eventHandle = new EventHandle(event);
      this.eventHandles.add(eventHandle);
    }

    // TODO handle recurring events!

    return false;
  }

  public boolean processAll()
  {

    return false;
  }
}
