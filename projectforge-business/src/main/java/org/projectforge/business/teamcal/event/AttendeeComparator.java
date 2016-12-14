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

package org.projectforge.business.teamcal.event;

import java.io.Serializable;
import java.util.Comparator;

import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;

public class AttendeeComparator implements Comparator<TeamEventAttendeeDO>, Serializable
{
  private static final long serialVersionUID = -8760753482884481226L;

  /**
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(final TeamEventAttendeeDO g1, final TeamEventAttendeeDO g2)
  {
    if (g1 != null && g2 != null && g1.getAddress() != null && g2.getAddress() != null) {
      return g1.getAddress().getFullName().compareTo(g2.getAddress().getFullName());
    }
    final String n1 = g1 != null && g1.getUrl() != null ? g1.getUrl().toLowerCase() : "";
    final String n2 = g2 != null && g2.getUrl() != null ? g2.getUrl().toLowerCase() : "";
    return n1.compareTo(n2);
  }
}
