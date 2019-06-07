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

package org.projectforge.business.user;

import java.io.Serializable;
import java.util.Comparator;

import org.projectforge.framework.persistence.user.entities.GroupDO;

public class GroupsComparator implements Comparator<GroupDO>, Serializable
{
  private static final long serialVersionUID = -8760753482884481204L;

  /**
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(final GroupDO g1, final GroupDO g2)
  {
    final String n1 = g1 != null && g1.getName() != null ? g1.getName().toLowerCase() : "";
    final String n2 = g2 != null && g2.getName() != null ? g2.getName().toLowerCase() : "";
    return n1.compareTo(n2);
  }
}
