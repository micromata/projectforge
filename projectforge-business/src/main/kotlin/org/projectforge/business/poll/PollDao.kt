/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.poll

import org.projectforge.business.group.service.GroupService
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class PollDao : BaseDao<PollDO>(PollDO::class.java) {

    @Autowired
    private lateinit var groupService: GroupService

    override fun newInstance(): PollDO {
        return PollDO()
    }

    override fun hasAccess(
        user: PFUserDO?,
        obj: PollDO?,
        oldObj: PollDO?,
        operationType: OperationType?,
        throwException: Boolean
    ): Boolean {

        if (obj == null && operationType == OperationType.SELECT) {
            return true
        };
        if (obj != null && operationType == OperationType.SELECT) {
            if (hasFullAccess(obj) || isAttendee(obj, ThreadLocalUserContext.user?.id!!))
                return true
        }
        if (obj != null) {
            return hasFullAccess(obj)
        }
        return false
    }

    // returns true if current user has full access, otherwise returns false
    fun hasFullAccess(obj: PollDO): Boolean {
        val loggedInUserId = ThreadLocalUserContext.userId!!
        if (!obj.fullAccessUserIds.isNullOrBlank()) {
            val userIdArray = PollDO.toIntArray(obj.fullAccessUserIds)
            if (userIdArray?.contains(loggedInUserId) == true) {
                return true
            }
        }
        if (obj.owner?.id == loggedInUserId) {
            return true
        }
        if (!obj.fullAccessGroupIds.isNullOrBlank()) {
            val groupIdArray = PollDO.toIntArray(obj.fullAccessGroupIds)
            val groupUsers = groupService.getGroupUsers(groupIdArray)
            groupUsers.map { it.id }.forEach { id ->
                if (id == loggedInUserId) {
                    return true
                }
            }
        }
        return false
    }

    fun isAttendee(obj: PollDO, user: Int): Boolean {
        return PollDO.toIntArray(obj.attendeeIds)?.contains(user) == true
    }
}