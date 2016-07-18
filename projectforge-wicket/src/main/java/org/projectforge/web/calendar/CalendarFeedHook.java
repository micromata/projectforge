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

import java.util.List;
import java.util.Map;

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VEvent;

/**
 * @author Johannes Unterstein(j.unterstein@micromata.de)
 * 
 */
public interface CalendarFeedHook
{

  /**
   * @param params
   * @param timezone The time zone of the ics framework (build from {@link ThreadLocalUserContext#getTimeZone()}.
   * @param cal
   */
  public List<VEvent> getEvents(final Map<String, String> params, TimeZone timeZone);

}
