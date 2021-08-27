/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web

import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.vacation.service.VacationSendMailService
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.projectforge.messaging.SmsSender
import org.projectforge.messaging.SmsSender.HttpResponseCode
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.sms.SmsSenderConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * 2FA/OTP services.
 */
@Service
class My2FAHttpService {
  /**
   * Result of sending a code for the caller.
   */
  class Result(val success: Boolean, var message: String)

  @Autowired
  private lateinit var sendMail: SendMail

  @Autowired
  private lateinit var smsSenderConfig: SmsSenderConfig

  private lateinit var smsSender: SmsSender

  val smsConfigured
    get() = SystemStatus.isDevelopmentMode() || smsSenderConfig.isSmsConfigured()

  /**
   * Creates a OTP (valid for 2 minutes), stores it in the user's session and text or mail this code to the user.
   * @param mobilePhone If given (in a valid format), a text message with the OTP is sent to this number.
   */
  fun createAndSendOTP(request: HttpServletRequest, mobilePhone: String?): Result {
    var error: String? = null
    if (!mobilePhone.isNullOrBlank() && StringHelper.checkPhoneNumberFormat(mobilePhone, false)) {
      val result = createAndTextOTP(request, mobilePhone)
      if (result.success) {
        return result
      }
      error = result.message
    }
    val result = createAndMailOTP(request)
    if (error != null) {
      result.message = "${result.message} ($error)"
    }
    return result
  }

  /**
   * Creates a OTP (2 minutes valid) in [ExpiringSessionAttributes] and sends the code to the mobile number.
   */
  private fun createAndTextOTP(request: HttpServletRequest, mobilePhone: String): Result {
    val code = createOTP(request)
    val msg = "${translateMsg("user.My2FACode.sendCode.mail.message", code)} ${translate("address.sendSms.doNotReply")}"
    val responseCode = if (SystemStatus.isDevelopmentMode()) {
      log.info { "Text message would be sent in production mode to '$mobilePhone': $msg" }
      HttpResponseCode.SUCCESS
    } else {
      smsSender.send(mobilePhone, msg)
    }
    return Result(responseCode == HttpResponseCode.SUCCESS, getResultMessage(responseCode, mobilePhone))
  }

  /**
   * Creates a OTP (2 minutes valid) in [ExpiringSessionAttributes] and sends the code to the mobile number.
   */
  private fun createAndMailOTP(request: HttpServletRequest): Result {
    val code = createOTP(request)
    val mail = Mail()
    mail.setTo(ThreadLocalUserContext.getUser())
    mail.subject = translate("user.My2FACode.sendCode.mail.title")
    mail.contentType = Mail.CONTENTTYPE_HTML
    val data: MutableMap<String, Any> = mutableMapOf("otp" to code)
    mail.content = sendMail.renderGroovyTemplate(mail, "mail/otpMail.html", data,
      mail.subject, ThreadLocalUserContext.getUser())
    val response = sendMail.send(mail)
    if (response) {
      return Result(true, translateMsg("user.My2FACode.sendCode.mail.sentSuccessfully", ThreadLocalUserContext.getUser().email))
    }
    return Result(false, translate("mail.error.exception"))
  }

  private fun createOTP(request: HttpServletRequest): String {
    val code = NumberHelper.getSecureRandomDigits(6)
    ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_OTP, code, TTL_MINUTES)
    return code
  }

  private fun getResultMessage(responseCode: HttpResponseCode, number: String): String {
    val errorKey = smsSender.getErrorMessage(responseCode)
    if (errorKey != null) {
      return translate(errorKey)
    }
    return translateMsg("address.sendSms.sendMessage.result.successful", number, PFDateTime.now().format())
  }

  /**
   * Checks the code entered by the user by getting the code created before via [createAndSendOTP].
   */
  fun checkOTP(request: HttpServletRequest, code: String): Boolean {
    val sessionCode =
      ExpiringSessionAttributes.getAttribute(request, SESSSION_ATTRIBUTE_OTP, String::class.java) ?: return false
    var counter = ExpiringSessionAttributes.getAttribute(request, SESSSION_ATTRIBUTE_FAILS_COUNTER, Int::class.java) ?: 0
    if (counter > 3) {
      log.warn { "User tries to enter otp 3 times without success. Removing OTP from user's session." }
      ExpiringSessionAttributes.removeAttribute(request, SESSSION_ATTRIBUTE_FAILS_COUNTER)
      ExpiringSessionAttributes.removeAttribute(request, SESSSION_ATTRIBUTE_OTP)
      return false
    }
    return if (sessionCode == code) {
      // Successful: invalidate it:
      ExpiringSessionAttributes.removeAttribute(request, SESSSION_ATTRIBUTE_OTP)
      ThreadLocalUserContext.getUserContext().updateLastSuccessful2FA()
      true
    } else {
      ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_FAILS_COUNTER, ++counter, TTL_MINUTES)
      false
    }
  }

  @PostConstruct
  fun postConstruct() {
    smsSender = SmsSender(smsSenderConfig)
  }

  companion object {
    private const val SESSSION_ATTRIBUTE_OTP = "My2FARestService.otp"
    private const val SESSSION_ATTRIBUTE_FAILS_COUNTER = "My2FARestService.otpFails"
    private const val TTL_MINUTES = 2
  }
}
