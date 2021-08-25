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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.filter.CookieService
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/2FASetup")
class My2FASetupPageRest : AbstractDynamicPageRest() {
  class My2FactorAuthentificationData(
    var userId: Int? = null,
    var username: String? = null,
    var authenticatorKey: String? = null,
    var showAuthenticatorKey: Boolean = false,
    var testCode: String? = null,
  )

  @Autowired
  private lateinit var authenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var cookieService: CookieService

  @Autowired
  private lateinit var my2FAService: My2FAService

  @Autowired
  private lateinit var userDao: UserDao

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val userId = ThreadLocalUserContext.getUserId()
    val user = userDao.getById(userId)
    val data = My2FactorAuthentificationData(userId, username = user.username)
    val layout = createLayout(data)
    return FormLayoutData(data, layout, createServerData(request))
  }

  /**
   * Will be called, if the user wants to see the encryption options.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<My2FactorAuthentificationData>): ResponseEntity<ResponseAction> {
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
   * For testing the Authenticator's code.
   */
  @PostMapping("checkOTP")
  fun checkOTP(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @Valid @RequestBody postData: PostData<My2FactorAuthentificationData>
  ): ResponseAction {
    val otp = postData.data.testCode
    if (otp == null || my2FAService.validateOTP(otp) != My2FAService.SUCCESS) {
      return UIToast.createToast(translate("user.My2FA.setup.check.fail"), color = UIColor.DANGER)
    }
    ThreadLocalUserContext.getUserContext().lastSuccessful2FA?.let { lastSuccessful2FA ->
      cookieService.addLast2FACookie(request, response, lastSuccessful2FA)
    }
    return UIToast.createToast(translate("user.My2FA.setup.check.success"), color = UIColor.SUCCESS)
  }

  /**
   * Enables the 2FA for the logged-in user (if not already enabled). Fails, if authenticator token is already configured.
   */
  @PostMapping("enable")
  fun enable(@Valid @RequestBody postData: PostData<My2FactorAuthentificationData>): ResponseEntity<ResponseAction> {
    if (!authenticationsService.getAuthenticatorToken().isNullOrBlank()) {
      log.error { "User tries to enable 2FA, but authenticator token is already given!" }
      throw IllegalArgumentException("2FA already configured.")
    }
    val user = ThreadLocalUserContext.getUser()
    val data = My2FactorAuthentificationData(user.id, username = user.username)
    authenticationsService.createNewAuthenticatorToken()
    data.showAuthenticatorKey = true
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", createLayout(data))
        .addVariable("data", data)
    )
  }

  /**
   * Disables the 2FA for the logged-in user (if enabled). Fails, if authenticator token isn't configured.
   * Requires a valid 2FA not older than 1 minute.
   */
  @PostMapping("disable")
  fun disable(@Valid @RequestBody postData: PostData<My2FactorAuthentificationData>): ResponseEntity<ResponseAction> {
    if (authenticationsService.getAuthenticatorToken().isNullOrBlank()) {
      log.error { "User tries to disable 2FA, but authenticator token isn't given!" }
      throw IllegalArgumentException("2FA not configured.")
    }
    val otp = postData.data.testCode
    if (!otp.isNullOrBlank()) {
      my2FAService.validateOTP(otp) // Try to do the fresh 2FA
    }
    if (!my2FAService.checklastSuccessful2FA(10, My2FAService.Unit.MINUTES)) {
      return showValidationErrors(ValidationError(translate("user.My2FA.required"), "testCode"))
    }
    val user = ThreadLocalUserContext.getUser()
    val data = My2FactorAuthentificationData(user.id, username = user.username)
    authenticationsService.clearAuthenticatorToken()
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", createLayout(data))
        .addVariable("data", data)
    )
  }

  private fun createLayout(data: My2FactorAuthentificationData): UILayout {
    val authenticatorKey = authenticationsService.getAuthenticatorToken()
    val layout = UILayout("user.My2FA.setup.title")
    val userLC = LayoutContext(PFUserDO::class.java)
    val fieldset = UIFieldset(12)
    layout.add(fieldset)
    val firstRow = UIRow()
    fieldset.add(firstRow)
    firstRow.add(UICol(md = 6).add(UIReadOnlyField("username", userLC)))

    if (!authenticatorKey.isNullOrBlank()) {
      // Show validate function only, if authenticator token of the user is given.
      firstRow.add(
        UICol(md = 6).add(UIInput("testCode", label = "user.My2FACode.code", tooltip = "user.My2FACode.code.info"))
          .add(
            UIButton(
              "validate",
              title = translate("user.My2FACode.code.validate"),
              color = UIColor.PRIMARY,
              responseAction = ResponseAction("/rs/2FASetup/checkOTP", targetType = TargetType.POST)
            )
          )
      )
    }
    if (authenticatorKey.isNullOrBlank()) {
      fieldset.add(
        UIButton(
          "enable",
          title = translate("user.My2FA.setup.enable"),
          tooltip = "user.My2FA.setup.enable.info",
          color = UIColor.DANGER,
          responseAction = ResponseAction("/rs/2FASetup/enable", targetType = TargetType.POST),
          confirmMessage = translate("user.My2FA.setup.enable.confirmMessage")
        )
      )
    } else {
      fieldset.add(
        UIButton(
          "disable",
          title = translate("user.My2FA.setup.disable"),
          tooltip = "user.My2FA.setup.disable.info",
          color = UIColor.DANGER,
          responseAction = ResponseAction("/rs/2FASetup/disable", targetType = TargetType.POST),
          confirmMessage = translate("user.My2FA.setup.disable.confirmMessage")
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
        if (!my2FAService.checklastSuccessful2FA(10, My2FAService.Unit.MINUTES)) {
          fieldset.add(
            UIAlert(
              message = "user.My2FA.required",
              markdown = true,
              color = UIColor.DANGER
            )
          )
          data.showAuthenticatorKey = false // Uncheck the checkbox
        } else {
          fieldset.add(
            UIAlert(
              message = "user.My2FA.setup.info",
              markdown = true,
              color = UIColor.SUCCESS
            )
          )
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
                  .add(UICustomized("image", mutableMapOf("src" to barcodeUrl, "alt" to barcodeUrl)))
              )
              .add(
                UICol(md = 6)
                  .add(UIReadOnlyField("authenticatorKey", label = "user.My2FA.setup.athenticatorKey"))
                  .add(
                    UIAlert(
                      message = "user.My2FA.setup.warning",
                      markdown = true,
                      color = UIColor.DANGER
                    )
                  )
              )
          )
        }
      }
    }
    layout.watchFields.add("showAuthenticatorKey")
    LayoutUtils.process(layout)
    return layout
  }
}
