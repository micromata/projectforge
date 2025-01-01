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

package org.projectforge.web.timesheet;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public class TimesheetEditFilter implements Serializable
{
  private static final long serialVersionUID = -7685135320311389741L;

  private final List<String> ignoredLocations;

  /**
   * 
   */
  public TimesheetEditFilter()
  {
    ignoredLocations = new LinkedList<String>();
  }

  public List<String> getIgnoredLocations()
  {
    return ignoredLocations;
  }

  public void addIgnoredLocation(final String location)
  {
    if(ignoredLocations.contains(location) == false) {
      ignoredLocations.add(location);
    }
  }

  public void removeIgnoredLocation(final String location)
  {
    if (ignoredLocations.contains(location)) {
      ignoredLocations.remove(location);
    }
  }
}
