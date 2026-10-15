/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.business.test

import org.junit.jupiter.api.Assertions
import org.projectforge.mail.IMailAttachment
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import java.io.File

/**
 * Writes rendered mails as .eml files (complete MIME messages incl. attachments) to build/tmp/mail-preview of the
 * module, so they can be viewed in a mail client without any mail server: `open` on an .eml file
 * opens them in Apple Mail, Outlook and Thunderbird can open them too. The HTML content is written next to them
 * for the browser.
 */
object MailPreview {
    const val DIR = "build/tmp/mail-preview"

    @JvmStatic
    @JvmOverloads
    fun write(sendMail: SendMail, name: String, mail: Mail, attachments: Collection<IMailAttachment>? = null): File {
        Assertions.assertFalse(mail.content.contains("not found!"), "Missing template variable in '$name': ${mail.content}")
        if (mail.to.isEmpty()) {
            mail.setTo("preview@example.org", "Preview")
        }
        if (mail.from == null && sendMail.mailFromStandardEmailSender.isNullOrBlank()) {
            mail.from = "ProjectForge <projectforge@example.org>"
        }
        val dir = File(DIR).also { it.mkdirs() }
        File(dir, "$name.html").writeText(mail.content)
        val file = File(dir, "$name.eml")
        file.outputStream().use { sendMail.createMimeMessage(mail, attachments = attachments).writeTo(it) }
        return file
    }
}
