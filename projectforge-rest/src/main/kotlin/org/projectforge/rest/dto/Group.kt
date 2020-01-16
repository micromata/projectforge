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

package org.projectforge.rest.dto

import org.projectforge.common.StringHelper
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class Group(id: Int? = null,
            displayName: String? = null,
            var name: String? = null,
            var assignedUsers: MutableSet<PFUserDO>? = null
) : BaseDTODisplayObject<GroupDO>(id = id, displayName = displayName) {
    override fun copyFromMinimal(src: GroupDO) {
        super.copyFromMinimal(src)
        name = src.name
    }

    companion object {
        /**
         * Converts csv list of group ids to list of groups (only with id and displayName = "???", no other content).
         */
        fun toGroupList(str: String?): MutableList<Group>? {
            if (str.isNullOrBlank()) return null
            val groups = mutableListOf<Group>()
            StringHelper.splitToInts(str, ",", false).forEach { groups.add(Group(it, displayName = "???")) }
            return groups
        }

        /**
         * Converts group list to ints (of format supported by [toGroupList]).
         */
        fun toIntList(groups: List<Group>?): String? {
            return groups?.joinToString { "${it.id}" }
        }
    }
}
