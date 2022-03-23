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

package org.projectforge.framework.persistence.user.service

import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.user.UserLocale
import org.projectforge.mail.SendMail
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.mail.Mail
import org.projectforge.security.SecurityLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

/**
 * After requesting a password reset by the user (username or e-mail must match a single existing user and user must
 * have system access), an e-mail is sent to the user with a link including a token, which is valid for a short period
 * (see [PasswordResetTokenStore]).
 * On the web page for password reset, the existence of the token will be checked.
 */
@Service
class PasswordResetService {
  @Autowired
  private lateinit var sendMail: SendMail

  @Autowired
  private lateinit var userService: UserService

  /**
   * @param Checks the existence of the given token.
   * @return The user assigned to the token or null, if no such user is found.
   */
  fun checkToken(token: String): PFUserDO? {
    val userId = PasswordResetTokenStore.checkToken(token) ?: return null
    userService.internalGetById(userId)?.let { user ->
      return PFUserDO.createCopyWithoutSecretFields(user)
    }
    return null
  }

  /**
   * @param Deletes the given token, if exists.
   * @see PasswordResetTokenStore.deleteToken
   */
  fun deleteToken(token: String) {
    PasswordResetTokenStore.deleteToken(token)
  }

  /**
   * @param Link in e-mail for password reset. "<token>" will be reset by the generated token.
   *        E. g. projectforge.acme.com/react/public/passwordReset/dynamic/?token=IBMwcF3b1f80OvH6bcbOcqWCaUtFr4
   */
  fun sendMail(usernameEmail: String, link: String) {
    /**
     * Start the following procedure as thread to prevent, that an attacker
     * will get any information of the user existence regarding the duration of the request
     */
    thread(start = true) {
      if (usernameEmail.contains("@")) {
        userService.findUserByMail(usernameEmail)?.let { users ->
          val size = users.size
          if (size > 1) {
            log.warn { "Can't reset user password by e-mail, because the mail address '$usernameEmail' is used by ${size} users." }
          } else if (size == 1) {
            sendPasswordReset(usernameEmail, users[0], link)
            return@thread
          }
        }
      }
      // User may contain '@'-char, so do not user else branch:
      val user = userService.getInternalByUsername(usernameEmail)
      if (user == null) {
        SecurityLogging.logSecurityWarn(
          PasswordResetService::class.java,
          "User with e-mail '$usernameEmail' not found for password reset."
        )
      } else {
        sendPasswordReset(usernameEmail, user, link)
      }
    }
  }

  private fun sendPasswordReset(usernameEmail: String, user: PFUserDO, link: String) {
    // User found by mail
    log.info { "Password reset requested by '$usernameEmail' for user ${user.username}." }
    if (!user.hasSystemAccess()) {
      log.warn { "A user without system access required a password reset by '$usernameEmail': ${user}." }
      return
    }
    val mail = Mail()
    val locale = UserLocale.determineUserLocale(user)
    mail.subject = translate(locale,"password.forgotten.mail.subject")
    mail.setTo(user)
    mail.contentType = Mail.CONTENTTYPE_HTML
    val token = PasswordResetTokenStore.createToken(user.id)
    val resolvedLink = link.replace("TOKEN", token)
    val data = mutableMapOf<String, Any?>("link" to resolvedLink)
    mail.content = sendMail.renderGroovyTemplate(
      mail, "mail/passwordResetMail.html", data,
      mail.subject, user
    )
    if (SystemStatus.isDevelopmentMode()) {
      log.info { "Development mode: Mail with password reset will be sent to '${mail.to}'. Link is '$resolvedLink'." }
    }
    sendMail.send(mail)
  }
}
