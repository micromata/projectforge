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
import org.projectforge.business.fibu.kost.KostCache
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
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

/**
 * Ad hoc booking grants ([TaskBookingGrantDO]) of a task, for the task form of projectforge-next, and the invitation
 * page a granted user accepts a link on.
 *
 * Who may grant on a task is the DAO's decision ([TaskBookingGrantDao.mayGrant]): who may edit the time sheets of
 * others on it. Checked by every endpoint for the task stored in the database.
 */
@RestController
@RequestMapping("${Rest.URL}/taskBookingGrant")
class TaskBookingGrantRest {
    enum class Status { ACTIVE, UPCOMING, EXPIRED, PENDING, INVITATION_EXPIRED, REVOKED }

    class Entry(
        val id: Long,
        val userId: Long?,
        val userName: String?,
        val validFrom: LocalDate?,
        val validUntil: LocalDate?,
        val kost2Id: Long?,
        val kost2Title: String?,
        val comment: String?,
        val grantedByName: String?,
        val created: Date?,
        val status: Status,
        val statusAsString: String,
    )

    /**
     * @param mayGrant Whether the logged-in user may grant on the task; if not, nothing else is sent.
     * @param bookable Whether time sheets may be booked on the task at all (a grant on it is refused otherwise).
     */
    class Info(
        val mayGrant: Boolean,
        val bookable: Boolean = false,
        val kost2List: List<TaskServicesRest.Kost2>? = null,
        val grants: List<Entry>? = null,
    )

    class GrantRequest(
        val taskId: Long? = null,
        val userId: Long? = null,
        val validFrom: LocalDate? = null,
        val validUntil: LocalDate? = null,
        val kost2Id: Long? = null,
        val comment: String? = null,
        val asInvitation: Boolean? = null,
    )

    /**
     * @param message The translated error, if [success] is false.
     * @param invitationUrl The link of an invitation, only known now (its token isn't stored).
     */
    class Result(
        val success: Boolean,
        val message: String? = null,
        val invitationUrl: String? = null,
        val info: Info? = null,
    )

    /** What the invited user sees of the grant before accepting it. */
    class Invitation(
        val taskId: Long,
        val taskPath: String,
        val validFrom: LocalDate?,
        val validUntil: LocalDate?,
        val grantedByName: String?,
        val comment: String?,
    )

    class AcceptRequest(val token: String? = null)

    class AcceptResult(val success: Boolean, val message: String? = null, val taskId: Long? = null)

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var taskBookingGrantDao: TaskBookingGrantDao

    @Autowired
    private lateinit var taskBookingGrantService: TaskBookingGrantService

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @AccessChecked("TaskBookingGrantDao.mayGrant: who may edit foreign time sheets on the task, otherwise only mayGrant=false")
    @GetMapping("info")
    fun getInfo(@RequestParam("taskId") taskId: Long): Info {
        return buildInfo(taskId)
    }

    @AccessChecked("TaskBookingGrantDao: insert access on the task (mayGrant)")
    @PostMapping("grant")
    fun grant(@RequestBody request: GrantRequest): Result {
        val taskId = request.taskId ?: return Result(false, translate("task.bookingGrant.error.taskNotBookable"))
        val obj = TaskBookingGrantDO()
        obj.task = taskTree.getTaskById(taskId) ?: return Result(false, translate("task.bookingGrant.error.taskNotBookable"))
        obj.user = userGroupCache.getUser(request.userId)
        obj.validFrom = request.validFrom
        obj.validUntil = request.validUntil
        obj.kost2 = request.kost2Id?.let { kostCache.getKost2(it) }
        obj.comment = request.comment?.trim()?.takeIf { it.isNotEmpty() }
        return runCatchingUserException {
            val result = taskBookingGrantService.grant(obj, asInvitation = request.asInvitation == true)
            Result(true, invitationUrl = result.invitationUrl, info = buildInfo(taskId))
        }
    }

