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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Grants ad hoc bookings ([TaskBookingGrantDO]) to several users at once and informs each of them by mail, with the
 * links to book on the tasks. No token is needed: the right is the user's grant, so a forwarded link is of no use to
 * anybody else.
 */
@Service
open class TaskBookingGrantService {
    /**
     * A task of the grants to give, with the cost units it may be booked on (null or empty for all).
     */
    class Position(val taskId: Long, val kost2Ids: Set<Long>? = null)

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var taskBookingGrantDao: TaskBookingGrantDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var sendMail: SendMail

    /**
     * Inserts one grant per user, each with all positions, for the logged-in user as granting user, and mails it to
     * its user. All or nothing: every grant is validated and access checked (by the DAO) before the first is inserted.
     * @throws org.projectforge.common.i18n.UserException if a grant isn't valid or the logged-in user may not grant
     * on one of the tasks.
     */
    open fun grant(
        userIds: Collection<Long>,
        positions: List<Position>,
        validFrom: LocalDate?,
        validUntil: LocalDate?,
        comment: String?,
    ): List<TaskBookingGrantDO> {
        val loggedInUser = ThreadLocalUserContext.requiredLoggedInUser
        val users = userIds.distinct().map { userGroupCache.getUser(it) }
        val grants = (users.ifEmpty { listOf(null) }).map { user ->
            TaskBookingGrantDO().also { grant ->
                grant.user = user
                grant.validFrom = validFrom
                grant.validUntil = validUntil
                grant.comment = comment?.trim()?.takeIf { it.isNotEmpty() }
                positions.forEach { position ->
                    grant.addPosition(TaskBookingGrantTaskDO().also {
                        it.task = taskTree.getTaskById(position.taskId)
                        it.kost2IdSet = position.kost2Ids
                    })
                }
            }
        }
        grants.forEach {
            taskBookingGrantDao.hasLoggedInUserInsertAccess(it, throwException = true)
            taskBookingGrantDao.validate(it)
        }
        persistenceService.runInTransaction { _ ->
            grants.forEach { taskBookingGrantDao.insert(it) }
        }
        grants.forEach { sendGrantMail(it, loggedInUser) }
        return grants
    }

    private fun sendGrantMail(grant: TaskBookingGrantDO, grantedBy: PFUserDO) {
        val recipient = userGroupCache.getUser(grant.user?.id) ?: return
        if (recipient.email.isNullOrBlank()) {
            log.info { "User #${recipient.id} has no mail address, the booking grant #${grant.id} isn't mailed." }
            return
        }
        try {
            sendMail.send(prepareMail(grant, recipient, grantedBy))
        } catch (ex: Exception) {
            // The grant is given anyway, its user finds the tasks in the structure tree.
            log.error(ex) { "Couldn't mail the booking grant #${grant.id} to user #${recipient.id}: ${ex.message}" }
        }
    }

    /**
     * A plain text mail: no user data needs escaping. Each task with its path, its cost units and the link to book on
     * it.
     */
    internal fun prepareMail(grant: TaskBookingGrantDO, recipient: PFUserDO, grantedBy: PFUserDO): Mail {
        val period = "${PFDay.from(grant.validFrom!!).isoString} - ${PFDay.from(grant.validUntil!!).isoString}"
        val positions = grant.positions ?: emptyList()
        val paths = positions.map { formatPath(it.task?.id) }
        val mail = Mail()
        mail.contentType = Mail.CONTENTTYPE_TEXT
        mail.subject = if (paths.size == 1) {
            translate(recipient, "task.bookingGrant.mail.subject", paths.first())
        } else {
            translate(recipient, "task.bookingGrant.mail.subject.multiple", paths.size)
        }
        mail.setTo(recipient)
        mail.content = buildString {
            appendLine(translate(recipient, "task.bookingGrant.mail.greeting", recipient.firstname ?: ""))
            appendLine()
            appendLine(translate(recipient, "task.bookingGrant.mail.text", grantedBy.getFullname(), period))
            grant.comment?.takeIf { it.isNotBlank() }?.let {
                appendLine()
                appendLine(it)
            }
            positions.forEachIndexed { index, position ->
                appendLine()
                appendLine("- ${paths[index]}")
                position.kost2IdSet?.let { kost2Ids ->
                    val kost2 = kost2Ids.mapNotNull { kostCache.getKost2(it) }
                        .joinToString(", ") { KostFormatter.instance.formatKost2(it, KostFormatter.FormatType.TEXT, 80) }
                    appendLine("  ${translate(recipient, "fibu.kost2")}: $kost2")
                }
                appendLine("  ${sendMail.buildUrl("${Constants.NEXT_APP_PATH}timesheet/new?taskId=${position.task?.id}")}")
            }
        }
        return mail
    }

    private fun formatPath(taskId: Long?): String {
        return taskTree.getPathToRoot(taskId).joinToString(" | ") { it.task.title ?: "" }
    }

    private fun translate(recipient: PFUserDO, i18nKey: String, vararg params: Any): String {
        return I18nHelper.getLocalizedMessage(recipient, i18nKey, *params)
    }
}
