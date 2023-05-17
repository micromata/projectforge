package org.projectforge.rest.poll

import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PollMailService {

    @Autowired
    private lateinit var sendMail: SendMail


    fun sendMail(to:String ,subject: String,content: String, mailAttachments: List<MailAttachment>?= null){
        val mail = Mail()
        mail.subject = subject
        mail.contentType = Mail.CONTENTTYPE_HTML
        mail.setTo(to)
        mail.content = content
        sendMail.send(mail, attachments = mailAttachments)

    }





}