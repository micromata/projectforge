/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.security.SecurityLogging.logSecurityWarn
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
    val stayLoggedInCookie = getStayLoggedInCookie(request)
    if (stayLoggedInCookie != null) {
      val value = stayLoggedInCookie.value
      if (StringUtils.isBlank(value)) {
        return null
      }
      val cookieValue = StayLoggedInCookieValue.deserialize(value) ?: return null
      val user = userAuthenticationsService.getUserByToken(
        request,
        cookieValue.username,
        UserTokenType.STAY_LOGGED_IN_KEY,
        cookieValue.stayLoggedInKey
      )
      if (user == null) {
        val msg =
          "Invalid cookie found (user not found, stay-logged-in key, maybe renewed and/or user password changed): " + StringUtils.abbreviate(
            value,
            10
          )
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
      // update the cookie, especially the max age
      addCookie(request, response, stayLoggedInCookie, COOKIE_STAY_LOGGED_IN_MAX_AGE)
      userDao.updateUserAfterLoginSuccess(user)
      log.info("User successfully logged in using stay-logged-in method: " + user.userDisplayName + " (request=" + request.requestURI + ").")
      val userContext = UserContext(user)
      // Restore any last successful 2FA from cookie:
      // *** 2FA userContext.lastSuccessful2FA = getLast2FA(request, user.id)
      return userContext
    }
    return null
  }

  fun addStayLoggedInCookie(
    request: HttpServletRequest,
    response: HttpServletResponse,
    loggedInUser: PFUserDO,
    stayLoggedInKey: String?,
  ) {
    stayLoggedInKey ?: return
    val info = StayLoggedInCookieValue("${loggedInUser.id}", loggedInUser.username ?: "???", stayLoggedInKey)
    val cookie = Cookie(COOKIE_NAME_FOR_STAY_LOGGED_IN, info.serialize())
    addCookie(request, response, cookie, COOKIE_STAY_LOGGED_IN_MAX_AGE)
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
      response.addCookie(cookie)
    }
  }

  companion object {
    private const val COOKIE_STAY_LOGGED_IN_MAX_AGE = 30 * 24 * 3600 // 30 days.
    private const val COOKIE_NAME_FOR_STAY_LOGGED_IN = "stayLoggedIn"
    private const val COOKIE_LAST_2FA_MAX_AGE = 89 * 24 * 3600 // 90 days.
    private const val COOKIE_NAME_FOR_LAST_2FA = "last2FA"
  }
}
