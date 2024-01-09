/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.login.*
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserPrefCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.UserXmlPreferencesCache
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.security.My2FARequestConfiguration
import org.projectforge.security.My2FAService
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

@Service
open class LoginService {
  private val logoutListeners = mutableListOf<LogoutListener>()

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  @Autowired
  private lateinit var cookieService: CookieService

  @Autowired
  private lateinit var my2FAService: My2FAService

  @Autowired
  private lateinit var my2FARequestConfiguration: My2FARequestConfiguration

  @Autowired
  private lateinit var userPrefCache: UserPrefCache

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var userXmlPreferencesCache: UserXmlPreferencesCache

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
   * Checks, if the user is logged-in (session) or has a valid stay-logged-in cookie. If not logged-in and a valid
   * stay-logged-in-cookie is found, the user will be logged-in by this method.
   * Handles also 2FA after login.
   */
  fun checkLogin(request: HttpServletRequest, response: HttpServletResponse): UserContext? {
    getUserContext(request)?.let { userContext ->
      // Get the fresh user from the user cache.
      userContext.refreshUser()
      // Check 2FA if session is kept alive for a longer time:
      handle2FARequiredAfterLogin(request, userContext)
      if (!ensureSystemAccess(request, response, userContext)) {
        // User has no access or the user has to do a 2FA check first.
        return null
      }
      if (log.isDebugEnabled) {
        log.debug("User found in session: ${request.requestURI}")
      }
      return userContext
    }
    val userContext = checkStayLoggedIn(request, response)
    handle2FARequiredAfterLogin(request, userContext)
    if (!ensureSystemAccess(request, response, userContext)) {
      // User has no access or the user has to do a 2FA check first.
      return null
    }
    return userContext
  }

  private fun ensureSystemAccess(
    request: HttpServletRequest,
    response: HttpServletResponse,
    userContext: UserContext?
  ): Boolean {
    userContext ?: return false
    if (userContext.user?.hasSystemAccess() != true) {
      log.warn { "Logged-in user has no system access (deactivated by admin?). The user will be logged out immediately.: ${userContext.user}" }
      logout(request, response)
      return false
    }
    return !userContext.new2FARequired
  }

  /**
   * Tries to authenticate the user with the given credentials. Stay-logged-in flag will also be handled.
   * Brute force attacks will be prevented by using [LoginProtection].
   * Handles also 2FA after login.
   */
  fun authenticate(
    request: HttpServletRequest,
    response: HttpServletResponse,
    loginData: LoginData
  ): LoginResultStatus {
    val loginResult = authenticate(request, loginData)
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
    val userContext = UserContext(user)
    handle2FARequiredAfterLogin(request, userContext)
    internalLogin(request, userContext)
    return LoginResultStatus.SUCCESS
  }

  private fun authenticate(request: HttpServletRequest, loginData: LoginData): LoginResult {
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
    val result = loginHandler.checkLogin(loginData.username, loginData.password)
    LoginHandler.clearPassword(loginData.password)
    if (result.loginResultStatus == LoginResultStatus.SUCCESS) {
      loginProtection.clearLoginTimeOffset(result.user?.username, result.user?.id, clientIpAddress)
    } else if (result.loginResultStatus == LoginResultStatus.FAILED) {
      loginProtection.incrementFailedLoginTimeOffset(loginData.username, clientIpAddress)
    }
    return result
  }

  private fun getClientIp(request: ServletRequest): String? {
    return WebUtils.getClientIp(request)
  }

  /**
   * @param request
   * @param response Needed for clearing cookies.
   */
  fun logout(request: HttpServletRequest, response: HttpServletResponse) {
    val user = getUser(request)
    logoutListeners.forEach {
      it.logout(request, response)
    }
    request.getSession(false)?.let { session ->
      session.removeAttribute(SESSION_KEY_USER)
      session.invalidate()
    }
    cookieService.clearAllCookies(request, response)
    user?.let { user ->
      userXmlPreferencesCache.flushToDB(user.id)
      userXmlPreferencesCache.clear(user.id)
      userPrefCache.flushToDB(user.id)
      userPrefCache.clear(user.id)
      log.info("User '${user.username}' logged out.")
    }
  }

  fun register(listener: LogoutListener) {
    if (logoutListeners.contains(listener)) {
      log.warn { "Don't register listener twice: ${listener::class.java}." }
    } else {
      logoutListeners.add(listener)
    }
  }

