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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.user.filter.CookieService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.pub.LoginPageRest
import org.projectforge.rest.pub.My2FAPublicServicesRest
import org.projectforge.security.My2FAData
import org.projectforge.security.My2FAService
import org.projectforge.security.OTPCheckResult
import org.projectforge.ui.*
import org.projectforge.web.My2FAHttpService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

private val log = KotlinLogging.logger {}

/**
 * User may setup his/her 2FA and may check this. 2FA is usable via Authenticator apps (Microsoft or Google Authenticator),
 * via texting messages (if sms is configured) or via e-mails as a fall back.
 */
@RestController
@RequestMapping("${Rest.URL}/2FA")
class My2FAServicesRest {
  @Autowired
  private lateinit var cookieService: CookieService

  @Autowired
  private lateinit var my2FAService: My2FAService

  @Autowired
  private lateinit var my2FAHttpService: My2FAHttpService

  /**
   * For validating the Authenticator's OTP, or OTP sent by sms or e-mail.
   * @param afterLogin Used by [org.projectforge.rest.pub.My2FAPublicServicesRest]. If true, no toast should be
   *                   created, a CHECK_AUTHENTICATION should be returned.
   */
  @PostMapping("checkOTP")
  fun checkOTP(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @Valid @RequestBody postData: PostData<My2FAData>,
    @RequestParam("redirect", required = false) redirect: String?,
    afterLogin: Boolean = false,
  ): ResponseEntity<ResponseAction> {
    val otp = postData.data.code
    val password = postData.data.password
    if (otp.isNullOrBlank()) {
      return UIToast.createToastResponseEntity(translate("user.My2FA.setup.check.fail"), color = UIColor.DANGER)
    }
    val otpCheck = my2FAHttpService.checkOTP(request, code = otp, password = password)
    if (otpCheck == OTPCheckResult.SUCCESS) {
      ThreadLocalUserContext.getUserContext().lastSuccessful2FA?.let { lastSuccessful2FA ->
        cookieService.addLast2FACookie(request, response, lastSuccessful2FA)
      }
      if (afterLogin) {
        val redirectUrl = LoginPageRest.getRedirectUrl(request, postData.serverData)
        return ResponseEntity(
          ResponseAction(targetType = TargetType.CHECK_AUTHENTICATION, url = redirectUrl),
          HttpStatus.OK
        )
      }
      postData.data.let { data ->
        data.code = ""
        data.lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo()
        return UIToast.createToastResponseEntity(
          translate("user.My2FA.setup.check.success"),
          color = UIColor.SUCCESS,
          mutableMapOf("data" to postData.data),
          merge = true,
          targetType = TargetType.UPDATE
        )
      }
    }
    // otp check wasn't successful:
    otpCheck.userMessage?.let { msg ->
      return AbstractDynamicPageRest.showValidationErrors(
        ValidationError(msg)
      )
    }
    if (otpCheck == OTPCheckResult.WRONG_LOGIN_PASSWORD) {
      return AbstractDynamicPageRest.showValidationErrors(
        ValidationError(
          translate("user.My2FACode.password.wrong"),
          "password"
        )
      )
    }
    return AbstractDynamicPageRest.showValidationErrors(
      ValidationError(
        translate("user.My2FACode.error.validation"),
        "code"
      )
    )
  }

  /**
   * Sends a OTP as code (text to mobile phone of logged-in user).
   */
  @GetMapping("sendSmsCode")
  fun sendSmsCode(request: HttpServletRequest): ResponseEntity<*> {
    val user = ThreadLocalUserContext.getUser()
    val mobilePhone = user.mobilePhone
    if (mobilePhone == null) {
      log.error { "User '${user.username}' tried to send 2FA code as text message, but mobile phone isn't available." }
      return ResponseEntity<Any>(HttpStatus.BAD_REQUEST)
    }
    if (!my2FAService.smsConfigured) {
      log.error { "User tried to send a text message, but sms isn't configured." }
      return ResponseEntity<Any>(HttpStatus.BAD_REQUEST)
    }
    val result = my2FAHttpService.createAndTextOTP(request, mobilePhone)
    return createResponseEntity(result)
  }


  /**
   * Sends a OTP as code vial mail.
   */
  @GetMapping("sendMailCode")
  fun sendMailCode(request: HttpServletRequest): ResponseEntity<*> {
    val user = ThreadLocalUserContext.getUser()
    if (user.email.isNullOrBlank()) {
      log.error { "User '${user.username}' tried to send 2FA code as mail, but e-mail address isn't available." }
      return ResponseEntity<Any>(HttpStatus.BAD_REQUEST)
    }
    val result = my2FAHttpService.createAndMailOTP(request)
    return createResponseEntity(result)
  }

