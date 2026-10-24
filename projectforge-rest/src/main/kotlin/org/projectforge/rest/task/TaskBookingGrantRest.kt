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

package org.projectforge.rest.task

import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.task.bookinggrant.TaskBookingGrantDO
import org.projectforge.business.task.bookinggrant.TaskBookingGrantDao
import org.projectforge.business.task.bookinggrant.TaskBookingGrantService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

/**
 * Ad hoc booking grants ([TaskBookingGrantDO]) of a task and its descendants, for the task form of projectforge-next.
 *
 * Who may grant on a task is the DAO's decision ([TaskBookingGrantDao.mayGrant]): who may edit the time sheets of
 * others on it. Checked by every endpoint for the tasks stored in the database, a grant with several tasks needs it on
 * all of them. An employee who may book on a task proposes grants ([TaskBookingGrantDao.mayPropose]) and sees and
 * revokes the own proposals only.
 */
@RestController
@RequestMapping("${Rest.URL}/taskBookingGrant")
class TaskBookingGrantRest {
    enum class Status { ACTIVE, UPCOMING, EXPIRED, REVOKED }

    /**
     * A task of a grant.
     * @param path The titles from the task the info is asked for down to this task, or the whole path for a task
     * outside of it.
     * @param kost2Titles The cost units it may be booked on, null for all.
     */
    class Position(val taskId: Long, val path: String, val kost2Titles: List<String>?)

    class Entry(
        val id: Long,
        val userId: Long?,
        val userName: String?,
        val validFrom: LocalDate?,
        val validUntil: LocalDate?,
        val positions: List<Position>,
        /** The number of tasks of the grant the logged-in user doesn't see: neither to grant nor to propose on. */
        val hiddenPositions: Int,
        val comment: String?,
        val grantedByName: String?,
        val created: Date?,
        val status: Status,
        val statusAsString: String,
        /** Not revoked or expired, and the logged-in user may grant on all of its tasks or proposed it. */
        val revocable: Boolean,
        /** Proposed by an employee: its time sheets are to be accepted ([TaskBookingGrantDO.acceptanceRequired]). */
        val acceptanceRequired: Boolean = false,
        /** The number of its time sheets still to be accepted. */
        val pendingTimesheets: Int = 0,
        /** Proposed, not revoked or expired, and the logged-in user may grant on all of its tasks. */
        val confirmable: Boolean = false,
    )

    /**
     * A task a grant may be given on: the task the info is asked for or one of its descendants, bookable.
     * @param path See [Position.path].
     */
    class GrantableTask(val taskId: Long, val path: String, val kost2List: List<TaskServicesRest.Kost2>?)

    /**
     * @param mayGrant Whether the logged-in user may grant on the task; if neither this nor [mayPropose], nothing else
     * is sent.
     * @param mayPropose Whether the logged-in user may only propose grants (on tasks of the subtree they book on): the
     * time sheets are to be accepted, the period is [TaskBookingGrantDao.MAX_PROPOSAL_DAYS] days at most, [grants] are
     * the own proposals only.
     * @param tasks The tasks a grant may be given on, the task itself first (if bookable).
     * @param tasksTruncated Whether there are more than [MAX_GRANTABLE_TASKS] such tasks, the others aren't sent.
     */
    class Info(
        val mayGrant: Boolean,
        val mayPropose: Boolean = false,
        val tasks: List<GrantableTask>? = null,
        val tasksTruncated: Boolean? = null,
        val grants: List<Entry>? = null,
    )

    class PositionRequest(val taskId: Long? = null, val kost2Ids: List<Long>? = null)

    class GrantRequest(
        /** The task whose form the grant is given in: the answer is its info. */
        val taskId: Long? = null,
        val userIds: List<Long>? = null,
        val positions: List<PositionRequest>? = null,
        val validFrom: LocalDate? = null,
        val validUntil: LocalDate? = null,
        val comment: String? = null,
    )

    /**
     * @param message The translated error, if [success] is false.
     */
    class Result(
        val success: Boolean,
        val message: String? = null,
        val info: Info? = null,
    )

    @Autowired
    private lateinit var taskBookingGrantDao: TaskBookingGrantDao

