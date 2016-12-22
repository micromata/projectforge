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

package org.projectforge.business.fibu;

import java.io.Serializable;
import java.util.Comparator;

public class EmployeeComparator implements Comparator<EmployeeDO>, Serializable
{
  private static final long serialVersionUID = -8760753482884481226L;

  /**
   * @see Comparator#compare(Object, Object)
   */
  @Override
  public int compare(final EmployeeDO g1, final EmployeeDO g2)
  {
    return g1.getPk().compareTo(g2.getPk());
  }
}
