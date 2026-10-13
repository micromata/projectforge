/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.login.Login
import org.projectforge.business.password.PasswordQualityService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.i18n.I18nKeyAndParams
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.RestCsrfProtection
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Changes the login password of the logged-in user, or - admins only - of another user (projectforge-next page
 * `/next/changePassword`). [ChangeWlanPasswordPageRest] does the same for the WLAN password.
 *
 * Both classes are registered for the second factor ([org.projectforge.security.ProjectForge2FAInitialization],
 * shortcut PASSWORD), so every call needs a recent 2FA.
 */
@RestController
@RequestMapping("${Rest.URL}/changePassword")
class ChangePasswordPageRest : AbstractDynamicPageRest() {

  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var userDao: UserDao

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var passwordQualityService: PasswordQualityService

  /**
   * @param userIdString is optional, so admins (only) are able to change passwords of other users.
   */
  @AccessChecked("Own user only; another user (userId): ADMIN_GROUP")
  @GetMapping("init")
  fun getInit(@RequestParam("userId", required = false) userIdString: String?): ChangePasswordInit {
    return internalGetInit(userIdString, PasswordType.LOGIN)
  }

  @AccessChecked("Own user only; another user (userId): ADMIN_GROUP. CSRF token required")
  @PostMapping("change")
  fun change(request: HttpServletRequest, @RequestBody postData: PostData<ChangePasswordData>): ResponseEntity<*> {
    return internalChange(request, postData.data, PasswordType.LOGIN) { data, changeOwn ->
      if (changeOwn) {
        log.info { "The user wants to change his password." }
        userService.changePassword(data.userId, data.loginPassword, data.newPassword)
      } else {
        log.info { "Admin user wants to change password of user with id ${data.userId}." }
        userService.changePasswordByAdmin(data.userId, data.newPassword)
      }
    }
  }

  internal fun internalGetInit(userIdString: String?, type: PasswordType): ChangePasswordInit {
    val userId = userIdString?.toLongOrNull() ?: ThreadLocalUserContext.loggedInUserId!!
    val changeOwn = checkChangeOwn(userId)
    // Loaded, not taken from the UserGroupCache: right after a change the cache may still hold the old date.
    val user = userDao.find(userId, checkAccess = false)
    return ChangePasswordInit(
      userId = userId,
      userDisplayName = user?.userDisplayName,
      changeOwn = changeOwn,
      supported = isSupported(user, type),
      lastChangeFormatted = user?.let { type.lastChange(it) }?.let { TimeAgo.getDateAndMessage(it) },
      passwordQualityHint = if (type.checksQuality) {
        I18nHelper.getLocalizedMessage(passwordQualityService.passwordQualityI18nKeyAndParams)
      } else {
        null
      },
    )
  }

  /**
   * Checks the csrf token, the access, the support of the change and the input, then changes the password. Errors
   * are answered with 406 and validation errors, on the field they belong to if any.
   *
   * The csrf token is checked here on purpose, although [RestCsrfProtection] does so for next clients already:
   * the filter only checks requests declaring themselves as next client, so a password change must not rely on it.
   * Without a session (a client authenticated by token) there is no token to check, and no password change either.
   */
  internal fun internalChange(
    request: HttpServletRequest,
    data: ChangePasswordData,
    type: PasswordType,
    changePassword: (data: ChangePasswordData, changeOwnPassword: Boolean) -> List<I18nKeyAndParams>?,
  ): ResponseEntity<*> {
    try {
      if (request.getSession(false) == null
        || !sessionCsrfService.checkToken(request, request.getHeader(RestCsrfProtection.CSRF_TOKEN_HEADER))
      ) {
        log.warn { "Password change declined: no session or csrf token check failed." }
        // Same answer as RestCsrfProtection.deny: the next client refreshes its token and repeats the call once.
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
          .body(mapOf("csrfTokenRequired" to true))
      }
      val userId = data.userId ?: ThreadLocalUserContext.loggedInUserId!!
      data.userId = userId
      val changeOwnPassword = checkChangeOwn(userId)
      if (!isSupported(userDao.find(userId, checkAccess = false), type)) {
        log.warn { "Password change (${type.name}) of user with id $userId declined: not supported." }
        return ResponseEntity(
          ResponseAction(validationErrors = listOf(ValidationError.create(MESSAGE_KEY_NOT_SUPPORTED))),
          HttpStatus.NOT_ACCEPTABLE,
        )
      }
      val validationErrors = mutableListOf<ValidationError>()
      if (changeOwnPassword && data.loginPassword.isNullOrEmpty()) {
        validationErrors.add(
          ValidationError.createFieldRequired(FIELD_LOGIN_PASSWORD, translate(type.loginPasswordI18nKey))
        )
      }
      if (data.newPassword.isNullOrEmpty()) {
        validationErrors.add(
          ValidationError.createFieldRequired(FIELD_NEW_PASSWORD, translate(type.newPasswordI18nKey))
        )
      } else if (!Arrays.equals(data.newPassword, data.passwordRepeat)) {
        validationErrors.add(
          ValidationError.create("user.error.passwordAndRepeatDoesNotMatch", fieldId = FIELD_PASSWORD_REPEAT)
        )
      }
      if (validationErrors.isEmpty()) {
        changePassword(data, changeOwnPassword)?.forEach {
          validationErrors.add(ValidationError.create(it, fieldId = fieldIdOf(it.key)))
        }
      }
      if (validationErrors.isNotEmpty()) {
        return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
      }
      return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    } finally {
      data.clear()
    }
  }

