/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
package org.projectforge.mail

import de.micromata.genome.util.validation.ValContext
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.scripting.GroovyEngine
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.i18n.UserException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.activation.MimetypesFileTypeMap
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

/**
 * Helper class for creating and transporting E-Mails. Groovy script is use-able for e-mail template mechanism.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class SendMail {
    @Autowired
    private val configurationService: ConfigurationService? = null

    @Autowired
    private val domainService: DomainService? = null
    private val random = Random()

    /**
     * @param composedMessage the message to send
     * @param attachments     other attachments to add
     * @return true for successful sending, otherwise an exception will be thrown.
     * @throws UserException          if to address is not given.
     * @throws InternalErrorException due to technical failures.
     */
    fun send(composedMessage: Mail?,
             attachments: Collection<MailAttachment>?): Boolean {
        return send(composedMessage, null, attachments, true)
    }
    /**
     * @param composedMessage the message to send
     * @return true for successful sending, otherwise an exception will be thrown.
     * @throws UserException          if to address is not given.
     * @throws InternalErrorException due to technical failures.
     */
    /**
     * @param composedMessage the message to send
     * @param icalContent     the ical content to add
     * @param attachments     other attachments to add
     * @return true for successful sending, otherwise an exception will be thrown.
     * @throws UserException          if to address is not given.
     * @throws InternalErrorException due to technical failures.
     */
    @JvmOverloads
    fun send(composedMessage: Mail?, icalContent: String? = null,
             attachments: Collection<MailAttachment>? = null, async: Boolean = true): Boolean {
        if (composedMessage == null) {
            log.error("No message object of type org.projectforge.mail.Mail given. E-Mail not sent.")
            return false
        }
        val to = composedMessage.to
        if (to == null || to.size == 0) {
            log.error("No to address given. Sending of mail cancelled: $composedMessage")
            throw UserException("mail.error.missingToAddress")
        }
        val cf = configurationService!!.createMailSessionLocalSettingsConfigModel()
        if (cf == null || !cf.isEmailEnabled) {
            log.error("No e-mail host configured. E-Mail not sent: $composedMessage")
            return false
        }
        if (async) {
            CompletableFuture.runAsync { sendIt(composedMessage, icalContent, attachments) }
        } else {
            sendIt(composedMessage, icalContent, attachments)
        }
        return true
    }

    private val session: Session
        private get() {
            val cf = configurationService!!.createMailSessionLocalSettingsConfigModel()
            if (!cf.isEmailEnabled) {
                log.error("Sending email is not enabled")
                throw InternalErrorException("mail.error.exception")
            }
            val ctx = ValContext()
            cf.validate(ctx)
            if (ctx.hasErrors()) {
                log.error("SMPT configuration has validation errors")
                for (msg in ctx.messages) {
                    log.error(msg.toString())
                }
                throw InternalErrorException("mail.error.exception")
            }
            val addp = Properties()
            addp["mail.mime.charset"] = "UTF-8"
            if (cf.standardEmailSender != null) {
                addp["mail.from"] = cf.standardEmailSender
            }
            return cf.createMailSession(addp)
        }

    private fun sendIt(composedMessage: Mail, icalContent: String?,
                       attachments: Collection<MailAttachment>?) {
        log.info("Start sending e-mail message: " + StringUtils.join(composedMessage.to, ", "))
        try {
            val session = session
            val message = MimeMessage(session)
            if (composedMessage.from != null) {
                message.setFrom(InternetAddress(composedMessage.from))
            } else {
                message.setFrom()
            }
            message.setRecipients(Message.RecipientType.TO,
                    composedMessage.to.toTypedArray())
            if (CollectionUtils.isNotEmpty(composedMessage.cc)) {
                message.setRecipients(Message.RecipientType.CC,
                        composedMessage.cc.toTypedArray())
            }
            val subject = composedMessage.subject
            val sendMailConfig = configurationService!!.sendMailConfiguration
            message.setSubject(subject, sendMailConfig.charset)
            message.sentDate = Date()
            if (StringUtils.isBlank(icalContent) && attachments == null) {
                // create message without attachments
                if (composedMessage.contentType != null) {
                    message.setText(composedMessage.content, composedMessage.charset, composedMessage.contentType)
                } else {
                    message.setText(composedMessage.content, sendMailConfig.charset)
                }
            } else {
                // create message with attachments
                val mp = createMailAttachmentContent(message, composedMessage, icalContent, attachments, sendMailConfig)
                message.setContent(mp)
            }
            message.saveChanges() // don't forget this
            Transport.send(message)
        } catch (ex: Exception) {
            log.error("While creating and sending message: $composedMessage", ex)
            throw InternalErrorException("mail.error.exception")
        }
        log.info("E-Mail successfully sent: $composedMessage")
    }

    @Throws(MessagingException::class)
    private fun createMailAttachmentContent(message: MimeMessage, composedMessage: Mail, icalContent: String?,
                                            attachments: Collection<MailAttachment>?,
                                            sendMailConfig: SendMailConfig): MimeMultipart {
        // create and fill the first message part
        val mbp1 = MimeBodyPart()
        var type: String? = "text/"
        if (StringUtils.isNotBlank(composedMessage.contentType)) {
            type += composedMessage.contentType
            type += "; charset="
            type += composedMessage.charset
        } else {
            type = "text/html; charset="
            type += sendMailConfig.charset
        }
        mbp1.setContent(composedMessage.content, type)
        mbp1.setHeader("Content-Transfer-Encoding", "8bit")
        // create the Multipart and its parts to it
        val mp = MimeMultipart()
        mp.addBodyPart(mbp1)
        if (StringUtils.isNotBlank(icalContent)) {
            message.addHeaderLine("method=REQUEST")
            message.addHeaderLine("charset=UTF-8")
            message.addHeaderLine("component=VEVENT")
            val icalBodyPart = MimeBodyPart()
            icalBodyPart.setHeader("Content-Class", "urn:content-  classes:calendarmessage")
            icalBodyPart.setHeader("Content-ID", "calendar_message")
            icalBodyPart.dataHandler = DataHandler(
                    ByteArrayDataSource(icalContent!!.toByteArray(), "text/calendar"))
            val s = Integer.toString(random.nextInt(Int.MAX_VALUE))
            icalBodyPart.fileName = "ICal-$s.ics"
            mp.addBodyPart(icalBodyPart)
        }
        if (attachments != null && !attachments.isEmpty()) {
            // create an Array of message parts for Attachments
            val mbp = arrayOfNulls<MimeBodyPart>(attachments.size)
            // remember you can extend this functionality with META-INF/mime.types
            // See http://docs.oracle.com/javaee/5/api/javax/activation/MimetypesFileTypeMap.html
            val mimeTypesMap = MimetypesFileTypeMap()
            var i = 0
            for (attachment in attachments) {
                // create the next message part
                mbp[i] = MimeBodyPart()
                // only by file name
                var mimeType = mimeTypesMap.getContentType(attachment.filename)
                if (StringUtils.isBlank(mimeType)) {
                    mimeType = "application/octet-stream"
                }
                // attach the file to the message
                val ds: DataSource = ByteArrayDataSource(attachment.content, mimeType)
                mbp[i]!!.dataHandler = DataHandler(ds)
                mbp[i]!!.fileName = attachment.filename
                mp.addBodyPart(mbp[i])
                i++
            }
        }
        return mp
    }

    /**
     * @param composedMessage
     * @param groovyTemplate
     * @param data
     * @see GroovyEngine.executeTemplateFile
     */
    fun renderGroovyTemplate(composedMessage: Mail?, groovyTemplate: String,
                             data: MutableMap<String?, Any?>,
                             recipient: PFUserDO): String {
        val user = ThreadLocalUserContext.getUser()
        data["createdLabel"] = ThreadLocalUserContext.getLocalizedString("created")
        data["loggedInUser"] = user
        data["recipient"] = recipient
        data["msg"] = composedMessage
        log.debug("groovyTemplate=$groovyTemplate")
        val logoBasename = configurationService!!.logoBasename
        // data.put("logoUrl", )
        val engine = GroovyEngine(configurationService, data, recipient.locale,
                recipient.timeZoneObject)
        return engine.executeTemplateFile(groovyTemplate)
    }

    fun buildUrl(subPath: String?): String {
        return ""
    }

    companion object {
        private const val STANDARD_SUBJECT_PREFIX = "[ProjectForge] "
        private val log = LoggerFactory.getLogger(SendMail::class.java)

        /**
         * Get the ProjectForge standard subject: "[ProjectForge] ..."
         *
         * @param subject
         */
        fun getProjectForgeSubject(subject: String): String {
            return STANDARD_SUBJECT_PREFIX + subject
        }
    }
}
