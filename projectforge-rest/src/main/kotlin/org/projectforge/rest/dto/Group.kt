/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.group.service.GroupService
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
  override fun copyFromMinimal(src: GroupDO) {
    super.copyFromMinimal(src)
    name = src.name
  }

  override fun copyFrom(src: GroupDO) {
    super.copyFrom(src)
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
      UserGroupCache.getInstance().getUser(u.id)?.let { userDO ->
        newAssignedUsers.add(userDO)
      }
    }
    if (newAssignedUsers.isNotEmpty()) {
      dest.assignedUsers = newAssignedUsers
    }
  }

  companion object {
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
     * @see GroupService.getDisplayName
     */
    fun restoreDisplayNames(groups: List<Group>?, groupService: GroupService) {
      groups?.forEach { it.displayName = groupService.getDisplayName(it.id) }
    }

    /**
     * Converts csv of group ids to list of group names).
     */
    fun toGroupNames(groupIds: String?, groupService: GroupService): String {
      val groups = toGroupList(groupIds)
      Group.restoreDisplayNames(groups, groupService)
      return groups?.joinToString { it.displayName ?: "???" } ?: ""
    }
  }
}
