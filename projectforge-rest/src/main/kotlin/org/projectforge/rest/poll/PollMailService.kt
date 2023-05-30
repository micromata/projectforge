package org.projectforge.rest.poll

import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PollMailService {

    @Autowired
    private lateinit var sendMail: SendMail

    private val log: Logger = LoggerFactory.getLogger(PollMailService::class.java)

    fun sendMail(from: String, to: List<String>, subject: String, content: String, mailAttachments: List<MailAttachment>? = null) {
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

    fun sendPollDeletedMail(to: String, pollDto: Poll, mailAttachments: List<MailAttachment>? = null) {
        val mail = Mail()
        mail.setTo(to)
        mail.subject = "Poll ${pollDto.title} deleted"
        mail.content = "Poll ${pollDto.title} deleted. The Excel files are attached."
        sendMail.send(mail, attachments = mailAttachments)
    }

}