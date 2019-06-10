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

package org.projectforge.business.common;

import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Base class for objects supporting user and group specific rights. You may define single group and user ids for the
 * different access types, such as owner, full access, readonly access and minimal access.
 *
 * @author Kai Reinhard (k.reinhard@me.de)
 */
public abstract class BaseUserGroupRight<T extends BaseUserGroupRightsDO> extends UserRightAccessCheck<T> {

  private transient UserGroupCache userGroupCache;

  protected BaseUserGroupRight(final AccessChecker accessChecker,
                               final IUserRightId id,
                               final UserRightCategory category,
                               final UserRightValue... rightValues) {
    super(accessChecker, id, category, rightValues);
  }

  /**
   * General select access.
   *
   * @return true
   * @see UserRightAccessCheck#hasSelectAccess(PFUserDO)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user) {
    return true;
  }

  /**
   * @see UserRightAccessCheck#hasSelectAccess(PFUserDO,
   * Object)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final T obj) {
    if (isOwner(user, obj) || accessChecker.isUserMemberOfAdminGroup(user)) {
      // User has full access to his own object.
      return true;
    }
    final Integer userId = user.getId();
    return hasFullAccess(obj, userId) || hasReadonlyAccess(obj, userId) || hasMinimalAccess(obj, userId);
  }

  /**
   * General insert access.
   *
   * @return true
   * @see UserRightAccessCheck#hasInsertAccess(PFUserDO)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user) {
    return true;
  }

  /**
   * Owners and administrators are able to insert new objects.
   *
   * @see UserRightAccessCheck#hasInsertAccess(PFUserDO,
   * Object)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user, final T obj) {
    return isOwner(user, obj) || accessChecker.isUserMemberOfAdminGroup(user);
  }

  /**
   * Owners and administrators are able to update objects.
   *
   * @see UserRightAccessCheck#hasUpdateAccess(PFUserDO,
   * Object, Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final T obj, final T oldObj) {
    return hasInsertAccess(user, oldObj);
  }

  /**
   * If user is not reporter or assignee and task is given the access to task is assumed, meaning if the user has the
   * right to delete the tasks he is allowed to delete to-do's to.
   *
   * @see UserRightAccessCheck#hasDeleteAccess(PFUserDO,
   * Object, Object)
   */
  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final T obj, final T oldObj) {
    return hasInsertAccess(user, oldObj);
  }

  /**
   * @see UserRightAccessCheck#hasHistoryAccess(PFUserDO,
   * Object)
   */
  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final T obj) {
    if (obj == null) {
      return true;
    }
    return hasInsertAccess(user, obj);
  }

  public boolean isOwner(final PFUserDO user, final T obj) {
    return user != null && obj != null && isOwner(user.getId(), obj);
  }

  public boolean isOwner(final Integer userId, final T obj) {
    return obj != null && userId != null && userId.equals(obj.getOwnerId());
  }

  /**
  * @return {@link DataobjectAccessType#NONE}, {@link DataobjectAccessType#MINIMAL}, {@link DataobjectAccessType#READONLY} or
   * {@link DataobjectAccessType#FULL}. null will never be returned!
   */
  public DataobjectAccessType getAccessType(final T obj, final Integer userId) {
    if (obj == null || userId == null) {
      return DataobjectAccessType.NONE;
    }
    if (userId.equals(obj.getOwnerId())) {
      return DataobjectAccessType.OWNER;
    }
    Integer[] groupIds = StringHelper.splitToIntegers(obj.getFullAccessGroupIds(), ",");
    Integer[] userIds = StringHelper.splitToIntegers(obj.getFullAccessUserIds(), ",");
    if (isMemberOfAny(groupIds, userIds, userId)) {
      return DataobjectAccessType.FULL;
    }
    groupIds = StringHelper.splitToIntegers(obj.getReadonlyAccessGroupIds(), ",");
    userIds = StringHelper.splitToIntegers(obj.getReadonlyAccessUserIds(), ",");
    if (isMemberOfAny(groupIds, userIds, userId)) {
      return DataobjectAccessType.READONLY;
    }
    groupIds = StringHelper.splitToIntegers(obj.getMinimalAccessGroupIds(), ",");
    userIds = StringHelper.splitToIntegers(obj.getMinimalAccessUserIds(), ",");
    if (isMemberOfAny(groupIds, userIds, userId)) {
      return DataobjectAccessType.MINIMAL;
    }
    return DataobjectAccessType.NONE;
  }

  public boolean hasFullAccess(final T obj, final Integer userId) {
    return getAccessType(obj, userId).hasFullAccess();
  }

  public boolean hasReadonlyAccess(final T obj, final Integer userId) {
    return getAccessType(obj, userId) == DataobjectAccessType.READONLY;
  }

  public boolean hasMinimalAccess(final T obj, final Integer userId) {
    return getAccessType(obj, userId) == DataobjectAccessType.MINIMAL;
  }

  private boolean isMemberOfAny(final Integer[] groupIds, final Integer[] userIds, final Integer userId) {
    if (getUserGroupCache().isUserMemberOfAtLeastOneGroup(userId, groupIds)) {
      return true;
    }
    if (userIds == null) {
      return false;
    }
    for (final Integer id : userIds) {
      if (id == null) {
        continue;
      }
      if (id.equals(userId)) {
        return true;
      }
    }
    return false;
  }

  private UserGroupCache getUserGroupCache() {
    if (userGroupCache == null) {
      userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    }
    return userGroupCache;
  }
}