  private fun checkStayLoggedIn(request: HttpServletRequest, response: HttpServletResponse): UserContext? {
    val userContext = cookieService.checkStayLoggedIn(request, response) ?: return null
    val userId = userContext.user?.id
    val lastSuccessfulFA = if (userId != null) cookieService.getLast2FA(request, userId) else null
    userContext.lastSuccessful2FA = lastSuccessfulFA
    val timeOfLastSuccessful2FA = PFDateTime.fromOrNull(lastSuccessfulFA)
    val last2FAText =
      if (timeOfLastSuccessful2FA != null) ", last successful 2FA: ${timeOfLastSuccessful2FA.isoString} UTC (${
        TimeAgo.getMessage(timeOfLastSuccessful2FA)
      })" else ""
    log.info("User's stay logged-in cookie found: ${request.requestURI}$last2FAText")
    if (log.isDebugEnabled) {
      request.cookies?.forEach { cookie ->
        log.debug("Cookie found: ${cookie.name}, path=${cookie.path}, value=${cookie.value}, secure=${cookie.version}, maxAge=${cookie.maxAge}, domain=${cookie.domain}")
      }
    }
    internalLogin(request, userContext)
    return userContext
  }

  /**
   * @return true, if 2FA is required after login, otherwise false.
   */
  private fun check2FARequiredAfterLogin(userContext: UserContext): Boolean {
    my2FARequestConfiguration.loginExpiryDays.let { days ->
      if (days != null) {
        if (!my2FAService.checklastSuccessful2FA(days.toLong(), My2FAService.Unit.DAYS, userContext)) {
          log.info { "User is forced for 2FA after login: ${userContext.user?.username}." }
          return true
        }
      } else {
        // Check current logged-in user if Authenticator-App is configured and no 2FA is done for now.
        if (userContext.lastSuccessful2FA == null && userContext.authenticatorAppConfigured == true) {
          log.info { "User is forced for 2FA after login: ${userContext.user?.username}, because his/her Authenticator App is configured." }
          return true
        }
      }
    }
    return false
  }

  /**
   * Sets the userContext flag new2FARequired to true, if a 2FA is required after login or if session is a long running
   * one and 2FA is required.
   */
  private fun handle2FARequiredAfterLogin(request: HttpServletRequest, userContext: UserContext?) {
    userContext ?: return
    if (userContext.authenticatorAppConfigured == null) {
      // State of Authenticator App isn't yet given:
      val userId =
        userContext.user?.id ?: throw IllegalArgumentException("User id in UserContext is null (not expected).")
      userContext.authenticatorAppConfigured = userAuthenticationsService.isAuthenticatorAppConfigured(userId)
    }
    if (userContext.new2FARequired) {
      // Already set.
      return
    }
    request.getSession(false)?.getAttribute(SESSION_KEY_LAST_2FA_AFTER_LOGIN_CHECK)?.let { value ->
      if (System.currentTimeMillis() - (value as Long) < CHECK_2FA_AFTER_LOGIN_INTERVAL_MS) {
        // Don't need to re-check.
        return
      }
    }
    if (check2FARequiredAfterLogin(userContext)) {
      userContext.new2FARequired = true
    }
    request.getSession(false)?.setAttribute(SESSION_KEY_LAST_2FA_AFTER_LOGIN_CHECK, System.currentTimeMillis())
  }

  companion object {
    private const val SESSION_KEY_USER = "LoginService.user"
    private const val SESSION_KEY_LAST_2FA_AFTER_LOGIN_CHECK = "LoginService.last2FALoginCheck"

    // Check hourly
    private const val CHECK_2FA_AFTER_LOGIN_INTERVAL_MS = 3_600_000

    /**
     * Used for storing given userContext in user's session. A new session id is generated (avoids attack with session fixation).
     * Should only be used internally by [LoginService] itself and setup page.
     * @param request
     * @param userContext
     */
    @JvmStatic
    fun internalLogin(request: HttpServletRequest, userContext: UserContext?) {
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
     * @param request
     * @param createSession Default is false (no new session is created if none existing).
     */
    @JvmStatic
    @JvmOverloads
    fun getUserContext(request: HttpServletRequest, createSession: Boolean = false): UserContext? {
      val session = request.getSession(createSession) ?: return null
      val userContext = session.getAttribute(SESSION_KEY_USER) as? UserContext?
      if (log.isDebugEnabled) {
        log.debug("User '${userContext?.user?.username}' successfully restored from http session (request=${request.requestURI}).")
      }
      return userContext
    }
  }
}
