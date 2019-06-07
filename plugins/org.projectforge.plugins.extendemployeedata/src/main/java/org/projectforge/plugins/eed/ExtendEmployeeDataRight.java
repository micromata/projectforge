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

package org.projectforge.plugins.eed;

import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;

public class ExtendEmployeeDataRight extends UserRightAccessCheck<EmployeeConfigurationDO>
{
  public ExtendEmployeeDataRight(AccessChecker accessChecker)
  {
    super(accessChecker, ExtendEmployeeDataPluginUserRightId.PLUGIN_EXTENDEMPLOYEEDATA, UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
  }

  /**
   * @return true if the owner is equals to the logged-in user, otherwise false.
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final EmployeeConfigurationDO obj, final EmployeeConfigurationDO oldObj,
      final OperationType operationType)
  {
    return true;
    /*
    final EmployeeConfigurationDO timesheet = oldObj != null ? oldObj : obj;
    if (timesheet == null) {
      return true; // General insert and select access given by default.
    }*/
  }
}
