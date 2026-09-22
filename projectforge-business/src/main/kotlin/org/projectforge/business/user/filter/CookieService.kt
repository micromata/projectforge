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

package org.projectforge.business.user.filter

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.login.Login
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.user.StayLoggedInTokenDao
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.security.SecurityLogging.logSecurityWarn
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.stereotype.Service
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

@Service
class CookieService {
  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var stayLoggedInTokenDao: StayLoggedInTokenDao

  @Autowired
  private lateinit var serverProperties: ServerProperties

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var userDao: UserDao

  /**
   * User is not logged. Checks a stay-logged-in-cookie.
   *
   * @return user if valid cookie found, otherwise null.
   */
  fun checkStayLoggedIn(request: HttpServletRequest, response: HttpServletResponse): UserContext? {
    val stayLoggedInCookie = getStayLoggedInCookie(request) ?: return null
    val token = stayLoggedInCookie.value
    if (StringUtils.isBlank(token)) {
      return null
    }
    // Brute force protection. The key is the client's ip, not a username: the cookie carries nothing but the
    // token, so there is no user to name before the token has been resolved. Own authentication type, so
    // cookie attempts can't lock a user's password login out (and vice versa).
    //
    // The ip is passed as the *user* argument (and clientIpAddress stays null): LoginProtection's ip map only
    // starts penalizing after 1000 attempts (DEFAULT_NUMBER_OF_FAILED_LOGINS_BEFORE_INCREMENTING_FOR_IP,
    // deliberately high because a whole office shares one NAT address), which would be no brake at all. In the
    // user map the threshold is 1, and the authentication type namespaces the key to "stayLoggedIn:<ip>", so
    // nothing else keyed by that ip is affected.
    val loginProtection = LoginProtection.instance()
    val clientIp = WebUtils.getClientIp(request)
    val offset = loginProtection.getFailedLoginTimeOffsetIfExists(clientIp, null, AUTHENTICATION_TYPE)
    if (offset > 0) {
      // Blocked, not delayed: the request is refused right away (no sleep), the offset only says how long
      // this ip stays refused. Milliseconds, because the first attempts are penalized sub-second.
      val msg =
        "Stay-logged-in is blocked for $offset ms for ip $clientIp due to failed attempts (request=${request.requestURI})."
      log.warn(msg)
      logSecurityWarn(this.javaClass, "LOGIN FAILED", msg)
      return null
    }
    val entry = stayLoggedInTokenDao.getValidToken(token)
    val user = entry?.user?.id?.let { userService.find(it, false) }
    if (user == null) {
      loginProtection.incrementFailedLoginTimeOffset(clientIp, null, AUTHENTICATION_TYPE)
      val msg =
        "Invalid stay-logged-in cookie found (unknown, expired or revoked token, e. g. after a logout or a password change), ip=$clientIp."
      log.warn(msg)
      logSecurityWarn(this.javaClass, "LOGIN FAILED", msg)
      clearCookie(response, stayLoggedInCookie)
      return null
    }
    // Used to be part of UserAuthenticationsDao.getUserByToken: a deactivated or deleted user must not be able
    // to restore a session from a cookie issued while the account was still active.
    if (!user.hasSystemAccess()) {
      val msg = "Deactivated or deleted user tried to log in via stay-logged-in cookie: ${user.userDisplayName}."
      log.warn(msg)
      logSecurityWarn(this.javaClass, "LOGIN FAILED", msg)
      clearCookie(response, stayLoggedInCookie)
      return null
    }
    if (!Login.getInstance().checkStayLoggedIn(user)) {
      val msg = "Stay-logged-in wasn't accepted by the login handler: " + user.userDisplayName
      log.warn(msg)
      logSecurityWarn(this.javaClass, "LOGIN FAILED", msg)
      return null
    }
    loginProtection.clearLoginTimeOffset(clientIp, null, null, AUTHENTICATION_TYPE)
    stayLoggedInTokenDao.updateLastAccessIfDue(entry, request)
    userAuthenticationsService.registerLogAccess(request, UserTokenType.STAY_LOGGED_IN_KEY, user.id)
    // update the cookie, especially the max age
    addCookie(request, response, stayLoggedInCookie, COOKIE_STAY_LOGGED_IN_MAX_AGE)
    userDao.updateUserAfterLoginSuccess(user)
    log.info("User successfully logged in using stay-logged-in method: " + user.userDisplayName + " (request=" + request.requestURI + ").")
    // The last successful 2FA is restored by the caller (LoginService.checkStayLoggedIn), which has the user id
    // at hand and logs it.
    return UserContext(user)
  }

  /**
   * Issues a token for this device and puts it into the cookie. The token isn't stored anywhere else in clear
   * text, see [org.projectforge.business.user.StayLoggedInTokenDO].
   */
  fun addStayLoggedInCookie(
    request: HttpServletRequest,
    response: HttpServletResponse,
    loggedInUser: PFUserDO,
  ) {
    val token = stayLoggedInTokenDao.createToken(loggedInUser, request)
    val cookie = Cookie(COOKIE_NAME_FOR_STAY_LOGGED_IN, token)
    addCookie(request, response, cookie, COOKIE_STAY_LOGGED_IN_MAX_AGE)
  }

