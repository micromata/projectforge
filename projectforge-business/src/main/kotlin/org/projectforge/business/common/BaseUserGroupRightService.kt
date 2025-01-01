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

package org.projectforge.business.common

import jakarta.annotation.PostConstruct
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Utils for user and group based rights.
 *
 * @author Kai Reinhard (k.reinhard@me.de)
 */
@Service
class BaseUserGroupRightService {
    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    fun getSortedFullAccessGroups(rights: BaseUserGroupRightsDO): Collection<GroupDO> {
        return groupService.getSortedGroups(rights.fullAccessGroupIds)
    }

    fun getSortedReadonlyAccessUsers(rights: BaseUserGroupRightsDO): Collection<PFUserDO> {
        return userService.getSortedUsers(rights.readonlyAccessUserIds)
    }

    fun getSortedFullAccessUsers(rights: BaseUserGroupRightsDO): Collection<PFUserDO> {
        return userService.getSortedUsers(rights.fullAccessUserIds)
    }

    fun getSortedReadonlyAccessGroups(rights: BaseUserGroupRightsDO): Collection<GroupDO> {
        return groupService.getSortedGroups(rights.readonlyAccessGroupIds)
    }

    fun getSortedMinimalAccessUsers(rights: BaseUserGroupRightsDO): Collection<PFUserDO> {
        return userService.getSortedUsers(rights.minimalAccessUserIds)
    }

    fun getSortedMinimalAccessGroups(rights: BaseUserGroupRightsDO): Collection<GroupDO> {
        return groupService.getSortedGroups(rights.minimalAccessGroupIds)
    }

    /**
     * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param groups Collection of IdObject's. Null values are ignored.
     */
    fun setFullAccessGroups(rights: BaseUserGroupRightsDO, groups: Collection<IdObject<Long>?>?) {
        rights.fullAccessGroupIds = asSortedIdStrings(groups)
    }

    /**
     * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param groupIds The csv list of id's. Null values are ignored. The existence of the id's is not checked.
     * @return ordered list of id's.
     *
     */
    fun setFullAccessGroups(rights: BaseUserGroupRightsDO, groupIds: String?) {
        rights.fullAccessGroupIds = asSortedIdStrings(groupIds)
    }


    /**
     * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param users Collection of IdObject's. Null values are ignored.
     */
    fun setFullAccessUsers(rights: BaseUserGroupRightsDO, users: Collection<IdObject<Long>?>?) {
        rights.fullAccessUserIds = asSortedIdStrings(users)
    }

    /**
     * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param userIds The csv list of id's. Null values are ignored. The existence of the id's is not checked.
     * @return ordered list of id's.
     */
    fun setFullAccessUsers(rights: BaseUserGroupRightsDO, userIds: String?) {
        rights.fullAccessUserIds = asSortedIdStrings(userIds)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param groups Collection of IdObject's. Null values are ignored.
     */
    fun setReadonlyAccessGroups(rights: BaseUserGroupRightsDO, groups: Collection<IdObject<Long>?>?) {
        rights.readonlyAccessGroupIds = asSortedIdStrings(groups)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param groupIds The csv list of id's. Null values are ignored. The existence of the id's is not checked.
     * @return ordered list of id's.
     */
    fun setReadonlyAccessGroups(rights: BaseUserGroupRightsDO, groupIds: String?) {
        rights.readonlyAccessGroupIds = asSortedIdStrings(groupIds)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param users Collection of IdObject's. Null values are ignored.
     */
    fun setReadonlyAccessUsers(rights: BaseUserGroupRightsDO, users: Collection<IdObject<Long>?>?) {
        rights.readonlyAccessUserIds = asSortedIdStrings(users)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param userIds The csv list of id's. Null values are ignored. The existence of the id's is not checked.
     * @return ordered list of id's.
     */
    fun setReadonlyAccessUsers(rights: BaseUserGroupRightsDO, userIds: String?) {
        rights.readonlyAccessUserIds = asSortedIdStrings(userIds)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param groups Collection of IdObject's. Null values are ignored.
     */
    fun setMinimalAccessGroups(rights: BaseUserGroupRightsDO, groups: Collection<IdObject<Long>?>?) {
        rights.minimalAccessGroupIds = asSortedIdStrings(groups)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param groups The csv list of id's. Null values are ignored. The existence of the id's is not checked.
     * @return ordered list of id's.
     */
    fun setMinimalAccessGroups(rights: BaseUserGroupRightsDO, groups: String?) {
        rights.minimalAccessGroupIds = asSortedIdStrings(groups)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param users Collection of IdObject's. Null values are ignored.
     */
    fun setMinimalAccessUsers(rights: BaseUserGroupRightsDO, users: Collection<IdObject<Long>?>?) {
        rights.minimalAccessUserIds = asSortedIdStrings(users)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param rights dest object.
     * @param users The csv list of id's. Null values are ignored. The existence of the id's is not checked.
     * @return ordered list of id's.
     */
    fun setMinimalAccessUsers(rights: BaseUserGroupRightsDO, users: String?) {
        rights.minimalAccessUserIds = asSortedIdStrings(users)
    }

    companion object {
        /**
         * Example: Group(id=3),Group(id=1),Group(id=2),Group(id=4) -> "1,2,3,4"
         * Example: Group(id=3),null,Group(id=2),Group(id=4) -> "2,3,4"
         * @param collection Collection of users, groups etc. Null values are ignored.
         * @return sorted list of ids as string. null if collection is null.
         */
        @JvmStatic
        fun asSortedIdStrings(collection: Collection<IdObject<Long>?>?): String? {
            val col = collection?.filterNotNull()
            if (col.isNullOrEmpty()) return null
            return col.sortedBy { it.id }.joinToString(",") { it.id.toString() }
        }

        @JvmStatic
        fun asSortedIdStrings(idString: String?): String? {
            idString ?: return null
            return idString.split(",").asSequence().filter { it.isNotBlank() }
                .map { it.trim().toLongOrNull() }.filterNotNull().sorted().joinToString(",")
        }

        @JvmStatic
        lateinit var instance: BaseUserGroupRightService
            private set
    }
}
