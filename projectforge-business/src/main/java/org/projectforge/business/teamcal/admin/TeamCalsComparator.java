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

package org.projectforge.business.teamcal.admin;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;

import java.io.Serializable;
import java.util.Comparator;

public class TeamCalsComparator implements Comparator<TeamCalDO>, Serializable
{
  private static final long serialVersionUID = -1458885789659898564L;

  /**
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(final TeamCalDO g1, final TeamCalDO g2)
  {
    final String n1 = g1 != null && g1.getTitle() != null ? g1.getTitle().toLowerCase() : "";
    final String n2 = g2 != null && g2.getTitle() != null ? g2.getTitle().toLowerCase() : "";
    return n1.compareTo(n2);
  }
}
