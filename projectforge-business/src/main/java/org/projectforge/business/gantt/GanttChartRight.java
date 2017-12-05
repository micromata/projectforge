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

package org.projectforge.business.gantt;

import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * 
 * @author Kai Reinhard (k.reinhard@me.de)
 * 
 */
public class GanttChartRight extends UserRightAccessCheck<GanttChartDO>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GanttChartRight.class);

  private static final long serialVersionUID = -1711148447929915434L;

  public GanttChartRight(AccessChecker accessChecker)
  {
    super(accessChecker, UserRightId.PM_GANTT, UserRightCategory.PM);
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

  /**
   * If the user is owner of the GanttChartDO he has access, otherwise he needs at least select access to the root task.
   * For project managers the user must be additional of the group of the project manager group (assigned to this task)
   * or if no project manager group is available for this task the user should be a member of
   * {@link ProjectForgeGroup#PROJECT_MANAGER}.
   * 
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(java.lang.Object)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final GanttChartDO obj)
  {
    if (obj == null) {
      return false;
    }
    return hasAccess(user, obj, obj.getReadAccess());
  }

  /**
   * If the user is owner of the GanttChartDO he has access, otherwise he needs at least select access to the root task.
   * For project managers the user must be additional of the group of the project manager group (assigned to this task)
   * or if no project manager group is available for this task the user should be a member of
   * {@link ProjectForgeGroup#PROJECT_MANAGER}.
   * 
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(java.lang.Object)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final GanttChartDO obj, final GanttChartDO oldObj,
      final OperationType operationType)
  {
    if (obj == null) {
      return false;
    }
    final GanttChartDO gc = oldObj != null ? oldObj : obj;
    return hasAccess(user, gc, gc.getWriteAccess());
  }

  @Override
  public boolean hasInsertAccess(final PFUserDO user)
  {
    return true;
  }

  private boolean hasAccess(final PFUserDO user, final GanttChartDO obj, final GanttAccess access)
  {
    if (accessChecker.userEqualsToContextUser(obj.getOwner()) == true) {
      // Owner has always access:
      return true;
    }
    if (access == null || access == GanttAccess.OWNER) {
      // No access defined, so only owner has access:
      return false;
    }
    if (access.isIn(GanttAccess.ALL, GanttAccess.PROJECT_MANAGER) == true) {
      if (obj.getTask() == null) {
        // Task needed for these GanttAccess types, so no access:
        return false;
      }
      if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASKS, OperationType.SELECT,
          false) == false) {
        // User has no task access:
        return false;
      }
      if (access == GanttAccess.ALL) {
        // User has task access:
        return true;
      }
      final TaskTree taskTree = TaskTreeHelper.getTaskTree();
      final ProjektDO project = taskTree.getProjekt(obj.getTaskId());
      if (project == null) {
        // Project manager group not found:
        return accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.PROJECT_MANAGER);
      }
      // Users of the project manager group have access:
      final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
      return userGroupCache.isUserMemberOfGroup(user, project.getProjektManagerGroupId());
    } else {
      log.error("Unsupported GanttAccess type: " + access);
    }
    return false;

  }
}
