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

package org.projectforge.rest.dto

import org.projectforge.business.PfCaches
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class Group(
    id: Long? = null,
    displayName: String? = null,
    var name: String? = null,
    var assignedUsers: MutableList<User>? = null,
    var localGroup: Boolean = false,
    var organization: String? = null,
    var description: String? = null,
    var ldapValues: String? = null,
    var groupOwner: User? = null,
    var emails: String? = null,
    /** LDAP value, if in use: */
    var gidNumber: Int? = null,
) : BaseDTODisplayObject<GroupDO>(id = id, displayName = displayName) {

    /**
     * Populates the `emails` field with a concatenated list of sorted email addresses of the users
     * assigned to the group. The email addresses are retrieved from the `UserService`.
     *
     * This method queries the `UserGroupCache` for each user in the `assignedUsers` set to obtain
     * their email address. If an email address is found, it's added to a mutable set to ensure
     * uniqueness. Once all email addresses are collected, they are sorted and joined into a
     * comma-separated string, which is assigned to the `emails` field.
     */
    fun populateEmails() {
        val userGroupCache = UserGroupCache.getInstance()
        val mails = mutableSetOf<String>()
        assignedUsers?.forEach { user ->
            userGroupCache.getUser(user.id)?.email?.let { email ->
                mails.add(email)
            }
        }
        emails = mails.sorted().joinToString()
    }

    override fun copyFromMinimal(src: GroupDO) {
        name = src.name
        displayName = src.displayName
        id = src.id
        if (userGroupCache.isUserMemberOfAdminGroup) {
            super.copyFromMinimal(src)
        }
    }

    override fun copyFrom(src: GroupDO) {
        copyFromMinimal(src)
        if (userGroupCache.isUserMemberOfAdminGroup) {
            super.copyFrom(src)
        } else {
            // Only copy fields that are allowed for non-admin users:
            description = src.description
            organization = src.organization
        }
        // Assigned users are visible for all users (for double-checking leavers):
        val newAssignedUsers = mutableSetOf<User>()
        src.assignedUsers?.forEach { userDO ->
            val user = User()
            user.copyFromMinimal(userDO)
            if (!newAssignedUsers.any { it.id == userDO.id }) {
                newAssignedUsers.add(user)
            }
        }
        assignedUsers = newAssignedUsers.sortedBy { it.displayName?.lowercase() }.toMutableList()
    }

    override fun copyTo(dest: GroupDO) {
        super.copyTo(dest)
        val newAssignedUsers = mutableSetOf<PFUserDO>()
        assignedUsers?.forEach { u ->
            userGroupCache.getUser(u.id)?.let { userDO ->
                newAssignedUsers.add(userDO)
            }
        }
        if (newAssignedUsers.isNotEmpty()) {
            dest.assignedUsers = newAssignedUsers
        }
    }

    companion object {
        private val userGroupCache: UserGroupCache by lazy { UserGroupCache.getInstance() }

        /**
         * Converts csv of group ids to list of groups (only with id and displayName = "???", no other content).
         */
        fun toGroupList(str: String?): List<Group>? {
            if (str.isNullOrBlank()) return null
            return toLongArray(str)?.map { Group(it, "???") }
        }

        /**
         * Converts csv of group ids to list of user id's.
         */
        fun toLongArray(str: String?): LongArray? {
            return User.toLongArray(str)
        }

        /**
         * Converts group list to long values (of format supported by [toGroupList]).
         */
        fun toLongList(groups: List<Group>?): String? {
            return groups?.joinToString { "${it.id}" }
        }

        /**
         * Set display names of any existing group in the given list.
         */
        fun restoreDisplayNames(groups: List<Group>?) {
            val caches = PfCaches.instance
            groups?.forEach { it.displayName = caches.getGroup(it.id)?.displayName ?: "???" }
        }

        /**
         * Converts csv of group ids to list of group names).
         */
        fun toGroupNames(groupIds: String?): String {
            val groups = toGroupList(groupIds)
            Group.restoreDisplayNames(groups)
            return groups?.joinToString { it.displayName ?: "???" } ?: ""
        }
    }
}
