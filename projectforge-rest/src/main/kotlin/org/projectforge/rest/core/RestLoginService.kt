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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.login.LoginHandler
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.login.LoginResult
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.login.LoginService
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.pub.LoginPageRest
import org.projectforge.security.My2FARequestConfiguration
import org.projectforge.security.My2FAService
import org.projectforge.web.rest.AbstractRestUserFilter
import org.projectforge.web.rest.RestAuthenticationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * Handling the login stuff.
 */
@Service
class RestLoginService {
  @Autowired
  private lateinit var loginService: LoginService

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var my2FARequestConfiguration: My2FARequestConfiguration

  @Autowired
  private lateinit var my2FAService: My2FAService

  @Autowired
  private lateinit var cookieService: CookieService

  fun login(
    request: HttpServletRequest,
    response: HttpServletResponse,
    loginData: LoginPageRest.LoginData
  ): LoginResultStatus {
    val loginResult = checkLogin(request, loginData)
    val user = loginResult.user
    if (user == null || loginResult.loginResultStatus != LoginResultStatus.SUCCESS) {
      return loginResult.loginResultStatus
    }
    log.info("User successfully logged in: " + user.userDisplayName)
    if (loginData.stayLoggedIn == true) {
      val loggedInUser = userService.internalGetById(user.id)
      val stayLoggedInKey = userAuthenticationsService.internalGetToken(user.id, UserTokenType.STAY_LOGGED_IN_KEY)
      cookieService.addStayLoggedInCookie(request, response, loggedInUser, stayLoggedInKey)
    }
    // Execute login:
    val userContext = UserContext(PFUserDO.createCopyWithoutSecretFields(user)!!)
    // Copy 2FA status of LoginResult to UserContext:
    userContext.secondFARequiredAfterLogin = loginResult.loginResultStatus.isSecondFARequiredAfterLogin
    AbstractRestUserFilter.executeLogin(request, userContext)
    return LoginResultStatus.SUCCESS
  }

  private fun checkLogin(request: HttpServletRequest, loginData: LoginPageRest.LoginData): LoginResult {
    if (loginData.username == null || loginData.password == null) {
      return LoginResult().setLoginResultStatus(LoginResultStatus.FAILED)
    }
    val loginProtection = LoginProtection.instance()
    val clientIpAddress = getClientIp(request)
    val offset = loginProtection.getFailedLoginTimeOffsetIfExists(loginData.username, clientIpAddress)
    if (offset > 0) {
      val seconds = (offset / 1000).toString()
      log.warn("The account for '${loginData.username}' is locked for $seconds seconds due to failed login attempts. Please try again later.")

      val numberOfFailedAttempts = loginProtection.getNumberOfFailedLoginAttempts(loginData.username, clientIpAddress)
      return LoginResult().setLoginResultStatus(LoginResultStatus.LOGIN_TIME_OFFSET).setMsgParams(
        seconds,
        numberOfFailedAttempts.toString()
      )
    }
    val result = loginService.loginHandler.checkLogin(loginData.username, loginData.password)
    LoginHandler.clearPassword(loginData.password)
    if (result.loginResultStatus == LoginResultStatus.SUCCESS) {
      loginProtection.clearLoginTimeOffset(result.user?.username, result.user?.id, clientIpAddress)
      // Check 2FA
      my2FARequestConfiguration.loginExpiryDays?.let { days ->
        if (!my2FAService.checklastSuccessful2FA(days.toLong(), My2FAService.Unit.DAYS)) {
          log.info { "User is forced for 2FA after login: ${result.user?.username}." }
          result.loginResultStatus.isSecondFARequiredAfterLogin = true
        }
      }
      if (SystemStatus.isDevelopmentMode()) {
        log.warn { "********* Force 2FA after login in test system." }
        result.loginResultStatus.isSecondFARequiredAfterLogin = true
      }
    } else if (result.loginResultStatus == LoginResultStatus.FAILED) {
      loginProtection.incrementFailedLoginTimeOffset(loginData.username, clientIpAddress)
    }
    return result
  }

  private fun getClientIp(request: ServletRequest): String? {
    return RestUtils.getClientIp(request)
  }
}
