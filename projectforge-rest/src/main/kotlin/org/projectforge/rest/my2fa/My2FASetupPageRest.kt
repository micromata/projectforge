/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.my2fa

import mu.KotlinLogging
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.calendar.BarcodeServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.security.My2FAService
import org.projectforge.security.OTPCheckResult
import org.projectforge.security.TimeBased2FA
import org.projectforge.security.WebAuthnServicesRest
import org.projectforge.security.webauthn.WebAuthnEntryDO
import org.projectforge.security.webauthn.WebAuthnSupport
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import javax.validation.Valid

private val log = KotlinLogging.logger {}

/**
 * User may setup his/her 2FA and may check this. 2FA is usable via Authenticator apps (Microsoft or Google Authenticator),
 * via texting messages (if sms is configured) or via e-mails as a fall back.
 */
@RestController
@RequestMapping("${Rest.URL}/2FASetup")
class My2FASetupPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var authenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var my2FAService: My2FAService

  @Autowired
  private lateinit var my2FAServicesRest: My2FAServicesRest

  @Autowired
  private lateinit var my2FASetupMenuBadge: My2FASetupMenuBadge

  @Autowired
  private lateinit var userDao: UserDao

  @Autowired
  private lateinit var webAuthnSupport: WebAuthnSupport

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, response: HttpServletResponse): FormLayoutData {
    val data = My2FASetupData.create(webAuthnSupport, userDao)
    data.setDate(authenticationsService.getAuthenticatorTokenCreationDate())
    val layout = createLayout(request, response, data)
    return FormLayoutData(data, layout, createServerData(request))
  }

  @PostMapping("checkOTP")
  fun checkOTP(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @Valid @RequestBody postData: PostData<My2FASetupData>,
  ): ResponseEntity<ResponseAction> {
    val result = my2FAServicesRest.checkOTP(request, response, postData)
    return modifyResponseEntity(result, request, response, postData.data)
  }

  @PostMapping("webAuthnFinish")
  fun webAuthnFinish(
    request: HttpServletRequest,
    httpResponse: HttpServletResponse,
    @RequestBody postData: PostData<My2FAServicesRest.My2FAWebAuthnData>,
  ): ResponseEntity<ResponseAction> {
    val result = my2FAServicesRest.webAuthnFinish(request, httpResponse, postData)
    val data = My2FASetupData.create(webAuthnSupport, userDao)
    return modifyResponseEntity(result, request, httpResponse, data)
  }

  @GetMapping("sendSmsCode")
  fun sendSmsCode(request: HttpServletRequest): ResponseEntity<*> {
    return my2FAServicesRest.sendSmsCode(request)
  }

  @GetMapping("sendMailCode")
  fun sendMailCode(request: HttpServletRequest): ResponseEntity<*> {
    return my2FAServicesRest.sendMailCode(request)
  }

  private fun modifyResponseEntity(
    result: ResponseEntity<ResponseAction>,
    request: HttpServletRequest,
    response: HttpServletResponse,
    data: My2FASetupData,
  ): ResponseEntity<ResponseAction> {
    if (result.body?.targetType == TargetType.UPDATE) {
      // Update also the ui of the client (on success, the password fields will be shown after 2FA).
      result.body.let {
        it.addVariable("data", data) // Renew data (needed for webAuthnFinish)
        it.addVariable("ui", createLayout(request, response, data))
      }
    }
    return result
  }

  /**
   * Will be called, if the user wants to see the encryption options.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @Valid @RequestBody postData: PostData<My2FASetupData>
  ): ResponseEntity<ResponseAction> {
    val data = postData.data
    data.setDate(authenticationsService.getAuthenticatorTokenCreationDate())
    if (data.showAuthenticatorKey) {
      getLastSuccessful2FAResponseEntity(request, response, data)?.let {
        return it
      }
      data.authenticatorKey = authenticationsService.getAuthenticatorToken()
      if (data.authenticatorKey == null) {
        authenticationsService.createNewAuthenticatorToken()
        data.authenticatorKey = authenticationsService.getAuthenticatorToken()
      }
    } else {
      data.authenticatorKey = null
    }
    if (data.authenticatorKey == null) {
      data.showAuthenticatorKey = false // AuthenticatorKey not given, so set checkbox to false.
    }
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", createLayout(request, response, data))
        .addVariable("data", data)
    )
  }

  /**
   * Enables the 2FA for the logged-in user (if not already enabled). Fails, if authenticator token is already configured.
   */
  @Suppress("UNUSED_PARAMETER")
  @PostMapping("enableAuthenticatorApp")
  fun enableAuthenticatorApp(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @Valid @RequestBody postData: PostData<My2FASetupData>,
  ): ResponseEntity<ResponseAction> {
    if (!authenticationsService.getAuthenticatorToken().isNullOrBlank()) {
      log.error { "User tries to enable authenticator app, but authenticator token is already given!" }
      throw IllegalArgumentException("2FA already configured.")
    }
    getLastSuccessful2FAResponseEntity(request, response, postData.data)?.let {
      return it
    }
    val data = My2FASetupData.create(webAuthnSupport, userDao)
    data.mobilePhone = postData.data.mobilePhone
    authenticationsService.createNewAuthenticatorToken()
    data.showAuthenticatorKey = true
    data.authenticatorKey = authenticationsService.getAuthenticatorToken()
    data.setDate(authenticationsService.getAuthenticatorTokenCreationDate())
    my2FASetupMenuBadge.refreshUserBadgeCounter()
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", createLayout(request, response, data))
        .addVariable("data", data)
    )
  }

  /**
   * Disables the AuthenticatorApp for the logged-in user (if enabled) by deleting the authenticator token.
   * Fails, if authenticator token isn't configured.
   * Requires a valid 2FA not older than 1 minute.
   */
  @PostMapping("disableAuthenticatorApp")
  fun disableAuthenticatorApp(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @Valid @RequestBody postData: PostData<My2FASetupData>
  ): ResponseEntity<ResponseAction> {
    if (authenticationsService.getAuthenticatorToken().isNullOrBlank()) {
      log.error { "User tries to disable 2FA, but authenticator token isn't given!" }
      throw IllegalArgumentException("2FA not configured.")
    }
    val otp = postData.data.code
    if (!otp.isNullOrBlank()) {
      my2FAService.validateAuthenticatorOTP(otp) // Try to do the fresh 2FA
    }
    getLastSuccessful2FAResponseEntity(request, response, postData.data)?.let {
      return it
    }
    val data = My2FASetupData.create(webAuthnSupport, userDao)
    data.mobilePhone = postData.data.mobilePhone
    authenticationsService.clearAuthenticatorToken()
    my2FASetupMenuBadge.refreshUserBadgeCounter()
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", createLayout(request, response, data))
        .addVariable("data", data)
    )
  }

  /**
   * Save the mobile phone field as is is. Must be empty or in a valid phone number format.
   */
  @PostMapping("saveMobilePhone")
  fun saveMobilePhone(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @Valid @RequestBody postData: PostData<My2FASetupData>
  ): ResponseEntity<ResponseAction> {
    val mobilePhone = postData.data.mobilePhone
    if (!mobilePhone.isNullOrBlank() && !StringHelper.checkPhoneNumberFormat(mobilePhone, false)) {
      return showValidationErrors(ValidationError(translate("user.mobilePhone.invalidFormat"), "mobilePhone"))
    }
    getLastSuccessful2FAResponseEntity(request, response, postData.data)?.let {
      return it
    }
    val user = userDao.internalGetById(ThreadLocalUserContext.loggedInUserId)
    user!!.mobilePhone = mobilePhone
    userDao.internalUpdate(user)
    ThreadLocalUserContext.loggedInUser?.mobilePhone = mobilePhone // Update for showing the button 'send sms'
    my2FASetupMenuBadge.refreshUserBadgeCounter()
    return UIToast.createToastResponseEntity(
      translate("operation.updated"), color = UIColor.SUCCESS, targetType = TargetType.UPDATE,
      variables = mutableMapOf(
        "ui" to createLayout(request, response, postData.data),
        "data" to postData.data
      )
    )
  }

  private fun createLayout(request: HttpServletRequest, response: HttpServletResponse, data: My2FASetupData): UILayout {
    data.lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo()
    val smsConfigured = my2FAService.smsConfigured
    val authenticatorKey = authenticationsService.getAuthenticatorToken()
    val layout = UILayout("user.My2FA.setup.title")
    val userLC = LayoutContext(PFUserDO::class.java)
    var fieldset = UIFieldset(12, title = "user.My2FA.setup.info.title")
    layout.add(fieldset)
    fieldset
      .add(
        UIAlert(
          message = "user.My2FA.setup.info.1",
          markdown = true,
          color = UIColor.LIGHT
        )
      )
    if (!my2FAService.userConfigured2FA) {
      // Authenticator App and mobile phone is highly recommended:
      fieldset.add(
        UIAlert(
          message = "user.My2FA.setup.info.2",
          markdown = true,
          color = UIColor.WARNING
        )
      )
    }
    fieldset.add(
      UIAlert(
        message = "user.My2FA.setup.info.3",
        markdown = true,
        color = UIColor.LIGHT
      )
    )
    my2FAServicesRest.fill2FA(layout, fieldset, data, restServiceClass = this::class.java)

    val row = UIRow()
    layout.add(row)
    val leftCol = UICol(lg = 6)
    row.add(leftCol)
    val rightCol = UICol(lg = 6)
    row.add(rightCol)

    if (smsConfigured) {
      fieldset = UIFieldset(title = "user.My2FA.setup.sms.info.title")
      leftCol.add(fieldset)
      fieldset.add(
        UIAlert(
          message = "user.My2FA.setup.sms.info",
          markdown = true,
          color = UIColor.LIGHT
        )
      )
      val mobileCol = UICol()
      if (data.mobilePhone.isNullOrBlank()) {
        mobileCol.add(
          UIAlert(
            message = "user.My2FA.setup.sms.mobileNumberRecommended",
            markdown = true,
            color = UIColor.WARNING
          )
        )
      }
      fieldset.add(
        mobileCol
          .add(UIInput("mobilePhone", userLC))
          .add(
            UIButton.createSaveButton(
              responseAction = ResponseAction("/rs/2FASetup/saveMobilePhone", targetType = TargetType.POST),
              default = false,
            )
          )
      )
    }

    fieldset = UIFieldset(title = "webauthn.title")
    fieldset.add(UIAlert(message = "webauthn.info", color = UIColor.LIGHT))
    leftCol.add(fieldset)
    if (!data.webAuthnEntries.isNullOrEmpty()) {
      val grid = UIAgGrid("webAuthnEntries")
      grid.rowClickRedirectUrl =
        "${PagesResolver.getDynamicPageUrl(WebAuthnEntryPageRest::class.java, absolute = true)}id"
      val lc = LayoutContext(WebAuthnEntryDO::class.java)
      grid.columnDefs.add(UIAgGridColumnDef.createCol(lc, "created"))
      grid.columnDefs.add(UIAgGridColumnDef.createCol(lc, "lastUpdate"))
      grid.columnDefs.add(UIAgGridColumnDef.createCol(lc, "signCount"))
      grid.columnDefs.add(UIAgGridColumnDef.createCol(lc, "displayName"))
      fieldset.add(grid)
    }
    if (checkLastSuccessful2FA()) {
      fieldset.add(
        UIButton.createAddButton(
          ResponseAction(
            PagesResolver.getDynamicPageUrl(WebAuthnEntryPageRest::class.java, absolute = true, id = "id"),
            targetType = TargetType.REDIRECT,
          )
        )
      )
    } else {
      fieldset.add(UIAlert("webauthn.registration.2FARequired.info", color = UIColor.WARNING))
    }


    fieldset = UIFieldset(title = "user.My2FA.setup.authenticator.title")
    rightCol.add(fieldset)
    if (authenticatorKey.isNullOrBlank()) {
      fieldset.add(
        UIAlert(
          message = "user.My2FA.setup.authenticator.intro",
          markdown = true,
          color = UIColor.WARNING
        )
      )
      fieldset.add(
        UIButton.createDangerButton(
          layout,
          "enableAuthenticatorApp",
          title = "user.My2FA.setup.enableAuthenticatorApp",
          tooltip = "user.My2FA.setup.enableAuthenticatorApp.info",
          responseAction = ResponseAction("/rs/2FASetup/enableAuthenticatorApp", targetType = TargetType.POST),
          confirmMessage = "user.My2FA.setup.enableAuthenticatorApp.confirmMessage"
        )
      )
    } else {
      fieldset.add(
        UIButton.createDangerButton(
          layout,
          "disableAuthenticatorApp",
          title = "user.My2FA.setup.disableAuthenticatorApp",
          tooltip = "user.My2FA.setup.disableAuthenticatorApp.info",
          responseAction = ResponseAction("/rs/2FASetup/disableAuthenticatorApp", targetType = TargetType.POST),
          confirmMessage = "user.My2FA.setup.disableAuthenticatorApp.confirmMessage"
        )
      )
      fieldset.add(
        UIReadOnlyField("authenticatorKeyCreated", label = "created")
      )
      // Authenticator token is available
      fieldset.add(
        UICheckbox(
          "showAuthenticatorKey",
          label = "user.My2FA.setup.showAuthenticatorKey",
        )
      )
      if (data.showAuthenticatorKey) {
        if (checkLastSuccessful2FA(request, response, data) == CheckState.OTP_REQUIRED) {
          fieldset.add(
            UIAlert(
              message = "user.My2FA.required.extended",
              markdown = true,
              color = UIColor.DANGER
            )
          )
          data.showAuthenticatorKey = false // Uncheck the checkbox
        } else {
          val queryURL = TimeBased2FA.standard.getAuthenticatorUrl(
            authenticatorKey,
            ThreadLocalUserContext.loggedInUser!!.username!!,
            domainService.plainDomain ?: "unknown"
          )
          val barcodeUrl = BarcodeServicesRest.getBarcodeGetUrl(queryURL)
          fieldset.add(
            UIAlert(
              message = "user.My2FA.setup.authenticator.info",
              markdown = true,
              color = UIColor.SUCCESS
            )
          )
            .add(UIReadOnlyField("authenticatorKey", label = "user.My2FA.setup.authenticatorKey"))
            .add(UICustomized("image", mutableMapOf("src" to barcodeUrl, "alt" to barcodeUrl)))
        }
      }
    }

    layout.watchFields.add("showAuthenticatorKey")
    LayoutUtils.process(layout)
    WebAuthnServicesRest.addAuthenticateTranslations(layout)
    WebAuthnServicesRest.addRegisterTranslations(layout)
    return layout
  }

  private fun getLastSuccessful2FAResponseEntity(
    request: HttpServletRequest,
    response: HttpServletResponse,
    data: My2FASetupData,
  ): ResponseEntity<ResponseAction>? {
    return when (val checkState = checkLastSuccessful2FA(request, response, data)) {
      CheckState.OK -> null
      CheckState.OTP_REQUIRED -> showValidationErrors(
        ValidationError(translate("user.My2FA.required"), "code"),
        ValidationError(translate("user.My2FA.required.extended")) // For better visibility and more info twice.
      )
      CheckState.OTP_FAILED -> showValidationErrors(ValidationError(checkState.userMessage))
    }
  }

  private fun checkLastSuccessful2FA(
    request: HttpServletRequest,
    response: HttpServletResponse,
    data: My2FASetupData,
  ): CheckState {
    // Check any code if entered:
    val otpCheckResult = my2FAServicesRest.internalCheckOTP(request, response, otp = data.code)
    // SUCCESS, WRONG_LOGIN_PASSWORD, BLOCKED, NOT_CONFIGURED, FAILED, CODE_EMPTY
    if (otpCheckResult in arrayOf(OTPCheckResult.WRONG_LOGIN_PASSWORD, OTPCheckResult.FAILED, OTPCheckResult.BLOCKED)) {
      return CheckState.OTP_FAILED.withMessage(otpCheckResult.userMessage)
    } else if (otpCheckResult == OTPCheckResult.SUCCESS) {
      data.lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo()
      data.code = "" // Clear code after usage.
    }
    return if (checkLastSuccessful2FA()) {
      CheckState.OK
    } else {
      CheckState.OTP_REQUIRED
    }
  }

  /**
   * @see My2FAService.checklastSuccessful2FA
   */
  internal fun checkLastSuccessful2FA(): Boolean {
    return my2FAService.checklastSuccessful2FA(10, My2FAService.Unit.MINUTES)
  }

  private enum class CheckState {
    OK, OTP_REQUIRED, OTP_FAILED;

    var userMessage: String? = null

    fun withMessage(msg: String?): CheckState {
      this.userMessage = msg ?: "---"
      return this
    }
  }
}
