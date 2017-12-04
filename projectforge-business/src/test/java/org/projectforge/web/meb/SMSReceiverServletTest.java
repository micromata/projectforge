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

package org.projectforge.web.meb;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.meb.MebDao;
import org.projectforge.business.meb.MebEntryDO;
import org.projectforge.common.TestHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.web.servlet.SMSReceiverServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class SMSReceiverServletTest extends AbstractTestBase
{
  private static Logger loggerSpy;

  @Autowired
  private MebDao mebDao;

  @Autowired
  SMSReceiverServlet servlet;

  @Autowired
  private ConfigurationService configService;

  @Test
  public void receiveSMS() throws Exception
  {
    final String origKey = (String) TestHelper.getDeclaredFieldValue(configService, "receiveSmsKey");
    TestHelper.setDeclaredField(configService, "receiveSmsKey", "otieZae9Aiphai5o");
    init();
    logon(TEST_ADMIN_USER);
    PFUserDO user = new PFUserDO();
    user.setUsername("MebTestUser");
    user.setPersonalMebMobileNumbers("(0170) 12345678, 0170/987654");
    user = userService.getById(userService.save(user));

    reset(loggerSpy);
    HttpServletRequest request = mockRequest("otieZae9Aiphai5o", "20100521172833", "0170 12345678", "Hello world.");
    final HttpServletResponse response = mockResponse("");
    servlet.doGet(request, response);
    verify(loggerSpy, never()).warn(anyString());
    List<MebEntryDO> list = mebDao.internalLoadAll();
    assertEquals(1, list.size());
    assertEquals(user.getId(), list.get(0).getOwnerId());
    servlet.doGet(request, response);
    assertEquals(1, mebDao.internalLoadAll().size());
    request = mockRequest("otieZae9Aiphai5o", "20100521172833", "0170 987654", "Hello world.");
    servlet.doGet(request, response);
    request = mockRequest("otieZae9Aiphai5o", "20100521172833", "034567890", "Unknown sender.");
    servlet.doGet(request, response);
    list = mebDao.internalLoadAll();
    assertEquals(3, list.size());
    for (final MebEntryDO entry : list) {
      if (entry.getMessage().equals("Hello world.") == true) {
        assertEquals(user.getId(), entry.getOwnerId());
      } else {
        assertNull(entry.getOwnerId());
      }
    }
    TestHelper.setDeclaredField(configService, "receiveSmsKey", origKey);
  }

  @Test
  public void receiveSMSWithWrongRequest() throws Exception
  {
    init();
    final String origKey = (String) TestHelper.getDeclaredFieldValue(configService, "receiveSmsKey");
    TestHelper.setDeclaredField(configService, "receiveSmsKey", null);
    final Logger mebDaoLoggerSpy = spy(LoggerFactory.getLogger(MebDao.class));
    TestHelper.setDeclaredStaticField(MebDao.class, "log", mebDaoLoggerSpy);
    HttpServletRequest request = mockRequest("wrongKey", null, null, null);
    final HttpServletResponse response = mockResponse("");
    servlet.doGet(request, response);

    TestHelper.setDeclaredField(configService, "receiveSmsKey", "otieZae9Aiphai5o");
    servlet.doGet(request, response);
    request = mockRequest("otieZae9Aiphai5o", null, null, null);
    servlet.doGet(request, response);
    request = mockRequest("otieZae9Aiphai5o", "date", null, null);
    servlet.doGet(request, response);
    request = mockRequest("otieZae9Aiphai5o", "date", "0170m123456", null);
    servlet.doGet(request, response);
    request = mockRequest("otieZae9Aiphai5o", "date", "0170m123456", "Hello");
    servlet.doGet(request, response);
    request = mockRequest("otieZae9Aiphai5o", "1274480915", "0170m123456", "Hello");
    servlet.doGet(request, response);
    request = mockRequest("otieZae9Aiphai5o", "3000000000", "0170m123456", "Hello");
    servlet.doGet(request, response);
    request = mockRequest("otieZae9Aiphai5o", "20sjhj4567", "0170m123456", "Hello");
    servlet.doGet(request, response);
    verify(loggerSpy)
        .warn("Servlet call for receiving sms ignored because receiveSmsKey is not given in config.xml file.");
    verify(loggerSpy)
        .warn("Servlet call for receiving sms ignored because receiveSmsKey does not match given key: wrongKey");
    verify(loggerSpy).warn("Servlet call for receiving sms ignored because parameter 'date' is not given.");
    verify(loggerSpy).warn("Servlet call for receiving sms ignored because parameter 'sender' is not given.");
    verify(loggerSpy).warn("Servlet call for receiving sms ignored because parameter 'msg' is not given.");
    final String logMsg1 = "Servlet call for receiving sms ignored because date string is not parseable (format '"
        + MebDao.DATE_FORMAT
        + "' expected): ";
    final String logMsg2 = "Servlet call for receiving sms ignored because date string is not parseable (millis since 01/01/1970 or format '"
        + MebDao.DATE_FORMAT
        + "' expected): ";
    final String logMsg3 = "Servlet call for receiving sms ignored because date string is not parseable (format '"
        + MebDao.DATE_FORMAT
        + "' expected): ";
    verify(mebDaoLoggerSpy).warn(logMsg1 + "date");
    verify(mebDaoLoggerSpy).warn(logMsg2 + "1274480915");
    verify(mebDaoLoggerSpy).warn(logMsg2 + "3000000000");
    verify(mebDaoLoggerSpy).warn(logMsg3 + "20sjhj4567");
    TestHelper.setDeclaredField(configService, "receiveSmsKey", origKey);
  }

  private HttpServletRequest mockRequest(final String key, final String date, final String sender, final String msg)
  {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("key")).thenReturn(key);
    when(request.getParameter("date")).thenReturn(date);
    when(request.getParameter("sender")).thenReturn(sender);
    when(request.getParameter("msg")).thenReturn(msg);
    return request;
  }

  private HttpServletResponse mockResponse(final String result)
  {
    final HttpServletResponse response = mock(HttpServletResponse.class);
    try {
      when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
    return response;
  }

  public void init()
  {
    if (loggerSpy == null) {
      loggerSpy = spy(LoggerFactory.getLogger(SMSReceiverServlet.class));
      TestHelper.setDeclaredField(configService, "receiveSmsKey", "otieZae9Aiphai5o");
      TestHelper.setDeclaredStaticField(SMSReceiverServlet.class, "log", loggerSpy);
    }
  }
}
