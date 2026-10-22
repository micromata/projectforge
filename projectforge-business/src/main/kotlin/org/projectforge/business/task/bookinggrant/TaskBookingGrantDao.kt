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

package org.projectforge.business.task.bookinggrant

import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * DAO of the ad hoc booking grants ([TaskBookingGrantDO]).
 *
 * Who may edit the time sheets of others on a task ([TimesheetDao.hasForeignTimesheetWriteAccess]) grants, changes and
 * revokes (deletes) bookings on it and reads its grants. The user of a grant reads it, but changes nothing: accepting
 * an invitation is done by [TaskBookingGrantService.accept]. The task of a grant is always the stored one, so a
 * posted task id moves no grant to a task the user may not grant on.
 */
@Service
open class TaskBookingGrantDao : BaseDao<TaskBookingGrantDO>(TaskBookingGrantDO::class.java) {
    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    /** Only within [internalUpdateAccepted]: the invitation fields may change. */
    private val acceptingInvitation = ThreadLocal<Boolean>()

    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    override fun newInstance(): TaskBookingGrantDO {
        return TaskBookingGrantDO()
    }

    /**
     * Every user, the single grants are checked by [hasUserSelectAccess] with an object.
     */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    /**
     * Without a task, nobody may be said to grant: only admins and the finance group may grant on every task.
     */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user) ||
                accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)
    }

    override fun hasAccess(
        user: PFUserDO,
        obj: TaskBookingGrantDO?,
        oldObj: TaskBookingGrantDO?,
        operationType: OperationType,
        throwException: Boolean,
    ): Boolean {
        requireNotNull(obj) { "Given TaskBookingGrantDO as obj parameter mustn't be null." }
        // The stored task, if there is one: the posted one may differ.
        val taskId = oldObj?.task?.id ?: obj.task?.id
        val allowed = mayGrant(user, taskId) ||
                (operationType == OperationType.SELECT && obj.user?.id != null && obj.user?.id == user.id)
        if (!allowed && throwException) {
            throw AccessException("access.exception.noAccess")
        }
        return allowed
    }

    override fun hasHistoryAccess(user: PFUserDO, obj: TaskBookingGrantDO, throwException: Boolean): Boolean {
        val allowed = mayGrant(user, obj.task?.id)
        if (!allowed && throwException) {
            throw AccessException("access.exception.noAccess")
        }
        return allowed
    }

    /**
     * Whether the user may grant ad hoc bookings on the given task: who may edit the time sheets of others on it.
     */
    open fun mayGrant(user: PFUserDO, taskId: Long?): Boolean {
        return timesheetDao.hasForeignTimesheetWriteAccess(user, taskId)
    }

    override fun onInsert(obj: TaskBookingGrantDO) {
        validate(obj)
    }

    override fun onUpdate(obj: TaskBookingGrantDO, dbObj: TaskBookingGrantDO) {
        if (obj.task?.id != dbObj.task?.id || obj.user?.id != dbObj.user?.id) {
            // Another task or user is another grant.
            throw UserException(ERROR_TASK_OR_USER_CHANGED)
        }
        obj.grantedBy = dbObj.grantedBy // Set by the insert only.
        if (acceptingInvitation.get() != true) {
            // Set by the insert and by accepting the invitation only.
            obj.acceptedAt = dbObj.acceptedAt
            obj.invitationTokenHash = dbObj.invitationTokenHash
            obj.invitationExpiresAt = dbObj.invitationExpiresAt
        }
        validate(obj)
    }

    /**
     * Stores the accepted invitation, see [TaskBookingGrantService.accept], which checks that the grant is the
     * logged-in user's: the user of a grant may not change it otherwise.
     */
    internal fun internalUpdateAccepted(obj: TaskBookingGrantDO) {
        acceptingInvitation.set(true)
        try {
            update(obj, checkAccess = false)
        } finally {
            acceptingInvitation.remove()
        }
    }

    /**
     * The task must be bookable, the period complete and the cost unit, if any, one of the task.
     */
    open fun validate(obj: TaskBookingGrantDO) {
        val node = taskTree.getTaskNodeById(obj.task?.id) ?: throw UserException(ERROR_TASK_NOT_BOOKABLE)
        val validFrom = obj.validFrom
        val validUntil = obj.validUntil
        val error = when {
            obj.user?.id == null -> ERROR_USER_NOT_SET
            !node.isBookableForTimesheets || taskTree.isRootNode(node) -> ERROR_TASK_NOT_BOOKABLE
            validFrom == null || validUntil == null -> ERROR_PERIOD_NOT_SET
            validUntil.isBefore(validFrom) -> ERROR_END_BEFORE_START
            obj.kost2?.id != null && taskTree.getKost2List(node.taskId)?.none { it.id == obj.kost2?.id } != false ->
                ERROR_INVALID_KOST2

            else -> null
        }
        if (error != null) {
            throw UserException(error)
        }
    }

    /**
     * The grants on the given task, revoked ones included (newest first). The caller checks the access.
     */
    open fun selectByTask(taskId: Long): List<TaskBookingGrantDO> {
        return persistenceService.executeQuery(
            "SELECT g FROM TaskBookingGrantDO g WHERE g.task.id = :taskId ORDER BY g.validUntil DESC, g.id DESC",
            TaskBookingGrantDO::class.java,
            Pair("taskId", taskId),
        )
    }

    /**
     * The grant of the given invitation token hash, if any. The caller checks the access.
     */
    open fun selectByInvitationTokenHash(hash: String): TaskBookingGrantDO? {
        return persistenceService.selectSingleResult(
            "SELECT g FROM TaskBookingGrantDO g WHERE g.invitationTokenHash = :hash",
            TaskBookingGrantDO::class.java,
            Pair("hash", hash),
        )
    }

    companion object {
        const val ERROR_END_BEFORE_START = "task.bookingGrant.error.endBeforeStart"
        const val ERROR_INVALID_KOST2 = "task.bookingGrant.error.invalidKost2"
        const val ERROR_PERIOD_NOT_SET = "task.bookingGrant.error.periodNotSet"
        const val ERROR_TASK_NOT_BOOKABLE = "task.bookingGrant.error.taskNotBookable"
        const val ERROR_TASK_OR_USER_CHANGED = "task.bookingGrant.error.taskOrUserChanged"
        const val ERROR_USER_NOT_SET = "task.bookingGrant.error.userNotSet"

        private val DEFAULT_SORT_PROPERTIES = arrayOf(SortProperty("validUntil", SortOrder.DESCENDING))
    }
}
