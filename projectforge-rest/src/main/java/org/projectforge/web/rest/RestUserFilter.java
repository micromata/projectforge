/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.rest;

import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Does the authentication stuff for restfull requests.
 *
 * @author Daniel Ludwig (d.ludwig@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class RestUserFilter implements Filter {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestUserFilter.class);

  private WebApplicationContext springContext;

  @Autowired
  private RestAuthenticationUtils restAuthenticationUtils;

  public static void executeLogin(HttpServletRequest request, UserContext userContext) {
    // Wicket part: (page.getSession() as MySession).login(userContext, page.getRequest())
    UserFilter.login(request, userContext);
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext());
    final AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
    beanFactory.autowireBean(this);
  }

  /**
   * Authentication via request header.
   * <ol>
   * <li>Authentication userId (authenticationUserId) and authenticationToken (authenticationToken) or</li>
   * <li>Authentication username (authenticationUsername) and password (authenticationPassword) or</li>
   * </ol>
   *
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   * javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
          throws IOException,
          ServletException {
    final RestAuthenticationInfo userInfo = restAuthenticationUtils.authenticate(request, response);
    if (userInfo == null) {
      // Not authenticated.
      return;
    }
    try {
      restAuthenticationUtils.registerUser(request, userInfo);
      chain.doFilter(request, response);
    } finally {
      restAuthenticationUtils.unregister(request, response, userInfo);
    }
  }

  @Override
  public void destroy() {
    // NOOP
  }

  /**
   * Only for test cases.
   * @param restAuthenticationUtils
   */
  void setRestAuthenticationUtils(RestAuthenticationUtils restAuthenticationUtils) {
    this.restAuthenticationUtils = restAuthenticationUtils;
  }
}
