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

package org.projectforge.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

/**
 * This filter adds HTTP security headers to every response.
 * <p>
 * See https://blog.appcanary.com/2017/http-security-headers.html for details about the headers.
 */
public class SecurityHeaderFilter implements Filter
{
  public static final String PARAM_CSP_HEADER_VALUE = "csp";

  private String cspHeaderValue;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException
  {
    cspHeaderValue = filterConfig.getInitParameter(PARAM_CSP_HEADER_VALUE);
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException
  {
    if (response instanceof HttpServletResponse) {
      final HttpServletResponse res = (HttpServletResponse) response;

      res.addHeader("X-XSS-Protection", "1; mode=block");
      res.addHeader("X-DNS-Prefetch-Control", "off");
      res.addHeader("X-Frame-Options", "SAMEORIGIN");
      res.addHeader("X-Content-Type-Options", "nosniff");

      // add Content Security Policy header if available, see http://cspisawesome.com/
      if (StringUtils.isNotBlank(cspHeaderValue)) {
        res.addHeader("Content-Security-Policy", cspHeaderValue);
        res.addHeader("X-Content-Security-Policy", cspHeaderValue);
        res.addHeader("X-WebKit-CSP", cspHeaderValue);
      }
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy()
  {
    // nothing to do
  }
}
