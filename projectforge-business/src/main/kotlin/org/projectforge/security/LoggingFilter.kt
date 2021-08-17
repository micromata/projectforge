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

package org.projectforge.security

import mu.KotlinLogging
import org.projectforge.common.logging.MDC_IP
import org.projectforge.common.logging.MDC_SESSION
import org.projectforge.common.logging.MDC_USER_AGENT
import org.slf4j.MDC
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * LoggingFilter called first for all requests. Puts IP, SESSION and USER_AGENT to MDC and logs access, if debug is enabled.
 */
class LoggingFilter : Filter {
  @Throws(IOException::class, ServletException::class)
  override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
    val request = req as HttpServletRequest

    try {
      MDC.put(MDC_IP, request.remoteAddr)
      MDC.put(MDC_SESSION, request.session.id)
      MDC.put(MDC_USER_AGENT, request.getHeader("User-Agent"))
      if (log.isDebugEnabled) {
        log.debug("doFilter " + request.requestURI + ": " + request.session.id)
      }
      chain.doFilter(req, resp)
    } finally {
      MDC.remove(MDC_IP)
      MDC.remove(MDC_SESSION)
      MDC.remove(MDC_USER_AGENT)
    }
  }
}
