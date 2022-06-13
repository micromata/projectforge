/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class Group(
  id: Int? = null,
  displayName: String? = null,
  var name: String? = null,
  var assignedUsers: MutableSet<User>? = null,
  var localGroup: Boolean = false,
  var organization: String? = null,
  var description: String? = null,
  var ldapValues: String? = null,
  var groupOwner: User? = null,
  var emails: String? = null,
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
      newAssignedUsers.add(user)
    }
    assignedUsers = newAssignedUsers
  }

  override fun copyTo(dest: GroupDO) {
    super.copyTo(dest)
    val newAssignedUsers = mutableSetOf<PFUserDO>()
    assignedUsers?.forEach { u ->
      val userDO = PFUserDO()
      userDO.id = u.id
      newAssignedUsers.add(userDO)
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
      return toIntArray(str)?.map { Group(it, "???") }
    }

    /**
     * Converts csv of group ids to list of user id's.
     */
    fun toIntArray(str: String?): IntArray? {
      return User.toIntArray(str)
    }

    /**
     * Converts group list to ints (of format supported by [toGroupList]).
     */
    fun toIntList(groups: List<Group>?): String? {
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
