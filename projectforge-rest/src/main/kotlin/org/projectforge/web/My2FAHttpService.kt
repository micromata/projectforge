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
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.login.LoginHandlerService
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.projectforge.messaging.SmsSender
import org.projectforge.messaging.SmsSender.HttpResponseCode
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.security.My2FAService
import org.projectforge.sms.SmsSenderConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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

  enum class OTPCheckResult { SUCCESS, WRONG_LOGIN_PASSWORD, FAILED }

  @Autowired
  private lateinit var loginHandlerService: LoginHandlerService

  @Autowired
  private lateinit var my2FAService: My2FAService

  @Autowired
  private lateinit var sendMail: SendMail

  @Autowired
  private lateinit var smsSenderConfig: SmsSenderConfig

  private lateinit var smsSender: SmsSender

  val smsConfigured
    get() = smsSenderConfig.isSmsConfigured() || SystemStatus.isDevelopmentMode()

  /**
   * Creates a OTP (valid for 2 minutes), stores it in the user's session and text or mail this code to the user.
   * @param mobilePhone If given (in a valid format), a text message with the OTP is sent to this number.
   */
  fun createAndSendOTP(request: HttpServletRequest, mobilePhone: String?): Result {
    var error: String? = null
    if (smsConfigured && !mobilePhone.isNullOrBlank() && StringHelper.checkPhoneNumberFormat(mobilePhone, false)) {
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
    if (SystemStatus.isDevelopmentMode()) {
      log.info { "Text message would be sent in production mode to '$mobilePhone': $msg" }
      if (!smsConfigured) {
        ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_MOBILE_OTP, code, TTL_MINUTES)
        return Result(true, getResultMessage(HttpResponseCode.SUCCESS, mobilePhone))
      }
    }
    val responseCode = smsSender.send(mobilePhone, msg)
    val result = Result(responseCode == HttpResponseCode.SUCCESS, getResultMessage(responseCode, mobilePhone))
    if (result.success) {
      ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_MOBILE_OTP, code, TTL_MINUTES)
    }
    return result
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
    mail.content = sendMail.renderGroovyTemplate(
      mail, "mail/otpMail.html", data,
      mail.subject, ThreadLocalUserContext.getUser()
    )
    val response = sendMail.send(mail)
    if (response) {
      ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_MAIL_OTP, code, TTL_MINUTES)
      return Result(
        true,
        translateMsg("user.My2FACode.sendCode.mail.sentSuccessfully", ThreadLocalUserContext.getUser().email)
      )
    }
    return Result(false, translate("mail.error.exception"))
  }

  /**
   * Creates a new 6 digit code. Removes any previous code from the user's session.
   */
  private fun createOTP(request: HttpServletRequest): String {
    removeAllAttributes(request)
    return NumberHelper.getSecureRandomDigits(6)
  }

  private fun getResultMessage(responseCode: HttpResponseCode, number: String): String {
    val errorKey = smsSender.getErrorMessage(responseCode)
    if (errorKey != null) {
      return translate(errorKey)
    }
    return translateMsg("address.sendSms.sendMessage.result.successful", number, PFDateTime.now().format())
  }

  /**
   * Checks the code (otp) entered by the user by getting the code created before via [createAndSendOTP].
   * If no otp is available, the code is checked by [My2FAService.validateOTP]
   * @param password Is only needed, if user received code by mail (due to security reasons).
   */
  fun checkOTP(request: HttpServletRequest, code: String, password: CharArray? = null): OTPCheckResult {
    var sessionCode =
      ExpiringSessionAttributes.getAttribute(request, SESSSION_ATTRIBUTE_MOBILE_OTP, String::class.java)
    if (sessionCode == null) {
      sessionCode = ExpiringSessionAttributes.getAttribute(request, SESSSION_ATTRIBUTE_MAIL_OTP, String::class.java)
      if (sessionCode == null) {
        // No otp is present, check the 2FA via authenticator app, if configured:
        return if (check2FA(code)) {
          OTPCheckResult.SUCCESS
        } else {
          OTPCheckResult.FAILED
        }
      }
      // Check password as an additional security factor (because OTP was sent by e-mail).
      if (password == null || password.isEmpty() || loginHandlerService.loginHandler.checkLogin(
          ThreadLocalUserContext.getUser().username,
          password
        ).loginResultStatus != LoginResultStatus.SUCCESS
      ) {
        return OTPCheckResult.WRONG_LOGIN_PASSWORD
      }
    }
    var counter =
      ExpiringSessionAttributes.getAttribute(request, SESSSION_ATTRIBUTE_FAILS_COUNTER, Int::class.java) ?: 0
    if (counter > 3) {
      log.warn { "User tries to enter otp 3 times without success. Removing OTP from user's session." }
      removeAllAttributes(request)
      return OTPCheckResult.FAILED
    }
    if (sessionCode == code) {
      // Successful: invalidate it:
      removeAllAttributes(request)
      ThreadLocalUserContext.getUserContext().updateLastSuccessful2FA()
      return OTPCheckResult.SUCCESS
    }
    // Check also the 2FA via authenticator if the user decided to use this instead of the created otp
    if (check2FA(code)) {
      removeAllAttributes(request)
      return OTPCheckResult.SUCCESS
    }
    ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_FAILS_COUNTER, ++counter, TTL_MINUTES)
    return OTPCheckResult.FAILED
  }

  private fun check2FA(code: String): Boolean {
    return my2FAService.validateOTP(code, true) == My2FAService.SUCCESS
  }

  private fun removeAllAttributes(request: HttpServletRequest) {
    ExpiringSessionAttributes.removeAttribute(request, SESSSION_ATTRIBUTE_MOBILE_OTP)
    ExpiringSessionAttributes.removeAttribute(request, SESSSION_ATTRIBUTE_MAIL_OTP)
    ExpiringSessionAttributes.removeAttribute(request, SESSSION_ATTRIBUTE_FAILS_COUNTER)
  }

  @PostConstruct
  fun postConstruct() {
    smsSender = SmsSender(smsSenderConfig)
  }

  companion object {
    private const val SESSSION_ATTRIBUTE_MOBILE_OTP = "My2FARestService.otp.mobile"
    private const val SESSSION_ATTRIBUTE_MAIL_OTP = "My2FARestService.otp.mail"
    private const val SESSSION_ATTRIBUTE_FAILS_COUNTER = "My2FARestService.otpFails"
    private const val TTL_MINUTES = 2
  }
}
