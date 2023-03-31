package org.projectforge.rest

import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
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
        if (!emails.isNullOrEmpty())
            emails.forEach { address ->
                val mail = Mail()
                mail.subject = subject
                mail.contentType = Mail.CONTENTTYPE_HTML
                mail.setTo(address)
                mail.content = content
                sendMail.send(mail, attachments = mailAttachments)
            }
    }

    private fun getEMailAddressesFromConfig(): MutableList<String>? {
        if (!birthdayListConfiguration.emailAddresses.isNullOrBlank()) {
            val splitEmails = birthdayListConfiguration.emailAddresses?.split(",")
            val trimmedEmails = mutableListOf<String>()
            if (!splitEmails.isNullOrEmpty()){
                splitEmails.forEach {
                    if (it.isNotBlank())
                        trimmedEmails.add(it.trim())
                }
                return trimmedEmails.ifNotEmpty { trimmedEmails }
            }
        }
        return null
    }
}