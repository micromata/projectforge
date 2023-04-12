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

package org.projectforge.rest

import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.projectforge.common.StringHelper
import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.projectforge.rest.config.BirthdayListConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

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
                log.info { "Send mail to $address" }
            }
    }

    private fun getEMailAddressesFromConfig(): MutableList<String>? {
        if (!birthdayListConfiguration.emailAddresses.isNullOrBlank()) {
            val splitEmails = birthdayListConfiguration.emailAddresses?.split(",")
            val validEmailAddresses = mutableListOf<String>()

            if (!splitEmails.isNullOrEmpty()) {
                splitEmails.forEach { address ->
                    if (address.isNotBlank() && StringHelper.isEmailValid(address.trim()))
                        validEmailAddresses.add(address.trim())
                    else
                        log.error { "Invalid email address: $address" }
                }
                return validEmailAddresses.ifNotEmpty { validEmailAddresses }
            }
        }
        return null
    }
}
