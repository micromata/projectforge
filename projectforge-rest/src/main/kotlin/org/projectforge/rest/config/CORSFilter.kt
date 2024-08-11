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

package org.projectforge.rest.config

import kotlin.Throws
import java.io.IOException
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * CrossOrigin filter in development mode (for npm/yarn web development).
 * See: http://stackoverflow.com/a/28067653
 */
class CORSFilter : Filter {
  /**
   * NOP.
   * @see Filter.destroy
   */
  override fun destroy() {}

  /**
   * @see Filter.doFilter
   */
  @Throws(IOException::class, ServletException::class)
  override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain) {
    val request = servletRequest as HttpServletRequest
    // Authorize (allow) all domains(the domain the request came from) to consume the content
    (servletResponse as HttpServletResponse).addHeader("Access-Control-Allow-Origin", request.getHeader("Origin"))
    servletResponse.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS, HEAD, PUT, POST, DELETE")
    servletResponse.addHeader("Access-Control-Allow-Credentials", "true")
    servletResponse.addHeader("Access-Control-Allow-Headers", "Content-Type")
    // "Content-Disposition" for filenames of downloads.
    servletResponse.addHeader("Access-Control-Expose-Headers", "Content-Disposition")
    // For HTTP OPTIONS verb/method reply with ACCEPTED status code -- per CORS handshake
    if (request.method == "OPTIONS") {
      servletResponse.status = HttpServletResponse.SC_ACCEPTED
      return
    }
    // pass the request along the filter chain
    chain.doFilter(request, servletResponse)
  }

  /**
   * NOP.
   * @see Filter.init
   */
  @Throws(ServletException::class)
  override fun init(fConfig: FilterConfig) {
  }
}
