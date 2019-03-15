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

package org.projectforge.web.rest;

import org.projectforge.business.login.LoginProtection;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.rest.Authentication;
import org.projectforge.rest.ConnectionSettings;
import org.projectforge.rest.converter.DateTimeFormat;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Does the authentication stuff for restfull requests.
 *
 * @author Daniel Ludwig (d.ludwig@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class RestUserFilter implements Filter {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestUserFilter.class);

  private WebApplicationContext springContext;

  @Autowired
  private UserService userService;

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
    final HttpServletRequest req = (HttpServletRequest) request;
    String userString = getAttribute(req, Authentication.AUTHENTICATION_USER_ID);
    final LoginProtection loginProtection = LoginProtection.instance();
    final String clientIpAddress = request.getRemoteAddr();
    PFUserDO user = null;

    if (userString != null) {
      if (checkLoginProtection((HttpServletResponse) response, userString, loginProtection, clientIpAddress)) {
        // access denied
        return;
      }

      final Integer userId = NumberHelper.parseInteger(userString);
      if (userId != null) {
        final String cachedAuthenticationToken = userService.getCachedAuthenticationToken(userId);
        final String authenticationToken = getAttribute(req, Authentication.AUTHENTICATION_TOKEN);

        if (cachedAuthenticationToken == null) {
          log.error(Authentication.AUTHENTICATION_USER_ID + " '" + userId + "' does not exist. Rest call forbidden.");
        } else if (authenticationToken == null) {
          log.error(Authentication.AUTHENTICATION_TOKEN + " not given for userId '" + userId + "'. Rest call forbidden.");
        } else if (authenticationToken.equals(cachedAuthenticationToken) == false) {
          log.error(
                  Authentication.AUTHENTICATION_TOKEN + " doesn't match for " + Authentication.AUTHENTICATION_USER_ID + " '" + userId + "'. Rest call forbidden.");
        } else {
          user = userService.getUser(userId);
        }
      } else {
        log.error(Authentication.AUTHENTICATION_USER_ID + " is not an integer: '" + userString + "'. Rest call forbidden.");
      }
    } else {
      userString = getAttribute(req, Authentication.AUTHENTICATION_USERNAME);
      if (checkLoginProtection((HttpServletResponse) response, userString, loginProtection, clientIpAddress)) {
        // access denied
        return;
      }
      final String password = getAttribute(req, Authentication.AUTHENTICATION_PASSWORD);
      if (userString != null && password != null) {
        user = userService.authenticateUser(userString, password);
        if (user == null) {
          log.error("Authentication failed for "
                  + Authentication.AUTHENTICATION_USERNAME
                  + "='"
                  + userString
                  + "' with given password. Rest call forbidden.");
        }
      } else {
        // Try to get the user by session id:
        user = UserFilter.getUser(req);
        if (user == null) {
          log.error("Neither "
                  + Authentication.AUTHENTICATION_USER_ID
                  + " nor "
                  + Authentication.AUTHENTICATION_USERNAME
                  + "/"
                  + Authentication.AUTHENTICATION_PASSWORD
                  + " is given for rest call: " + ((HttpServletRequest) request).getRequestURI() + " . Rest call forbidden.");
        }
      }
    }

    if (user == null) {
      loginProtection.incrementFailedLoginTimeOffset(userString, clientIpAddress);
      final HttpServletResponse resp = (HttpServletResponse) response;
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    try {
      loginProtection.clearLoginTimeOffset(userString, clientIpAddress);
      ThreadLocalUserContext.setUser(getUserGroupCache(), user);
      final ConnectionSettings settings = getConnectionSettings(req);
      ConnectionSettings.set(settings);
      final String ip = request.getRemoteAddr();
      if (ip != null) {
        MDC.put("ip", ip);
      } else {
        // Only null in test case:
        MDC.put("ip", "unknown");
      }
      MDC.put("user", user.getUsername());
      log.info("User: " + user.getUsername() + " calls RestURL: " + ((HttpServletRequest) request).getRequestURI()
              + " with ip: "
              + clientIpAddress);
      chain.doFilter(request, response);
    } finally {
      ThreadLocalUserContext.setUser(getUserGroupCache(), null);
      ConnectionSettings.set(null);
      MDC.remove("ip");
      MDC.remove("user");
    }
  }

  private boolean checkLoginProtection(final HttpServletResponse response, final String userString, final LoginProtection loginProtection,
                                       final String clientIpAddress) throws IOException {
    final long offset = loginProtection.getFailedLoginTimeOffsetIfExists(userString, clientIpAddress);
    if (offset > 0) {
      final String seconds = String.valueOf(offset / 1000);
      log.warn("The account for '"
              + userString
              + "' is locked for "
              + seconds
              + " seconds due to failed login attempts (ip=" + clientIpAddress + ").");
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return true;
    }
    return false;
  }

  private ConnectionSettings getConnectionSettings(final HttpServletRequest req) {
    final ConnectionSettings settings = new ConnectionSettings();
    final String dateTimeFormatString = getAttribute(req, ConnectionSettings.DATE_TIME_FORMAT);
    if (dateTimeFormatString != null) {
      final DateTimeFormat dateTimeFormat = DateTimeFormat.valueOf(dateTimeFormatString.toUpperCase());
      if (dateTimeFormat != null) {
        settings.setDateTimeFormat(dateTimeFormat);
      }
    }
    return settings;
  }

  private String getAttribute(final HttpServletRequest req, final String key) {
    String value = req.getHeader(key);
    if (value == null) {
      value = req.getParameter(key);
    }
    return value;
  }

  @Override
  public void destroy() {
    // NOOP
  }

  /**
   * Only for tests
   */
  void setUserService(UserService userService) {
    this.userService = userService;
  }

  private TenantRegistry getTenantRegistry() {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  private UserGroupCache getUserGroupCache() {
    return getTenantRegistry().getUserGroupCache();
  }

}
