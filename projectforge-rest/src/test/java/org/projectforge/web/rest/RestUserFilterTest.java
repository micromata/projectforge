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

package org.projectforge.web.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.projectforge.business.user.UserAuthenticationsService;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserTokenType;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.rest.Authentication;
import org.projectforge.rest.config.Rest;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

public class RestUserFilterTest extends AbstractTestBase {
  @Autowired
  private UserAuthenticationsService userAuthenticationsService;

  @Autowired
  private UserGroupCache userGroupCache;

  final RestUserFilter filter = new RestUserFilter();

  int userId = 0;

  String userToken = "token";

  private static boolean initialized;

  @BeforeEach
  public void init() {
    if (initialized)
      return;
    initialized = true;
    PFUserDO user = userGroupCache.getUser(AbstractTestBase.TEST_USER);
    this.userId = user.getId();
    ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this.filter);
    logon(AbstractTestBase.TEST_ADMIN_USER);
    this.userToken = userAuthenticationsService.getToken(this.userId, UserTokenType.REST_CLIENT); // Admin access required.
    logoff();
  }

  @Test
  public void testAuthentication() throws IOException, ServletException, InterruptedException {
    final HttpServletResponse response = mock(HttpServletResponse.class);

    // Wrong password
    HttpServletRequest request = mockRequest(AbstractTestBase.TEST_USER, "failed".toCharArray(), null, null);
    FilterChain chain = mock(FilterChain.class);
    filter.doFilter(request, response, chain);
    verify(chain, never()).doFilter(Mockito.any(HttpServletRequest.class), Mockito.any(HttpServletResponse.class));
    Thread.sleep(1100); // Login penalty.
    // Correct user name and password
    request = mockRequest(AbstractTestBase.TEST_USER, AbstractTestBase.TEST_USER_PASSWORD, null, null);
    chain = mock(FilterChain.class);
    filter.doFilter(request, response, chain);

    // Wrong token
    request = mockRequest(null, null, userId, "wrongToken");
    chain = mock(FilterChain.class);
    filter.doFilter(request, response, chain);
    verify(chain, never()).doFilter(Mockito.any(HttpServletRequest.class), Mockito.any(HttpServletResponse.class));
    Thread.sleep(2100); // Login penalty.
    // Correct user name and password
    request = mockRequest(null, null, userId, userToken);
    chain = mock(FilterChain.class);
    filter.doFilter(request, response, chain);
    verify(chain).doFilter(Mockito.eq(request), Mockito.eq(response));
  }

  private HttpServletRequest mockRequest(final String username, final char[] password, final Integer userId,
                                         final String authenticationToken) {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    if (username != null) {
      when(request.getHeader(Mockito.eq(Authentication.AUTHENTICATION_USERNAME))).thenReturn(username);
    }
    if (password != null) {
      when(request.getHeader(Mockito.eq(Authentication.AUTHENTICATION_PASSWORD))).thenReturn(new String(password));
    }
    if (userId != null) {
      when(request.getHeader(Mockito.eq(Authentication.AUTHENTICATION_USER_ID))).thenReturn(userId.toString());
    }
    if (authenticationToken != null) {
      when(request.getHeader(Mockito.eq(Authentication.AUTHENTICATION_TOKEN))).thenReturn(authenticationToken);
      when(request.getRequestURI()).thenReturn(Rest.URL + "....");
    }
    return request;
  }
}
