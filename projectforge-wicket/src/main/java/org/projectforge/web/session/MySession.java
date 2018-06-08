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

package org.projectforge.web.session;

import java.io.Serializable;
import java.util.Collection;
import java.util.TimeZone;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Session;
import org.apache.wicket.core.request.ClientInfo;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.Request;
import org.projectforge.Version;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.user.UserPreferencesHelper;

public class MySession extends WebSession
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MySession.class);

  private static final long serialVersionUID = -1783696379234637066L;

  private static final String USER_PREF_KEY_CURRENT_TENANT = UserContext.class.getName() + ":currentTenantId";

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

  public MySession(final Request request)
  {
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
    initActualTenant();
    this.csrfToken = NumberHelper.getSecureRandomUrlSaveString(20);
  }

  private void initActualTenant()
  {
    if (ThreadLocalUserContext.getUserContext() == null) {
      return;
    }
    UserContext userContext = ThreadLocalUserContext.getUserContext();
    TenantService tenantService = ApplicationContextProvider.getApplicationContext().getBean(TenantService.class);
    PFUserDO user = userContext.getUser();
    if (user.getId() != null && tenantService.isMultiTenancyAvailable() == true) {
      // Try to find the last used tenant of the user:
      final Integer currentTenantId = (Integer) UserPreferencesHelper.getEntry(USER_PREF_KEY_CURRENT_TENANT);
      if (currentTenantId != null) {
        setCurrentTenant(tenantService.getTenant(currentTenantId));
      } else {
        final Collection<TenantDO> tenants = tenantService.getTenantsOfUser(user.getId());
        if (CollectionUtils.isNotEmpty(tenants) == true) {
          setCurrentTenant(tenants.iterator().next());
        }
      }
    }
  }

  public static MySession get()
  {
    return (MySession) Session.get();
  }

  /**
   * @return The logged-in user or null if no user is logged-in.
   */
  public synchronized PFUserDO getUser()
  {
    return userContext != null ? userContext.getUser() : null;
  }

  public synchronized UserContext getUserContext()
  {
    return userContext;
  }

  /**
   * @return the randomized cross site request forgery token
   */
  public String getCsrfToken()
  {
    return csrfToken;
  }

  /**
   * @return The id of the logged-in user or null if no user is logged-in.
   */
  public synchronized Integer getUserId()
  {
    final PFUserDO user = getUser();
    return user != null ? user.getId() : null;
  }

  public synchronized void setUserContext(final UserContext userContext)
  {
    this.userContext = userContext;
    dirty();
  }

  public synchronized boolean isAuthenticated()
  {
    final PFUserDO user = getUser();
    return (user != null);
  }

  public synchronized TimeZone getTimeZone()
  {
    final PFUserDO user = getUser();
    return user != null ? user.getTimeZoneObject() : Configuration.getInstance().getDefaultTimeZone();
  }

  public String getUserAgent()
  {
    return userAgent;
  }

  /**
   * @param tenant the currentTenant to set
   * @return this for chaining.
   */
  public MySession setCurrentTenant(final TenantDO tenant)
  {
    if (tenant == null) {
      log.warn("Can't switch to current tenant=null!");
      return this;
    }
    if (tenant.getId() == null) {
      log.warn("Can't switch to current tenant with id=null!");
      return this;
    }
    if (this.userContext.getCurrentTenant() != null
        && tenant.getId().equals(this.userContext.getCurrentTenant().getId()) == false) {
      log.info("User switched the tenant: [" + tenant.getName() + "] (was ["
          + this.userContext.getCurrentTenant().getName() + "]).");
      this.userContext.setCurrentTenant(tenant);
      UserPreferencesHelper.putEntry(USER_PREF_KEY_CURRENT_TENANT, tenant.getId(), true);
    }
    return this;
  }

  /**
   * @return the userAgentOS
   */
  public UserAgentOS getUserAgentOS()
  {
    return userAgentOS;
  }

  /**
   * @return true, if the user agent device is an iPad, iPhone or iPod.
   */
  public boolean isIOSDevice()
  {
    return this.userAgentDevice != null
        && this.userAgentDevice.isIn(UserAgentDevice.IPAD, UserAgentDevice.IPHONE, UserAgentDevice.IPOD);
  }

  /**
   * @return true, if the user agent is a mobile agent and ignoreMobileUserAgent isn't set, otherwise false.
   */
  public boolean isMobileUserAgent()
  {
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
  public boolean isIgnoreMobileUserAgent()
  {
    return ignoreMobileUserAgent;
  }

  /**
   * @return the userAgentBrowser
   */
  public UserAgentBrowser getUserAgentBrowser()
  {
    return userAgentBrowser;
  }

  /**
   * @return the userAgentBrowserVersion
   */
  public String getUserAgentBrowserVersionString()
  {
    return userAgentBrowserVersionString;
  }

  /**
   * @return the userAgentBrowserVersion
   */
  public Version getUserAgentBrowserVersion()
  {
    if (userAgentBrowserVersion == null && userAgentBrowserVersionString != null) {
      userAgentBrowserVersion = new Version(userAgentBrowserVersionString);
    }
    return userAgentBrowserVersion;
  }

  /**
   * @return the userAgentDevice
   */
  public UserAgentDevice getUserAgentDevice()
  {
    return userAgentDevice;
  }

  public void setIgnoreMobileUserAgent(final boolean ignoreMobileUserAgent)
  {
    this.ignoreMobileUserAgent = ignoreMobileUserAgent;
  }

  public void login(final UserContext userContext, final Request request)
  {
    super.replaceSession();
    this.userContext = userContext;
    final PFUserDO user = userContext != null ? userContext.getUser() : null;
    if (user == null) {
      log.warn("Oups, no user given to log in.");
      return;
    }
    log.debug("User logged in: " + user.getShortDisplayName());
    ThreadLocalUserContext.setUserContext(userContext);
    setLocale(request);
  }

  /**
   * Sets or updates the locale of the user's session. Takes the locale of the user account or if not given the locale
   * of the given request.
   *
   * @param request
   */
  public void setLocale(final Request request)
  {
    setLocale(ThreadLocalUserContext.getLocale(request.getLocale()));
  }

  public void logout()
  {
    PFUserDO user = userContext != null ? userContext.getUser() : null;
    if (user != null) {
      log.info("User logged out: " + user.getShortDisplayName());
      user = null;
    }
    ThreadLocalUserContext.clear();
    userContext = null;
    super.clear();
    super.invalidateNow();
  }

  public void put(final String name, final Serializable value)
  {
    super.setAttribute(name, value);
  }

  public Object get(final String name)
  {
    return super.getAttribute(name);
  }

  /**
   * @return the clientProperties
   */
  public ClientProperties getClientProperties()
  {
    return clientProperties;
  }
}
