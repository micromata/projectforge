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

package org.projectforge.business.user.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.MDC;
import org.projectforge.Const;
import org.projectforge.business.login.Login;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.servlet.LogoServlet;
import org.projectforge.web.servlet.SMSReceiverServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Ensures that an user is logged in and put the user id, locale and ip to the logging mdc.<br/>
 * Ignores login for: /ProjectForge/wa/resources/* with the suffixes: *.js, *.css, *.gif, *.png. <br/>
 * Don't forget to call setServletContext on applications start-up!
 */
public class UserFilter implements Filter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserFilter.class);

  private final static String SESSION_KEY_USER = "UserFilter.user";

  private static final int COOKIE_MAX_AGE = 30 * 24 * 3600; // 30 days.

  // private static String IGNORE_PREFIX_WICKET;

  // private static String IGNORE_PREFIX_DOC;

  // private static String IGNORE_PREFIX_SITE_DOC;

  private static String IGNORE_PREFIX_LOGO;

  private static String IGNORE_PREFIX_SMS_REVEIVE_SERVLET;

  private static String WICKET_PAGES_PREFIX;

  private static String CONTEXT_PATH;

  @Autowired
  private UserDao userDao;

  private static boolean updateRequiredFirst = false;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException
  {
    WebApplicationContext springContext = WebApplicationContextUtils
        .getRequiredWebApplicationContext(filterConfig.getServletContext());
    final AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
    beanFactory.autowireBean(this);
    CONTEXT_PATH = filterConfig.getServletContext().getContextPath();
    WICKET_PAGES_PREFIX = CONTEXT_PATH + "/" + Const.WICKET_APPLICATION_PATH;
    // IGNORE_PREFIX_WICKET = WICKET_PAGES_PREFIX + "resources";
    // IGNORE_PREFIX_DOC = contextPath + "/secure/doc";
    // IGNORE_PREFIX_SITE_DOC = contextPath + "/secure/site";
    IGNORE_PREFIX_LOGO = CONTEXT_PATH + "/" + LogoServlet.BASE_URL;
    IGNORE_PREFIX_SMS_REVEIVE_SERVLET = CONTEXT_PATH + "/" + SMSReceiverServlet.URL;
  }

  public static void setUpdateRequiredFirst(final boolean value)
  {
    updateRequiredFirst = value;
  }

  public static boolean isUpdateRequiredFirst()
  {
    return updateRequiredFirst;
  }

  public static Cookie getStayLoggedInCookie(final HttpServletRequest request)
  {
    return getCookie(request, Const.COOKIE_NAME_FOR_STAY_LOGGED_IN);
  }

  private static Cookie getCookie(final HttpServletRequest request, final String name)
  {
    final Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (final Cookie cookie : cookies) {
        if (name.equals(cookie.getName()) == true) {
          return cookie;
        }
      }
    }
    return null;
  }

  /**
   * Adds or refresh the given cookie.
   * 
   * @param request
   * @param response
   * @param stayLoggedInCookie
   */
  public static void addStayLoggedInCookie(final HttpServletRequest request, final HttpServletResponse response,
      final Cookie stayLoggedInCookie)
  {
    stayLoggedInCookie.setMaxAge(COOKIE_MAX_AGE);
    stayLoggedInCookie.setPath("/");
    if (request.isSecure() == true) {
      log.debug("Set secure cookie");
      stayLoggedInCookie.setSecure(true);
    } else {
      log.debug("Set unsecure cookie");
    }
    stayLoggedInCookie.setHttpOnly(true);
    response.addCookie(stayLoggedInCookie); // Refresh cookie.
  }

  /**
   * @param request
   * @param userContext
   */
  public static void login(final HttpServletRequest request, final UserContext userContext)
  {
    final HttpSession session = request.getSession();
    session.setAttribute(SESSION_KEY_USER, userContext);
  }

  public static void refreshUser(final HttpServletRequest request)
  {
    final UserContext userContext = getUserContext(request);
    userContext.refreshUser();
  }

  public static PFUserDO getUser(final HttpServletRequest request)
  {
    final UserContext userContext = getUserContext(request);
    return userContext != null ? userContext.getUser() : null;
  }

  private static UserContext getUserContext(final HttpServletRequest request)
  {
    final HttpSession session = request.getSession();
    if (session == null) {
      return null;
    }
    final UserContext userContext = (UserContext) session.getAttribute(SESSION_KEY_USER);
    return userContext;
  }

  @Override
  public void destroy()
  {
    // do nothing
  }

  @Override
  public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
      throws IOException, ServletException
  {
    HttpServletRequest request = (HttpServletRequest) req;
    if (log.isDebugEnabled() == true) {
      log.debug("doFilter " + request.getRequestURI() + ": " + request.getSession().getId());
      final Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (final Cookie cookie : cookies) {
          log.debug("Cookie "
              + cookie.getName()
              + ", path="
              + cookie.getPath()
              + ", value="
              + cookie.getValue()
              + ", secure="
              + cookie.getVersion()
              + ", maxAge="
              + cookie.getMaxAge()
              + ", domain="
              + cookie.getDomain());
        }
      }
    }
    final HttpServletResponse response = (HttpServletResponse) resp;
    UserContext userContext = null;
    try {
      MDC.put("ip", request.getRemoteAddr());
      MDC.put("session", request.getSession().getId());
      if (ignoreFilterFor(request) == true) {
        // Ignore the filter for this request:
        if (log.isDebugEnabled() == true) {
          log.debug("Ignore: " + request.getRequestURI());
        }
        chain.doFilter(request, response);
      } else {
        // final boolean sessionTimeout = request.isRequestedSessionIdValid() == false;
        userContext = (UserContext) request.getSession().getAttribute(SESSION_KEY_USER);
        if (userContext != null) {
          if (updateRequiredFirst == false) {
            // Get the fresh user from the user cache (not in maintenance mode because user group cache is perhaps not initialized correctly
            // if updates of e. g. the user table are necessary.
            userContext.refreshUser();
          }
          if (log.isDebugEnabled() == true) {
            log.debug("User found in session: " + request.getRequestURI());
          }
        } else if (updateRequiredFirst == false) {
          // Ignore stay-logged-in if redirect to update page is required.
          userContext = checkStayLoggedIn(request, response);
          if (userContext != null) {
            if (log.isDebugEnabled() == true) {
              log.debug("User's stay logged-in cookie found: " + request.getRequestURI());
            }
            userContext.setStayLoggedIn(true); // Used by MenuMobilePage.
            UserFilter.login(request, userContext);
          }
        }
        final PFUserDO user = userContext != null ? userContext.getUser() : null;
        if (user != null) {
          MDC.put("user", user.getUsername());
          ThreadLocalUserContext.setUserContext(userContext);
          request = decorateWithLocale(request);
          chain.doFilter(request, response);
        } else {
          if (((HttpServletRequest) req).getRequestURI().startsWith(WICKET_PAGES_PREFIX) == true) {
            // Access-checking is done by Wicket, not by this filter:
            request = decorateWithLocale(request);
            chain.doFilter(request, response);
          } else {
            response.getWriter().append("No access.");
          }
        }
      }
    } finally {
      ThreadLocalUserContext.clear();
      MDC.remove("ip");
      MDC.remove("session");
      final PFUserDO user = userContext != null ? userContext.getUser() : null;
      if (user != null) {
        MDC.remove("user");
      }
      if (log.isDebugEnabled() == true) {
        log.debug("doFilter finished for " + request.getRequestURI() + ": " + request.getSession().getId());
      }
    }
  }

  /**
   * User is not logged. Checks a stay-logged-in-cookie.
   * 
   * @return user if valid cookie found, otherwise null.
   */
  private UserContext checkStayLoggedIn(final HttpServletRequest request, final HttpServletResponse response)
  {
    final Cookie stayLoggedInCookie = getStayLoggedInCookie(request);
    if (stayLoggedInCookie != null) {
      final String value = stayLoggedInCookie.getValue();
      if (StringUtils.isBlank(value) == true) {
        return null;
      }
      final String[] values = value.split(":");
      if (values.length != 3) {
        log.warn("Invalid cookie found: " + value);
        return null;
      }
      final Integer userId = NumberHelper.parseInteger(values[0]);
      final PFUserDO user = userDao.internalGetById(userId);
      if (user == null) {
        log.warn("Invalid cookie found (user not found): " + value);
        return null;
      }
      if (user.getUsername().equals(values[1]) == false) {
        log.warn("Invalid cookie found (user name wrong, maybe changed): " + value);
        return null;
      }
      if (values[2] == null || values[2].equals(user.getStayLoggedInKey()) == false) {
        log.warn("Invalid cookie found (stay-logged-in key, maybe renewed and/or user password changed): " + value);
        return null;
      }
      if (Login.getInstance().checkStayLoggedIn(user) == false) {
        log.warn("Stay-logged-in wasn't accepted by the login handler: " + user.getUserDisplayname());
        return null;
      }
      // update the cookie, especially the max age
      addStayLoggedInCookie(request, response, stayLoggedInCookie);
      log.info("User successfully logged in using stay-logged-in method: " + user.getUserDisplayname());
      return new UserContext(PFUserDO.createCopyWithoutSecretFields(user), getUserGroupCache());
    }
    return null;
  }

  /**
   * @param request
   * @return
   */
  protected HttpServletRequest decorateWithLocale(HttpServletRequest request)
  {
    final Locale locale = ThreadLocalUserContext.getLocale(request.getLocale());
    request = new HttpServletRequestWrapper(request)
    {
      @Override
      public Locale getLocale()
      {
        return locale;
      }

      @Override
      public Enumeration<Locale> getLocales()
      {
        return Collections.enumeration(Collections.singleton(locale));
      }
    };
    return request;
  }

  /**
   * Will be called by doFilter.
   * 
   * @param req from do Filter.
   * @return true, if the filter should ignore this request, otherwise false.
   */
  protected boolean ignoreFilterFor(final ServletRequest req)
  {
    final HttpServletRequest hreq = (HttpServletRequest) req;
    final String uri = hreq.getRequestURI();
    // If you have an NPE you have probably forgotten to call setServletContext on applications start-up.
    // Paranoia setting. May-be there could be a vulnerability with request parameters:
    if (uri.contains("?") == false) {
      // if (uri.startsWith(IGNORE_PREFIX_WICKET) && StringHelper.endsWith(uri, ".js", ".css", ".gif", ".png") == true) {
      // No access checking for Wicket resources.
      // return true;
      // } else if (StringHelper.startsWith(uri, IGNORE_PREFIX_DOC, IGNORE_PREFIX_SITE_DOC) == true
      // && StringHelper.endsWith(uri, ".html", ".pdf", ".js", ".css", ".gif", ".png") == true) {
      // No access checking for documentation (including site doc).
      // return true;
      // } else
      if (StringHelper.startsWith(uri, IGNORE_PREFIX_LOGO, IGNORE_PREFIX_SMS_REVEIVE_SERVLET) == true) {
        // No access checking for logo and sms receiver servlet.
        // The sms receiver servlet has its own authentification (key).
        return true;
      }
    }
    return false;
  }

  public TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  /**
   * @return the UserGroupCache with groups and rights (tenant specific).
   */
  public UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }
}
