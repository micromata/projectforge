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

package org.projectforge.business.teamcal.admin.right;

import org.apache.commons.lang.ObjectUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.teamcal.admin.model.TeamCalAccessType;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Kai Reinhard (k.reinhard@me.de)
 * 
 */
public class TeamCalRight extends UserRightAccessCheck<TeamCalDO>
{
  private static final long serialVersionUID = -2928342166476350773L;

  private transient UserGroupCache userGroupCache;

  public TeamCalRight(AccessChecker accessChecker)
  {
    super(accessChecker, UserRightId.PLUGIN_CALENDAR, UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
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
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final TeamCalDO obj)
  {
    if (isOwner(user, obj) == true || accessChecker.isUserMemberOfAdminGroup(user) == true) {
      // User has full access to his own calendars.
      return true;
    }
    final Integer userId = user.getId();
    if (hasFullAccess(obj, userId) == true || hasReadonlyAccess(obj, userId) == true
        || hasMinimalAccess(obj, userId) == true) {
      return true;
    }
    return false;
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
   * Owners and administrators are able to insert new calendars.
   * 
   * @see org.projectforge.business.user.UserRightAccessCheck#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user, final TeamCalDO obj)
  {
    return isOwner(user, obj) == true || accessChecker.isUserMemberOfAdminGroup(user) == true;
  }

  /**
   * Owners and administrators are able to update calendars.
   * 
   * @see org.projectforge.business.user.UserRightAccessCheck#hasUpdateAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final TeamCalDO obj, final TeamCalDO oldObj)
  {
    return hasInsertAccess(user, oldObj) == true;
  }

  /**
   * If user is not reporter or assignee and task is given the access to task is assumed, meaning if the user has the
   * right to delete the tasks he is allowed to delete to-do's to.
   * 
   * @see org.projectforge.business.user.UserRightAccessCheck#hasDeleteAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final TeamCalDO obj, final TeamCalDO oldObj)
  {
    return hasInsertAccess(user, oldObj) == true;
  }

  /**
   * @see org.projectforge.business.user.UserRightAccessCheck#hasHistoryAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final TeamCalDO obj)
  {
    if (obj == null) {
      return true;
    }
    return hasInsertAccess(user, obj) == true;
  }

  public boolean isOwner(final PFUserDO user, final TeamCalDO cal)
  {
    if (cal == null) {
      return false;
    }
    return ObjectUtils.equals(user.getId(), cal.getOwnerId()) == true;
  }

  public boolean isOwner(final Integer userId, final TeamCalDO cal)
  {
    if (cal == null || userId == null) {
      return false;
    }
    return ObjectUtils.equals(userId, cal.getOwnerId()) == true;
  }

  public boolean isMemberOfAtLeastOneGroup(final PFUserDO user, final Integer... groupIds)
  {
    return getUserGroupCache().isUserMemberOfAtLeastOneGroup(user.getId(), groupIds);
  }

  /**
   * @param calendar
   * @param userId
   * @return {@link TeamCalAccessType#NONE}, {@link TeamCalAccessType#MINIMAL}, {@link TeamCalAccessType#READONLY} or
   *         {@link TeamCalAccessType#FULL}. null will never be returned!
   */
  public TeamCalAccessType getAccessType(final TeamCalDO calendar, final Integer userId)
  {
    if (calendar == null || userId == null) {
      return TeamCalAccessType.NONE;
    }
    if (hasFullAccess(calendar, userId) == true) {
      return TeamCalAccessType.FULL;
    } else if (hasReadonlyAccess(calendar, userId) == true) {
      return TeamCalAccessType.READONLY;
    } else if (hasMinimalAccess(calendar, userId) == true) {
      return TeamCalAccessType.MINIMAL;
    }
    return TeamCalAccessType.NONE;
  }

  public boolean hasFullAccess(final TeamCalDO calendar, final Integer userId)
  {
    if (calendar == null || userId == null) {
      return false;
    }
    if (isOwner(userId, calendar) == true) {
      return true;
    }
    final Integer[] groupIds = StringHelper.splitToIntegers(calendar.getFullAccessGroupIds(), ",");
    final Integer[] userIds = StringHelper.splitToIntegers(calendar.getFullAccessUserIds(), ",");
    return hasAccess(groupIds, userIds, userId);
  }

  public boolean hasReadonlyAccess(final TeamCalDO calendar, final Integer userId)
  {
    if (calendar == null || userId == null) {
      return false;
    }
    if (hasFullAccess(calendar, userId) == true) {
      // User has full access (which is more than read-only access).
      return false;
    }
    final Integer[] groupIds = StringHelper.splitToIntegers(calendar.getReadonlyAccessGroupIds(), ",");
    final Integer[] userIds = StringHelper.splitToIntegers(calendar.getReadonlyAccessUserIds(), ",");
    return hasAccess(groupIds, userIds, userId);
  }

  public boolean hasMinimalAccess(final TeamCalDO calendar, final Integer userId)
  {
    if (calendar == null || userId == null) {
      return false;
    }
    if (hasFullAccess(calendar, userId) == true || hasReadonlyAccess(calendar, userId) == true) {
      // User has full access or read-only access (which is more than minimal access).
      return false;
    }
    final Integer[] groupIds = StringHelper.splitToIntegers(calendar.getMinimalAccessGroupIds(), ",");
    final Integer[] userIds = StringHelper.splitToIntegers(calendar.getMinimalAccessUserIds(), ",");
    return hasAccess(groupIds, userIds, userId);
  }

  private boolean hasAccess(final Integer[] groupIds, final Integer[] userIds, final Integer userId)
  {
    if (getUserGroupCache().isUserMemberOfAtLeastOneGroup(userId, groupIds) == true) {
      return true;
    }
    if (userIds == null) {
      return false;
    }
    for (final Integer id : userIds) {
      if (id == null) {
        continue;
      }
      if (id.equals(userId) == true) {
        return true;
      }
    }
    return false;
  }

  private UserGroupCache getUserGroupCache()
  {
    if (userGroupCache == null) {
      userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    }
    return userGroupCache;
  }
}
