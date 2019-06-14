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

package org.projectforge.business.humanresources;

import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightServiceImpl;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * 
 * @author Kai Reinhard (k.reinhard@me.de)
 * 
 */
public class HRPlanningRight extends UserRightAccessCheck<HRPlanningDO>
{
  private static final long serialVersionUID = 3318798287641861759L;

  public HRPlanningRight(AccessChecker accessChecker)
  {
    super(accessChecker, UserRightId.PM_HR_PLANNING, UserRightCategory.PM,
        UserRightServiceImpl.FALSE_READONLY_READWRITE);
    initializeUserGroupsRight(UserRightServiceImpl.FALSE_READONLY_READWRITE, UserRightServiceImpl.FIBU_ORGA_PM_GROUPS)
        // All project managers have read write access:
        .setAvailableGroupRightValues(ProjectForgeGroup.PROJECT_MANAGER, UserRightValue.READWRITE)
        // All project assistants have no, read or read-write access:
        .setAvailableGroupRightValues(ProjectForgeGroup.PROJECT_ASSISTANT,
            UserRightServiceImpl.FALSE_READONLY_READWRITE)
        // Read only access for controlling users:
        .setReadOnlyForControlling();
  }

  /**
   * @return true.
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.access.AccessChecker,
   *      org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user)
  {
    return true;
  }

  @Override
  public boolean hasSelectAccess(final PFUserDO user, final HRPlanningDO obj)
  {
    if (accessChecker.userEquals(user, obj.getUser()) == true) {
      return true;
    }
    return accessChecker.hasRight(user, getId(), UserRightValue.READONLY, UserRightValue.READWRITE);
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final HRPlanningDO obj, final HRPlanningDO oldObj,
      final OperationType operationType)
  {
    return accessChecker.hasRight(user, getId(), UserRightValue.READWRITE);
  }

  /**
   * History access only allowed for users with read and/or write access.
   * 
   * @see org.projectforge.business.user.UserRightAccessCheck#hasHistoryAccess(java.lang.Object)
   */
  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final HRPlanningDO obj)
  {
    return accessChecker.hasRight(user, getId(), UserRightValue.READONLY, UserRightValue.READWRITE);
  }
}