    @Autowired
    private lateinit var taskBookingGrantService: TaskBookingGrantService

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @AccessChecked("TaskBookingGrantDao.mayGrant/mayPropose: who may edit foreign time sheets on the task (or propose: own proposals only), otherwise only mayGrant=false")
    @GetMapping("info")
    fun getInfo(@RequestParam("taskId") taskId: Long): Info {
        return buildInfo(taskId)
    }

    /**
     * One grant per user, each with all positions. All or nothing.
     */
    @AccessChecked("TaskBookingGrantDao: insert access on all tasks of the grants (mayGrant, or mayPropose for a proposal)")
    @PostMapping("grant")
    fun grant(@RequestBody request: GrantRequest): Result {
        val taskId = request.taskId ?: return Result(false, translate("task.bookingGrant.error.noTask"))
        return runCatchingUserException {
            taskBookingGrantService.grant(
                userIds = request.userIds ?: emptyList(),
                positions = (request.positions ?: emptyList()).mapNotNull { position ->
                    position.taskId?.let { TaskBookingGrantService.Position(it, position.kost2Ids?.toSet()) }
                },
                validFrom = request.validFrom,
                validUntil = request.validUntil,
                comment = request.comment,
            )
            Result(true, info = buildInfo(taskId))
        }
    }

    /**
     * Revokes the grant (marks it as deleted): the user books no more on its tasks, but keeps reading the own time
     * sheets. Access checked by the DAO for the stored tasks.
     * @param taskId The task whose form the grant is revoked in: the answer is its info.
     */
    @AccessChecked("TaskBookingGrantDao: delete access on all stored tasks of the grant (mayGrant), or the own proposal")
    @PostMapping("revoke")
    fun revoke(@RequestParam("id") id: Long, @RequestParam("taskId") taskId: Long): Result {
        return runCatchingUserException {
            val obj = taskBookingGrantDao.selectWithPositions(id) ?: throw AccessException("access.exception.noAccess")
            taskBookingGrantDao.markAsDeleted(obj)
            Result(true, info = buildInfo(taskId))
        }
    }

    /**
     * Confirms a proposed grant: the time sheets booked by it from now on aren't to be accepted anymore.
     * @param taskId The task whose form the grant is confirmed in: the answer is its info.
     */
    @AccessChecked("TaskBookingGrantService.confirm: who may grant on all stored tasks of the grant (mayGrantAll)")
    @PostMapping("confirm")
    fun confirm(@RequestParam("id") id: Long, @RequestParam("taskId") taskId: Long): Result {
        return runCatchingUserException {
            taskBookingGrantService.confirm(id)
            Result(true, info = buildInfo(taskId))
        }
    }

    private fun buildInfo(taskId: Long): Info {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val node = taskTree.getTaskNodeById(taskId) ?: return Info(mayGrant = false)
        val mayGrant = taskBookingGrantDao.mayGrant(user, taskId)
        val descendantIds = node.descendantIds
        val subtree = descendantIds.toSet() + taskId
        val candidates = listOf(node) + descendantIds.mapNotNull { taskTree.getTaskNodeById(it) }
        val grantable = if (mayGrant) {
            candidates.filter { isGrantable(user, it) }
        } else {
            candidates.filter { !it.task.deleted && taskBookingGrantDao.mayPropose(user, it.taskId) }
        }
        if (!mayGrant && grantable.isEmpty()) {
            return Info(mayGrant = false)
        }
        val today = PFDay.now().localDate
        val allGrantDOs = taskBookingGrantDao.selectByTasks(subtree)
        val grantedBy = taskBookingGrantDao.selectGrantedByUserIds(allGrantDOs.mapNotNull { it.id })
        // Who only proposes sees the own proposals only; who may grant those with a task of the subtree they may grant
        // on (the rights may be withdrawn in a part of it), and the own proposals.
        val grantDOs = allGrantDOs.filter { grant ->
            grantedBy[grant.id] == user.id || mayGrant && grant.positions.orEmpty().any { position ->
                position.task?.id?.let { it in subtree && taskBookingGrantDao.mayGrant(user, it) } == true
            }
        }
        val pending = taskBookingGrantDao.selectPendingTimesheetCounts(grantDOs.mapNotNull { it.id })
        val grants = grantDOs.map {
            toEntry(user, it, taskId, subtree, today, grantedBy[it.id], pending[it.id] ?: 0)
        }
        return Info(
            mayGrant = mayGrant,
            mayPropose = !mayGrant,
            tasks = grantable.take(MAX_GRANTABLE_TASKS).map { task ->
                GrantableTask(
                    taskId = task.taskId,
                    path = formatPath(task.taskId, taskId, subtree),
                    kost2List = taskTree.getKost2List(task.taskId)?.map { TaskServicesRest.Kost2(it.id!!, format(it)) },
                )
            },
            tasksTruncated = grantable.size > MAX_GRANTABLE_TASKS,
            grants = grants,
        )
    }

