/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.Const
import org.projectforge.business.login.Login
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO.Companion.createCopyWithoutSecretFields
import org.projectforge.security.SecurityLogging.logSecurityWarn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.stereotype.Service
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

@Service
class CookieService {
  @Autowired
  private val userAuthenticationsService: UserAuthenticationsService? = null

  @Autowired
  private val serverProperties: ServerProperties? = null

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
      val values = value.split(":".toRegex()).toTypedArray()
      if (values.size != 3) {
        val msg = "Invalid cookie found: " + StringUtils.abbreviate(value, 10)
        log.warn(msg)
        logSecurityWarn(this.javaClass, "LOGIN FAILED", msg)
        return null
      }
      val username = values[1]
      val stayLoggedInKey = values[2]
      val user = userAuthenticationsService!!.getUserByToken(
        request,
        username,
        UserTokenType.STAY_LOGGED_IN_KEY,
        stayLoggedInKey
      )
      if (user == null) {
        val msg =
          "Invalid cookie found (user not found, stay-logged-in key, maybe renewed and/or user password changed): " + StringUtils.abbreviate(
            value,
            10
          )
        log.warn(msg)
        logSecurityWarn(this.javaClass, "LOGIN FAILED", msg)
        return null
      }
      if (!Login.getInstance().checkStayLoggedIn(user)) {
        val msg = "Stay-logged-in wasn't accepted by the login handler: " + user.userDisplayName
        log.warn(msg)
        logSecurityWarn(this.javaClass, "LOGIN FAILED", msg)
        return null
      }
      // update the cookie, especially the max age
      addStayLoggedInCookie(request, response, stayLoggedInCookie)
      log.info("User successfully logged in using stay-logged-in method: " + user.userDisplayName + " (request=" + request.requestURI + ").")
      return UserContext(createCopyWithoutSecretFields(user)!!)
    }
    return null
  }

  /**
   * Adds or refresh the given cookie.
   */
  fun addStayLoggedInCookie(request: HttpServletRequest, response: HttpServletResponse, stayLoggedInCookie: Cookie) {
    stayLoggedInCookie.maxAge = COOKIE_MAX_AGE
    stayLoggedInCookie.path = "/"
    if (request.isSecure || isSecureCookieConfigured) {
      log.debug("Set secure cookie (request=" + request.requestURI + ").")
      stayLoggedInCookie.secure = true
    } else {
      log.debug("Set unsecure cookie (request=" + request.requestURI + ").")
    }
    stayLoggedInCookie.isHttpOnly = true
    response.addCookie(stayLoggedInCookie) // Refresh cookie.
  }

  /**
   * Reads the secure cookie setting from the spring boot configuration.
   */
  private val isSecureCookieConfigured: Boolean
    get() {
      val secure = serverProperties!!.servlet.session.cookie.secure
      return secure != null && secure
    }

  fun getStayLoggedInCookie(request: HttpServletRequest): Cookie? {
    return getCookie(request, Const.COOKIE_NAME_FOR_STAY_LOGGED_IN)
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

  companion object {
    private const val COOKIE_MAX_AGE = 30 * 24 * 3600 // 30 days.
  }
}
