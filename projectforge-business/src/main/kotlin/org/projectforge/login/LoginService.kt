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

package org.projectforge.login

import mu.KotlinLogging
import org.projectforge.business.ldap.LdapMasterLoginHandler
import org.projectforge.business.ldap.LdapSlaveLoginHandler
import org.projectforge.business.login.Login
import org.projectforge.business.login.LoginDefaultHandler
import org.projectforge.business.login.LoginHandler
import org.projectforge.business.user.UserPrefCache
import org.projectforge.business.user.UserXmlPreferencesCache
import org.projectforge.business.user.filter.CookieService
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

@Service
open class LoginService {
  @Autowired
  private lateinit var applicationContext: ApplicationContext

  @Autowired
  private lateinit var userXmlPreferencesCache: UserXmlPreferencesCache

  @Autowired
  private lateinit var userPrefCache: UserPrefCache

  @Autowired
  private lateinit var cookieService: CookieService

  /**
   * If given then this login handler will be used instead of [LoginDefaultHandler]. For ldap please use e. g.
   * org.projectforge.ldap.LdapLoginHandler.
   */
  @Value("\${projectforge.login.handlerClass}")
  private val loginHandlerClass: String? = null

  lateinit var loginHandler: LoginHandler
    private set

  @PostConstruct
  fun init() {
    loginHandler = when (loginHandlerClass) {
      "LdapMasterLoginHandler" -> applicationContext.getBean(LdapMasterLoginHandler::class.java)
      "LdapSlaveLoginHandler" -> applicationContext.getBean(LdapSlaveLoginHandler::class.java)
      else -> applicationContext.getBean(LoginDefaultHandler::class.java)
    }
    Login.getInstance().setLoginHandler(loginHandler)
    loginHandler.initialize()
  }

  /**
   * Checks, if the user is logged-in (session) or has a valid stay-logge-in cookie. If not logged-in and a valid
   * stay-logged-in-cookie is found, the user will be logged-in by this method.
   */
  fun checkLogin(request: HttpServletRequest, response: HttpServletResponse): UserContext? {
    var userContext = request.session.getAttribute(SESSION_KEY_USER) as? UserContext?
    if (userContext != null) {
      // Get the fresh user from the user cache (not in maintenance mode because user group cache is perhaps not initialized correctly
      // if updates of e. g. the user table are necessary.
      userContext.refreshUser()
      if (log.isDebugEnabled) {
        log.debug("User found in session: ${request.requestURI}")
      }
      return userContext
    }
    userContext = checkStayLoggedIn(request, response)
    return userContext
  }

  /**
   * @param request
   * @param response Needed for clearing cookies.
   */
  fun logout(request: HttpServletRequest, response: HttpServletResponse) {
    val session = request.session
    session.removeAttribute(SESSION_KEY_USER)
    session.invalidate()

    cookieService.clearAllCookies(request, response)
    getUser(request)?.let { user ->
      userXmlPreferencesCache.flushToDB(user.id)
      userXmlPreferencesCache.clear(user.id)
      userPrefCache.flushToDB(user.id)
      userPrefCache.clear(user.id)
      log.info("User '${user.username}' logged out.")
    }
  }


  private fun checkStayLoggedIn(request: HttpServletRequest, response: HttpServletResponse): UserContext? {
    val userContext = cookieService.checkStayLoggedIn(request, response) ?: return null
    log.info("User's stay logged-in cookie found: ${request.requestURI}")
    if (log.isDebugEnabled) {
      request.cookies?.forEach { cookie ->
        log.debug("Cookie found: ${cookie.name}, path=${cookie.path}, value=${cookie.value}, secure=${cookie.version}, maxAge=${cookie.maxAge}, domain=${cookie.domain}")
      }
    }
    login(request, userContext)
    return userContext
  }

  companion object {
    private const val SESSION_KEY_USER = "UserFilter.user"

    /**
     * @param request
     * @param userContext
     */
    @JvmStatic
    fun login(request: HttpServletRequest, userContext: UserContext?) {
      // Session Fixation: Change JSESSIONID after login (due to security reasons / XSS attack on login page)
      request.getSession(false)?.let { session ->
        if (!session.isNew) {
          session.invalidate()
        }
      }
      val session = request.getSession(true) // create the session
      // do the login (store the user in the session, or whatever)
      session.setAttribute(SESSION_KEY_USER, userContext)
    }

    @JvmStatic
    fun getUser(request: HttpServletRequest): PFUserDO? {
      return getUserContext(request)?.user
    }

    /**
     * Creates a user session if not exist.
     *
     * @param request
     */
    @JvmStatic
    fun getUserContext(request: HttpServletRequest): UserContext? {
      return getUserContext(request, true)
    }

    @JvmStatic
    fun getUserContext(request: HttpServletRequest, createSession: Boolean): UserContext? {
      val session = request.getSession(createSession) ?: return null
      val userContext = session.getAttribute(SESSION_KEY_USER) as? UserContext?
      if (log.isDebugEnabled) {
        log.debug("User '${userContext?.user?.username}' successfully restored from http session (request=${request.requestURI}).")
      }
      return userContext
    }
  }
}
