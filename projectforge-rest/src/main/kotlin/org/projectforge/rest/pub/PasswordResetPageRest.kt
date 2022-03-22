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

package org.projectforge.rest.pub

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.service.PasswordResetService
import org.projectforge.rest.My2FAServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.ServerData
import org.projectforge.security.My2FAData
import org.projectforge.security.My2FAService
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


/**
 * This rest service should be available without login (public).
 * On this page, the user may enter his login name or e-mail and request a password reset.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/passwordReset")
open class PasswordResetPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var passwordResetService: PasswordResetService

  @Autowired
  private lateinit var my2FAService: My2FAService

  @Autowired
  private lateinit var my2FAServicesRest: My2FAServicesRest

  /**
   * @param token The token sent by mail (is mandatory for getting and checking the user).
   * @see [PasswordResetService.checkToken]
   */
  @GetMapping("dynamic")
  fun getForm(@RequestParam("token") token: String): FormLayoutData {
    val user = passwordResetService.checkToken(token)
    val layout = UILayout("password.reset.title")
    if (user == null) {
      layout.add(
        UIAlert(
          message = "password.reset.error",
          color = UIColor.DANGER
        )
      )
    } else {
      val userContext = UserContext(user)
      my2FAServicesRest.fillLayout4PublicPage(layout, userContext)
      layout
        .add(
          UIInput(
            "newPassword",
            label = "user.changePassword.newPassword",
            dataType = UIDataType.PASSWORD,
            required = true
          )
        )
        .add(
          UIInput(
            "newPasswordRepeat",
            label = "passwordRepeat",
            dataType = UIDataType.PASSWORD,
            required = true
          )
        )
    }
    layout.add(
      UIButton(
        "cancel",
        translate("cancel"),
        UIColor.DANGER,
      ).redirectToDefaultPage()
    )
    if (user != null) {
      layout.add(
        UIButton(
          "reset",
          translate("password.forgotten.request"),
          UIColor.SUCCESS,
          responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java), targetType = TargetType.POST),
          default = true
        )
      )
    }
    LayoutUtils.process(layout)
    return FormLayoutData(null, layout, ServerData())
  }

  @PostMapping
  fun post(@RequestBody postData: PostData<PasswordResetData>)
      : ResponseEntity<ResponseAction> {
    val newPassword = postData.data.newPassword
    val newPasswordRepeat = postData.data.newPasswordRepeat
    val code = postData.data.code
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
    )
  }

  class PasswordResetData : My2FAData() {
    var newPassword: CharArray? = null
    var newPasswordRepeat: CharArray? = null
  }
}
