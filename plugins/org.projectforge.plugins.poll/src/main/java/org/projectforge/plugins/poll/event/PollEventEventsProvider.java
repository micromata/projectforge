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

import net.ftlines.wicket.fullcalendar.CalendarResponse;
import net.ftlines.wicket.fullcalendar.Event;
import net.ftlines.wicket.fullcalendar.callback.ClickedEvent;
import net.ftlines.wicket.fullcalendar.callback.DroppedEvent;
import net.ftlines.wicket.fullcalendar.callback.ResizedEvent;
import net.ftlines.wicket.fullcalendar.callback.SelectedRange;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.projectforge.plugins.poll.PollDO;
import org.projectforge.web.calendar.MyFullCalendarEventsProvider;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public class PollEventEventsProvider extends MyFullCalendarEventsProvider
{
  private static final long serialVersionUID = -1869612916168574011L;

  private final Map<PollEventDO, Event> pollEventCache;

  private final PollDO poll;

  private final Random randomizer;

  /**
   */
  public PollEventEventsProvider(final PollDO poll)
  {
    this.poll = poll;
    pollEventCache = new HashMap<>();
    randomizer = new Random(System.currentTimeMillis());
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#getEvents(org.joda.time.DateTime,
   *      org.joda.time.DateTime)
   */
  @Override
  public Collection<Event> getEvents(final DateTime start, final DateTime end)
  {
    events.clear();
    for (final PollEventDO iterationEvent : pollEventCache.keySet()) {
      Event event = pollEventCache.get(iterationEvent);
      if (event == null) {
        event = new Event();
        // randomizer is needed if the system adds events and the machine is able to add
        // more than one element per millisecond -> double entries for one id is not allowed!
        event.setId("" + (System.currentTimeMillis() % randomizer.nextInt()));
        event.setStart(new DateTime(iterationEvent.getStartDate().getTime()));
        event.setEnd(new DateTime(iterationEvent.getEndDate().getTime()));
        event.setTitle("");
        pollEventCache.put(iterationEvent, event);
      }
      events.put("" + event.getId(), event);
    }
    return events.values();
  }

  /**
   * Just use getEvents, no caching enabled at this page!
   * 
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime,
   *      org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end)
  {
    getEvents(start, end);
  }

  /**
   * @param range
   * @param response
   */
  public void addEvent(final SelectedRange range, final CalendarResponse response)
  {
    final PollEventDO newEvent = new PollEventDO();
    newEvent.setPoll(poll);
    newEvent.setStartDate(new Timestamp(range.getStart().getMillis()));
    newEvent.setEndDate(new Timestamp(range.getEnd().getMillis()));
    pollEventCache.put(newEvent, null);
    clearSelection(response);
  }

  /**
   * Clears the FullCalendar JS Selection and udpates the events
   * 
   * @param response
   */
  private void clearSelection(final CalendarResponse response)
  {
    if (response != null) {
      response.clearSelection().refetchEvents();
    }
  }

  /**
   * @param event
   * @param response
   * @return
   */
  public boolean resizeEvent(final ResizedEvent event, final CalendarResponse response)
  {
    return modifyEvent(event.getEvent(), null, event.getNewEndTime(), response);
  }

  /**
   * @param event
   * @param response
   * @return
   */
  public boolean dropEvent(final DroppedEvent event, final CalendarResponse response)
  {
    return modifyEvent(event.getEvent(), event.getNewStartTime(), event.getNewEndTime(), response);
  }

  /**
   * @param event
   * @param newEndTime
   * @param newStartTime
   * @param response
   * @return
   */
  private boolean modifyEvent(final Event event, final DateTime newStartTime, final DateTime newEndTime,
      final CalendarResponse response)
  {
    if (event != null) {
      final PollEventDO eventDO = searchById(event.getId());
      if (eventDO != null) {
        if (newStartTime != null) {
          eventDO.setStartDate(new Timestamp(newStartTime.getMillis()));
          event.setStart(newStartTime);
        }
        if (newEndTime != null) {
          eventDO.setEndDate(new Timestamp(newEndTime.getMillis()));
          event.setEnd(newEndTime);
        }
        clearSelection(response);
        return false;
      }
    }
    clearSelection(response);
    return true;
  }

  /**
   * 
   * @param event
   */
  public void removeElement(final PollEventDO event)
  {
    pollEventCache.remove(event);
  }

  /**
   * @param event
   * @param response
   */
  public void eventClicked(final ClickedEvent event, final CalendarResponse response)
  {
    final PollEventDO clickEvent = searchById(event.getEvent().getId());
    if (clickEvent != null) {
      // TODO remove when side bar is ready
      pollEventCache.remove(clickEvent);
    }
    clearSelection(response);
  }

  private PollEventDO searchById(final String id)
  {
    PollEventDO result = null;
    Event temp = null;
    for (final PollEventDO key : pollEventCache.keySet()) {
      temp = pollEventCache.get(key);
      if (temp != null && StringUtils.equals(temp.getId(), id)) {
        result = key;
        break;
      }
    }
    return result;
  }

  public Collection<PollEventDO> getAllEvents()
  {
    return pollEventCache.keySet();
  }

  /**
   * @param pollEvent
   * @return
   */
  public Event getEventForPollEvent(final PollEventDO pollEvent)
  {
    return pollEventCache.get(pollEvent);
  }
}
