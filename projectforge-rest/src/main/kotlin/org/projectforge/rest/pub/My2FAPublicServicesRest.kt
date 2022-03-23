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

import mu.KotlinLogging
import org.projectforge.business.user.filter.CookieService
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.login.LoginService
import org.projectforge.rest.My2FAServicesRest
import org.projectforge.rest.My2FAType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.dto.PostData
import org.projectforge.security.My2FAData
import org.projectforge.security.RegisterUser4Thread
import org.projectforge.security.SecurityLogging
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

private val log = KotlinLogging.logger {}

/**
 * This rest service should be available without login (public). It's only available direct after login if the
 * user has to enter any OTP.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/2FA")
open class My2FAPublicServicesRest {
  @Autowired
  private lateinit var cookieService: CookieService

  @Autowired
  private lateinit var my2FAServicesRest: My2FAServicesRest

  /**
   * For validating the Authenticator's OTP, or OTP sent by sms or e-mail. The user must be pre-logged-in before by username/password.
   * This is called by the login page after password check, if a 2FA is required after login.
   */
  @PostMapping("checkOTP")
  fun checkOTP(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @Valid @RequestBody postData: PostData<My2FAData>,
    @RequestParam("redirect", required = false) redirect: String?
  ): ResponseEntity<*> {
    val result = securityCheck(request)
    result.badRequestResponseEntity?.let { return it }
    try {
      RegisterUser4Thread.registerUser(result.userContext!!)
      return my2FAServicesRest.checkOTP(request, response, postData, redirect, afterLogin = true)
    } finally {
      RegisterUser4Thread.unregister()
    }
  }

  /**
   * Sends a OTP as code (text to mobile phone of logged-in user).
   */
  @GetMapping("sendSmsCode")
  fun sendSmsCode(request: HttpServletRequest): ResponseEntity<*> {
    val result = securityCheck(request, My2FAType.SMS)
    result.badRequestResponseEntity?.let { return it }
    try {
      RegisterUser4Thread.registerUser(result.userContext!!)
      return my2FAServicesRest.sendSmsCode(request)
    } finally {
      RegisterUser4Thread.unregister()
    }
  }

  /**
   * Sends a OTP as code vial mail.
   */
  @GetMapping("sendMailCode")
  fun sendMailCode(request: HttpServletRequest): ResponseEntity<*> {
    val result = securityCheck(request, My2FAType.MAIL)
    result.badRequestResponseEntity?.let { return it }
    try {
      // Don't use user from security check. For mail, the user must be given in LoginService.getUserContext()!!! (Just to be sure...)
      RegisterUser4Thread.registerUser(LoginService.getUserContext(request)!!)
      return my2FAServicesRest.sendMailCode(request)
    } finally {
      RegisterUser4Thread.unregister()
    }
  }

  /**
   * Cancel the login process (clears the user's session) as well as the stay-logged-in cookie.
   */
  @GetMapping("cancel")
  fun cancel(request: HttpServletRequest, response:HttpServletResponse): ResponseAction {
    request.getSession(false)?.invalidate()
    cookieService.clearAllCookies(request, response)
    return RestUtils.getRedirectToDefaultPageAction()
  }

  /**
   * User must be pre-logged-in by password (UserContext must be given in user's http session), or
   * user must be registered via [registerUserForPublic2FA].
   * @param type OTP per mail is not allowed for non-context-users (especially for password reset).
   */
  private fun securityCheck(request: HttpServletRequest, type: My2FAType? = null): SecurityCheckResult {
    LoginService.getUserContext(request)?.let { userContxt ->
      return SecurityCheckResult(userContxt)
    }
    SecurityLogging.logSecurityWarn(
      request,
      this::class.java,
      "Not logged-in user tried to do call checkOTP (denied)"
    )
    return SecurityCheckResult(badRequestResponseEntity = ResponseEntity<Any>(HttpStatus.BAD_REQUEST))
  }

  internal class SecurityCheckResult(
    val userContext: UserContext? = null,
    val badRequestResponseEntity: ResponseEntity<*>? = null
  )
}
