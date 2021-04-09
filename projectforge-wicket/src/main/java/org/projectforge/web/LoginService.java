/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web;

import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.projectforge.business.login.LoginHandler;
import org.projectforge.business.user.UserAuthenticationsService;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserPrefCache;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.business.user.filter.CookieService;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.login.LoginHandlerService;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.WicketUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class LoginService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginService.class);

  @Autowired
  private UserService userService;

  @Autowired
  private UserAuthenticationsService userAuthenticationsService;

  @Autowired
  private CookieService cookieService;

  @Autowired
  private LoginHandlerService loginHandlerService;

  private LoginHandler loginHandler;

  public void logout(final MySession mySession, final WebRequest request, final WebResponse response,
                     final UserXmlPreferencesCache userXmlPreferencesCache,
                     final UserPrefCache userPrefCache) {
    final Cookie stayLoggedInCookie = cookieService.getStayLoggedInCookie(WicketUtils.getHttpServletRequest(request));
    logout(mySession, stayLoggedInCookie, userXmlPreferencesCache, userPrefCache);
    if (stayLoggedInCookie != null) {
      response.addCookie(stayLoggedInCookie);
    }
  }

  public void logout(final MySession mySession, final HttpServletRequest request,
                     final HttpServletResponse response,
                     final UserXmlPreferencesCache userXmlPreferencesCache,
                     final UserPrefCache userPrefCache) {
    final Cookie stayLoggedInCookie = cookieService.getStayLoggedInCookie(request);
    logout(mySession, stayLoggedInCookie, userXmlPreferencesCache, userPrefCache);
    if (stayLoggedInCookie != null) {
      response.addCookie(stayLoggedInCookie);
    }
  }

  private void logout(final MySession mySession, final Cookie stayLoggedInCookie,
                      final UserXmlPreferencesCache userXmlPreferencesCache,
                      final UserPrefCache userPrefCache) {
    final PFUserDO user = mySession.getUser();
    if (user != null) {
      userXmlPreferencesCache.flushToDB(user.getId());
      userXmlPreferencesCache.clear(user.getId());
      userPrefCache.flushToDB(user.getId());
      userPrefCache.clear(user.getId());
    }
    mySession.logout();
    if (stayLoggedInCookie != null) {
      stayLoggedInCookie.setMaxAge(0);
      stayLoggedInCookie.setValue(null);
      stayLoggedInCookie.setPath("/");
    }
  }
}
