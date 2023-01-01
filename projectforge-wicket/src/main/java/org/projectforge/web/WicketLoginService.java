/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.login.LoginService;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.WicketUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class WicketLoginService {

  @Autowired
  private LoginService loginService;

  @PostConstruct
  private void init() {
    loginService.register(new WicketLogoutListener());
  }

  public void logout(final MySession mySession, final WebRequest request, final WebResponse response) {
    logout(mySession, WicketUtils.getHttpServletRequest(request), WicketUtils.getHttpServletResponse(response));
  }

  public void logout(final MySession mySession, final HttpServletRequest request,
                     final HttpServletResponse response) {
    loginService.logout(request, response);
    mySession.internalLogout();
  }
}
