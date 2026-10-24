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

import org.hibernate.Hibernate
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

/**
 * DAO of the ad hoc booking grants ([TaskBookingGrantDO]).
 *
 * Who may edit the time sheets of others on all tasks of a grant ([TimesheetDao.hasForeignTimesheetWriteAccess])
 * grants, changes and revokes (deletes) it and reads it. The user of a grant reads it, but changes nothing. On an
 * update, the stored tasks count as well as the posted ones, so a grant can't be moved to a task the user may not
 * grant on.
 *
 * An employee without this right proposes a grant ([TaskBookingGrantDO.acceptanceRequired]) on tasks they may book own
 * time sheets on by their groups ([mayPropose]), for [MAX_PROPOSAL_DAYS] days at most, and reads and revokes the own
 * proposals (who proposed is the user of the insert in the history).
 */
@Service
open class TaskBookingGrantDao : BaseDao<TaskBookingGrantDO>(TaskBookingGrantDO::class.java) {
    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

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
        // The stored tasks as well: the posted ones may differ.
        val allowed = mayGrantAll(user, taskIdsOf(obj) + taskIdsOf(oldObj)) ||
                (operationType == OperationType.SELECT && obj.user?.id != null && obj.user?.id == user.id) ||
                isProposalAccess(user, obj, operationType)
        if (!allowed && throwException) {
            throw AccessException("access.exception.noAccess")
        }
        return allowed
    }

    override fun hasHistoryAccess(user: PFUserDO, obj: TaskBookingGrantDO, throwException: Boolean): Boolean {
        val allowed = mayGrantAll(user, taskIdsOf(obj)) || isProposalAccess(user, obj, OperationType.SELECT)
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

    /**
     * Whether the user may propose a grant on the given task ([TaskBookingGrantDO.acceptanceRequired]): who may book
     * own time sheets on it by the groups, it's bookable. A grant of the user doesn't count, a grant isn't passed on.
     */
    open fun mayPropose(user: PFUserDO, taskId: Long?): Boolean {
        val node = taskTree.getTaskNodeById(taskId) ?: return false
        return !taskTree.isRootNode(node) && node.isBookableForTimesheets &&
                accessChecker.hasPermission(user, taskId, AccessType.OWN_TIMESHEETS, OperationType.INSERT, false) &&
                timesheetDao.checkTaskBookable(node, false)
    }

    /**
     * Whether the user may grant on the task, or propose a grant on it or one of its descendants: the section of the
     * grants in the task form.
     */
    open fun mayGrantOrPropose(user: PFUserDO, taskId: Long?): Boolean {
        if (mayGrant(user, taskId)) {
            return true
        }
        val node = taskTree.getTaskNodeById(taskId) ?: return false
        return (sequenceOf(node.taskId) + node.descendantIds.asSequence()).any { mayPropose(user, it) }
    }

    /**
     * Proposing (insert of a grant requiring the acceptance on tasks the user may propose on), reading and revoking
     * the own proposals: proposed by the user (the insert in the history).
     */
    private fun isProposalAccess(user: PFUserDO, obj: TaskBookingGrantDO, operationType: OperationType): Boolean {
        return when (operationType) {
            OperationType.INSERT -> obj.acceptanceRequired && taskIdsOf(obj).let { taskIds ->
                taskIds.isNotEmpty() && taskIds.all { mayPropose(user, it) }
            }

            OperationType.SELECT, OperationType.DELETE -> obj.id?.let { selectGrantedByUserIds(listOf(it))[it] } == user.id

            else -> false
        }
    }

    /**
     * Whether the user may grant on every given task; false for none.
     */
    open fun mayGrantAll(user: PFUserDO, taskIds: Collection<Long>): Boolean {
        return taskIds.isNotEmpty() && taskIds.all { mayGrant(user, it) }
    }

    /**
     * The tasks of the grant; of a grant loaded without its positions from the database.
     */
    private fun taskIdsOf(obj: TaskBookingGrantDO?): Set<Long> {
        obj ?: return emptySet()
        val positions = obj.positions
        if (positions != null && Hibernate.isInitialized(positions)) {
            return positions.mapNotNullTo(mutableSetOf()) { it.task?.id }
        }
        val id = obj.id ?: return emptySet()
        return persistenceService.executeQuery(
            "SELECT p.task.id FROM TaskBookingGrantTaskDO p WHERE p.grant.id = :id",
            java.lang.Long::class.java,
            Pair("id", id),
        ).mapTo(mutableSetOf()) { it.toLong() }
    }

    override fun onInsert(obj: TaskBookingGrantDO) {
        validate(obj)
    }

    override fun onUpdate(obj: TaskBookingGrantDO, dbObj: TaskBookingGrantDO) {
        if (obj.user?.id != dbObj.user?.id) {
            // Another user is another grant.
            throw UserException(ERROR_USER_CHANGED)
        }
        if (!obj.deleted) {
            // Revoking is always possible, also when a task is closed in the meantime.
            validate(obj)
        }
    }

    /**
     * The user and the period must be set, there must be at least one task, none twice, each bookable, and the cost
     * units of each, if any, must be ones of its task.
     */
    open fun validate(obj: TaskBookingGrantDO) {
        val validFrom = obj.validFrom
        val validUntil = obj.validUntil
        val positions = obj.positions ?: emptyList()
        val taskIds = positions.map { it.task?.id }
        val error = when {
            obj.user?.id == null -> ERROR_USER_NOT_SET
            validFrom == null || validUntil == null -> ERROR_PERIOD_NOT_SET
            validUntil.isBefore(validFrom) -> ERROR_END_BEFORE_START
            positions.isEmpty() -> ERROR_NO_TASK
            taskIds.toSet().size != taskIds.size -> ERROR_TASK_TWICE
            else -> positions.firstNotNullOfOrNull { validatePosition(it) }
                ?: ERROR_PERIOD_TOO_LONG.takeIf {
                    obj.acceptanceRequired && ChronoUnit.DAYS.between(validFrom, validUntil) >= MAX_PROPOSAL_DAYS
                }
        }
        if (error != null) {
            throw UserException(error)
        }
    }

    private fun validatePosition(position: TaskBookingGrantTaskDO): String? {
        val node = taskTree.getTaskNodeById(position.task?.id) ?: return ERROR_TASK_NOT_BOOKABLE
        if (!node.isBookableForTimesheets || taskTree.isRootNode(node)) {
            return ERROR_TASK_NOT_BOOKABLE
        }
        val kost2Ids = position.kost2IdSet ?: return null
        val allowed = taskTree.getKost2List(node.taskId)?.mapNotNullTo(mutableSetOf()) { it.id } ?: emptySet()
        return if (allowed.containsAll(kost2Ids)) null else ERROR_INVALID_KOST2
    }

    /**
     * The grants with a position on one of the given tasks, revoked ones included (newest first). The caller checks
     * the access.
     */
    open fun selectByTasks(taskIds: Collection<Long>): List<TaskBookingGrantDO> {
        val ids = persistenceService.executeQueryBatched(
            "SELECT DISTINCT p.grant.id FROM TaskBookingGrantTaskDO p WHERE p.task.id IN :taskIds",
            java.lang.Long::class.java,
            batchParam = "taskIds",
            batchValues = taskIds,
        ).mapTo(mutableSetOf()) { it.toLong() }
        return persistenceService.executeQueryBatched(
            "SELECT DISTINCT g FROM TaskBookingGrantDO g LEFT JOIN FETCH g.positions WHERE g.id IN :ids",
            TaskBookingGrantDO::class.java,
            batchParam = "ids",
            batchValues = ids,
        ).sortedWith(compareByDescending<TaskBookingGrantDO> { it.validUntil }.thenByDescending { it.id })
    }

    /**
     * Who granted, by grant id: the user of the insert in the history. The caller checks the access to the grants.
     */
    open fun selectGrantedByUserIds(grantIds: Collection<Long>): Map<Long, Long> {
        return persistenceService.executeQueryBatched(
            "SELECT h.entityId, h.modifiedBy FROM HistoryEntryDO h WHERE h.entityName = :entityName" +
                    " AND h.entityOpType = :opType AND h.entityId IN :ids",
            Array<Any?>::class.java,
            batchParam = "ids",
            batchValues = grantIds,
            Pair("entityName", TaskBookingGrantDO::class.java.name),
            Pair("opType", EntityOpType.Insert),
        ).mapNotNull { row ->
            val grantId = (row[0] as? Number)?.toLong() ?: return@mapNotNull null
            val userId = (row[1] as? String)?.toLongOrNull() ?: return@mapNotNull null
            grantId to userId
        }.toMap()
    }

    /**
     * The number of time sheets to be accepted per grant id (grants without any are missing). The caller checks the
     * access to the grants.
     */
    open fun selectPendingTimesheetCounts(grantIds: Collection<Long>): Map<Long, Int> {
        return persistenceService.executeQueryBatched(
            "SELECT t.bookingGrant.id, COUNT(t) FROM TimesheetDO t WHERE t.acceptancePending = true" +
                    " AND t.deleted = false AND t.bookingGrant.id IN :ids GROUP BY t.bookingGrant.id",
            Array<Any?>::class.java,
            batchParam = "ids",
            batchValues = grantIds,
        ).mapNotNull { row ->
            val grantId = (row[0] as? Number)?.toLong() ?: return@mapNotNull null
            grantId to ((row[1] as? Number)?.toInt() ?: 0)
        }.toMap()
    }

    /**
     * The grant with its positions, if any. The caller checks the access.
     */
    open fun selectWithPositions(id: Long): TaskBookingGrantDO? {
        return persistenceService.executeQuery(
            "SELECT g FROM TaskBookingGrantDO g LEFT JOIN FETCH g.positions WHERE g.id = :id",
            TaskBookingGrantDO::class.java,
            Pair("id", id),
        ).firstOrNull()
    }

    /**
     * All grants with their positions, for [TaskBookingGrantCache]. No access check.
     */
    internal fun selectAllWithPositions(): List<TaskBookingGrantDO> {
        return persistenceService.executeQuery(
            "SELECT DISTINCT g FROM TaskBookingGrantDO g LEFT JOIN FETCH g.positions",
            TaskBookingGrantDO::class.java,
        )
    }

    companion object {
        const val ERROR_END_BEFORE_START = "task.bookingGrant.error.endBeforeStart"
        const val ERROR_INVALID_KOST2 = "task.bookingGrant.error.invalidKost2"
        const val ERROR_NO_TASK = "task.bookingGrant.error.noTask"
        const val ERROR_PERIOD_NOT_SET = "task.bookingGrant.error.periodNotSet"
        const val ERROR_PERIOD_TOO_LONG = "task.bookingGrant.error.periodTooLong"
        const val ERROR_TASK_NOT_BOOKABLE = "task.bookingGrant.error.taskNotBookable"
        const val ERROR_TASK_TWICE = "task.bookingGrant.error.taskTwice"
        const val ERROR_USER_CHANGED = "task.bookingGrant.error.userChanged"
        const val ERROR_USER_NOT_SET = "task.bookingGrant.error.userNotSet"

        /** The longest period of a proposed grant ([TaskBookingGrantDO.acceptanceRequired]), in days. */
        const val MAX_PROPOSAL_DAYS = 31L

        private val DEFAULT_SORT_PROPERTIES = arrayOf(SortProperty("validUntil", SortOrder.DESCENDING))
    }
}
