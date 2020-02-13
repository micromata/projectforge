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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.login.LoginProtection;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.filter.CookieService;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.rest.Authentication;
import org.projectforge.rest.AuthenticationOld;
import org.projectforge.rest.ConnectionSettings;
import org.projectforge.rest.converter.DateTimeFormat;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Does the authentication stuff for restfull requests.
 *
 * @author Daniel Ludwig (d.ludwig@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class RestAuthenticationUtils {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestAuthenticationUtils.class);

  @Autowired
  private UserService userService;

  @Autowired
  private CookieService cookieService;

  public static void executeLogin(HttpServletRequest request, UserContext userContext) {
    // Wicket part: (page.getSession() as MySession).login(userContext, page.getRequest())
    UserFilter.login(request, userContext);
  }

  /**
   * Authentication via request header.
   * <ol>
   * <li>Authentication userId (authenticationUserId) and authenticationToken (authenticationToken) or</li>
   * <li>Authentication username (authenticationUsername) and password (authenticationPassword) or</li>
   * </ol>
   */
  public RestAuthenticationInfo authenticate(final ServletRequest request, final ServletResponse response)
          throws IOException {
    if (UserFilter.isUpdateRequiredFirst()) {
      log.warn("Update of the system is required first. Login via Rest not available. Administrators login required.");
      return null;
    }
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;
    String userString = getAttribute(req, Authentication.AUTHENTICATION_USER_ID, AuthenticationOld.AUTHENTICATION_USER_ID);
    final LoginProtection loginProtection = LoginProtection.instance();
    final String clientIpAddress = request.getRemoteAddr();
    PFUserDO user = null;

    if (userString != null) {
      if (checkLoginProtection((HttpServletResponse) response, userString, loginProtection, clientIpAddress)) {
        // access denied
        return null;
      }

      final Integer userId = NumberHelper.parseInteger(userString);
      if (userId != null) {
        final String authenticationToken = getAttribute(req, Authentication.AUTHENTICATION_TOKEN, AuthenticationOld.AUTHENTICATION_TOKEN);
        if (userService.checkAuthenticationToken(userId,
                authenticationToken,
                Authentication.AUTHENTICATION_USER_ID,
                Authentication.AUTHENTICATION_TOKEN)) {
          user = userService.getUser(userId);
          return new RestAuthenticationInfo(user, userString, clientIpAddress);
        }
      } else {
        log.error(Authentication.AUTHENTICATION_USER_ID + " is not an integer: '" + userString + "'. Rest call forbidden.");
      }
      return null;
    }

    userString = getAttribute(req, Authentication.AUTHENTICATION_USERNAME, AuthenticationOld.AUTHENTICATION_USERNAME);
    if (checkLoginProtection((HttpServletResponse) response, userString, loginProtection, clientIpAddress)) {
      // access denied
      return null;
    }
    String password = getAttribute(req, Authentication.AUTHENTICATION_PASSWORD, AuthenticationOld.AUTHENTICATION_PASSWORD);
    if (userString != null && password != null) {
      user = userService.authenticateUser(userString, password);
      if (user != null) {
        return new RestAuthenticationInfo(user, userString, clientIpAddress);
      }
      log.error("Authentication failed for "
              + Authentication.AUTHENTICATION_USERNAME
              + "='"
              + userString
              + "' with given password. Rest call forbidden.");
      return null;
    }

    String authHeader = req.getHeader("Authorization");
    if (authHeader != null) {
      // Try basic authorization
      final String[] basic = StringUtils.split(authHeader);
      if (basic.length == 2 && StringUtils.equalsIgnoreCase(basic[0], "Basic")) {
        String credentials = new String(Base64.decodeBase64(basic[1]), "UTF-8");
        int p = credentials.indexOf(":");
        if (p != -1) {
          final String username = credentials.substring(0, p).trim();
          password = credentials.substring(p + 1).trim();
          user = userService.authenticateUser(username, password);
          if (user != null) {
            return new RestAuthenticationInfo(user, userString, clientIpAddress);
          }
          log.error("Basic authentication failed for user '" + username + "'.");
        }
        log.error("Basic authentication failed.");
        return null;
      }
    }
    // Try to get the user by session id:
    user = UserFilter.getUser(req);
    if (user == null) {
      UserContext userContext = cookieService.checkStayLoggedIn(req, resp);
      if (userContext != null) {
        if (log.isDebugEnabled()) {
          log.debug("User's stay logged-in cookie found: " + req.getRequestURI());
        }
        executeLogin(req, userContext);
        user = userContext.getUser();
      }
    }
    if (user == null) {
      String requestURI = ((HttpServletRequest) request).getRequestURI();
      // Don't log error for userStatus (used by React client for checking weather the user is logged in or not).
      if (requestURI == null || !requestURI.equals("/rs/userStatus")) {
        log.error("Neither "
                + Authentication.AUTHENTICATION_USER_ID
                + " nor "
                + Authentication.AUTHENTICATION_USERNAME
                + "/"
                + Authentication.AUTHENTICATION_PASSWORD
                + " is given for rest call: " + requestURI + " . Rest call forbidden.");
      }
    }

    if (user == null) {
      loginProtection.incrementFailedLoginTimeOffset(userString, clientIpAddress);
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return null;
    }
    return new RestAuthenticationInfo(user, userString, clientIpAddress);
  }

  /**
   * You must use try { registerUser(...) } finally { unregisterUser() }!!!!
   *
   * @param request
   * @param userInfo
   */
  public void registerUser(final ServletRequest request, final RestAuthenticationInfo userInfo) {
    final PFUserDO user = userInfo.user;
    final String clientIpAddress = userInfo.clientIpAddress;
    LoginProtection.instance().clearLoginTimeOffset(userInfo.userString, clientIpAddress);
    ThreadLocalUserContext.setUser(getUserGroupCache(), user);
    final HttpServletRequest req = (HttpServletRequest) request;
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
  }

  public void unregister(final ServletRequest request, final ServletResponse response,
                         final RestAuthenticationInfo userInfo) {
    ThreadLocalUserContext.setUser(getUserGroupCache(), null);
    ConnectionSettings.set(null);
    MDC.remove("ip");
    MDC.remove("user");
    final int resultCode = ((HttpServletResponse) response).getStatus();
    if (resultCode != HttpStatus.OK.value() && resultCode != HttpStatus.MULTI_STATUS.value()) {
      // MULTI_STATUS (207) will be returned by milton.io (CalDAV/CardDAV), because XML is returned.
      final PFUserDO user = userInfo.user;
      final String clientIpAddress = userInfo.clientIpAddress;
      log.error("User: " + user.getUsername() + " calls RestURL: " + ((HttpServletRequest) request).getRequestURI()
              + " with ip: "
              + clientIpAddress
              + ": Response status not OK: status=" + ((HttpServletResponse) response).getStatus()
              + ".");
    }
  }

  private boolean checkLoginProtection(final HttpServletResponse response, final String userString,
                                       final LoginProtection loginProtection,
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

  /**
   * @param req
   * @param keys Name of the parameter key. Additional keys may be given as alternative keys if first key isn't found.
   *             Might be used for backwards compatibility.
   * @return
   */
  private String getAttribute(final HttpServletRequest req, final String... keys) {
    if (keys == null) {
      return null;
    }
    for (String key : keys) {
      String value = req.getHeader(key);
      if (value == null) {
        value = req.getParameter(key);
      }
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private TenantRegistry getTenantRegistry() {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  private UserGroupCache getUserGroupCache() {
    return getTenantRegistry().getUserGroupCache();
  }
}
