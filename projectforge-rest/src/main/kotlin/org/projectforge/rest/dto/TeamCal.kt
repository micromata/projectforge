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

import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class TeamCal(var title: String? = null,
              var owner: PFUserDO? = null,
              var description: String? = null,
              var fullAccessGroups: MutableList<Group>? = null,
              var fullAccessUsers: MutableList<User>? = null,
              var readonlyAccessGroups: MutableList<Group>? = null,
              var readonlyAccessUsers: MutableList<User>? = null,
              var minimalAccessGroups: MutableList<Group>? = null,
              var minimalAccessUsers: MutableList<User>? = null,
              var includeLeaveDaysForUsers: MutableList<User>? = null,
              var includeLeaveDaysForGroups: MutableList<Group>? = null,
              var externalSubscription: Boolean = false,
              var externalSubscriptionUrl: String? = null,
              var externalSubscriptionUpdateInterval: Int? = null,
              var externalSubscriptionUrlAnonymized: String? = null,
              var vacation4Groups: List<Int>? = null,
              var vacation4Users: List<Int>? = null
) : BaseDTO<TeamCalDO>() {
    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyFrom(src: TeamCalDO) {
        super.copyFrom(src)
        fullAccessGroups = Group.toGroupList(src.fullAccessGroupIds)
        fullAccessUsers = User.toUserList(src.fullAccessUserIds)
        readonlyAccessGroups = Group.toGroupList(src.readonlyAccessGroupIds)
        readonlyAccessUsers = User.toUserList(src.readonlyAccessUserIds)
        minimalAccessGroups = Group.toGroupList(src.minimalAccessGroupIds)
        minimalAccessUsers = User.toUserList(src.minimalAccessUserIds)

        includeLeaveDaysForGroups = Group.toGroupList(src.includeLeaveDaysForGroups)
        includeLeaveDaysForUsers = User.toUserList(src.includeLeaveDaysForUsers)
    }

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyTo(dest: TeamCalDO) {
        super.copyTo(dest)
        dest.fullAccessGroupIds = Group.toIntList(fullAccessGroups)
        dest.fullAccessUserIds = User.toIntList(fullAccessUsers)
        dest.readonlyAccessGroupIds = Group.toIntList(readonlyAccessGroups)
        dest.readonlyAccessUserIds = User.toIntList(readonlyAccessUsers)
        dest.minimalAccessGroupIds = Group.toIntList(minimalAccessGroups)
        dest.minimalAccessUserIds = User.toIntList(minimalAccessUsers)

        dest.includeLeaveDaysForGroups = Group.toIntList(includeLeaveDaysForGroups)
        dest.includeLeaveDaysForUsers = User.toIntList(includeLeaveDaysForUsers)
    }
}
