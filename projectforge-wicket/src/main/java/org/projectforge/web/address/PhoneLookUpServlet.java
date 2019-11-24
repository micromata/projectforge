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

package org.projectforge.web.address;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Assign a phone number to a full name and organization using ProjectForge address database. <br/>
 * This servlet may be used by e. g. Asterix scripts for displaying incoming phone callers.
 * <p>
 * Is replaced by AddressServiceRest#phoneLookup
 *
 * @author Maximilian Lauterbach (m.lauterbach@micromata.de)
 */
@Deprecated
@WebServlet("/phoneLookup")
public class PhoneLookUpServlet extends HttpServlet {
  private static final long serialVersionUID = 8042634752943344080L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PhoneLookUpServlet.class);

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private ConfigurationService configService;

  private WebApplicationContext springContext;

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
    springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
    final AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
    beanFactory.autowireBean(this);
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
          throws ServletException, IOException {
    log.info("From: " + req.getRemoteAddr() + ", request-URL: " + ((HttpServletRequest) req).getRequestURL());
    final String key = req.getParameter("key");
    final String expectedKey = configService.getPhoneLookupKey();
    if (StringUtils.isBlank(expectedKey) || !expectedKey.equals(key)) {
      log.warn("Servlet call for receiving phone lookups ignored because phoneLookupKey is not given or doesn't match the configured one in file projectforge.properties (projectforge.phoneLookupKey).");
      resp.sendError(HttpStatus.SC_FORBIDDEN);
      return;
    }

    final String number = req.getParameter("nr");
    if (StringUtils.isBlank(number) || !StringUtils.containsOnly(number, "+1234567890 -/")) {
      log.warn("Bad request, request parameter nr not given or contains invalid characters (only +0123456789 -/ are allowed): "
              + number);
      resp.sendError(HttpStatus.SC_BAD_REQUEST);
      return;
    }

    String result = addressDao.internalPhoneLookUp(number);
    resp.setContentType("text/plain");
    if (result != null) {
      resp.getOutputStream().print(result);
    } else {
      // 0 - unknown, no entry found.
      resp.getOutputStream().print(0);
    }
  }
}