    /**
     * Revokes the grant (marks it as deleted): the user books no more on the task, but keeps reading the own time
     * sheets. Access checked by the DAO for the stored task.
     */
    @AccessChecked("TaskBookingGrantDao: delete access on the stored task (mayGrant)")
    @PostMapping("revoke")
    fun revoke(@RequestParam("id") id: Long): Result {
        return runCatchingUserException {
            val obj = taskBookingGrantDao.find(id) ?: throw AccessException("access.exception.noAccess")
            taskBookingGrantDao.markAsDeleted(obj)
            Result(true, info = buildInfo(obj.task?.id!!))
        }
    }

    /**
     * The grant of an invitation link, only for its user and only as long as it may be accepted. Anybody else gets
     * a 404, nothing of the grant.
     */
    @AccessChecked("Token and the logged-in user must be the grant's (TaskBookingGrantService.getInvitation)")
    @GetMapping("invitation")
    fun getInvitation(@RequestParam("token") token: String): ResponseEntity<Invitation> {
        val grant = taskBookingGrantService.getInvitation(token) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val taskId = grant.task?.id!!
        return ResponseEntity.ok(
            Invitation(
                taskId = taskId,
                taskPath = TaskServicesRest.formatPath(taskId),
                validFrom = grant.validFrom,
                validUntil = grant.validUntil,
                grantedByName = userGroupCache.getUser(grant.grantedBy?.id)?.getFullname(),
                comment = grant.comment,
            )
        )
    }

    @AccessChecked("Token and the logged-in user must be the grant's (TaskBookingGrantService.accept)")
    @PostMapping("accept")
    fun accept(@RequestBody request: AcceptRequest): AcceptResult {
        return try {
            val grant = taskBookingGrantService.accept(request.token)
            AcceptResult(true, taskId = grant.task?.id)
        } catch (ex: UserException) {
            AcceptResult(false, translateMsg(ex))
        }
    }

    private fun buildInfo(taskId: Long): Info {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        if (!taskBookingGrantDao.mayGrant(user, taskId)) {
            return Info(mayGrant = false)
        }
        val node = taskTree.getTaskNodeById(taskId) ?: return Info(mayGrant = false)
        val kost2List = taskTree.getKost2List(taskId)?.map {
            TaskServicesRest.Kost2(it.id!!, KostFormatter.instance.formatKost2(it, formatType = KostFormatter.FormatType.TEXT, 80))
        }
        val today = PFDay.now().localDate
        val now = Date()
        val grants = taskBookingGrantDao.selectByTask(taskId).map { grant ->
            val status = when {
                grant.deleted -> Status.REVOKED
                !grant.accepted && grant.invitationExpiresAt?.before(now) != false -> Status.INVITATION_EXPIRED
                !grant.accepted -> Status.PENDING
                grant.validUntil?.isBefore(today) == true -> Status.EXPIRED
                grant.validFrom?.isAfter(today) == true -> Status.UPCOMING
                else -> Status.ACTIVE
            }
            val kost2 = grant.kost2?.id?.let { kostCache.getKost2(it) }
            Entry(
                id = grant.id!!,
                userId = grant.user?.id,
                userName = userGroupCache.getUser(grant.user?.id)?.getFullname(),
                validFrom = grant.validFrom,
                validUntil = grant.validUntil,
                kost2Id = kost2?.id,
                kost2Title = kost2?.let { KostFormatter.instance.formatKost2(it, formatType = KostFormatter.FormatType.TEXT, 80) },
                comment = grant.comment,
                grantedByName = userGroupCache.getUser(grant.grantedBy?.id)?.getFullname(),
                created = grant.created,
                status = status,
                statusAsString = translate("task.bookingGrant.status.${status.name}"),
            )
        }
        return Info(
            mayGrant = true,
            bookable = node.isBookableForTimesheets && !taskTree.isRootNode(node),
            kost2List = kost2List,
            grants = grants,
        )
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
}
