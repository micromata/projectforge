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

package org.projectforge.business.address;

import org.apache.commons.lang.ObjectUtils;
import org.projectforge.business.common.DataobjectAccessType;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Florian Blumenstein
 */
public class AddressbookRight extends UserRightAccessCheck<AddressbookDO>
{
  private static final long serialVersionUID = -2928342166476350773L;

  private transient UserGroupCache userGroupCache;

  public AddressbookRight(AccessChecker accessChecker)
  {
    super(accessChecker, UserRightId.MISC_ADDRESSBOOK, UserRightCategory.MISC,
        UserRightValue.TRUE);
  }

  /**
   * General select access.
   *
   * @return true
   * @see UserRightAccessCheck#hasSelectAccess(PFUserDO)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user)
  {
    return true;
  }

  private boolean checkGlobal(final AddressbookDO obj)
  {
    return obj != null && obj.getId() != null && AddressbookDao.GLOBAL_ADDRESSBOOK_ID == obj.getId();
  }

  /**
   * @see UserRightAccessCheck#hasSelectAccess(PFUserDO,
   * Object)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final AddressbookDO obj)
  {
    if (isOwner(user, obj) == true || accessChecker.isUserMemberOfAdminGroup(user) == true || checkGlobal(obj) || accessChecker.isLoggedInUserMemberOfGroup(
        ProjectForgeGroup.ORGA_TEAM)) {
      return true;
    }
    final Integer userId = user.getId();
    if (hasFullAccess(obj, userId) == true || hasReadonlyAccess(obj, userId) == true) {
      return true;
    }
    return false;
  }

  /**
   * General insert access.
   *
   * @return true
   * @see UserRightAccessCheck#hasInsertAccess(PFUserDO)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user)
  {
    return true;
  }

  /**
   * Owners and administrators are able to insert new addressbooks.
   *
   * @see UserRightAccessCheck#hasInsertAccess(PFUserDO,
   * Object)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user, final AddressbookDO obj)
  {
    return isOwner(user, obj) == true || accessChecker.isUserMemberOfAdminGroup(user) == true || checkGlobal(obj) || accessChecker.isLoggedInUserMemberOfGroup(
        ProjectForgeGroup.ORGA_TEAM);
  }

  /**
   * Owners and administrators are able to update addressbooks.
   *
   * @see UserRightAccessCheck#hasUpdateAccess(PFUserDO,
   * Object, Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final AddressbookDO obj, final AddressbookDO oldObj)
  {
    return hasInsertAccess(user, oldObj) == true;
  }

  /**
   * If user is not reporter or assignee and task is given the access to task is assumed, meaning if the user has the
   * right to delete the tasks he is allowed to delete to-do's to.
   *
   * @see UserRightAccessCheck#hasDeleteAccess(PFUserDO,
   * Object, Object)
   */
  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final AddressbookDO obj, final AddressbookDO oldObj)
  {
    return hasInsertAccess(user, oldObj) == true;
  }

  /**
   * @see UserRightAccessCheck#hasHistoryAccess(PFUserDO,
   * Object)
   */
  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final AddressbookDO obj)
  {
    if (obj == null) {
      return true;
    }
    return hasInsertAccess(user, obj) == true;
  }

  public boolean isOwner(final PFUserDO user, final AddressbookDO ab)
  {
    if (ab == null) {
      return false;
    }
    return ObjectUtils.equals(user.getId(), ab.getOwnerId()) == true;
  }

  public boolean isOwner(final Integer userId, final AddressbookDO ab)
  {
    if (ab == null || userId == null) {
      return false;
    }
    return ObjectUtils.equals(userId, ab.getOwnerId()) == true;
  }

  public boolean isMemberOfAtLeastOneGroup(final PFUserDO user, final Integer... groupIds)
  {
    return getUserGroupCache().isUserMemberOfAtLeastOneGroup(user.getId(), groupIds);
  }

  /**
   * @param addressbook
   * @param userId
   * @return {@link DataobjectAccessType#NONE}, {@link DataobjectAccessType#MINIMAL}, {@link DataobjectAccessType#READONLY} or
   * {@link DataobjectAccessType#FULL}. null will never be returned!
   */
  public DataobjectAccessType getAccessType(final AddressbookDO ab, final Integer userId)
  {
    if (ab == null || userId == null) {
      return DataobjectAccessType.NONE;
    }
    if (hasFullAccess(ab, userId) == true) {
      return DataobjectAccessType.FULL;
    } else if (hasReadonlyAccess(ab, userId) == true) {
      return DataobjectAccessType.READONLY;
    }
    return DataobjectAccessType.NONE;
  }

  public boolean hasFullAccess(final AddressbookDO ab, final Integer userId)
  {
    if (ab == null || userId == null) {
      return false;
    }
    if (isOwner(userId, ab) == true) {
      return true;
    }
    final Integer[] groupIds = StringHelper.splitToIntegers(ab.getFullAccessGroupIds(), ",");
    final Integer[] userIds = StringHelper.splitToIntegers(ab.getFullAccessUserIds(), ",");
    return hasAccess(groupIds, userIds, userId);
  }

  public boolean hasReadonlyAccess(final AddressbookDO ab, final Integer userId)
  {
    if (ab == null || userId == null) {
      return false;
    }
    if (hasFullAccess(ab, userId) == true) {
      // User has full access (which is more than read-only access).
      return false;
    }
    final Integer[] groupIds = StringHelper.splitToIntegers(ab.getReadonlyAccessGroupIds(), ",");
    final Integer[] userIds = StringHelper.splitToIntegers(ab.getReadonlyAccessUserIds(), ",");
    return hasAccess(groupIds, userIds, userId);
  }

  private boolean hasAccess(final Integer[] groupIds, final Integer[] userIds, final Integer userId)
  {
    if (getUserGroupCache().isUserMemberOfAtLeastOneGroup(userId, groupIds) == true || accessChecker.isLoggedInUserMemberOfGroup(
        ProjectForgeGroup.ORGA_TEAM)) {
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
