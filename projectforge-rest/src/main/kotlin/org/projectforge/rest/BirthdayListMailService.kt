package org.projectforge.rest

import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.projectforge.rest.config.BirthdayListConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BirthdayListMailService {
    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var birthdayListConfiguration: BirthdayListConfiguration

    fun sendMail(subject: String, content: String, mailAttachments: List<MailAttachment>?) {
        val emails = getEMailAddressesFromConfig()
        if (!emails.isNullOrEmpty()) {
            for (address in emails) {
                val mail = Mail()
                mail.subject = subject
                mail.contentType = Mail.CONTENTTYPE_HTML
                mail.setTo(address)
                mail.content = content
                sendMail.send(mail, attachments = mailAttachments)
            }
        }
    }

    private fun getEMailAddressesFromConfig(): MutableList<String>? {
        if (!birthdayListConfiguration.emails.isNullOrBlank()){
            val splittedEmailsList = birthdayListConfiguration.emails!!.split(",")
            var trimmedEmailsList = mutableListOf<String>()
            for (mail in splittedEmailsList) {
                if (mail.isNotBlank())
                    trimmedEmailsList.add(mail.trim())
            }
            if (trimmedEmailsList.size != 0)
                return trimmedEmailsList
        }
        return null
    }
}