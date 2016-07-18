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

package org.projectforge.web.calendar;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;

import net.ftlines.wicket.fullcalendar.Event;
import net.ftlines.wicket.fullcalendar.EventNotFoundException;
import net.ftlines.wicket.fullcalendar.EventProvider;

/**
 * Creates events for FullCalendar.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class MyFullCalendarEventsProvider implements EventProvider
{
  private static final long serialVersionUID = 4530847672654878127L;

  protected final Map<String, Event> events = new HashMap<String, Event>();

  protected DateTime lastStart, lastEnd;

  /**
   */
  public MyFullCalendarEventsProvider()
  {
  }

  protected abstract void buildEvents(DateTime start, DateTime end);

  /**
   * Force {@link #buildEvents(DateTime, DateTime)} on next call of {@link #getEvents(DateTime, DateTime)}.
   */
  public void forceReload()
  {
    this.lastStart = this.lastEnd = null;
  }

  @Override
  public Collection<Event> getEvents(final DateTime start, final DateTime end)
  {
    if (this.lastStart != null
        && this.lastEnd != null
        && DateHelper.isSameDay(this.lastStart, start) == true
        && DateHelper.isSameDay(this.lastEnd, end) == true) {
      // Nothing to be done.
      return events.values();
    }
    events.clear();
    buildEvents(start, end);
    this.lastStart = start;
    this.lastEnd = end;
    return events.values();
  }

  public void resetEventCache()
  {
    this.lastStart = null;
    this.lastEnd = null;
  }

  @Override
  public Event getEventForId(final String id) throws EventNotFoundException
  {
    final Event event = events.get(id);
    if (event != null) {
      return event;
    }
    throw new EventNotFoundException("Event with id: " + id + " not found");
  }

  protected String getString(final String key)
  {
    return ThreadLocalUserContext.getLocalizedString(key);
  }
}
