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

package org.projectforge.rest.dto

import org.projectforge.business.address.AddressbookDO
import org.projectforge.common.StringHelper

class Addressbook(var title: String? = null,
                  var owner: User? = null,
                  var description: String? = null,
                  var fullAccessGroups: MutableList<Group>? = null,
                  var fullAccessUsers: MutableList<User>? = null,
                  var readonlyAccessGroups: MutableList<Group>? = null,
                  var readonlyAccessUsers: MutableList<User>? = null
) : BaseObject<AddressbookDO>() {

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyFrom(src: AddressbookDO) {
        super.copyFrom(src)
        fullAccessGroups = toGroupList(src.fullAccessGroupIds)
        fullAccessUsers = toUserList(src.fullAccessUserIds)
        readonlyAccessGroups = toGroupList(src.readonlyAccessGroupIds)
        readonlyAccessUsers = toUserList(src.readonlyAccessUserIds)
    }

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyTo(dest: AddressbookDO) {
        super.copyTo(dest)
        dest.fullAccessGroupIds = fullAccessGroups?.joinToString { "${it.id}" }
        dest.fullAccessUserIds = fullAccessUsers?.joinToString { "${it.id}" }
        dest.readonlyAccessGroupIds = readonlyAccessGroups?.joinToString { "${it.id}" }
        dest.readonlyAccessUserIds = readonlyAccessUsers?.joinToString { "${it.id}" }
    }

    private fun toUserList(str: String?): MutableList<User>? {
        if (str.isNullOrBlank()) return null
        val users = mutableListOf<User>()
        StringHelper.splitToInts(str, ",", false).forEach { users.add(User(it, fullname = "Kai Reinhard")) }
        return users
    }

    private fun toGroupList(str: String?): MutableList<Group>? {
        if (str.isNullOrBlank()) return null
        val groups = mutableListOf<Group>()
        StringHelper.splitToInts(str, ",", false).forEach { groups.add(Group(it, name = "Gruppe")) }
        return groups
    }
}
