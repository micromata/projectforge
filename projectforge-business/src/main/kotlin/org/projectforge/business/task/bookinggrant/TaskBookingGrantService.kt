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
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Grants ad hoc bookings ([TaskBookingGrantDO]) directly or as an invitation link, and accepts such a link.
 *
 * An invitation link is bound to the user of the grant: only this user may accept it, after logging in. Its token is
 * random, used once and valid for [INVITATION_VALIDITY_DAYS] days at most; only its hash is stored.
 */
@Service
open class TaskBookingGrantService {
    /**
     * A grant just inserted, and the url of its invitation link (null for a grant given directly). The url is only
     * known now: the token isn't stored.
     */
    class GrantResult(val grant: TaskBookingGrantDO, val invitationUrl: String?)

    @Autowired
    private lateinit var taskBookingGrantDao: TaskBookingGrantDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var sendMail: SendMail

    /**
     * Inserts the grant for the logged-in user as granting user (access checked by the DAO) and informs the user of
     * the grant by mail.
     * @param asInvitation If true, the grant gives no access until its user accepts the invitation link; otherwise it
     * is accepted right away.
     */
    open fun grant(obj: TaskBookingGrantDO, asInvitation: Boolean): GrantResult {
        val loggedInUser = ThreadLocalUserContext.requiredLoggedInUser
        obj.grantedBy = loggedInUser
        var token: String? = null
        if (asInvitation) {
            token = NumberHelper.getSecureRandomAlphanumeric(TOKEN_LENGTH)
            obj.acceptedAt = null
            obj.invitationTokenHash = hash(token)
            val expires = PFDay.now().plusDays(INVITATION_VALIDITY_DAYS)
                .let { if (obj.validUntil != null && it.localDate.isAfter(obj.validUntil)) PFDay.from(obj.validUntil!!) else it }
            obj.invitationExpiresAt = PFDateTime.from(expires.localDate).endOfDay.utilDate
        } else {
            obj.acceptedAt = Date()
            obj.invitationTokenHash = null
            obj.invitationExpiresAt = null
        }
        taskBookingGrantDao.insert(obj)
        val invitationUrl = token?.let { buildInvitationUrl(it) }
        sendGrantMail(obj, invitationUrl)
        return GrantResult(obj, invitationUrl)
    }

    /**
     * The grant of the given invitation token, if the logged-in user is its user and it may still be accepted.
     * Otherwise null: nobody else learns anything of the grant (the attempt of another user is logged).
     */
    open fun getInvitation(token: String?): TaskBookingGrantDO? {
        token ?: return null
        val grant = taskBookingGrantDao.selectByInvitationTokenHash(hash(token)) ?: return null
        val loggedInUserId = ThreadLocalUserContext.loggedInUserId
        if (grant.user?.id != loggedInUserId) {
            log.warn { "User #$loggedInUserId tried to use the booking invitation #${grant.id} of user #${grant.user?.id}." }
            return null
        }
        if (grant.deleted || grant.accepted || grant.invitationExpiresAt?.before(Date()) != false) {
            return null
        }
        return grant
    }

    /**
     * Accepts the invitation of the given token for the logged-in user: from now on the grant gives access.
     * @throws UserException if the token isn't valid (any more) or isn't the logged-in user's.
     */
    open fun accept(token: String?): TaskBookingGrantDO {
        val grant = getInvitation(token) ?: throw UserException(ERROR_INVITATION_INVALID)
        grant.acceptedAt = Date()
        grant.invitationTokenHash = null
        grant.invitationExpiresAt = null
        // Not by TaskBookingGrantDao.update: its user may read, but not change the grant; and onUpdate keeps the
        // invitation fields as stored.
        taskBookingGrantDao.internalUpdateAccepted(grant)
        return grant
    }

    private fun sendGrantMail(grant: TaskBookingGrantDO, invitationUrl: String?) {
        val recipient = userGroupCache.getUser(grant.user?.id) ?: return
        if (recipient.email.isNullOrBlank()) {
            log.info { "User #${recipient.id} has no mail address, the booking grant #${grant.id} isn't mailed." }
            return
        }
        try {
            sendMail.send(prepareMail(grant, recipient, invitationUrl))
        } catch (ex: Exception) {
            // The grant is given anyway, the link is shown to the granting user.
            log.error(ex) { "Couldn't mail the booking grant #${grant.id} to user #${recipient.id}: ${ex.message}" }
        }
    }

    /**
     * A plain text mail: no user data needs escaping.
     */
    internal fun prepareMail(grant: TaskBookingGrantDO, recipient: PFUserDO, invitationUrl: String?): Mail {
        val grantedBy = userGroupCache.getUser(grant.grantedBy?.id)?.getFullname() ?: ""
        val taskPath = taskTree.getPathToRoot(grant.task?.id).joinToString(" | ") { it.task.title ?: "" }
        val period = "${PFDay.from(grant.validFrom!!).isoString} - ${PFDay.from(grant.validUntil!!).isoString}"
        val url = invitationUrl ?: sendMail.buildUrl(
            "${Constants.NEXT_APP_PATH}timesheet/new?taskId=${grant.task?.id}"
        )
        val mail = Mail()
        mail.contentType = Mail.CONTENTTYPE_TEXT
        mail.subject = translate(recipient, "task.bookingGrant.mail.subject", taskPath)
        mail.setTo(recipient)
        mail.content = buildString {
            appendLine(translate(recipient, "task.bookingGrant.mail.greeting", recipient.firstname ?: ""))
            appendLine()
            appendLine(translate(recipient, "task.bookingGrant.mail.text", grantedBy, taskPath, period))
            grant.comment?.takeIf { it.isNotBlank() }?.let {
                appendLine()
                appendLine(it)
            }
            appendLine()
            appendLine(
                translate(
                    recipient,
                    if (invitationUrl != null) "task.bookingGrant.mail.invitation" else "task.bookingGrant.mail.link"
                )
            )
            appendLine(url)
        }
        return mail
    }

    private fun buildInvitationUrl(token: String): String {
        return sendMail.buildUrl(
            "${Constants.NEXT_APP_PATH}bookingInvitation?token=${URLEncoder.encode(token, StandardCharsets.UTF_8)}"
        )
    }

    private fun translate(recipient: PFUserDO, i18nKey: String, vararg params: Any): String {
        return I18nHelper.getLocalizedMessage(recipient, i18nKey, *params)
    }

    companion object {
        const val ERROR_INVITATION_INVALID = "task.bookingGrant.error.invitationInvalid"
        const val INVITATION_VALIDITY_DAYS = 7L
        private const val TOKEN_LENGTH = 32

        internal fun hash(token: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
