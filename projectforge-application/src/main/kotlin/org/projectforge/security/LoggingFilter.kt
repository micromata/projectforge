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

package org.projectforge.security

import mu.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.caldav.config.DAVMethodsInterceptor
import org.projectforge.common.logging.MDC_IP
import org.projectforge.common.logging.MDC_SESSION
import org.projectforge.common.logging.MDC_USER
import org.projectforge.common.logging.MDC_USER_AGENT
import org.projectforge.login.LoginService
import org.projectforge.web.rest.RestAuthenticationUtils
import org.slf4j.MDC
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * LoggingFilter called first for all requests. Puts IP, SESSION, USERx and USER_AGENT to MDC and logs access, if debug is enabled.
 * Logs also requested urls by the clients for detecting suspicious access (used by e. g. fail2ban).
 * @see ConfigurationService.accessLogConfiguration
 */
class LoggingFilter : Filter {
  @Throws(IOException::class, ServletException::class)
  override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
    val request = req as HttpServletRequest

    try {
      val userAgent = request.getHeader("User-Agent")
      val sessionId = request.getSession(false)?.id
      val clientIp = request.remoteAddr
      MDC.put(MDC_IP, clientIp)
      MDC.put(MDC_SESSION, sessionId ?: "")
      MDC.put(MDC_USER_AGENT, userAgent)
      val username = LoginService.getUserContext(request)?.user?.username
      MDC.put(MDC_USER, username ?: "")

      if (log.isDebugEnabled) {
        log.debug("doFilter " + request.requestURI + ": " + request.getSession(false)?.id)
      }
      when (ConfigurationServiceAccessor.get().accessLogConfiguration) {
        "NONE" -> {
          // Do nothing.
        }
        "ALL" -> {
          // Log all
          if (!logSuspiciousURI(request, username)) {
            // If not logged as warning (suspicious uri), then log it as info (expected access):
            SecurityLogging.logAccessInfo(request, this.javaClass)
          }
        }
        else -> {
          logSuspiciousURI(request, username)
        }
      }
      chain.doFilter(req, resp)
    } finally {
      MDC.remove(MDC_IP)
      MDC.remove(MDC_SESSION)
      MDC.remove(MDC_USER_AGENT)
      MDC.remove(MDC_USER)
    }
  }

  companion object {
    private fun logSuspiciousURI(request: HttpServletRequest, username: String?): Boolean {
      val uri = request.requestURI
      if (uri.isNullOrBlank() ||
        uri.startsWith("/rs/") ||
        uri.startsWith("/react/") ||
        uri.startsWith("/wa/") || // Wicket
        uri.startsWith("/rsPublic/") ||
        uri == "/favicon.ico" ||
        uri == "/favicon.png" ||
        uri.startsWith("/static/") || // resources (css, images, js, ...)
        uri.startsWith("/export/") || // ProjectForge.ics
        uri.startsWith("/styles/") || // Used by Wicket pages
        uri.startsWith("/fonts/") || // Used by Wicket pages
        uri.startsWith("/images/") || // Used by Wicket pages
        uri.startsWith("/include/") || // Used by Wicket pages
        uri.startsWith("/scripts/") || // Used by Wicket pages
        uri.startsWith("/secure/") || // Used by Wicket pages (/secure/Logo.png)
        uri == "/wa" || // Wicket start page
        DAVMethodsInterceptor.handledByMiltonFilter(request)
      ) {
        return false
      }
      val user = username ?: RestAuthenticationUtils.getUserInfo(request)
      val title = if (user.isNullOrEmpty()) {
        "ANONYMOUS SUSPICIOUS REQUEST"
      } else {
        "SUSPICIOUS REQUEST BY USER: $user"
      }
      SecurityLogging.logWarn(request, this::class.java, title, logAccess = true, logSecurity = true)
      return true
    }
  }
}
