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

package org.projectforge.business.user;

import java.io.Serializable;
import java.util.Comparator;

import org.projectforge.framework.persistence.user.entities.PFUserDO;

public class UsersComparator implements Comparator<PFUserDO>, Serializable
{
  private static final long serialVersionUID = -8760753482884481204L;

  /**
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(final PFUserDO u1, final PFUserDO u2)
  {
    final String n1 = u1 != null && u1.getFullname() != null ? u1.getFullname().toLowerCase() : "";
    final String n2 = u2 != null && u2.getFullname() != null ? u2.getFullname().toLowerCase() : "";
    return n1.compareTo(n2);
  }
}
