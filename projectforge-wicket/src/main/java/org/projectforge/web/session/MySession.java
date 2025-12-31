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

package org.projectforge.web.session;

import org.apache.wicket.Session;
import org.apache.wicket.core.request.ClientInfo;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.projectforge.Version;
import org.projectforge.framework.ToStringUtil;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.login.LoginService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Objects;
import java.util.TimeZone;

public class MySession extends WebSession {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MySession.class);

  private static final long serialVersionUID = -1783696379234637066L;

  private UserContext userContext;

  private String userAgent;

  private UserAgentDevice userAgentDevice = UserAgentDevice.UNKNOWN;

  private UserAgentBrowser userAgentBrowser = UserAgentBrowser.UNKNOWN;

  private ClientProperties clientProperties;

  private String userAgentBrowserVersionString = null;

  private Version userAgentBrowserVersion = null;

  private UserAgentOS userAgentOS = UserAgentOS.UNKNOWN;

  private boolean mobileUserAgent;

  private boolean ignoreMobileUserAgent;

  /**
   * Random cross site request forgery token.
   */
  private final String csrfToken;

  public MySession(final Request request) {
    super(request);
    setLocale(request);
    final ClientInfo info = getClientInfo();
    if (info instanceof WebClientInfo) {
      clientProperties = ((WebClientInfo) clientInfo).getProperties();
      clientProperties.setTimeZone(ThreadLocalUserContext.getTimeZone());
      userAgent = ((WebClientInfo) info).getUserAgent();
      userAgentDevice = UserAgentDevice.getUserAgentDevice(userAgent);
      userAgentOS = UserAgentOS.getUserAgentOS(userAgent);
      mobileUserAgent = userAgentDevice.isMobile();
      final UserAgentDetection userAgentDetection = UserAgentDetection.browserDetect(userAgent);
      userAgentBrowser = userAgentDetection.getUserAgentBrowser();
      userAgentBrowserVersionString = userAgentDetection.getUserAgentBrowserVersion();
    } else {
      log.error("Oups, ClientInfo is not from type WebClientInfo: " + info);
    }
    setUserContext(ThreadLocalUserContext.getUserContext());
    this.csrfToken = NumberHelper.getSecureRandomAlphanumeric(20);
  }

  public static MySession get() {
    return (MySession) Session.get();
  }

  /**
   * @return The logged-in user or null if no user is logged-in.
   */
  public synchronized PFUserDO getUser() {
    if (userContext == null) {
      // Happens after login via React page or if user isn't logged in.
      userContext = ThreadLocalUserContext.getUserContext();
      if (userContext != null && userContext.getUser() != null) {
        final HttpServletRequest request = ((ServletWebRequest) RequestCycle.get().getRequest()).getContainerRequest();
        final UserContext sessionUserContext = LoginService.getUserContext(request);
        if (sessionUserContext == null || sessionUserContext.getUser() == null) {
          log.warn("******* User is given in ThreadLocalUserContext, but not given in session. This paranoia setting shouldn't occur. User: "
                  + ToStringUtil.toJsonString(userContext));
          return null;
        }
        if (!Objects.equals(sessionUserContext.getUser().getId(), userContext.getUser().getId())) {
          log.warn("******* Security warning: User is given in ThreadLocalUserContext differs from user of session. This paranoia setting shouldn't occur. Thread local user="
                  + ToStringUtil.toJsonString(userContext)
                  + ", session user="
                  + ToStringUtil.toJsonString(sessionUserContext.getUser()));
          return null;
        }
        log.info("User '" + userContext.getUser().getUsername() + "' now also logged-in for Wicket stuff.");
        userContext = sessionUserContext;
      }
    }
    return userContext != null ? userContext.getUser() : null;
  }

  public synchronized UserContext getUserContext() {
    return userContext;
  }

  /**
   * @return the randomized cross site request forgery token
   */
  public String getCsrfToken() {
    return csrfToken;
  }

  /**
   * @return The id of the logged-in user or null if no user is logged-in.
   */
  public synchronized Long getUserId() {
    final PFUserDO user = getUser();
    return user != null ? user.getId() : null;
  }

  public synchronized void setUserContext(final UserContext userContext) {
    this.userContext = userContext;
    dirty();
  }

  public synchronized boolean isAuthenticated() {
    final PFUserDO user = getUser();
    return (user != null);
  }

  public synchronized TimeZone getTimeZone() {
    final PFUserDO user = getUser();
    return user != null ? user.getTimeZone() : Configuration.getInstance().getDefaultTimeZone();
  }

  public String getUserAgent() {
    return userAgent;
  }

  /**
   * @return the userAgentOS
   */
  public UserAgentOS getUserAgentOS() {
    return userAgentOS;
  }

  /**
   * @return true, if the user agent device is an iPad, iPhone or iPod.
   */
  public boolean isIOSDevice() {
    return this.userAgentDevice != null
            && this.userAgentDevice.isIn(UserAgentDevice.IPAD, UserAgentDevice.IPHONE, UserAgentDevice.IPOD);
  }

  /**
   * @return true, if the user agent is a mobile agent and ignoreMobileUserAgent isn't set, otherwise false.
   */
  public boolean isMobileUserAgent() {
    if (ignoreMobileUserAgent == true) {
      return false;
    }
    return mobileUserAgent;
  }

  /**
   * The user wants to ignore the mobile agent and wants to get the PC version (normal web version).
   *
   * @return
   */
  public boolean isIgnoreMobileUserAgent() {
    return ignoreMobileUserAgent;
  }

  /**
   * @return the userAgentBrowser
   */
  public UserAgentBrowser getUserAgentBrowser() {
    return userAgentBrowser;
  }

  /**
   * @return the userAgentBrowserVersion
   */
  public String getUserAgentBrowserVersionString() {
    return userAgentBrowserVersionString;
  }

  /**
   * @return the userAgentBrowserVersion
   */
  public Version getUserAgentBrowserVersion() {
    if (userAgentBrowserVersion == null && userAgentBrowserVersionString != null) {
      userAgentBrowserVersion = new Version(userAgentBrowserVersionString);
    }
    return userAgentBrowserVersion;
  }

  /**
   * @return the userAgentDevice
   */
  public UserAgentDevice getUserAgentDevice() {
    return userAgentDevice;
  }

  public void setIgnoreMobileUserAgent(final boolean ignoreMobileUserAgent) {
    this.ignoreMobileUserAgent = ignoreMobileUserAgent;
  }

  /**
   * Only used by SetupPage.
   * @param userContext
   * @param request
   */
  public void internalLogin(final UserContext userContext, final Request request) {
    super.replaceSession();
    this.userContext = userContext;
    final PFUserDO user = userContext != null ? userContext.getUser() : null;
    if (user == null) {
      log.warn("Oups, no user given to log in.");
      return;
    }
    log.debug("User logged in: " + user.getDisplayName());
    ThreadLocalUserContext.setUserContext(userContext);
    setLocale(request);
  }

  /**
   * Sets or updates the locale of the user's session. Takes the locale of the user account or if not given the locale
   * of the given request.
   *
   * @param request
   */
  public void setLocale(final Request request) {
    if (request == null) {
      // Should only occur on tests.
      return;
    }
    setLocale(ThreadLocalUserContext.getLocale(request.getLocale()));
  }

  /**
   * Only used by SetupPage and on logout button in Wicket context.
   */
  public void internalLogout() {
    super.clear();
    super.invalidateNow();
  }

  public void put(final String name, final Serializable value) {
    super.setAttribute(name, value);
  }

  public Object get(final String name) {
    return super.getAttribute(name);
  }

  /**
   * @return the clientProperties
   */
  public ClientProperties getClientProperties() {
    return clientProperties;
  }
}
