/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import net.ftlines.wicket.fullcalendar.FullCalendar;

public class MyFullCalendar extends FullCalendar
{
  /**
   * @param id
   * @param config
   */
  public MyFullCalendar(final String id, final MyFullCalendarConfig config)
  {
    super(id, config);
  }

  /**
   * @see net.ftlines.wicket.fullcalendar.FullCalendar#getConfig()
   */
  @Override
  public MyFullCalendarConfig getConfig()
  {
    return (MyFullCalendarConfig)super.getConfig();
  }

  private static final long serialVersionUID = -8152563244661679227L;
}
