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

package org.projectforge.business.user.filter

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.Const
import org.projectforge.common.StringHelper
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.security.My2FARequestHandler
import org.projectforge.web.servlet.SMSReceiverServlet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.support.WebApplicationContextUtils
import java.io.IOException
import java.net.URLEncoder
import java.util.*
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * Ensures that an user is logged in and put the user id, locale and ip to the logging mdc.
 * Ignores login for: /ProjectForge/wa/resources/ * with the suffixes: *.js, *.css, *.gif, *.png.
 *
 *
 */
class UserFilter : Filter {
  @Autowired
  private lateinit var cookieService: CookieService

  @Autowired
  private lateinit var my2FARequestHandler: My2FARequestHandler

  @Throws(ServletException::class)
  override fun init(filterConfig: FilterConfig) {
    WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.servletContext)
      .autowireCapableBeanFactory.autowireBean(this)
    CONTEXT_PATH = filterConfig.servletContext.contextPath
    WICKET_PAGES_PREFIX = "$CONTEXT_PATH/${Const.WICKET_APPLICATION_PATH}"
    IGNORE_PREFIX_SMS_REVEIVE_SERVLET = "$CONTEXT_PATH/${SMSReceiverServlet.URL}"
  }

  override fun destroy() {
    // do nothing
  }

  @Throws(IOException::class, ServletException::class)
  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    request as HttpServletRequest
    if (log.isDebugEnabled) {
      log.debug("doFilter ${request.requestURI}: ${request.session.id}")
    }
    try {
      if (ignoreFilterFor(request)) {
        // Ignore the filter for this request:
        if (log.isDebugEnabled) {
          log.debug("ignoring ${request.requestURI}: ${request.session.id}")
        }
        chain.doFilter(request, response)
        return
      }
      response as HttpServletResponse
      // final boolean sessionTimeout = request.isRequestedSessionIdValid() == false;
      var userContext = request.session.getAttribute(SESSION_KEY_USER) as? UserContext?
      if (userContext != null) {
        // Get the fresh user from the user cache (not in maintenance mode because user group cache is perhaps not initialized correctly
        // if updates of e. g. the user table are necessary.
        userContext.refreshUser()
        if (log.isDebugEnabled) {
          log.debug("User found in session: ${request.requestURI}")
        }
      } else {
        userContext = cookieService.checkStayLoggedIn(request, response)
        if (log.isDebugEnabled) {
          debugLogStayLoggedInCookie(request)
        }
        userContext?.let {
          if (log.isDebugEnabled) {
            log.debug("User's stay logged-in cookie found: ${request.requestURI}")
          }
          login(request, it)
        }
      }
      val user = userContext?.user
      if (user != null) {
        ThreadLocalUserContext.setUserContext(userContext)
        //if (!userContext.getSecondFARequiredAfterLogin() && my2FARequestHandler.handleRequest(request, response)) {
        if (my2FARequestHandler.handleRequest(request, response)) {
          // No 2FA is required:
          doFilterDecoratedWithLocale(request, response, chain)
        }
      } else {
        if (request.requestURI.startsWith(WICKET_PAGES_PREFIX!!)) {
          // Access-checking is done by Wicket, not by this filter:
          if (my2FARequestHandler.handleRequest(request, response)) {
            doFilterDecoratedWithLocale(request, response, chain)
          }
        } else {
          var url = request.requestURI
          val queryString = request.queryString
          if (StringUtils.isNotBlank(queryString)) {
            url = "$url?${URLEncoder.encode(queryString, "UTF-8")}"
          }
          response.sendRedirect("/react/public/login?url=$url")
        }
      }
    } finally {
      ThreadLocalUserContext.clear()
      if (log.isDebugEnabled) {
        logDebugRequest(request)
      }
    }
  }

  private fun doFilterDecoratedWithLocale(
    request: HttpServletRequest,
    response: HttpServletResponse,
    chain: FilterChain
  ) {
    val locale = ThreadLocalUserContext.getLocale(request.locale)
    val decoratedWithLocale = object : HttpServletRequestWrapper(request) {
      override fun getLocale(): Locale {
        return locale
      }

      override fun getLocales(): Enumeration<Locale> {
        return Collections.enumeration(setOf(locale))
      }
    }
    chain.doFilter(decoratedWithLocale, response)
  }

  /**
   * Will be called by doFilter.
   *
   * @param request from do Filter.
   * @return true, if the filter should ignore this request, otherwise false.
   */
  private fun ignoreFilterFor(request: ServletRequest): Boolean {
    request as HttpServletRequest
    val uri = request.requestURI
    // If you have an NPE you have probably forgotten to call setServletContext on applications start-up.
    // Paranoia setting. May-be there could be a vulnerability with request parameters:
    if (!uri.contains("?")) {
      // if (uri.startsWith(IGNORE_PREFIX_WICKET) && StringHelper.endsWith(uri, ".js", ".css", ".gif", ".png") == true) {
      // No access checking for Wicket resources.
      // return true;
      // } else if (StringHelper.startsWith(uri, IGNORE_PREFIX_DOC, IGNORE_PREFIX_SITE_DOC) == true
      // && StringHelper.endsWith(uri, ".html", ".pdf", ".js", ".css", ".gif", ".png") == true) {
      // No access checking for documentation (including site doc).
      // return true;
      // } else
      if (StringHelper.startsWith(uri, IGNORE_PREFIX_SMS_REVEIVE_SERVLET)) {
        // No access checking for sms receiver servlet.
        // The sms receiver servlet has its own authentication (key).
        if (log.isDebugEnabled) {
          log.debug("Ignoring UserFilter for '$uri'. No authentication needed.")
        }
        return true
      }
    }
    return false
  }

  private fun debugLogStayLoggedInCookie(request: HttpServletRequest) {
    request.cookies?.forEach { cookie ->
      log.debug("Cookie found: ${cookie.name}, path=${cookie.path}, value=${cookie.value}, secure=${cookie.version}, maxAge=${cookie.maxAge}, domain=${cookie.domain}")
    }
  }

  private fun logDebugRequest(request: HttpServletRequest) {
    log.debug("doFilter finished for ${request.requestURI}, session=${request.getSession(false)?.id}")
  }

  companion object {
    private const val SESSION_KEY_USER = "UserFilter.user"
    private var IGNORE_PREFIX_SMS_REVEIVE_SERVLET: String? = null
    private var WICKET_PAGES_PREFIX: String? = null
    private var CONTEXT_PATH: String? = null

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

    /**
     * @param request
     */
    @JvmStatic
    fun logout(request: HttpServletRequest) {
      val session = request.session
      session.removeAttribute(SESSION_KEY_USER)
      session.invalidate()
      log.info("User logged out.")
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
