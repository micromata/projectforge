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

import org.apache.log4j.MDC;
import org.projectforge.Const;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
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

  @Autowired
  private CookieService cookieService;

  // private static String IGNORE_PREFIX_WICKET;

  // private static String IGNORE_PREFIX_DOC;

  // private static String IGNORE_PREFIX_SITE_DOC;

  private static String IGNORE_PREFIX_LOGO;

  private static String IGNORE_PREFIX_SMS_REVEIVE_SERVLET;

  private static String WICKET_PAGES_PREFIX;

  private static String CONTEXT_PATH;

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
      MDC.put("ip", (Object) request.getRemoteAddr());
      MDC.put("session", (Object) request.getSession().getId());
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
          userContext = cookieService.checkStayLoggedIn(request, response);
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
          MDC.put("user", (Object) user.getUsername());
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
        StringBuffer sb = new StringBuffer();
        sb.append("doFilter finished for ");
        sb.append(request.getRequestURI());
        if (request.getSession(false) != null) {
          sb.append(request.getSession(false).getId());
        } else {
          sb.append("No active session available.");
        }
        log.debug(sb.toString());
      }
    }
  }

  private HttpServletRequest decorateWithLocale(HttpServletRequest request)
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
  private boolean ignoreFilterFor(final ServletRequest req)
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

}
