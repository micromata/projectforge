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

package org.projectforge.business.multitenancy;

import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.utils.StringComparator;

import java.io.Serializable;
import java.util.Comparator;

public class TenantsComparator implements Comparator<TenantDO>, Serializable
{
  private static final long serialVersionUID = -8061714560844557586L;

  /**
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(final TenantDO t1, final TenantDO t2)
  {
    final String n1 = t1 != null ? t1.getName() : null;
    final String n2 = t2 != null ? t2.getName() : null;
    return StringComparator.compare(n1, n2);
  }
}
