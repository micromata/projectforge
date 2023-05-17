package org.projectforge.rest.poll

import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Service
class PollMailService {

    @Autowired
    private lateinit var sendMail: SendMail

    private val log: Logger = LoggerFactory.getLogger(PollMailService::class.java)

    fun sendMail(from: String, to: List<String>, subject: String, content: String, mailAttachments: List<MailAttachment>? = null) {
        try {
            if (content.isNotEmpty()) {
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

}