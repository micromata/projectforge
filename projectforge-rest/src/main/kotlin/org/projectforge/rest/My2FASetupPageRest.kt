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
import javax.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/my2FASetup")
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
  fun checkOTP(@Valid @RequestBody postData: PostData<My2FactorAuthentificationData>): ResponseAction {
    val otp = postData.data.testCode
    if (otp == null || my2FAService.validateOTP(otp) != My2FAService.SUCCESS) {
      return UIToast.createToast(translate("user.My2FA.setup.check.fail"), color = UIColor.DANGER)
    }
    return UIToast.createToast(translate("user.My2FA.setup.check.success"), color = UIColor.SUCCESS)
  }

  private fun createLayout(data: My2FactorAuthentificationData): UILayout {
    val layout = UILayout("user.My2FA.setup.title")
    val userLC = LayoutContext(PFUserDO::class.java)
    val fieldset = UIFieldset(12)
    layout.add(fieldset)
    val firstRow = UIRow()
    fieldset.add(firstRow)
    firstRow.add(UICol(6).add(UIReadOnlyField("username", userLC)))
    firstRow.add(
      UICol(6).add(UIInput("testCode", label = "user.My2FACode.code", tooltip = "user.My2FACode.code.info"))
        .add(
          UIButton(
            "test",
            title = translate("user.My2FA.setup.check.button"),
            tooltip = "user.My2FA.setup.check.button.info",
            color = UIColor.PRIMARY,
            responseAction = ResponseAction("/rs/my2FASetup/checkOTP", targetType = TargetType.POST)
          )
        )
    )
    fieldset.add(
      UICheckbox(
        "showAuthenticatorKey",
        label = "user.My2FA.setup.showAuthenticatorKey",
      )
    )
    val authenticatorKey = data.authenticatorKey
    if (data.showAuthenticatorKey && !authenticatorKey.isNullOrEmpty()) {
      fieldset.add(
        UIAlert(
          message = "user.My2FA.setup.info",
          markdown = true,
          color = UIColor.SUCCESS
        )
      )
      fieldset.add(UIReadOnlyField("authenticatorKey", label = "user.My2FA.setup.athenticatorKey"))
      val queryURL = TimeBased2FA.standard.getAuthenticatorUrl(
        authenticatorKey,
        ThreadLocalUserContext.getUser().username!!,
        domainService.plainDomain ?: "unknown"
      )
      val barcodeUrl = BarcodeServicesRest.getBarcodeGetUrl(queryURL)
      fieldset.add(UICustomized("image", mutableMapOf("src" to barcodeUrl, "alt" to barcodeUrl)))
      fieldset.add(
        UIAlert(
          message = "user.My2FA.setup.warning",
          markdown = true,
          color = UIColor.DANGER
        )
      )
    }
    layout.watchFields.add("showAuthenticatorKey")
    LayoutUtils.process(layout)
    return layout
  }
}
