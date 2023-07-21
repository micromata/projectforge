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

package org.projectforge.rest.poll

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.projectforge.rest.dto.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PollMailService {

    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    private val log: Logger = LoggerFactory.getLogger(PollMailService::class.java)

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
                to.forEach { mail.addTo(it) }
                sendMail.send(mail, attachments = mailAttachments)
                log.info("Mail with subject $subject sent to $to")
            } else {
                log.error("There are missing parameters for sending mail: from: $from, to: $to, subject: $subject, content: $content")
            }
        } catch (e: Exception) {
            log.error(e.toString())
        }

    }

    fun getAllMails(poll: Poll): List<String> {
        val attendees = poll.attendees
        var fullAccessUser = poll.fullAccessUsers?.toMutableList() ?: mutableListOf()
        val accessGroupIds = poll.fullAccessGroups?.filter { it.id != null }?.map { it.id!! }?.toIntArray()
        val accessUserIds = UserService().getUserIds(groupService.getGroupUsers(accessGroupIds))
        val accessUsers = User.toUserList(accessUserIds)

        accessUsers?.forEach { user ->
            if (fullAccessUser.none { it.id == user.id }) {
                fullAccessUser.add(user)
            }
        }

        var owner = User.getUser(poll.owner?.id, false)
        if (owner != null) {
            fullAccessUser.add(owner)
        }
        attendees?.forEach {
            if (!fullAccessUser.contains(it)) {
                fullAccessUser.add(it)
            }
        }

        User.restoreDisplayNames(fullAccessUser, userService)
        return fullAccessUser.mapNotNull { it.email }
    }

}