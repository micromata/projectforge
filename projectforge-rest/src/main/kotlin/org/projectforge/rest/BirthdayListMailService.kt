package org.projectforge.rest

import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BirthdayListMailService {
    @Autowired
    private lateinit var sendMail: SendMail

    fun sendMail() {
        val mail = Mail()
        mail.subject = "Testmail"
        mail.contentType = Mail.CONTENTTYPE_TEXT
        mail.setTo("j.fleckenstein@micromata.de")
        mail.content = "Hallo, guten Tag. Dies ist eine Testmail von Jona Fleckenstein."
        sendMail.send(mail)
    }
}