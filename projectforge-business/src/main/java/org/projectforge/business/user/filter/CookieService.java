package org.projectforge.business.user.filter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.projectforge.Const;
import org.projectforge.business.login.Login;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Service;

@Service
public class CookieService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CookieService.class);

  private static final int COOKIE_MAX_AGE = 30 * 24 * 3600; // 30 days.

  @Autowired
  private UserDao userDao;

  @Autowired
  private ServerProperties serverProperties;

  /**
   * User is not logged. Checks a stay-logged-in-cookie.
   *
   * @return user if valid cookie found, otherwise null.
   */
  public UserContext checkStayLoggedIn(final HttpServletRequest request, final HttpServletResponse response)
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
      return new UserContext(PFUserDO.createCopyWithoutSecretFields(user), userDao.getUserGroupCache());
    }
    return null;
  }

  /**
   * Adds or refresh the given cookie.
   */
  public void addStayLoggedInCookie(final HttpServletRequest request, final HttpServletResponse response, final Cookie stayLoggedInCookie)
  {
    stayLoggedInCookie.setMaxAge(COOKIE_MAX_AGE);
    stayLoggedInCookie.setPath("/");
    if (request.isSecure() || isSecureCookieConfigured()) {
      log.debug("Set secure cookie");
      stayLoggedInCookie.setSecure(true);
    } else {
      log.debug("Set unsecure cookie");
    }
    stayLoggedInCookie.setHttpOnly(true);
    response.addCookie(stayLoggedInCookie); // Refresh cookie.
  }

  /**
   * Reads the secure cookie setting from the spring boot configuration.
   */
  private Boolean isSecureCookieConfigured()
  {
    final Boolean secure = serverProperties.getSession().getCookie().getSecure();
    return secure != null && secure == true;
  }

  public Cookie getStayLoggedInCookie(final HttpServletRequest request)
  {
    return getCookie(request, Const.COOKIE_NAME_FOR_STAY_LOGGED_IN);
  }

  private Cookie getCookie(final HttpServletRequest request, final String name)
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

}
