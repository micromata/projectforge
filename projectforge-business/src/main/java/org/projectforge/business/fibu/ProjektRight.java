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

package org.projectforge.business.fibu;

import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightServiceImpl;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Kai Reinhard (k.reinhard@me.de)
 */
public class ProjektRight extends UserRightAccessCheck<ProjektDO>
{
  private static final long serialVersionUID = -3712738266564403670L;

  public ProjektRight(AccessChecker accessChecker)
  {
    super(accessChecker, UserRightId.PM_PROJECT, UserRightCategory.PM,
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
    return accessChecker.isAvailable(user, UserRightId.PM_PROJECT);
  }

  @Override
  public boolean hasSelectAccess(final PFUserDO user, final ProjektDO obj)
  {
    if (obj == null) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.CONTROLLING_GROUP) == true) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.PROJECT_MANAGER,
        ProjectForgeGroup.PROJECT_ASSISTANT) == true) {
      Integer userId = user.getId();
      Integer headOfBusinessManagerId = obj.getHeadOfBusinessManager() != null ? obj.getHeadOfBusinessManager().getId() : null;
      Integer projectManagerId = obj.getProjectManager() != null ? obj.getProjectManager().getId() : null;
      Integer salesManageId = obj.getSalesManager() != null ? obj.getSalesManager().getId() : null;
      if (userId != null && (userId.equals(headOfBusinessManagerId) || userId.equals(projectManagerId) || userId.equals(salesManageId))) {
        return true;
      }

      final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
      if (obj.getProjektManagerGroup() != null
          && userGroupCache.isUserMemberOfGroup(ThreadLocalUserContext.getUserId(),
          obj.getProjektManagerGroupId()) == true) {
        if ((obj.getStatus() == null || obj.getStatus().isIn(ProjektStatus.ENDED) == false)
            && obj.isDeleted() == false) {
          // Ein Projektleiter sieht keine nicht aktiven oder gelöschten Projekte.
          return true;
        }
      }
      if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ORGA_TEAM,
          ProjectForgeGroup.FINANCE_GROUP) == true) {
        return accessChecker.hasReadAccess(user, getId(), false) == true;
      }
      return false;
    } else {
      return accessChecker.hasReadAccess(user, getId(), false) == true;
    }
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final ProjektDO obj, final ProjektDO oldObj,
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
  public boolean hasHistoryAccess(final PFUserDO user, final ProjektDO obj)
  {
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.CONTROLLING_GROUP) == true) {
      return true;
    }
    return accessChecker.hasRight(user, getId(), UserRightValue.READWRITE);
  }
}
