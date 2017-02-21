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

package org.projectforge.plugins.todo;

import org.apache.commons.lang.ObjectUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Every user has access to own to-do's or to-do's he's assigned to. All other users have access if the to-do is
 * assigned to a task and the user has the task access.
 * 
 * @author Kai Reinhard (k.reinhard@me.de)
 * 
 */
public class ToDoRight extends UserRightAccessCheck<ToDoDO>
{
  private static final long serialVersionUID = -2928342166476350773L;

  public ToDoRight(AccessChecker accessChecker)
  {
    super(accessChecker, TodoPluginUserRightId.PLUGIN_TODO, UserRightCategory.PLUGINS, UserRightValue.TRUE);
  }

  /**
   * General select access.
   * 
   * @return true
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user)
  {
    return true;
  }

  /**
   * @return true if user is assignee or reporter. If not, the task access is checked.
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final ToDoDO obj)
  {
    return hasAccess(user, obj, OperationType.SELECT);
  }

  /**
   * General insert access.
   * 
   * @return true
   * @see org.projectforge.business.user.UserRightAccessCheck#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user)
  {
    return true;
  }

  /**
   * If user is not reporter or assignee and task is given the access to task is assumed, meaning if the user has the
   * right to insert sub tasks he is allowed to insert to-do's to.
   * 
   * @see org.projectforge.business.user.UserRightAccessCheck#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user, final ToDoDO obj)
  {
    return hasAccess(user, obj, OperationType.INSERT);
  }

  /**
   * If user is not reporter or assignee and task is given the access to task is assumed, meaning if the user has the
   * right to delete the tasks he is allowed to delete to-do's to.
   * 
   * @see org.projectforge.business.user.UserRightAccessCheck#hasDeleteAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final ToDoDO obj, final ToDoDO oldObj)
  {
    return hasAccess(user, obj, OperationType.DELETE);
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final ToDoDO obj, final ToDoDO oldObj,
      final OperationType operationType)
  {
    return hasAccess(user, obj, operationType) == true || hasAccess(user, oldObj, operationType) == true;
  }

  private boolean hasAccess(final PFUserDO user, final ToDoDO toDo, final OperationType operationType)
  {
    if (toDo == null) {
      return true;
    }
    if (ObjectUtils.equals(user.getId(), toDo.getAssigneeId()) == true
        || ObjectUtils.equals(user.getId(), toDo.getReporterId()) == true) {
      return true;
    }
    if (toDo.getGroup() != null) {
      final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
      if (userGroupCache.isUserMemberOfGroup(user.getId(), toDo.getGroupId()) == true) {
        return true;
      }
    }
    if (toDo.getTaskId() != null) {
      return accessChecker.hasPermission(user, toDo.getTaskId(), AccessType.TASKS, operationType,
          false);
    } else {
      return false;
    }
  }
}
