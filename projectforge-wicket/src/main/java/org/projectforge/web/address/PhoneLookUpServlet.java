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

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Assign a phone number to a full name and organization using ProjectForge address database. <br/>
 * This servlet may be used by e. g. Asterix scripts for displaying incoming phone callers.
 *
 * @author Maximilian Lauterbach (m.lauterbach@micromata.de)
 */
@WebServlet("/phoneLookup")
public class PhoneLookUpServlet extends HttpServlet
{
  private static final long serialVersionUID = 8042634752943344080L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PhoneLookUpServlet.class);

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private ConfigurationService configService;

  private WebApplicationContext springContext;

  @Override
  public void init(final ServletConfig config) throws ServletException
  {
    super.init(config);
    springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
    final AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
    beanFactory.autowireBean(this);
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException
  {
    final String number = req.getParameter("nr");
    if (StringUtils.isBlank(number) == true || StringUtils.containsOnly(number, "+1234567890 -/") == false) {
      log.warn(
          "Bad request, request parameter nr not given or contains invalid characters (only +0123456789 -/ are allowed): "
              + number);
      resp.sendError(HttpStatus.SC_BAD_REQUEST);
      return;
    }

    final String key = req.getParameter("key");
    final String expectedKey = configService.getPhoneLookupKey();
    if (StringUtils.isBlank(expectedKey) == true) {
      log.warn(
          "Servlet call for receiving phonelookups ignored because phoneLookupKey is not given in config.xml file.");
      resp.sendError(HttpStatus.SC_BAD_REQUEST);
      return;
    }
    if (expectedKey.equals(key) == false) {
      log.warn("Servlet call for phonelookups ignored because phoneLookupKey does not match given key: " + key);
      resp.sendError(HttpStatus.SC_FORBIDDEN);
      return;
    }

    final String searchNumber = NumberHelper.extractPhonenumber(number);

    final BaseSearchFilter filter = new BaseSearchFilter();
    filter.setSearchString("*" + searchNumber);
    final QueryFilter queryFilter = new QueryFilter(filter);

    final StringBuffer buf = new StringBuffer();
    // Use internal get list method for avoiding access checking (no user is logged-in):
    final List<AddressDO> list = addressDao.internalGetList(queryFilter);
    if (list != null && list.size() >= 1) {
      AddressDO result = list.get(0);
      if (list.size() > 1) {
        // More than one result, therefore find the newest one:
        buf.append("+"); // Mark that more than one entry does exist.
        for (final AddressDO matchingUser : list) {
          if (matchingUser.getLastUpdate().after(result.getLastUpdate()) == true) {
            result = matchingUser;
          }
        }
      }
      resp.setContentType("text/plain");
      final String fullname = result.getFullName();
      final String organization = result.getOrganization();
      StringHelper.listToString(buf, "; ", fullname, organization);
      resp.getOutputStream().print(buf.toString());
    } else {
      /* mit Thomas abgesprochen. */
      resp.getOutputStream().print(0);
    }
  }
}
