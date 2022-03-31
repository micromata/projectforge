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
import org.projectforge.SystemStatus
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.login.LoginService
import org.projectforge.security.My2FARequestHandler
import org.projectforge.security.SecurityLogging
import org.projectforge.web.WebUtils
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
 * UserFilter is used for Wicket pages. For all rest calls (used e. g. by React client) please refer org.projectforge.web.rest.RestUserFilter.
 * Ensures that an user is logged in and put the user id, locale and ip to the logging mdc.
 */
@Suppress("SpringJavaAutowiredMembersInspection")
class WicketUserFilter : Filter {
  @Autowired
  private lateinit var loginService: LoginService

  @Autowired
  private lateinit var my2FARequestHandler: My2FARequestHandler

  @Autowired
  private lateinit var systemStatus: SystemStatus

  @Throws(ServletException::class)
  override fun init(filterConfig: FilterConfig) {
    WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.servletContext)
      .autowireCapableBeanFactory.autowireBean(this)
  }

  @Throws(IOException::class, ServletException::class)
  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    request as HttpServletRequest
    ThreadLocalUserContext.getUserContext()?.let { userContext ->
      // Paranoia:
      SecurityLogging.logSecurityWarn(
        request,
        this::class.java,
        "UserContext in ThreadLocal is given on request start!!!!!: ${userContext.user}"
      )
      ThreadLocalUserContext.setUserContext(null)
    }
    if (log.isDebugEnabled) {
      log.debug("doFilter ${request.requestURI}: ${request.getSession(false)?.id}")
    }
    try {
      response as HttpServletResponse
      val userContext = loginService.checkLogin(request, response)
      val user = userContext?.user
      if (user != null) {
        ThreadLocalUserContext.setUserContext(userContext)
        //if (!userContext.getSecondFARequiredAfterLogin() && my2FARequestHandler.handleRequest(request, response)) {
        if (my2FARequestHandler.handleRequest(request, response)) {
          // No 2FA is required:
          doFilterDecoratedWithLocale(request, response, chain)
        }
      } else {
        if (systemStatus.setupRequiredFirst == true) {
          val normalizedUri = WebUtils.getNormalizedUri(request) ?: ""
          if (normalizedUri.startsWith("/wa/setup") ||
            normalizedUri.startsWith("/wa/styles/") ||
            normalizedUri.startsWith("/wa/wicket/resource/")
          ) {
            // It's an empty data-base, therefore accept the call of setup-page:
            doFilterDecoratedWithLocale(request, response, chain)
            return
          }
        }
        var url = request.requestURI
        val queryString = request.queryString
        if (StringUtils.isNotBlank(queryString)) {
          url = "$url?${URLEncoder.encode(queryString, "UTF-8")}"
        }
        response.sendRedirect("/react/public/login?url=$url")
      }
    } finally {
      ThreadLocalUserContext.setUserContext(null)
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

  private fun logDebugRequest(request: HttpServletRequest) {
    log.debug("doFilter finished for ${request.requestURI}, session=${request.getSession(false)?.id}")
  }
}
