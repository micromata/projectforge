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
import org.projectforge.business.login.LoginHandler
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/changePassword")
class ChangePasswordPageRest : AbstractDynamicPageRest() {

  @Autowired
  private lateinit var userDao: UserDao

  @Autowired
  private lateinit var userService: UserService

  class PasswordData(
    var userId: Int? = null,
    var oldPassword: CharArray? = null,
    var newPassword: CharArray? = null,
    var passwordRepeat: CharArray? = null
  ) {
    fun clear() {
      LoginHandler.clearPassword(oldPassword)
      LoginHandler.clearPassword(newPassword)
      LoginHandler.clearPassword(passwordRepeat)
    }
  }

  @PostMapping
  fun save(request: HttpServletRequest, @RequestBody postData: PostData<PasswordData>)
      : ResponseEntity<ResponseAction> {
    validateCsrfToken(request, postData)?.let { return it }
    val data = postData.data
    check(ThreadLocalUserContext.getUserId() == data.userId) { "Oups, ChangePasswordPage is called with another than the logged in user!" }

    if (!Arrays.equals(data.newPassword, data.passwordRepeat)) {
      val validationErrors = listOf(ValidationError.create("user.error.passwordAndRepeatDoesNotMatch"))
      return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
    }
    log.info { "The user wants to change his password." }
    val errorMsgKeys = userService.changePassword(data.userId, data.oldPassword, data.newPassword)
    data.clear() // Clear all passwords, if not already done, due to security reasons.
    processErrorKeys(errorMsgKeys)?.let {
      return it // Error messages occured:
    }
    return ResponseEntity(
      ResponseAction(
        PagesResolver.getDefaultUrl(),
        message = ResponseAction.Message("user.changePassword.msg.passwordSuccessfullyChanged"),
        targetType = TargetType.REDIRECT
      ), HttpStatus.OK
    )
  }

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val userId = ThreadLocalUserContext.getUserId()
    val data = PasswordData(userId)

    val layout = UILayout("user.changePassword.title")
    val oldPassword = UIInput(
      "oldPassword",
      label = "user.changePassword.oldPassword",
      required = true,
      focus = true,
      dataType = UIDataType.PASSWORD,
      autoComplete = UIInput.AutoCompleteType.CURRENT_PASSWORD
    )
    val newPassword = UIInput(
      "newPassword",
      label = "user.changePassword.newPassword",
      dataType = UIDataType.PASSWORD,
      required = true
    )
    val passwordRepeat = UIInput(
      "passwordRepeat",
      label = "passwordRepeat",
      dataType = UIDataType.PASSWORD,
      required = true
    )

    layout.add(oldPassword)
      .add(newPassword)
      .add(passwordRepeat)
      .addAction(UIButton.createCancelButton())
      .addAction(
        UIButton.createUpdateButton(
          responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java), targetType = TargetType.POST),
        )
      )
    LayoutUtils.process(layout)
    return FormLayoutData(data, layout, createServerData(request))
  }
}