  /**
   * Invalidates the token of the calling device (logout), leaving the user's other devices logged in. Does
   * **not** clear the cookie, that is [clearAllCookies]' job.
   */
  fun invalidateStayLoggedInToken(request: HttpServletRequest) {
    val cookie = getStayLoggedInCookie(request) ?: return
    if (stayLoggedInTokenDao.deleteByToken(cookie.value) > 0) {
      log.info { "Stay-logged-in token of this device invalidated (logout)." }
    }
  }

  /**
   * Reads the secure cookie setting from the spring boot configuration.
   */
  private val isSecureCookieConfigured: Boolean
    get() {
      val secure = serverProperties.servlet.session.cookie.secure
      return (secure != null) && secure
    }

  fun clearAllCookies(request: HttpServletRequest, response: HttpServletResponse) {
    clearCookie(response, getStayLoggedInCookie(request))
    clearCookie(response, getLast2FACookie(request))
  }

  private fun getStayLoggedInCookie(request: HttpServletRequest): Cookie? {
    return getCookie(request, COOKIE_NAME_FOR_STAY_LOGGED_IN)
  }

  private fun getLast2FACookie(request: HttpServletRequest): Cookie? {
    return getCookie(request, COOKIE_NAME_FOR_LAST_2FA)
  }

  fun getLast2FA(request: HttpServletRequest, userId: Long): Long? {
    val cookie = getLast2FACookie(request) ?: return null
    try {
      val lastSuccessful2FA = userService.decrypt(cookie.value, userId) ?: return null
      return lastSuccessful2FA.toLongOrNull()
    } catch (ex: Exception) {
      log.info { "Can't decrypt cookie value for last 2FA. Password changed?" }
    }
    return null
  }

  /**
   * Adds or refresh the given cookie.
   */
  fun addLast2FACookie(request: HttpServletRequest, response: HttpServletResponse, lastSuccessful2FA: Long) {
    val value = userService.encrypt(lastSuccessful2FA.toString())
    val cookie = Cookie(COOKIE_NAME_FOR_LAST_2FA, value)
    addCookie(request, response, cookie, COOKIE_LAST_2FA_MAX_AGE)
  }

  private fun addCookie(request: HttpServletRequest, response: HttpServletResponse, cookie: Cookie, maxAge: Int) {
    cookie.maxAge = maxAge
    cookie.path = "/"
    // Same value as the session cookie (server.servlet.session.cookie.same-site): don't send these credentials on
    // requests a foreign page initiated. Lax (not Strict) keeps a top-level navigation from an external link (mail,
    // bookmark) working, which is the normal way of entering ProjectForge. That a Lax cookie is still sent on such a
    // navigation is why the public endpoints check Sec-Fetch-Site themselves before doing a restore, see
    // [org.projectforge.rest.core.RestCsrfProtection.isSameSiteRequest].
    cookie.setAttribute("SameSite", "Lax")
    if (request.isSecure || isSecureCookieConfigured) {
      if (log.isDebugEnabled) {
        log.debug("Set secure cookie (request=${request.requestURI}).")
      }
      cookie.secure = true
    } else {
      if (log.isDebugEnabled) {
        log.debug("Set unsecure cookie (request=${request.requestURI}).")
      }
    }
    cookie.isHttpOnly = true
    response.addCookie(cookie) // Refresh cookie.
  }

  private fun getCookie(request: HttpServletRequest, name: String): Cookie? {
    val cookies = request.cookies
    if (cookies != null) {
      for (cookie in cookies) {
        if (name == cookie.name) {
          return cookie
        }
      }
    }
    return null
  }

  private fun clearCookie(response: HttpServletResponse, cookie: Cookie?) {
    if (cookie != null) {
      cookie.maxAge = 0
      cookie.value = null
      cookie.path = "/"
      // Has to match the attributes the cookie was set with, otherwise the browser may keep the original one.
      cookie.setAttribute("SameSite", "Lax")
      response.addCookie(cookie)
    }
  }

  companion object {
    /**
     * Own namespace of [LoginProtection], so failed cookie attempts don't lock a user's password login (and
     * vice versa).
     */
    private const val AUTHENTICATION_TYPE = "stayLoggedIn"

    /**
     * Has to be [org.projectforge.business.user.StayLoggedInTokenDao.EXPIRY_MILLIS]: both are refreshed on
     * every successful check.
     */
    private const val COOKIE_STAY_LOGGED_IN_MAX_AGE = 30 * 24 * 3600 // 30 days.
    private const val COOKIE_NAME_FOR_STAY_LOGGED_IN = "stayLoggedIn"
    private const val COOKIE_LAST_2FA_MAX_AGE = 89 * 24 * 3600 // 90 days.
    private const val COOKIE_NAME_FOR_LAST_2FA = "last2FA"
  }
}
