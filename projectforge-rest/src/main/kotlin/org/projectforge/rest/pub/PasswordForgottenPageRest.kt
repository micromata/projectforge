/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.i18n.I18nKeys
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.service.PasswordResetService
import org.projectforge.login.LoginService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.ServerData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest


/**
 * This rest service should be available without login (public).
 * On this page, the user may enter his login name or e-mail and request a password reset.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/passwordForgotten")
open class PasswordForgottenPageRest : AbstractDynamicPageRest() {
  class PasswordForgottenData(var usernameEmail: String? = null)

  @Autowired
  private lateinit var passwordResetService: PasswordResetService

  /**
   */
  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    if (LoginService.getUserContext(request) != null) {
      return LayoutUtils.getMessageFormLayoutData(
        LAYOUT_TITLE,
        I18nKeys.ERROR_NOT_AVAILABLE_FOR_LOGGED_IN_USERS,
        UIColor.WARNING
      )
    }
    return FormLayoutData(null, getLayout(), ServerData())
  }

  @PostMapping
  fun post(request: HttpServletRequest, @RequestBody postData: PostData<PasswordForgottenData>): ResponseEntity<*> {
    if (LoginService.getUserContext(request) != null) {
      return RestUtils.badRequest(translate(I18nKeys.ERROR_NOT_AVAILABLE_FOR_LOGGED_IN_USERS))
    }
    val usernameEmail = postData.data.usernameEmail
    if (usernameEmail.isNullOrBlank()) {
      return showValidationErrors(
        ValidationError.createFieldRequired(
          FIELD_ID_USERNAME_EMAIL,
          translate(I18N_USERNAME_EMAIL)
        )
      )
    }
    val link = domainService.getDomain(
      PagesResolver.getDynamicPageUrl(PasswordResetPageRest::class.java, mapOf("token" to "TOKEN"))
    )
    passwordResetService.sendMail(usernameEmail, link)
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", getLayout(translateMsg("password.forgotten.mailSentTo", usernameEmail)))
    )
  }

  private fun getLayout(message: String? = null, statusOK: Boolean = true): UILayout {
    val layout = UILayout(LAYOUT_TITLE)

    if (!message.isNullOrBlank()) {
      layout.add(UIAlert("'$message", color = if (statusOK) UIColor.INFO else UIColor.DANGER))
    }
    layout.add(
      UIInput(
        FIELD_ID_USERNAME_EMAIL,
        required = true,
        label = I18N_USERNAME_EMAIL,
        focus = true,
        autoComplete = UIInput.AutoCompleteType.USERNAME
      )
    )
      .add(
        UIButton.createCancelButton()
      )
      .add(
        UIButton.createDefaultButton(
          "request",
          title = "password.forgotten.request",
          responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java), targetType = TargetType.POST),
        )
      )
    LayoutUtils.process(layout)
    return layout
  }

  companion object {
    private const val LAYOUT_TITLE = "password.forgotten.title"
    private const val I18N_USERNAME_EMAIL = "password.reset.username_email"
    private const val FIELD_ID_USERNAME_EMAIL = "usernameEmail"
  }
}
