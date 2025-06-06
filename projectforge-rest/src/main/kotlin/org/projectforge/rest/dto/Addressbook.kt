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

package org.projectforge.rest.dto

import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.common.BaseUserGroupRightService

class Addressbook(
    id: Long? = null,
    displayName: String? = null,
    var title: String? = null,
    var owner: User? = null,
    var description: String? = null,
    var fullAccessGroups: List<Group>? = null,
    var fullAccessUsers: List<User>? = null,
    var readonlyAccessGroups: List<Group>? = null,
    var readonlyAccessUsers: List<User>? = null,
    var fullAccessGroupsAsString: String? = null,
    var fullAccessUsersAsString: String? = null,
    var readonlyAccessGroupsAsString: String? = null,
    var readonlyAccessUsersAsString: String? = null,
) : BaseDTODisplayObject<AddressbookDO>(id, displayName = displayName) {

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyFrom(src: AddressbookDO) {
        super.copyFrom(src)
        fullAccessGroups = Group.toGroupList(src.fullAccessGroupIds)
        fullAccessUsers = User.toUserList(src.fullAccessUserIds)
        readonlyAccessGroups = Group.toGroupList(src.readonlyAccessGroupIds)
        readonlyAccessUsers = User.toUserList(src.readonlyAccessUserIds)
    }

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyTo(dest: AddressbookDO) {
        super.copyTo(dest)
        val svc = BaseUserGroupRightService.instance
        svc.setFullAccessGroups(dest, fullAccessGroups)
        svc.setFullAccessUsers(dest, fullAccessUsers)
        svc.setReadonlyAccessGroups(dest, readonlyAccessGroups)
        svc.setReadonlyAccessUsers(dest, readonlyAccessUsers)
    }

    companion object {
        fun transformFromDB(
            obj: AddressbookDO,
        ): Addressbook {
            val dto = Addressbook()
            dto.copyFrom(obj)
            Group.restoreDisplayNames(dto.fullAccessGroups)
            Group.restoreDisplayNames(dto.readonlyAccessGroups)

            User.restoreDisplayNames(dto.fullAccessUsers)
            User.restoreDisplayNames(dto.readonlyAccessUsers)

            dto.fullAccessUsersAsString = dto.fullAccessUsers?.joinToString { it.displayName ?: "???" } ?: ""
            dto.readonlyAccessUsersAsString = dto.readonlyAccessUsers?.joinToString { it.displayName ?: "???" } ?: ""
            dto.fullAccessGroupsAsString = dto.fullAccessGroups?.joinToString { it.displayName ?: "???" } ?: ""
            dto.readonlyAccessUsersAsString = dto.readonlyAccessUsers?.joinToString { it.displayName ?: "???" } ?: ""
            return dto
        }
    }
}
