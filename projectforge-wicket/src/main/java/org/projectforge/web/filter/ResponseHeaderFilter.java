/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Modify the HTTP Response Header based on init-parameters.
 * example usage from web.xml:
 * <pre>
 * &lt;filter&gt;
 *   &lt;filter-name&gt;noCacheFilter&lt;/filter-name&gt;
 *   &lt;filter-class&gt;de.micromata.evote.web.ResponseHeaderFilter&lt;/filter-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;Cache-Control&lt;/param-name&gt;
 *     &lt;param-value&gt;private, no-cache, no-store, must-revalidate, pre-check=0, post-check=0&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;Expires&lt;/param-name&gt;
 *     &lt;param-value&gt;Sat, 6 May 1995 12:00:00 GMT&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;Date&lt;/param-name&gt;
 *     &lt;param-value&gt;Sat, 6 May 1995 12:00:00 GMT&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;Pragma&lt;/param-name&gt;
 *     &lt;param-value&gt;no-cache&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/filter&gt;
 * 
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;noCacheFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;*.jsp&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * Multiple filters could be installed with different header-settings. 
 * @author Wolfgang Jung (w.jung@micromata.de)
 * 
 */
public class ResponseHeaderFilter implements Filter {
  /** the configuration of this filter */
  private FilterConfig fc;

  /**
   * apply the given Initparameters as Header-fields to the response
   * 
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse response = (HttpServletResponse) res;
    // set the provided HTTP response parameters
    for (Enumeration<?> e = fc.getInitParameterNames(); e.hasMoreElements();) {
      String headerName = (String) e.nextElement();
      String value = fc.getInitParameter(headerName);
      response.setHeader(headerName, value);
    }
    // pass the request/response on
    chain.doFilter(req, response);
  }

  /**
   * initialize the filter
   * @param filterConfig the configuration
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(FilterConfig filterConfig) {
    // log.debug("Starting filter " + this);
    this.fc = filterConfig;
  }

  /**
   * destroy the filter
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {
    this.fc = null;
  }
}
