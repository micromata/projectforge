/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.scripting.GroovyEngine
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.I18nHelper.getLocalizedMessage
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture
import jakarta.activation.DataHandler
import jakarta.activation.DataSource
import jakarta.activation.MimetypesFileTypeMap
import jakarta.annotation.PostConstruct
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource

private val log = KotlinLogging.logger {}

/**
 * Helper class for creating and transporting E-Mails. Groovy script is use-able for e-mail template mechanism.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
// Open needed for mocking.
@Service
open class SendMail {
  @Autowired
  private lateinit var configurationService: ConfigurationService

  @Autowired
  private lateinit var domainService: DomainService

  enum class Protocol {
    Plain, StartTLS, SSL;

    companion object {
      fun fromString(type: String?): Protocol {
        return when (type) {
          "StartTLS" -> StartTLS
          "SSL" -> SSL
          else -> Plain
        }
      }
    }
  }

  @Value("\${mail.session.pfmailsession.emailEnabled}")
  private var mailingEnabled: String? = null

  @Value("\${mail.session.pfmailsession.standardEmailSender}")
  var mailFromStandardEmailSender: String? = null
    private set

  /**
   * Mail protocol: Plain, StartTLS,SSL
   */
  @Value("\${mail.session.pfmailsession.encryption}")
  private var mailSmtpEncryptionProtocol: String? = null

  @Value("\${mail.session.pfmailsession.smtp.localhost}")
  private var mailLocalHost: String? = null

  @Value("\${mail.session.pfmailsession.smtp.host}")
  private var mailSmtpHost: String? = null

  @Value("\${mail.session.pfmailsession.smtp.port}")
  private var mailSmtpPort: String? = null

  @Value("\${mail.session.pfmailsession.smtp.auth}")
  private var mailSmtpAuth = false

  @Value("\${mail.session.pfmailsession.smtp.user}")
  private var mailSmtpUser: String? = null

  @Value("\${mail.session.pfmailsession.smtp.password}")
  private var mailSmtpPassword: String? = null

  private val random = Random()

  @PostConstruct
  private fun postConstruct() {
    log.info { info }
  }

  /**
   * @param composedMessage the message to send
   * @param attachments     other attachments to add
   * @return true for successful sending, otherwise an exception will be thrown.
   * @throws UserException          if to address is not given.
   * @throws InternalErrorException due to technical failures.
   */
  fun send(
    composedMessage: Mail?,
    attachments: Collection<IMailAttachment>?
  ): Boolean {
    return send(composedMessage, null, attachments, true)
  }

  /**
   * @param composedMessage the message to send
   * @param icalContent     the ical content to add
   * @param attachments     other attachments to add
   * @return true for successful sending, otherwise an exception will be thrown.
   * @throws UserException          if to address is not given.
   * @throws InternalErrorException due to technical failures.
   */
  @JvmOverloads
  fun send(
    composedMessage: Mail?,
    icalContent: String? = null,
    attachments: Collection<IMailAttachment>? = null,
    async: Boolean = true
  ): Boolean {
    if (composedMessage == null) {
      log.error("No message object of type org.projectforge.mail.Mail given. E-Mail not sent.")
      return false
    }
    if (!isConfigured) {
      log.error { "Sending of mails is not configured. Mail is ignored: $composedMessage" }
    }
    val to = composedMessage.to
    if (to == null || to.size == 0) {
      log.error("No to address given. Sending of mail cancelled: $composedMessage")
      throw UserException("mail.error.missingToAddress")
    }
    if (async) {
      CompletableFuture.runAsync { sendIt(composedMessage, icalContent, attachments) }
    } else {
      sendIt(composedMessage, icalContent, attachments)
    }
    return true
  }

  val isConfigured: Boolean
    get() = this.mailingEnabled == "true" && !this.mailSmtpHost.isNullOrBlank()

  private val session: Session?
    get() {
      val properties = Properties()
      properties["mail.smtp.host"] = this.mailSmtpHost // Replace with your SMTP server
      properties["mail.smtp.port"] = this.mailSmtpPort ?: "25" // Replace with your SMTP server port
      properties["mail.smtp.auth"] = this.mailSmtpAuth // Enable authentication
      this.mailLocalHost
        ?.takeIf { it.isNotBlank() }
        ?.let { properties["mail.smtp.localhost"] = it.trim() } // FQDN setzen
      // properties["mail.smtp.starttls.enable"] = "true"
      // properties["mail.smtp.ssl.protocols"] = "TLSv1.2"

      this.mailSmtpUser?.let { properties["mail.smtp.user"] = it }
      this.mailSmtpPassword?.let { properties["mail.smtp.password"] = it }
      when (Protocol.fromString(this.mailSmtpEncryptionProtocol)) {
        Protocol.StartTLS -> {
          properties["mail.smtp.starttls.enable"] = "true" // Enable TLS
        }

        Protocol.SSL -> {
          properties["mail.smtp.ssl.enable"] = "true" // Enable TLS
        }

        else -> {
        }
      }
      properties["mail.mime.charset"] = CHARSET
      mailFromStandardEmailSender
        ?.takeIf { it.isNotBlank() }
        ?.let { properties["mail.from"] = it.trim() }
      // properties.put("mail.debug", java.lang.Boolean.toString(smptDebug))

      // Create a mail session
      return Session.getInstance(properties)
    }

  private fun sendIt(
    composedMessage: Mail, icalContent: String?,
    attachments: Collection<IMailAttachment>?
  ) {
    log.info("Start sending e-mail message: " + StringUtils.join(composedMessage.to, ", "))
    try {
      val session = session
      /*if (SystemStatus.isDevelopmentMode()) {
        session!!.setDebug(true)
      }*/
      val message = MimeMessage(session)
      if (composedMessage.from != null) {
        message.setFrom(InternetAddress(composedMessage.from))
      } else {
        mailFromStandardEmailSender
          ?.takeIf { it.isNotBlank() }
          ?.let { message.setFrom(InternetAddress(it)) }
          ?: message.setFrom()
      }
      message.setRecipients(
        Message.RecipientType.TO,
        composedMessage.to.toTypedArray<Address>()
      )
      if (CollectionUtils.isNotEmpty(composedMessage.cc)) {
        message.setRecipients(
          Message.RecipientType.CC,
          composedMessage.cc.toTypedArray<Address>()
        )
      }
      //message.setHeader("Return-Path", "")
      //message.setHeader("Reply-To", "")
      val subject = composedMessage.subject
      message.setSubject(subject, CHARSET)
      message.sentDate = Date()
      if (StringUtils.isBlank(icalContent) && attachments == null) {
        // create message without attachments
        if (composedMessage.contentType != null) {
          message.setText(composedMessage.content, composedMessage.charset, composedMessage.contentType)
        } else {
          message.setText(composedMessage.content, CHARSET)
        }
        // message.setContent("Dies ist eine einfache Testnachricht.", "text/plain; charset=UTF-8");
        // message.setText("Einfache Textnachricht")
      } else {
        // create message with attachments
        val mp = createMailAttachmentContent(message, composedMessage, icalContent, attachments, CHARSET)
        message.setContent(mp)
      }
      message.saveChanges() // don't forget this
      if (testMode) {
        log.info("Test mode, do not really send e-mails (OK only for test cases).")
      } else {
        Transport.send(message)
      }
    } catch (ex: Exception) {
      log.error("While creating and sending message: $composedMessage", ex)
      throw InternalErrorException("mail.error.exception")
    }
    log.info("E-Mail successfully sent: $composedMessage")
  }

  @Throws(MessagingException::class)
  private fun createMailAttachmentContent(
    message: MimeMessage, composedMessage: Mail, icalContent: String?,
    attachments: Collection<IMailAttachment>?,
    charset: String
  ): MimeMultipart {
    // create and fill the first message part
    val mbp1 = MimeBodyPart()
    var type: String? = "text/"
    if (StringUtils.isNotBlank(composedMessage.contentType)) {
      type += composedMessage.contentType
      type += "; charset="
      type += composedMessage.charset
    } else {
      type = "text/html; charset="
      type += charset
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
        ByteArrayDataSource(icalContent!!.toByteArray(), "text/calendar")
      )
      val s = Integer.toString(random.nextInt(Int.MAX_VALUE))
      icalBodyPart.fileName = "ICal-$s.ics"
      mp.addBodyPart(icalBodyPart)
    }
    if (attachments != null && !attachments.isEmpty()) {
      // create an Array of message parts for Attachments
      val mbp = arrayOfNulls<MimeBodyPart>(attachments.size)
      // remember you can extend this functionality with META-INF/mime.types
      // See http://docs.oracle.com/javaee/5/api/jakarta.activationl/MimetypesFileTypeMap.html
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
   * @param title Title is put in the data map and may be received inside the mail template.
   * @see GroovyEngine.executeTemplateFile
   */
  fun renderGroovyTemplate(
    composedMessage: Mail, groovyTemplate: String,
    data: MutableMap<String, Any?>,
    title: String,
    recipient: PFUserDO?
  ): String {
    prepare(composedMessage, data, title, recipient)
    log.debug("groovyTemplate=$groovyTemplate")
    val engine = GroovyEngine(
      configurationService, data, recipient?.locale,
      recipient?.timeZone
    )
    return engine.executeTemplateFile(groovyTemplate)
  }

  /*
  fun renderKotlinTemplate(
    composedMessage: Mail, kotlinTemplate: String,
    data: MutableMap<String, Any?>,
    title: String,
    recipient: PFUserDO
  ): String {
    prepare(composedMessage, data, title, recipient)
    log.debug("kotlinTemplate=$kotlinTemplate")
    val engine = MyKotlinScriptEngineFactory().scriptEngine
    val bindings = engine.createBindings()
    data.forEach {
      bindings[it.key] = it.value
    }
    try {
      return engine.eval(kotlinTemplate, bindings)
    } catch (ex: Exception) {
      log.info("Exception on Kotlin script execution: ${ex.message}", ex)
      scriptExecutionResult.exception = ex
    }
    return scriptExecutionResult
    return engine.executeTemplateFile(kotlinTemplate)
  } */

  private fun prepare(
    composedMessage: Mail,
    data: MutableMap<String, Any?>,
    title: String,
    recipient: PFUserDO?
  ) {
    val user = ThreadLocalUserContext.loggedInUser
    data["createdLabel"] = getLocalizedMessage(user, "created")
    user?.let {
      data["loggedInUser"] = it
    }
    data["title"] = title
    if (recipient != null) {
      data["recipient"] = recipient
    }
    data["msg"] = composedMessage
    data["baseUrl"] = buildUrl("")
    if (configurationService.isLogoFileValid) {
      val logoBasename = configurationService.syntheticLogoName
      data["logoUrl"] = buildUrl("rsPublic/$logoBasename")
    }
  }

  fun buildUrl(subPath: String?): String {
    return domainService.getDomain(subPath)
  }

  private val info: String
    get() {
      if (!isConfigured) {
        return "SendMail isn't configured. No host configured in projectforge.properties."
      }
      val sb = StringBuilder()
      sb.append(
        "SendMail configured with host='$mailSmtpHost', protocol='${
          Protocol.fromString(
            mailSmtpEncryptionProtocol
          )
        }', auth=$mailSmtpAuth, port=$mailSmtpPort, from='$mailFromStandardEmailSender'"
      )
      mailSmtpUser?.let {
        if (it.isNotBlank()) {
          sb.append(", user='$it'")
        }
      }
      mailSmtpPassword?.let {
        if (it.isNotBlank()) {
          sb.append(", password='***'")
        }
      }
      return sb.toString()
    }

  companion object {
    private const val STANDARD_SUBJECT_PREFIX = "[ProjectForge] "

    private const val CHARSET = "UTF-8"

    /**
     * Get the ProjectForge standard subject: "[ProjectForge] ..."
     *
     * @param subject
     */
    @JvmStatic
    fun getProjectForgeSubject(subject: String): String {
      return STANDARD_SUBJECT_PREFIX + subject
    }

    fun formatUserWithMail(name: String, mail: String?): String {
      return if (mail == null) {
        name
      } else "<a href=\"mailto:$mail\">$name</a>"
    }

    private var testMode = false

    @JvmStatic
    fun internalSetTestMode() {
      testMode = true
    }
  }
}
