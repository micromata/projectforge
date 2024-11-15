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

package org.projectforge.caldav.config

import io.milton.servlet.MiltonFilter
import mu.KotlinLogging
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.caldav.service.DavSessionCache
import org.projectforge.rest.utils.RequestLog
import org.projectforge.security.SecurityLogging
import org.projectforge.web.rest.BasicAuthenticationData
import org.projectforge.web.rest.RestAuthenticationInfo
import org.projectforge.web.rest.RestAuthenticationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils
import java.io.IOException
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * Ensuring a white url list for using Milton filter. MiltonFilter at default supports only black list.
 */
class PFMiltonFilter : MiltonFilter() {
  private lateinit var springContext: WebApplicationContext

  @Autowired
  private lateinit var davSessionCache: DavSessionCache

  @Autowired
  private lateinit var restAuthenticationUtils: RestAuthenticationUtils

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Throws(ServletException::class)
  override fun init(filterConfig: FilterConfig) {
    super.init(filterConfig)
    springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.servletContext)
    val beanFactory = springContext.autowireCapableBeanFactory
    beanFactory.autowireBean(this)
  }

  private fun authenticate(authInfo: RestAuthenticationInfo) {
    if (log.isDebugEnabled) {
      log.debug("Trying to authenticate user (${RequestLog.asString(authInfo.request)})...")
    }
    val davSessionData = davSessionCache.getSessionData(authInfo.request)
    val davSessionUser = davSessionData?.user
    if (davSessionUser != null) {
      if (log.isDebugEnabled) {
        log.debug("User found by session id (${RequestLog.asString(authInfo.request)})...")
      }
      authInfo.user = davSessionUser
    } else {
      log.debug { "No user found by session id (${RequestLog.asString(authInfo.request)})..." }
      restAuthenticationUtils.basicAuthentication(
        authInfo,
        UserTokenType.DAV_TOKEN,
        true
      ) { userString, authenticationToken ->
        val authenticatedUser = userAuthenticationsService.getUserByToken(
          authInfo.request,
          userString,
          UserTokenType.DAV_TOKEN,
          authenticationToken
        )
        if (authenticatedUser == null) {
          val msg = "Can't authenticate user '$userString' by given token. User name and/or token invalid (${
            RequestLog.asString(authInfo.request)
          }."
          log.error(msg)
          SecurityLogging.logSecurityWarn(
            authInfo.request,
            this::class.java,
            "${UserTokenType.DAV_TOKEN.name} AUTHENTICATION FAILED",
            msg
          )
        } else {
          log.debug { "Registering authenticated user: ${RequestLog.asString(authInfo.request, authenticatedUser.username)}" }
          davSessionCache.registerSessionData(authInfo.request, authenticatedUser)
          log.info { "Authenticated user registered: ${RequestLog.asString(authInfo.request, authenticatedUser.username)}" }
        }
        authenticatedUser
      }
    }
  }

  @Throws(IOException::class, ServletException::class)
  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    request as HttpServletRequest
    if (log.isDebugEnabled) {
      var username: String? = null
      val authHeader = RestAuthenticationUtils.getHeader(request, "authorization", "Authorization")
      if (authHeader != null) {
        username = BasicAuthenticationData(request, authHeader).username
      }
      log.debug { "PFMiltonFilter.doFilter: ${RequestLog.asString(request, username)}" }
    }
    if (!DAVMethodsInterceptor.handledByMiltonFilter(request)) {
      if (log.isDebugEnabled) {
        log.debug(
          "Request is not for us (neither CalDAV nor CardDAV-call), processing normal filter chain (${
            RequestLog.asString(
              request
            )
          })..."
        )
      }
      // Not for us:
      chain.doFilter(request, response)
    } else {
      if (request.method == "PUT") {
        log.info { "DAV doesn't support PUT method (yet): ${request.requestURI}" }
        response as HttpServletResponse
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "PUT not (yet) supported by ProjectForge.")
        return
      }
      log.info("Request with method=${request.method} for Milton (${RequestLog.asString(request)})...")
      log.debug { "Request-Info: ${RequestLog.asJson(request, true)}" }
      restAuthenticationUtils.doFilter(request,
        response,
        UserTokenType.DAV_TOKEN,
        authenticate = { authInfo -> authenticate(authInfo) },
        doFilter = { -> super.doFilter(request, response, chain) }
      )
    }
  }
}
