/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.filter;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.*;
import java.io.IOException;

/**
 * @author wolle
 */
public class SpringThreadLocalFilter implements Filter
{
  private WebApplicationContext webApplicationContext;

  /**
   * @see jakarta.servlet.Filter#init(jakarta.servlet.FilterConfig)
   */
  public void init(FilterConfig cfg) throws ServletException
  {
    webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(cfg.getServletContext());
  }

  /**
   * @see jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
   */
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException
  {
    try {
      SpringContext.setWebApplicationContext(webApplicationContext);
      chain.doFilter(req, resp);
    } finally {
      SpringContext.setWebApplicationContext(null);
    }
  }

  /**
   * @see jakarta.servlet.Filter#destroy()
   */
  public void destroy()
  {
    // do nothing
  }

}