  fun fill2FA(layout: UILayout, my2FAData: My2FAData, redirectUrl: String? = null) {
    val fieldset = UIFieldset(12, title = "user.My2FACode.title")
    layout.add(fieldset)
    fill2FA(fieldset, my2FAData, redirectUrl)
  }

  fun fill2FA(col: UICol, my2FAData: My2FAData, redirectUrl: String? = null) {
    val row = UIRow()
    col.add(row)
    fill2FA(row, my2FAData, redirectUrl)
  }

  fun fill2FA(row: UIRow, my2FAData: My2FAData, redirectUrl: String? = null) {
    my2FAData.lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo()
    val codeCol = UICol(md = 6)
    row.add(codeCol)
    fillCodeCol(codeCol, redirectUrl)
    //val showPasswordCol = my2FARequestConfiguration.checkLoginPasswordRequired4Mail2FA()
    /*
      // Enable E-Mail with password (required for security reasons, if attacker has access to local client)
      val passwordCol = UICol(md = width)
      row.add(passwordCol)
      passwordCol.add(
        UIInput(
          "password",
          label = "password",
          tooltip = "user.My2FACode.password.info",
          dataType = UIDataType.PASSWORD,
          autoComplete = UIInput.AutoCompleteType.OFF
        )
      )*/

    val last2FACol = UICol(md = 6)
    row.add(last2FACol)
    last2FACol.add(UIReadOnlyField("lastSuccessful2FA", label = "user.My2FACode.lastSuccessful2FA"))
  }

  fun fillLayout4LoginPage(layout: UILayout, userContext: UserContext, redirectUrl: String?) {
    val fieldset = UIFieldset(12, title = "user.My2FACode.title")
    layout.add(fieldset)
    fillCodeCol(fieldset, redirectUrl, userContext.user?.mobilePhone, usePublicServices = true)
  }

  /**
   * @param usePublicServices Only true for login process (the private services aren't yet available.
   */
  private fun fillCodeCol(
    codeCol: UICol,
    redirectUrl: String? = null,
    mobilePhone: String? = ThreadLocalUserContext.getUser()?.mobilePhone,
    usePublicServices: Boolean = false,
  ) {
    val smsAvailable = my2FAHttpService.smsConfigured && NumberHelper.matchesPhoneNumber(mobilePhone)
    val restServiceClass = if (usePublicServices) My2FAPublicServicesRest::class.java else this::class.java
    codeCol
      .add(
        UIInput(
          "code", label = "user.My2FACode.code", tooltip = "user.My2FACode.code.info",
          autoComplete = UIInput.AutoCompleteType.OFF,
          focus = true,
        )
      )
      .add(
        UIButton(
          "validate",
          title = translate("user.My2FACode.code.validate"),
          color = UIColor.PRIMARY,
          responseAction = ResponseAction(
            RestResolver.getRestUrl(restServiceClass, "checkOTP", params = mapOf("redirect" to redirectUrl)),
            targetType = TargetType.POST
          ),
          default = true,
        )
      )
    if (smsAvailable) {
      codeCol.add(createSendButton(My2FAType.SMS, restServiceClass))
    }
    codeCol.add(createSendButton(My2FAType.MAIL, restServiceClass))
  }

  /**
   * @param type [My2FAType.MAIL] and [My2FAType.SMS] supported.
   * @param usePublicServices Only true for login process (the private services aren't yet available.
   */
  private fun createSendButton(type: My2FAType, restServiceClass: Class<*> = this::class.java): UIButton {
    val lowerType = type.name.lowercase()
    val type = lowerType.replaceFirstChar { it.uppercase() }
    return UIButton(
      "send${type}Code",
      title = translate("user.My2FACode.sendCode.$lowerType"), // user.My2FACode.sendCode.mail
      tooltip = "user.My2FACode.sendCode.$lowerType.info",
      color = UIColor.SECONDARY,
      responseAction = ResponseAction(
        RestResolver.getRestUrl(restServiceClass, "send${type}Code"),
        targetType = TargetType.GET
      ),
    )
  }

  companion object {
    internal fun createResponseEntity(result: My2FAHttpService.Result): ResponseEntity<ResponseAction> {
      val color = if (result.success) {
        UIColor.SUCCESS
      } else {
        UIColor.DANGER
      }
      return UIToast.createToastResponseEntity(result.message, color = color)
    }
  }
}
