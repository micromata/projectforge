/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserRightAccessCheck
import org.projectforge.business.user.UserRightCategory
import org.projectforge.business.user.UserRightValue
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Base class for objects supporting user and group specific rights. You may define single group and user ids for the
 * different access types, such as owner, full access, readonly access and minimal access.
 *
 * @author Kai Reinhard (k.reinhard@me.de)
 */
abstract class BaseUserGroupRight<T : BaseUserGroupRightsDO?> protected constructor(accessChecker: AccessChecker?,
                                                                                    id: IUserRightId?,
                                                                                    category: UserRightCategory?,
                                                                                    vararg rightValues: UserRightValue?) : UserRightAccessCheck<T>(accessChecker, id, category, *rightValues) {
    @Transient
    private var _userGroupCache: UserGroupCache? = null
    private val userGroupCache: UserGroupCache
        get() {
            if (_userGroupCache == null) {
                _userGroupCache = TenantRegistryMap.getInstance().tenantRegistry.userGroupCache
            }
            return _userGroupCache!!
        }

    /**
     * General select access.
     *
     * @return true
     * @see UserRightAccessCheck.hasSelectAccess
     */
    override fun hasSelectAccess(user: PFUserDO): Boolean {
        return true
    }

    /**
     * @see UserRightAccessCheck.hasSelectAccess
     */
    override fun hasSelectAccess(user: PFUserDO, obj: T): Boolean {
        if (isOwner(user, obj) || accessChecker.isUserMemberOfAdminGroup(user)) { // User has full access to his own object.
            return true
        }
        val userId = user.id
        return hasFullAccess(obj, userId) || hasReadonlyAccess(obj, userId) || hasMinimalAccess(obj, userId)
    }

    /**
     * General insert access.
     *
     * @return true
     * @see UserRightAccessCheck.hasInsertAccess
     */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return true
    }

    /**
     * Owners and administrators are able to insert new objects.
     *
     * @see UserRightAccessCheck.hasInsertAccess
     */
    override fun hasInsertAccess(user: PFUserDO, obj: T): Boolean {
        return isOwner(user, obj) || accessChecker.isUserMemberOfAdminGroup(user)
    }

    /**
     * Owners and administrators are able to update objects.
     *
     * @see UserRightAccessCheck.hasUpdateAccess
     */
    override fun hasUpdateAccess(user: PFUserDO, obj: T, oldObj: T): Boolean {
        return hasInsertAccess(user, oldObj)
    }

    /**
     * If user is not reporter or assignee and task is given the access to task is assumed, meaning if the user has the
     * right to delete the tasks he is allowed to delete to-do's to.
     *
     * @see UserRightAccessCheck.hasDeleteAccess
     */
    override fun hasDeleteAccess(user: PFUserDO, obj: T, oldObj: T): Boolean {
        return hasInsertAccess(user, oldObj)
    }

    /**
     * @see UserRightAccessCheck.hasHistoryAccess
     */
    override fun hasHistoryAccess(user: PFUserDO, obj: T): Boolean {
        return obj?.let { hasInsertAccess(user, it) } ?: true
    }

    fun isOwner(user: PFUserDO?, obj: T?): Boolean {
        return user != null && obj != null && isOwner(user.id, obj)
    }

    fun isOwner(userId: Int?, obj: T?): Boolean {
        return obj != null && userId != null && userId == obj.ownerId
    }

    /**
     * @return [DataobjectAccessType.NONE], [DataobjectAccessType.MINIMAL], [DataobjectAccessType.READONLY] or
     * [DataobjectAccessType.FULL]. null will never be returned!
     */
    fun getAccessType(obj: T?, userId: Int?): DataobjectAccessType {
        if (obj == null || userId == null) {
            return DataobjectAccessType.NONE
        }
        if (userId == obj.ownerId) {
            return DataobjectAccessType.OWNER
        }
        var groupIds = StringHelper.splitToIntegers(obj.fullAccessGroupIds, ",")
        var userIds = StringHelper.splitToIntegers(obj.fullAccessUserIds, ",")
        if (isMemberOfAny(groupIds, userIds, userId)) {
            return DataobjectAccessType.FULL
        }
        groupIds = StringHelper.splitToIntegers(obj.readonlyAccessGroupIds, ",")
        userIds = StringHelper.splitToIntegers(obj.readonlyAccessUserIds, ",")
        if (isMemberOfAny(groupIds, userIds, userId)) {
            return DataobjectAccessType.READONLY
        }
        groupIds = StringHelper.splitToIntegers(obj.minimalAccessGroupIds, ",")
        userIds = StringHelper.splitToIntegers(obj.minimalAccessUserIds, ",")
        return if (isMemberOfAny(groupIds, userIds, userId)) {
            DataobjectAccessType.MINIMAL
        } else DataobjectAccessType.NONE
    }

    fun hasFullAccess(obj: T, userId: Int?): Boolean {
        return getAccessType(obj, userId).hasFullAccess()
    }

    fun hasReadonlyAccess(obj: T, userId: Int?): Boolean {
        return getAccessType(obj, userId) == DataobjectAccessType.READONLY
    }

    fun hasMinimalAccess(obj: T, userId: Int?): Boolean {
        return getAccessType(obj, userId) == DataobjectAccessType.MINIMAL
    }

    private fun isMemberOfAny(groupIds: Array<Int>?, userIds: Array<Int>?, userId: Int): Boolean {
        if (!groupIds.isNullOrEmpty() && userGroupCache.isUserMemberOfAtLeastOneGroup(userId, *groupIds)) {
            return true
        }
        return userIds?.any { it == userId } ?: false
    }
}
