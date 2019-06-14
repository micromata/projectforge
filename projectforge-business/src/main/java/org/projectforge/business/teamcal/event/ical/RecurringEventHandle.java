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

import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

public class RecurringEventHandle extends EventHandle
{
  private List<EventHandle> relatedEvents;

  public RecurringEventHandle(final TeamEventDO event, final TeamCalDO calendar, final HandleMethod method)
  {
    super(event, calendar, method);
    this.relatedEvents = new ArrayList<>();
  }

  public List<EventHandle> getRelatedEvents()
  {
    return this.relatedEvents;
  }
}