  /**
   * The login handler decides (e.g. no WLAN password without an LDAP master, no password of an LDAP user on an LDAP
   * slave). The shared demo account keeps its password: its users must not lock each other out.
   */
  private fun isSupported(user: PFUserDO?, type: PasswordType): Boolean {
    user ?: return false
    if (AccessChecker.isDemoUser(user)) {
      return false
    }
    return type.isSupported(user)
  }

  private fun checkChangeOwn(userId: Long): Boolean {
    return if (userId != ThreadLocalUserContext.loggedInUserId) {
      accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
      false
    } else {
      true
    }
  }

  private fun CharArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()

  /**
   * The password a page is about: the login password or the WLAN (Samba) password.
   */
  internal enum class PasswordType(
    /** The label of the old (login) password field, used for its required message. */
    val loginPasswordI18nKey: String,
    val newPasswordI18nKey: String,
    /** Only the login password is checked by [PasswordQualityService], so only its page shows the rules. */
    val checksQuality: Boolean,
  ) {
    LOGIN("user.changePassword.oldPassword", "user.changePassword.newPassword", true) {
      override fun lastChange(user: PFUserDO) = user.lastPasswordChange
      override fun isSupported(user: PFUserDO) = Login.getInstance().isPasswordChangeSupported(user)
    },
    WLAN("user.changeWlanPassword.loginPassword", "user.changeWlanPassword.newPassword", false) {
      override fun lastChange(user: PFUserDO) = user.lastWlanPasswordChange
      override fun isSupported(user: PFUserDO) = Login.getInstance().isWlanPasswordChangeSupported(user)
    };

    abstract fun lastChange(user: PFUserDO): Date?
    abstract fun isSupported(user: PFUserDO): Boolean
  }

  companion object {
    internal const val MESSAGE_KEY_NOT_SUPPORTED = "user.changePassword.notSupported"
    private const val FIELD_LOGIN_PASSWORD = "loginPassword"
    private const val FIELD_NEW_PASSWORD = "newPassword"
    private const val FIELD_PASSWORD_REPEAT = "passwordRepeat"

    /**
     * The field an error key of [UserService] / [PasswordQualityService] belongs to, null for a general error.
     */
    internal fun fieldIdOf(i18nKey: String): String? {
      return when (i18nKey) {
        "user.changePassword.error.oldPasswordWrong",
        "user.changeWlanPassword.error.loginPasswordWrong" -> FIELD_LOGIN_PASSWORD

        "user.changePassword.error.notMinLength",
        "user.changePassword.error.noCharacter",
        "user.changePassword.error.noNonCharacter",
        "user.changePassword.error.oldPasswdEqualsNew",
        "user.changePassword.error.passwordQualityCheck" -> FIELD_NEW_PASSWORD

        else -> null
      }
    }
  }
}
