/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.login.LoginService
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.projectforge.messaging.SmsSender
import org.projectforge.messaging.SmsSender.HttpResponseCode
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.security.My2FARequestConfiguration
import org.projectforge.security.My2FAService
import org.projectforge.security.OTPCheckResult
import org.projectforge.security.SecurityLogging
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

  @Autowired
  private lateinit var loginService: LoginService

  @Autowired
  private lateinit var my2FAService: My2FAService

  @Autowired
  private lateinit var my2FARequestConfiguration: My2FARequestConfiguration

  @Autowired
  private lateinit var sendMail: SendMail

  @Autowired
  private lateinit var smsSenderConfig: SmsSenderConfig

  private lateinit var smsSender: SmsSender

  val smsConfigured
    get() = my2FAService.smsConfigured

  /**
   * Creates a OTP (2 minutes valid) in [ExpiringSessionAttributes] and sends the code to the mobile number.
   * @param mobilePhone If given (in a valid format), a text message with the OTP is sent to this number.
   */
  fun createAndTextOTP(request: HttpServletRequest, mobilePhone: String?): Result {
    if (!smsConfigured) {
      return Result(false, "SMS not configured.")
    }
    if (mobilePhone.isNullOrBlank() || !StringHelper.checkPhoneNumberFormat(mobilePhone, false)) {
      return Result(false, "Invalid mobile phone format: $mobilePhone")
    }
    val code = createOTP(request)
    val msg = "${translateMsg("user.My2FACode.sendCode.mail.message", code)} ${translate("address.sendSms.doNotReply")}"
    val responseCode = smsSender.send(mobilePhone, msg)
    val result = Result(responseCode == HttpResponseCode.SUCCESS, getResultMessage(responseCode))
    if (SystemStatus.isDevelopmentMode()) {
      log.info { "Development mode: Text message would be sent in production mode to '$mobilePhone': $msg" }
      if (!result.success) {
        ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_MOBILE_OTP, code, TTL_MINUTES)
        return Result(true, "Test system: ${getResultMessage(HttpResponseCode.SUCCESS)}")
      }
    }
    if (result.success) {
      ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_MOBILE_OTP, code, TTL_MINUTES)
    }
    return result
  }

  /**
   * Creates a OTP (valid for 2 minutes), stores it in the user's session and mails this code to the user.
   */
  fun createAndMailOTP(request: HttpServletRequest): Result {
    require(!my2FAService.isMail2FADisabledForUser()) {
      val msg = "2FA via mail is disabled for users of the configured groups. Does the user try to attack the system by forcing 2FA by mail?"
      SecurityLogging.logSecurityWarn(this::class.java, "Illegal rest call", msg)
      msg
    }
    val code = createOTP(request)
    val mail = Mail()
    val user = ThreadLocalUserContext.getUser()
    mail.setTo(user)
    mail.subject = translate("user.My2FACode.sendCode.mail.title")
    mail.contentType = Mail.CONTENTTYPE_HTML
    val data: MutableMap<String, Any?> = mutableMapOf("otp" to code)
    mail.content = sendMail.renderGroovyTemplate(
      mail, "mail/otpMail.html", data,
      mail.subject, user
    )
    if (SystemStatus.isDevelopmentMode()) {
      log.info { "Development mode: Text message would be sent in production mode to '${mail.to}'. Code is $code" }
    }
    val response = sendMail.send(mail)
    if (response) {
      ExpiringSessionAttributes.setAttribute(request, SESSSION_ATTRIBUTE_MAIL_OTP, code, TTL_MINUTES)
      return Result(
        true,
        translateMsg("user.My2FACode.sendCode.mail.sentSuccessfully", PFDateTime.now().format())
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

  private fun getResultMessage(responseCode: HttpResponseCode): String {
    val errorKey = smsSender.getErrorMessage(responseCode)
    if (errorKey != null) {
      return translate(errorKey)
    }
    return translateMsg("user.My2FACode.sendCode.sms.sentSuccessfully", PFDateTime.now().format())
  }

  /**
   * Checks the code (otp) entered by the user by getting the code created before via [createAndSendOTP].
   * If no otp is available, the code is checked by [My2FAService.validateAuthenticatorOTP]
   * @param password Is only needed, if user received code by mail (due to security reasons).
   */
  fun checkOTP(request: HttpServletRequest, code: String?, password: CharArray? = null): OTPCheckResult {
    if (code.isNullOrBlank()) {
      return OTPCheckResult.CODE_EMPTY
    }
    val smsCode = ExpiringSessionAttributes.getAttribute(request, SESSSION_ATTRIBUTE_MOBILE_OTP, String::class.java)
    val mailCode = ExpiringSessionAttributes.getAttribute(request, SESSSION_ATTRIBUTE_MAIL_OTP, String::class.java)
    if (!mailCode.isNullOrBlank()) {
      require(!my2FAService.isMail2FADisabledForUser()) { "2FA via mail is disabled for users of the configured groups. How does the user got this mail code?" }
    }
    val result = my2FAService.validateOTP(code, smsCode, mailCode)
    if (code == mailCode && my2FARequestConfiguration.checkLoginPasswordRequired4Mail2FA()) {
      // Code sent by E-Mail and password required as additional trust.
      // Check password as an additional security factor (because OTP was sent by e-mail).
      if (password == null || password.isEmpty() || loginService.loginHandler.checkLogin(
          ThreadLocalUserContext.getUser().username,
          password
        ).loginResultStatus != LoginResultStatus.SUCCESS
      ) {
        return OTPCheckResult.WRONG_LOGIN_PASSWORD
      }
    }
    if (result == OTPCheckResult.SUCCESS) {
      // Successful: invalidate it:
      removeAllAttributes(request)
    }
    return result
  }

  private fun removeAllAttributes(request: HttpServletRequest) {
    ExpiringSessionAttributes.removeAttribute(request, SESSSION_ATTRIBUTE_MOBILE_OTP)
    ExpiringSessionAttributes.removeAttribute(request, SESSSION_ATTRIBUTE_MAIL_OTP)
  }

  @PostConstruct
  fun postConstruct() {
    smsSender = SmsSender(smsSenderConfig)
  }

  companion object {
    private const val SESSSION_ATTRIBUTE_MOBILE_OTP = "My2FARestService.otp.mobile"
    private const val SESSSION_ATTRIBUTE_MAIL_OTP = "My2FARestService.otp.mail"
    private const val TTL_MINUTES = 2
  }
}
