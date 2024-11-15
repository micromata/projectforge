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

package org.projectforge.business.poll.filter

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.poll.PollDO
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO

class PollAssignmentFilter(val values: List<PollAssignment>) : CustomResultFilter<PollDO> {

    override fun match(list: MutableList<PollDO>, element: PollDO): Boolean {
        var foundUser: PFUserDO? = null
        if (!element.fullAccessGroupIds.isNullOrEmpty()) {
            val groupIds = element.fullAccessGroupIds!!.split(", ").map { it.toLong() }.toLongArray()
            val accessUsers = groupService.getGroupUsers(groupIds)
            val localUser = ThreadLocalUserContext.loggedInUserId!!
            foundUser = accessUsers.firstOrNull { user -> user.id == localUser }
        }

        values.forEach { filter ->
            if (element.getPollAssignment()
                    .contains(filter) || (filter == PollAssignment.ACCESS && foundUser != null)
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        private var _groupService: GroupService? = null
        private val groupService: GroupService
            get() {
                if (_groupService == null) {
                    _groupService = ApplicationContextProvider.getApplicationContext().getBean(GroupService::class.java)
                }
                return _groupService!!
            }
    }
}
