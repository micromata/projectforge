package org.projectforge.rest

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class BirthdayListMailService {
    @Autowired
    private lateinit var sendMail: SendMail

    fun sendMail() {

        val emailAddresses = getMailAddressesFromConfig()

        for (address in emailAddresses) {
            val mail = Mail()
            mail.subject = "Testmail"
            mail.contentType = Mail.CONTENTTYPE_HTML
            mail.setTo(address.email)
            mail.content = "Hallo, guten Tag. Dies ist eine Testmail von Jona Fleckenstein."
            sendMail.send(mail)
        }
    }

    private fun getMailAddressesFromConfig(): List<MailJson> {
        val mapper = jacksonObjectMapper()
        mapper.registerKotlinModule()
        mapper.registerModule(JavaTimeModule())
        return mapper.readValue<List<MailJson>>(File("projectforge-rest/src/main/kotlin/org/projectforge/rest/Test.json").readText(Charsets.UTF_8))
    }

    data class MailJson(val email: String) {
    }
}