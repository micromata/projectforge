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