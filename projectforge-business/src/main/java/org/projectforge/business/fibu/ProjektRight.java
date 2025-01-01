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

package org.projectforge.business.fibu;

import org.projectforge.business.user.*;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WicketSupport;

/**
 * @author Kai Reinhard (k.reinhard@me.de)
 */
public class ProjektRight extends UserRightAccessCheck<ProjektDO>
{
  private static final long serialVersionUID = -3712738266564403670L;

  public ProjektRight()
  {
    super(UserRightId.PM_PROJECT, UserRightCategory.PM,
        UserRightServiceImpl.FALSE_READONLY_READWRITE);
    initializeUserGroupsRight(UserRightServiceImpl.FALSE_READONLY_READWRITE, UserRightServiceImpl.FIBU_ORGA_PM_GROUPS)
        // All project managers have read only access:
        .setAvailableGroupRightValues(ProjectForgeGroup.PROJECT_MANAGER, UserRightValue.READONLY)
        // All project assistants have no, read or read-only access:
        .setAvailableGroupRightValues(ProjectForgeGroup.PROJECT_ASSISTANT, UserRightValue.READONLY)
        // Read only access for controlling users:
        .setReadOnlyForControlling();
  }

  /**
   * @return True, if {@link UserRightId#PM_PROJECT} is potentially available for the user (independent from the
   * configured value).
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.access.AccessChecker,
   * org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user)
  {
    return WicketSupport.getAccessChecker().isAvailable(user, UserRightId.PM_PROJECT);
  }

  @Override
  public boolean hasSelectAccess(final PFUserDO user, final ProjektDO obj)
  {
    if (obj == null) {
      return true;
    }
    var accessChecker = WicketSupport.getAccessChecker();
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.CONTROLLING_GROUP)) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.PROJECT_MANAGER,
        ProjectForgeGroup.PROJECT_ASSISTANT)) {
      Long userId = user.getId();
      Long headOfBusinessManagerId = obj.getHeadOfBusinessManager() != null ? obj.getHeadOfBusinessManager().getId() : null;
      Long projectManagerId = obj.getProjectManager() != null ? obj.getProjectManager().getId() : null;
      Long salesManageId = obj.getSalesManager() != null ? obj.getSalesManager().getId() : null;
      if (userId != null && (userId.equals(headOfBusinessManagerId) || userId.equals(projectManagerId) || userId.equals(salesManageId))) {
        return true;
      }

      final UserGroupCache userGroupCache = UserGroupCache.getInstance();
      if (obj.getProjektManagerGroup() != null
          && userGroupCache.isUserMemberOfGroup(ThreadLocalUserContext.getLoggedInUserId(),
          obj.getProjektManagerGroupId())) {
        if ((obj.getStatus() == null || !obj.getStatus().isIn(ProjektStatus.ENDED))
            && !obj.getDeleted()) {
          // Ein Projektleiter sieht keine nicht aktiven oder gelöschten Projekte.
          return true;
        }
      }
      if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ORGA_TEAM,
          ProjectForgeGroup.FINANCE_GROUP)) {
        return accessChecker.hasReadAccess(user, getId(), false);
      }
      return false;
    } else {
      return accessChecker.hasReadAccess(user, getId(), false);
    }
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final ProjektDO obj, final ProjektDO oldObj,
      final OperationType operationType)
  {
    return WicketSupport.getAccessChecker().hasRight(user, getId(), UserRightValue.READWRITE);
  }

  /**
   * History access only allowed for users with read and/or write access.
   *
   * @see org.projectforge.business.user.UserRightAccessCheck#hasHistoryAccess(java.lang.Object)
   */
  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final ProjektDO obj)
  {
    var accessChecker = WicketSupport.getAccessChecker();
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.CONTROLLING_GROUP)) {
      return true;
    }
    return accessChecker.hasRight(user, getId(), UserRightValue.READWRITE);
  }
}
