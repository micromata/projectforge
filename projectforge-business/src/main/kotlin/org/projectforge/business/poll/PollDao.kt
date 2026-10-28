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

package org.projectforge.business.poll

import org.projectforge.business.poll.filter.PollAssignment
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Service

/**
 * Access to polls. There is no user right: everybody may create a poll, and a poll is seen by its attendees and the
 * users with full access only (see [hasFullAccess], [isAttendee]). Groups are resolved on every check.
 */
@Service
open class PollDao : BaseDao<PollDO>(PollDO::class.java) {

    override fun newInstance(): PollDO {
        return PollDO()
    }

    /** Every user may create a poll (as its owner, see [hasAccess]). */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return true
    }

    override fun hasAccess(
        user: PFUserDO,
        obj: PollDO?,
        oldObj: PollDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        val userId = user.id
        val result = when {
            userId == null -> false
            // Lists: every entry is checked on its own.
            obj == null -> operationType == OperationType.SELECT || operationType == OperationType.INSERT
            operationType == OperationType.SELECT -> hasFullAccess(obj, userId) || (!obj.template && isAttendee(obj, userId))
            // A new poll belongs to the user creating it (an id is an undelete, decided by the stored poll).
            operationType == OperationType.INSERT && obj.id == null -> obj.owner?.id == userId
            // Decided by the stored poll, never by the posted one: otherwise anybody could add themselves to the
            // users with full access. A finished poll isn't changed anymore (but deleted), a template is.
            else -> {
                val dbObj = oldObj ?: obj.id?.let { find(it, checkAccess = false) }
                dbObj != null && hasFullAccess(dbObj, userId)
                        && (operationType != OperationType.UPDATE || dbObj.state != PollDO.State.FINISHED)
            }
        }
        if (!result && throwException) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "PollDO", operationType)
        }
        return result
    }

    /** The owner, the users with full access and the members of the groups with full access. */
    fun hasFullAccess(poll: PollDO, userId: Long?): Boolean {
        userId ?: return false
        return poll.owner?.id == userId
                || PollDO.toLongArray(poll.fullAccessUserIds).contains(userId)
                || isMemberOfAnyGroup(userId, poll.fullAccessGroupIds)
    }

    /** The attendees and the members of the attending groups. A template has no attendees. */
    fun isAttendee(poll: PollDO, userId: Long?): Boolean {
        userId ?: return false
        if (poll.template) {
            return false
        }
        return PollDO.toLongArray(poll.attendeeUserIds).contains(userId)
                || isMemberOfAnyGroup(userId, poll.attendeeGroupIds)
    }

    /** What the given user is to the poll; empty if nothing (no access at all). */
    fun getAssignments(poll: PollDO, userId: Long?): Set<PollAssignment> {
        val result = mutableSetOf<PollAssignment>()
        if (userId == null) {
            return result
        }
        if (poll.owner?.id == userId) {
            result.add(PollAssignment.OWNER)
        }
        if (hasFullAccess(poll, userId)) {
            result.add(PollAssignment.ACCESS)
        }
        if (isAttendee(poll, userId)) {
            result.add(PollAssignment.ATTENDEE)
        }
        return result
    }

    /**
     * The ids of all attendees: the attending users and the members of the attending groups (deleted and deactivated
     * users excluded).
     */
    fun getAttendeeIds(poll: PollDO): Set<Long> {
        if (poll.template) {
            return emptySet()
        }
        return resolveUserIds(poll.attendeeUserIds, poll.attendeeGroupIds)
    }

    /** The ids of all users with full access, the owner included (deleted and deactivated users excluded). */
    fun getFullAccessUserIds(poll: PollDO): Set<Long> {
        val result = resolveUserIds(poll.fullAccessUserIds, poll.fullAccessGroupIds).toMutableSet()
        poll.owner?.id?.let { ownerId ->
            if (isActiveUser(ownerId)) result.add(ownerId)
        }
        return result
    }

    private fun resolveUserIds(userIds: String?, groupIds: String?): Set<Long> {
        val result = mutableSetOf<Long>()
        PollDO.toLongArray(userIds).forEach { result.add(it) }
        PollDO.toLongArray(groupIds).forEach { groupId ->
            val group = userGroupCache.getGroup(groupId)
            if (group != null && !group.deleted) {
                group.assignedUsers?.forEach { user -> user.id?.let { result.add(it) } }
            }
        }
        return result.filter { isActiveUser(it) }.toSet()
    }

    private fun isActiveUser(userId: Long): Boolean {
        val user = userGroupCache.getUser(userId) ?: return false
        return !user.deleted && !user.deactivated
    }

    private fun isMemberOfAnyGroup(userId: Long, groupIds: String?): Boolean {
        val ids = PollDO.toLongArray(groupIds)
        if (ids.isEmpty()) {
            return false
        }
        return userGroupCache.isUserMemberOfAtLeastOneGroup(userId, *ids.toTypedArray())
    }
}