    private fun isGrantable(user: PFUserDO, node: TaskNode): Boolean {
        return node.isBookableForTimesheets && !taskTree.isRootNode(node) && !node.task.deleted &&
                taskBookingGrantDao.mayGrant(user, node.taskId)
    }

    /**
     * A task of the grant is only shown if the user may grant or propose on it: otherwise the user may not even know
     * its title ([Entry.hiddenPositions]).
     */
    private fun toEntry(
        user: PFUserDO,
        grant: TaskBookingGrantDO,
        baseTaskId: Long,
        subtree: Set<Long>,
        today: LocalDate,
        grantedByUserId: Long?,
        pendingTimesheets: Int,
    ): Entry {
        val status = when {
            grant.deleted -> Status.REVOKED
            grant.validUntil?.isBefore(today) == true -> Status.EXPIRED
            grant.validFrom?.isAfter(today) == true -> Status.UPCOMING
            else -> Status.ACTIVE
        }
        val positions = grant.positions ?: emptyList()
        val shown = positions.filter { position ->
            position.task?.id?.let {
                taskBookingGrantDao.mayGrant(user, it) || taskBookingGrantDao.mayPropose(user, it)
            } == true
        }
        val taskIds = positions.mapNotNull { it.task?.id }
        val open = status == Status.ACTIVE || status == Status.UPCOMING
        val mayGrantAll = taskBookingGrantDao.mayGrantAll(user, taskIds)
        return Entry(
            id = grant.id!!,
            userId = grant.user?.id,
            userName = userGroupCache.getUser(grant.user?.id)?.getFullname(),
            validFrom = grant.validFrom,
            validUntil = grant.validUntil,
            positions = shown.mapNotNull { position ->
                val positionTaskId = position.task?.id ?: return@mapNotNull null
                Position(
                    taskId = positionTaskId,
                    path = formatPath(positionTaskId, baseTaskId, subtree),
                    kost2Titles = position.kost2IdSet?.map { kost2Id ->
                        taskTree.getKost2List(positionTaskId)?.find { it.id == kost2Id }?.let { format(it) } ?: "#$kost2Id"
                    },
                )
            },
            hiddenPositions = positions.size - shown.size,
            comment = grant.comment,
            grantedByName = userGroupCache.getUser(grantedByUserId)?.getFullname(),
            created = grant.created,
            status = status,
            statusAsString = translate("task.bookingGrant.status.${status.name}"),
            revocable = open && (mayGrantAll || grantedByUserId == user.id),
            acceptanceRequired = grant.acceptanceRequired,
            pendingTimesheets = pendingTimesheets,
            confirmable = open && grant.acceptanceRequired && mayGrantAll,
        )
    }

    /**
     * The path from the base task down to the task, the base task included. The whole path for a task outside of it.
     */
    private fun formatPath(taskId: Long, baseTaskId: Long, subtree: Set<Long>): String {
        val node = taskTree.getTaskNodeById(taskId) ?: return ""
        val base = taskTree.getTaskNodeById(baseTaskId)
        if (base == null || taskId !in subtree) {
            return TaskServicesRest.formatPath(taskId)
        }
        return (listOf(base) + node.getPathToAncestor(baseTaskId)).joinToString(" | ") { it.task.title ?: "" }
    }

    private fun format(kost2: Kost2DO): String {
        return KostFormatter.instance.formatKost2(kost2, formatType = KostFormatter.FormatType.TEXT, 80)
    }

    /**
     * A refused grant (validation, access) is answered as a result with its message, not as an exception: the form
     * shows it.
     */
    private fun runCatchingUserException(block: () -> Result): Result {
        return try {
            block()
        } catch (ex: UserException) { // AccessException included
            Result(false, translateMsg(ex))
        }
    }

    companion object {
        const val MAX_GRANTABLE_TASKS = 200
    }
}
