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
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.PostData
import org.projectforge.security.My2FARequestConfiguration
import org.projectforge.security.My2FAService
import org.projectforge.ui.*
import org.projectforge.web.My2FAHttpService
import org.springframework.beans.factory.annotation.Autowired
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

  @Autowired
  private lateinit var my2FARequestConfiguration: My2FARequestConfiguration

  /**
   * For validating the Authenticator's OTP, or OTP sent by sms or e-mail.
   */
  @PostMapping("checkOTP")
  fun checkOTP(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @Valid @RequestBody postData: PostData<My2FAData>
  ): ResponseEntity<ResponseAction> {
    val otp = postData.data.code
    val password = postData.data.password
    if (otp.isNullOrBlank()) {
      return UIToast.createToastResponseEntity(translate("user.My2FA.setup.check.fail"), color = UIColor.DANGER)
    }
    val otpCheck = my2FAHttpService.checkOTP(request, code = otp, password = password)
    if (otpCheck == My2FAHttpService.OTPCheckResult.SUCCESS) {
      ThreadLocalUserContext.getUserContext().lastSuccessful2FA?.let { lastSuccessful2FA ->
        cookieService.addLast2FACookie(request, response, lastSuccessful2FA)
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
    if (otpCheck == My2FAHttpService.OTPCheckResult.WRONG_LOGIN_PASSWORD) {
      return AbstractDynamicPageRest.showValidationErrors(
        ValidationError(
          translate("user.My2FACode.password.wrong"),
          "password"
        )
      )
    }
    return UIToast.createToastResponseEntity(translate("user.My2FA.setup.check.fail"), color = UIColor.DANGER)
  }

  /**
   * Sends a OTP as code (text to mobile phone of logged-in user).
   */
  @GetMapping("sendSmsCode")
  fun sendSmsCode(request: HttpServletRequest): ResponseEntity<ResponseAction> {
    val mobilePhone = ThreadLocalUserContext.getUser()?.mobilePhone
      ?: throw IllegalArgumentException("Mobile pone number not given.")
    if (!my2FAService.smsConfigured) {
      log.error { "User tried to send a text message, but sms isn't configured." }
      throw IllegalArgumentException("Internal error")
    }
    val result = my2FAHttpService.createAndTextOTP(request, mobilePhone)
    return createResponseEntity(result)
  }

  /**
   * Sends a OTP as code vial mail.
   */
  @GetMapping("sendMailCode")
  fun sendMailCode(request: HttpServletRequest): ResponseEntity<ResponseAction> {
    ThreadLocalUserContext.getUser()?.email ?: throw IllegalArgumentException("E-mail not given.")
    val result = my2FAHttpService.createAndMailOTP(request)
    return createResponseEntity(result)
  }

  fun fill2FA(layout: UILayout, my2FAData: My2FAData) {
    val fieldset = UIFieldset(12, title = "user.My2FACode.title")
    layout.add(fieldset)
    fill2FA(fieldset, my2FAData)
  }

  fun fill2FA(col: UICol, my2FAData: My2FAData) {
    val row = UIRow()
    col.add(row)
    fill2FA(row, my2FAData)
  }

  fun fill2FA(row: UIRow, my2FAData: My2FAData) {
    val mobilePhone = ThreadLocalUserContext.getUser()?.mobilePhone
    my2FAData.lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo()
    val smsAvailable = my2FAHttpService.smsConfigured && NumberHelper.matchesPhoneNumber(mobilePhone)
    val showPasswordCol = my2FARequestConfiguration.checkLoginPasswordRequired4Mail2FA()
    val width = if (showPasswordCol) 4 else 6

    val codeCol = UICol(md = width)
    row.add(codeCol)
    codeCol
      .add(
        UIInput(
          "code", label = "user.My2FACode.code", tooltip = "user.My2FACode.code.info",
          autoComplete = UIInput.AutoCompleteType.OFF
        )
      )
      .add(
        UIButton(
          "validate",
          title = translate("user.My2FACode.code.validate"),
          color = UIColor.PRIMARY,
          responseAction = ResponseAction("/rs/2FA/checkOTP", targetType = TargetType.POST),
          default = true,
        )
      )
    if (smsAvailable) {
      codeCol.add(createSendButton(My2FAType.SMS))
    }
    if (showPasswordCol) {
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
      )
      passwordCol.add(createSendButton(My2FAType.MAIL))
    } else {
      codeCol.add(createSendButton(My2FAType.MAIL))
    }

    val last2FACol = UICol(md = width)
    row.add(last2FACol)
    last2FACol.add(UIReadOnlyField("lastSuccessful2FA", label = "user.My2FACode.lastSuccessful2FA"))
  }

  /**
   * @param type [My2FAType.MAIL] and [My2FAType.SMS] supported.
   */
  private fun createSendButton(type: My2FAType): UIButton {
    val lowerType = type.name.lowercase()
    val type = lowerType.replaceFirstChar { it.uppercase() }
    return UIButton(
      "send${type}Code",
      title = translate("user.My2FACode.sendCode.$lowerType"), // user.My2FACode.sendCode.mail
      tooltip = "user.My2FACode.sendCode.$lowerType.info",
      color = UIColor.SECONDARY,
      responseAction = ResponseAction(
        RestResolver.getRestUrl(this::class.java, "send${type}Code"),
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
