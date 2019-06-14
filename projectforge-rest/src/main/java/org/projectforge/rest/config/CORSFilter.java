/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.config;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CrossOrigin filter in development mode (for npm/yarn web development).
 * See: http://stackoverflow.com/a/28067653
 */
public class CORSFilter implements Filter {

  public CORSFilter() {
  }

  /**
   * NOP.
   * @see Filter#destroy()
   */
  public void destroy() {
  }

  /**
   * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
   */
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
          throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) servletRequest;
    // Authorize (allow) all domains(the domain the request came from) to consume the content
    ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
    ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Methods", "GET, OPTIONS, HEAD, PUT, POST, DELETE");
    ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Credentials", "true");
    ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Headers", "Content-Type");
    HttpServletResponse resp = (HttpServletResponse) servletResponse;
    // For HTTP OPTIONS verb/method reply with ACCEPTED status code -- per CORS handshake
    if (request.getMethod().equals("OPTIONS")) {
      resp.setStatus(HttpServletResponse.SC_ACCEPTED);
      return;
    }
    // pass the request along the filter chain
    chain.doFilter(request, servletResponse);
  }

  /**
   * NOP.
   * @see Filter#init(FilterConfig)
   */
  public void init(FilterConfig fConfig) throws ServletException {
  }
}
