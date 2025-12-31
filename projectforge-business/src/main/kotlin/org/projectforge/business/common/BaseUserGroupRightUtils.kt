/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.common

import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Utils for user and group based rights.
 *
 * @author Kai Reinhard (k.reinhard@me.de)
 */
object BaseUserGroupRightUtils {
  fun isOwner(user: PFUserDO?, obj: BaseUserGroupRightsDO?): Boolean {
    return isOwner(user?.id, obj)
  }

  fun isOwner(userId: Long?, obj: BaseUserGroupRightsDO?): Boolean {
    return obj != null && userId != null && userId == obj.ownerId
  }

  /**
   * @return [DataobjectAccessType.NONE], [DataobjectAccessType.MINIMAL], [DataobjectAccessType.READONLY] or
   * [DataobjectAccessType.FULL]. null will never be returned!
   */
  fun getAccessType(obj: BaseUserGroupRightsDO?, userId: Long?): DataobjectAccessType {
    if (obj == null || userId == null) {
      return DataobjectAccessType.NONE
    }
    if (userId == obj.ownerId) {
      return DataobjectAccessType.OWNER
    }
    var groupIds = StringHelper.splitToLongObjects(obj.fullAccessGroupIds, ",")
    var userIds = StringHelper.splitToLongObjects(obj.fullAccessUserIds, ",")
    if (isMemberOfAny(groupIds, userIds, userId)) {
      return DataobjectAccessType.FULL
    }
    groupIds = StringHelper.splitToLongObjects(obj.readonlyAccessGroupIds, ",")
    userIds = StringHelper.splitToLongObjects(obj.readonlyAccessUserIds, ",")
    if (isMemberOfAny(groupIds, userIds, userId)) {
      return DataobjectAccessType.READONLY
    }
    groupIds = StringHelper.splitToLongObjects(obj.minimalAccessGroupIds, ",")
    userIds = StringHelper.splitToLongObjects(obj.minimalAccessUserIds, ",")
    return if (isMemberOfAny(groupIds, userIds, userId)) {
      DataobjectAccessType.MINIMAL
    } else DataobjectAccessType.NONE
  }

  fun hasReadAccess(obj: BaseUserGroupRightsDO?, userId: Long?, throwException: Boolean = false): Boolean {
    return if (getAccessType(obj, userId).isIn(
        DataobjectAccessType.READONLY,
        DataobjectAccessType.FULL,
        DataobjectAccessType.OWNER,
      )
    ) {
      true
    } else {
      if (throwException) {
        throw AccessException("access.exception.noReadAccess")
      } else {
        false
      }
    }
  }

  fun hasWriteAccess(obj: BaseUserGroupRightsDO?, userId: Long?, throwException: Boolean = false): Boolean {
    return if (getAccessType(obj, userId).isIn(DataobjectAccessType.FULL, DataobjectAccessType.OWNER)) {
      true
    } else {
      if (throwException) {
        throw AccessException("access.exception.noWriteAccess")
      } else {
        false
      }
    }
  }

  fun hasAccess(
    obj: BaseUserGroupRightsDO?,
    userId: Long?,
    operationType: OperationType?,
    throwException: Boolean = false,
  ): Boolean {
    return hasAccess(obj, oldObj = null,  userId, operationType, throwException)
  }

  fun hasAccess(
    obj: BaseUserGroupRightsDO?,
    oldObj: BaseUserGroupRightsDO?,
    userId: Long?,
    operationType: OperationType?,
    throwException: Boolean = false,
  ): Boolean {
    if (operationType == null || operationType.isWriteType) {
      // Must check access of old object first, if exists:
      if (oldObj != null && hasWriteAccess(oldObj, userId, throwException)) {
        return true
      }
      // Check access of new object:
      return hasWriteAccess(obj, userId, throwException)
    }
    return hasReadAccess(oldObj ?: obj, userId, throwException)
  }

  private fun isMemberOfAny(groupIds: Array<Long>?, userIds: Array<Long?>?, userId: Long): Boolean {
    if (!groupIds.isNullOrEmpty() && UserGroupCache.getInstance().isUserMemberOfAtLeastOneGroup(userId, *groupIds)) {
      return true
    }
    return userIds?.any { it != null && it == userId } ?: false
  }
}
