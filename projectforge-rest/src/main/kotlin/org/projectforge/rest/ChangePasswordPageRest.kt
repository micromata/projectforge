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
import org.projectforge.business.password.PasswordQualityService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.i18n.I18nKeyAndParams
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
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
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var passwordQualityService: PasswordQualityService

  @PostMapping
  fun save(request: HttpServletRequest, @RequestBody postData: PostData<ChangePasswordData>)
      : ResponseEntity<ResponseAction> {
    return internalSave(request, postData) { data, changeOwn ->
      if (changeOwn) {
        log.info { "The user wants to change his password." }
        userService.changePassword(data.userId, data.loginPassword, data.newPassword)
      } else {
        log.info { "Admin user wants to change password of user '${data.userDisplayName}' with id ${data.userId}." }
        userService.changePasswordByAdmin(data.userId, data.newPassword)
      }
    }
  }

  /**
   * @param userId is optional, so admins (only) are able to change passwords of other users.
   */
  @GetMapping("dynamic")
  fun getForm(
    request: HttpServletRequest,
    @RequestParam("userId") userIdString: String?,
  ): FormLayoutData {
    return internalGetForm(
      request, userIdString = userIdString, i18nPrefix = "user.changePassword",
      loginPasswordI18nKey = "user.changePassword.oldPassword",
    )
  }

  internal fun internalSave(
    request: HttpServletRequest,
    @RequestBody postData: PostData<ChangePasswordData>,
    changePassword: (data: ChangePasswordData, changeOwnPassword: Boolean) -> List<I18nKeyAndParams>?,
  )
      : ResponseEntity<ResponseAction> {
    validateCsrfToken(request, postData)?.let { return it }
    val data = postData.data
    val userId = data.userId ?: ThreadLocalUserContext.getUserId()
    val changeOwnPassword = checkChangeOwn(userId)
    if (!Arrays.equals(data.newPassword, data.passwordRepeat)) {
      val validationErrors = listOf(ValidationError.create("user.error.passwordAndRepeatDoesNotMatch"))
      return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
    }
    val errorMsgKeys = changePassword(data, changeOwnPassword)
    processErrorKeys(errorMsgKeys)?.let {
      return it // Error messages occured:
    }
    data.clear()
    val responseAction = UIToast.createToastResponseAction(
      translate("user.changePassword.msg.passwordSuccessfullyChanged"),
      color = UIColor.SUCCESS,
      variables = mutableMapOf("data" to data),
      merge = true,
    )
    return ResponseEntity(responseAction, HttpStatus.OK)
  }

  /**
   * @param userId is optional, so admins (only) are able to change passwords of other users.
   */
  internal fun internalGetForm(
    request: HttpServletRequest,
    userIdString: String?,
    i18nPrefix: String,
    loginPasswordI18nKey: String,
  ): FormLayoutData {
    val userId = userIdString?.toIntOrNull() ?: ThreadLocalUserContext.getUserId()
    val changeOwnPassword = checkChangeOwn(userId)

    val data = ChangePasswordData(userId)
    UserGroupCache.getInstance().getUser(userId)?.let { user ->
      data.userDisplayName = user.userDisplayName
    }

    val layout = UILayout("$i18nPrefix.title")

    layout.add(UIReadOnlyField(ChangePasswordData::userDisplayName.name, label = "user.username"))
    if (changeOwnPassword) {
      layout.add(
        UIInput(
          ChangePasswordData::loginPassword.name,
          label = loginPasswordI18nKey,
          required = true,
          focus = true,
          dataType = UIDataType.PASSWORD,
          autoComplete = UIInput.AutoCompleteType.CURRENT_PASSWORD
        )
      )
    }
    val passwordQualityI18nKeyAndParams = passwordQualityService.passwordQualityI18nKeyAndParams
    layout.add(
      UIInput(
        ChangePasswordData::newPassword.name,
        label = "$i18nPrefix.newPassword",
        tooltip = I18nHelper.getLocalizedMessage(passwordQualityI18nKeyAndParams),
        dataType = UIDataType.PASSWORD,
        required = true
      )
    )
      .add(
        UIInput(
          ChangePasswordData::passwordRepeat.name,
          label = "passwordRepeat",
          dataType = UIDataType.PASSWORD,
          required = true
        )
      )
      .addAction(
        UIButton.createUpdateButton(
          responseAction = ResponseAction(
            RestResolver.getRestUrl(
              this::
              class.java
            ), targetType = TargetType.POST
          ),
        )
      )
    LayoutUtils.process(layout)
    return FormLayoutData(data, layout, createServerData(request))
  }

  private fun checkChangeOwn(userId: Int): Boolean {
    return if (userId != ThreadLocalUserContext.getUserId()) {
      accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
      false
    } else {
      true
    }
  }
}
