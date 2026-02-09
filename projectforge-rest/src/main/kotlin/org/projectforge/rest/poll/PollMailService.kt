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

package org.projectforge.rest.poll

import mu.KotlinLogging
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.business.user.service.UserService
import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class PollMailService {

    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    fun sendMail(
        from: String,
        to: List<String>,
        subject: String,
        content: String,
        mailAttachments: List<MailAttachment>? = null
    ) {
        try {
            if (content.isNotEmpty() && to.isNotEmpty()) {
                val mail = Mail()
                mail.subject = subject
                mail.contentType = Mail.CONTENTTYPE_HTML
                mail.content = content
                mail.from = from
                // Set sender as TO (required for a mail to have at least one "to")
                mail.addTo(from)
                // Add all recipients to CC (will be sent as BCC by SendMail.sendBcc())
                to.forEach { mail.addCC(it) }
                // Use sendBcc() to send CC recipients as BCC (only for Poll emails)
                sendMail.sendBcc(mail, null, mailAttachments, true)
                log.info("Mail with subject $subject sent to $to (as BCC)")
            } else {
                log.error("There are missing parameters for sending mail: from: $from, to: $to, subject: $subject, content: $content")
            }
        } catch (e: Exception) {
            log.error(e.message, e)
        }
    }

    fun getAllMails(poll: Poll): List<String> {
        val attendees = poll.attendees
        val fullAccessUser = poll.fullAccessUsers?.toMutableList() ?: mutableListOf()
        val accessGroupIds = poll.fullAccessGroups?.filter { it.id != null }?.map { it.id!! }?.toLongArray()
        val accessUserIds = UserService().getUserIds(groupService.getGroupUsers(accessGroupIds))
        val accessUsers = User.toUserList(accessUserIds)

        val userList = fullAccessUser
        accessUsers?.forEach { user ->
            if (fullAccessUser.none { it.id == user.id }) {
                userList.add(user)
            }
        }

        val owner = User.getUser(poll.owner?.id, false)
        if (owner != null) {
            userList.add(owner)
        }
        attendees?.forEach {
            if (!fullAccessUser.contains(it)) {
                userList.add(it)
            }
        }

        User.restoreDisplayNames(userList)
        User.restoreEmails(userList)
        return userList.mapNotNull { it.email }
    }

    fun getAllFullAccessEmails(poll: Poll): List<String> {
        val fullAccessUser = poll.fullAccessUsers?.toMutableList() ?: mutableListOf()
        poll.fullAccessGroups?.filter { it.id != null }?.map { it.id!! }?.toLongArray()

        val userList = fullAccessUser

        User.restoreEmails(userList)
        return userList.mapNotNull { it.email }
    }

    fun getAllAttendeesEmails(poll: Poll): List<String> {
        val attendees = poll.attendees

        val userList = attendees

        User.restoreEmails(userList)
        return userList!!.mapNotNull { it.email }
    }

    /**
     * Get emails for reminder and end mails:
     * - All Full Access Users (always get all mails)
     * - Only Attendees who haven't responded yet
     */
    fun getFilteredEmails(poll: Poll, pollId: Long): List<String> {
        // Full Access Users always get all mails
        val fullAccessEmails = getAllFullAccessEmails(poll)
        
        // Attendees who haven't responded yet
        val attendeesWithoutResponse = getEmailsOfUsersWhoHaventResponded(poll, pollId)
        
        // Combine both lists (remove duplicates)
        return (fullAccessEmails + attendeesWithoutResponse).distinct()
    }

    /**
     * Get emails of attendees who haven't responded yet
     */
    private fun getEmailsOfUsersWhoHaventResponded(poll: Poll, pollId: Long): List<String> {
        // All attendee emails
        val allAttendeesEmails = getAllAttendeesEmails(poll)
        
        // All responses for this poll
        val allResponses = pollResponseDao.selectAll(checkAccess = false)
            .filter { response -> response.poll?.id == pollId }
        
        // Emails of users who have already responded
        val respondedUserEmails = allResponses.mapNotNull { response ->
            response.owner?.email
        }.toSet()
        
        // Only emails of users who haven't responded yet
        return allAttendeesEmails.filter { email ->
            email !in respondedUserEmails
        }
    }
}
