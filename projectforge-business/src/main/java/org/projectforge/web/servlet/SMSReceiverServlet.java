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

package org.projectforge.web.servlet;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.meb.MebDao;
import org.projectforge.business.meb.MebEntryDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet which supports receiving SMS via http get:<br/>
 * https://www.projectforge.org/secure/SMSReceiver?key=5fcs2lp&date=20101105171233&sender=0170123456&msg=Hello... <br/>
 * Parameter should be given in Format yyyymmddhhMMss or as milliseconds since 1970/01/01.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@WebServlet("/secure/SMSReceiver")
public class SMSReceiverServlet extends HttpServlet
{
  public static final String URL = "secure/SMSReceiver";

  private static final long serialVersionUID = 5382567744977121735L;

  // Should not be final because this logger is replaced by a Mockito spy object in test class.
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SMSReceiverServlet.class);

  @Autowired
  private MebDao mebDao;

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
  public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException
  {
    log.debug("Start doPost");
    // https://projectforge.micromata.de/secure/SMSReceiver?key=<key>&date=20101105171233&sender=01701891142&msg=Hallo...
    req.setCharacterEncoding("UTF-8");
    final String key = req.getParameter("key");
    final String expectedKey = configService.getReceiveSmsKey();
    if (StringUtils.isBlank(expectedKey) == true) {
      log.warn("Servlet call for receiving sms ignored because receiveSmsKey is not given in config.xml file.");
      response(resp, "NOT YET CONFIGURED");
      return;
    }
    if (expectedKey.equals(key) == false) {
      log.warn("Servlet call for receiving sms ignored because receiveSmsKey does not match given key: " + key);
      response(resp, "DENIED");
      return;
    }
    final String dateString = req.getParameter("date");
    if (StringUtils.isBlank(dateString) == true) {
      log.warn("Servlet call for receiving sms ignored because parameter 'date' is not given.");
      response(resp, "Missing parameter 'date'.");
      return;
    }
    final String sender = req.getParameter("sender");
    if (StringUtils.isBlank(sender) == true) {
      log.warn("Servlet call for receiving sms ignored because parameter 'sender' is not given.");
      response(resp, "Missing parameter 'sender'.");
      return;
    }
    final String msg = req.getParameter("msg");
    if (StringUtils.isBlank(msg) == true) {
      log.warn("Servlet call for receiving sms ignored because parameter 'msg' is not given.");
      response(resp, "Missing parameter 'msg'.");
      return;
    }
    final Date date = MebDao.parseDate(dateString);
    if (date == null) {
      log.warn("Servlet call for receiving sms ignored because couln't parse parameter 'date'.");
      response(resp, "Unsupported date format.");
      return;
    }

    final MebEntryDO entry = new MebEntryDO();
    entry.setDate(date);
    entry.setSender(sender);
    entry.setMessage(msg);
    log.info("Servlet-call: date=[" + date + "], sender=[" + sender + "]");
    mebDao.checkAndAddEntry(entry, "SERVLET");
    response(resp, "OK");
  }

  private void response(final HttpServletResponse resp, final String result)
  {
    try {
      resp.getWriter().print(result);
    } catch (final IOException ex) {
      log.error("Exception encountered while writing servlet response: " + ex, ex);
    }
  }

}
