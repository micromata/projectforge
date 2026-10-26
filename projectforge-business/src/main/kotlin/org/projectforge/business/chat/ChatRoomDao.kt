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

package org.projectforge.business.chat

import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Access to chat rooms (PF-CHAT-ROOM). There is no user right: every user with system access may create a room (as
 * its owner), a room is seen by its participants only ([isParticipant]), edited and deleted by its owner and its
 * moderators ([isModerator]). Admins have no access by their role (SEC-06). Groups are resolved on every check.
 */
@Service
open class ChatRoomDao : BaseDao<ChatRoomDO>(ChatRoomDO::class.java) {
    @Autowired
    private lateinit var groupCache: UserGroupCache

    override val defaultSortProperties: Array<SortProperty>
        get() = arrayOf(SortProperty.asc("name"))

    override fun newInstance(): ChatRoomDO {
        return ChatRoomDO()
    }

    /** Lists: every room is checked on its own by [hasAccess]. */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    /** Every user with system access may create a room, as its owner (see [hasAccess]). */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return user.id != null && user.hasSystemAccess()
    }

    override fun hasAccess(
        user: PFUserDO,
        obj: ChatRoomDO?,
        oldObj: ChatRoomDO?,
        operationType: OperationType,
        throwException: Boolean,
    ): Boolean {
        val userId = user.id
        val result = when {
            userId == null -> false
            obj == null -> operationType == OperationType.SELECT || operationType == OperationType.INSERT && hasInsertAccess(user)
            operationType == OperationType.SELECT -> isParticipant(obj, userId)
            // A new room belongs to the user creating it (an id is an undelete, decided by the stored room).
            operationType == OperationType.INSERT && obj.id == null -> obj.ownerId == userId && hasInsertAccess(user)
            // Decided by the stored room, never by the posted one: otherwise anybody could make themselves moderator.
            else -> {
                val dbObj = oldObj ?: obj.id?.let { find(it, checkAccess = false) }
                dbObj != null && isModerator(dbObj, userId)
            }
        }
        if (!result && throwException) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "ChatRoomDO", operationType)
        }
        return result
    }

    override fun hasHistoryAccess(user: PFUserDO, obj: ChatRoomDO, throwException: Boolean): Boolean {
        val result = user.id?.let { isModerator(obj, it) } == true
        if (!result && throwException) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "ChatRoomDO", OperationType.SELECT)
        }
        return result
    }

    override fun onUpdate(obj: ChatRoomDO, dbObj: ChatRoomDO) {
        // The owner stays (ROOM-04).
        obj.ownerId = dbObj.ownerId
    }

    /** The owner and the moderators. */
    open fun isModerator(room: ChatRoomDO, userId: Long?): Boolean {
        userId ?: return false
        return room.ownerId == userId || userId in ChatRoomDO.toIds(room.moderatorUserIds)
    }

    /** The owner, the moderators, the users and the members of the groups of the room. */
    open fun isParticipant(room: ChatRoomDO, userId: Long?): Boolean {
        userId ?: return false
        if (isModerator(room, userId) || userId in ChatRoomDO.toIds(room.participantUserIds)) {
            return true
        }
        val groupIds = activeGroupIds(room)
        return groupIds.isNotEmpty() && groupCache.isUserMemberOfAtLeastOneGroup(userId, *groupIds.toTypedArray())
    }

    /** The ids of all participants with system access (deleted and deactivated users excluded). */
    open fun getParticipantIds(room: ChatRoomDO): Set<Long> {
        val userIds = ChatRoomDO.toIds(room.participantUserIds) + ChatRoomDO.toIds(room.moderatorUserIds) +
                listOfNotNull(room.ownerId)
        return groupCache.getMemberIds(activeGroupIds(room), userIds)
            .filter { groupCache.getUser(it)?.let { user -> !user.deleted && user.hasSystemAccess() } == true }
            .toSet()
    }

    /** The groups of the room not deleted (a deleted group has no participants any more). */
    private fun activeGroupIds(room: ChatRoomDO): List<Long> {
        return ChatRoomDO.toIds(room.participantGroupIds).filter { groupCache.getGroup(it)?.deleted == false }
    }
}
