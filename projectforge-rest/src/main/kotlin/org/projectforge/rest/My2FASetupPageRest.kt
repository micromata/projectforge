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
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.security.My2FAService
import org.projectforge.security.TimeBased2FA
import org.projectforge.ui.*
import org.projectforge.web.My2FAHttpService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
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
  private lateinit var my2FAHttpService: My2FAHttpService

  @Autowired
  private lateinit var my2FAServicesRest: My2FAServicesRest

  @Autowired
  private lateinit var userDao: UserDao

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val data = My2FAData()
    userDao.internalGetById(ThreadLocalUserContext.getUserId())?.let { user ->
      data.mobilePhone = user.mobilePhone
    }
    val layout = createLayout(data)
    return FormLayoutData(data, layout, createServerData(request))
  }

  /**
   * Will be called, if the user wants to see the encryption options.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<My2FAData>): ResponseEntity<ResponseAction> {
    val data = postData.data
    if (data.showAuthenticatorKey) {
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
        .addVariable("ui", createLayout(data))
        .addVariable("data", data)
    )
  }

  /**
   * Enables the 2FA for the logged-in user (if not already enabled). Fails, if authenticator token is already configured.
   */
  @Suppress("UNUSED_PARAMETER")
  @PostMapping("enableAuthenticatorApp")
  fun enableAuthenticatorApp(@Valid @RequestBody postData: PostData<My2FAData>): ResponseEntity<ResponseAction> {
    if (!authenticationsService.getAuthenticatorToken().isNullOrBlank()) {
      log.error { "User tries to enable authenticator app, but authenticator token is already given!" }
      throw IllegalArgumentException("2FA already configured.")
    }
    if (!checklastSuccessful2FA()) {
      return showValidationErrors(ValidationError(translate("user.My2FA.required"), "code"))
    }
    val data = My2FAData()
    data.mobilePhone = postData.data.mobilePhone
    authenticationsService.createNewAuthenticatorToken()
    data.showAuthenticatorKey = true
    data.authenticatorKey = authenticationsService.getAuthenticatorToken()
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", createLayout(data))
        .addVariable("data", data)
    )
  }

  /**
   * Disables the AuthenticatorApp for the logged-in user (if enabled) by deleting the authenticator token.
   * Fails, if authenticator token isn't configured.
   * Requires a valid 2FA not older than 1 minute.
   */
  @PostMapping("disableAuthenticatorApp")
  fun disableAuthenticatorApp(@Valid @RequestBody postData: PostData<My2FAData>): ResponseEntity<ResponseAction> {
    if (authenticationsService.getAuthenticatorToken().isNullOrBlank()) {
      log.error { "User tries to disable 2FA, but authenticator token isn't given!" }
      throw IllegalArgumentException("2FA not configured.")
    }
    val otp = postData.data.code
    if (!otp.isNullOrBlank()) {
      my2FAService.validateAuthenticatorOTP(otp) // Try to do the fresh 2FA
    }
    if (!checklastSuccessful2FA()) {
      return showValidationErrors(ValidationError(translate("user.My2FA.required"), "code"))
    }
    val data = My2FAData()
    data.mobilePhone = postData.data.mobilePhone
    authenticationsService.clearAuthenticatorToken()
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", createLayout(data))
        .addVariable("data", data)
    )
  }

  /**
   * Save the mobile phone field as is is. Must be empty or in a valid phone number format.
   */
  @PostMapping("saveMobilePhone")
  fun saveMobilePhone(@Valid @RequestBody postData: PostData<My2FAData>): ResponseEntity<ResponseAction> {
    val mobilePhone = postData.data.mobilePhone
    if (!mobilePhone.isNullOrBlank() && !StringHelper.checkPhoneNumberFormat(mobilePhone, false)) {
      return showValidationErrors(ValidationError(translate("user.mobilePhone.invalidFormat"), "mobilePhone"))
    }
    if (!checklastSuccessful2FA()) {
      return showValidationErrors(ValidationError(translate("user.My2FA.required"), "code"))
    }
    val user = userDao.internalGetById(ThreadLocalUserContext.getUserId())
    user.mobilePhone = mobilePhone
    userDao.internalUpdate(user)
    return UIToast.createToastResponseEntity(translate("operation.updated"), color = UIColor.SUCCESS)
  }

  private fun createLayout(data: My2FAData): UILayout {
    data.lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo()
    val smsConfigured = my2FAHttpService.smsConfigured
    val authenticatorKey = authenticationsService.getAuthenticatorToken()
    val layout = UILayout("user.My2FA.setup.title")
    val userLC = LayoutContext(PFUserDO::class.java)
    var fieldset = UIFieldset(12, title = "user.My2FA.setup.info.title")
    layout.add(fieldset)
    fieldset.add(
      UIAlert(
        message = "user.My2FA.setup.info",
        markdown = true,
        color = UIColor.LIGHT
      )
    )
    my2FAServicesRest.fill2FA(fieldset, data)

    val fieldsetLenth = if (smsConfigured) 6 else 12

    val row = UIRow()
    layout.add(row)

    fieldset = UIFieldset(md = fieldsetLenth, title = "user.My2FA.setup.athenticator.title")
    row.add(fieldset)
    fieldset.add(
      UIAlert(
        message = "user.My2FA.setup.authenticator.intro",
        markdown = true,
        color = UIColor.LIGHT
      )
    )
    if (authenticatorKey.isNullOrBlank()) {
      fieldset.add(
        UIButton(
          "enableAuthenticatorApp",
          title = translate("user.My2FA.setup.enableAuthenticatorApp"),
          tooltip = "user.My2FA.setup.enableAuthenticatorApp.info",
          color = UIColor.DANGER,
          responseAction = ResponseAction("/rs/2FASetup/enableAuthenticatorApp", targetType = TargetType.POST),
          confirmMessage = translate("user.My2FA.enableAuthenticatorApp.enable.confirmMessage")
        )
      )
    } else {
      fieldset.add(
        UIButton(
          "disableAuthenticatorApp",
          title = translate("user.My2FA.setup.disableAuthenticatorApp"),
          tooltip = "user.My2FA.setup.disableAuthenticatorApp.info",
          color = UIColor.DANGER,
          responseAction = ResponseAction("/rs/2FASetup/disableAuthenticatorApp", targetType = TargetType.POST),
          confirmMessage = translate("user.My2FA.setup.disableAuthenticatorApp.confirmMessage")
        )
      )
      // Authenticator token is available
      fieldset.add(
        UICheckbox(
          "showAuthenticatorKey",
          label = "user.My2FA.setup.showAuthenticatorKey",
        )
      )
      if (data.showAuthenticatorKey) {
        if (!checklastSuccessful2FA()) {
          fieldset.add(
            UIAlert(
              message = "user.My2FA.required",
              markdown = true,
              color = UIColor.DANGER
            )
          )
          data.showAuthenticatorKey = false // Uncheck the checkbox
        } else {
          val queryURL = TimeBased2FA.standard.getAuthenticatorUrl(
            authenticatorKey,
            ThreadLocalUserContext.getUser().username!!,
            domainService.plainDomain ?: "unknown"
          )
          val barcodeUrl = BarcodeServicesRest.getBarcodeGetUrl(queryURL)
          fieldset.add(
            UIRow()
              .add(
                UICol(md = 6)
                  .add(
                    UIAlert(
                      message = "user.My2FA.setup.authenticator.info",
                      markdown = true,
                      color = UIColor.SUCCESS
                    )
                  )
                  .add(UIReadOnlyField("authenticatorKey", label = "user.My2FA.setup.athenticatorKey"))
              )
              .add(
                UICol(md = 6)
                  .add(UICustomized("image", mutableMapOf("src" to barcodeUrl, "alt" to barcodeUrl)))
              )
          )
        }
      }
    }

    if (smsConfigured) {
      fieldset = UIFieldset(md = fieldsetLenth, title = "user.My2FA.setup.sms.info.title")
      row.add(fieldset)
      fieldset.add(
        UIAlert(
          message = "user.My2FA.setup.sms.info",
          markdown = true,
          color = UIColor.LIGHT
        )
      )
      fieldset.add(
        UICol()
          .add(UIInput("mobilePhone", userLC))
          .add(
            UIButton(
              "save",
              title = translate("save"),
              color = UIColor.LIGHT,
              responseAction = ResponseAction("/rs/2FASetup/saveMobilePhone", targetType = TargetType.POST),
            )
          )
      )
    }

    layout.watchFields.add("showAuthenticatorKey")
    LayoutUtils.process(layout)
    return layout
  }


  private fun checklastSuccessful2FA(): Boolean {
    return my2FAService.checklastSuccessful2FA(10, My2FAService.Unit.MINUTES)
  }
}
