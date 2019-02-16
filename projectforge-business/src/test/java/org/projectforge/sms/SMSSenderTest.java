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

package org.projectforge.sms;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class SMSSenderTest {

  @Test
  public void test() throws Exception {
    Assert.assertEquals(SMSSender.HttpResponseCode.SUCCESS,
            testGetCall("hurzel", "0123456", "Hello_world", 200, "OK. Perfect."));
    Assert.assertEquals(SMSSender.HttpResponseCode.UNKNOWN_ERROR,
            testGetCall("hurzel", "0123456", "Hello_world", 200, "ERROR"));
    Assert.assertEquals(SMSSender.HttpResponseCode.MESSAGE_TO_LARGE,
            testGetCall("hurzel", "0123456", "Hello_world", 100, "MESSAGE TO LARGE"));

    Assert.assertEquals(SMSSender.HttpResponseCode.UNKNOWN_ERROR,
            testPostCall("hurzel", "0123456", "Hello_world", 100, "Ingore this message."));
  }

  private SMSSender.HttpResponseCode testPostCall(String url, String phoneNumber, String message, int fakedReturnCode, String fakedResponseString) throws Exception {
    PostMethod httpMethod = spy(new PostMethod(url));
    SMSSender sender = createSender(url, httpMethod, fakedReturnCode, fakedResponseString);
    SMSSender.HttpResponseCode responseCode = sender.send(phoneNumber, message);
    Assert.assertEquals("smsUser", httpMethod.getParameter("user").getValue());
    Assert.assertEquals("smsPassword", httpMethod.getParameter("password").getValue());
    Assert.assertEquals(message, httpMethod.getParameter("message").getValue());
    Assert.assertEquals(phoneNumber, httpMethod.getParameter("to").getValue());
    verify(sender).createHttpMethod(url);
    return responseCode;
  }

  private SMSSender.HttpResponseCode testGetCall(String url, String phoneNumber, String message, int fakedReturnCode, String fakedResponseString) throws Exception {
    GetMethod httpMethod = spy(new GetMethod(url));
    SMSSender sender = createSender(url, httpMethod, fakedReturnCode, fakedResponseString);
    SMSSender.HttpResponseCode responseCode = sender.send(phoneNumber, message);
    String queryString = httpMethod.getQueryString();
    assertContains(queryString, "user=smsUser");
    assertContains(queryString, "password=smsPassword");
    assertContains(queryString, "message=" + message);
    assertContains(queryString, "to=" + phoneNumber);
    verify(sender).createHttpMethod(url);
    return responseCode;
  }

  private SMSSender createSender(String url, HttpMethod httpMethod, int fakedReturnCode, String fakedResponseString) throws Exception {
    Map<String, String> params = createParams("user", "smsUser", "password", "smsPassword", "message", "#message", "to", "#number");
    SMSSender sender = spy(new SMSSender(httpMethod instanceof GetMethod ? "get" : "post", url, params));
    sender.setSmsReturnPatternSuccess("^OK.*")
            .setSmsReturnPatternError("ERROR")
            .setSmsReturnPatternMessageError("MESSAGE ERROR")
            .setSmsReturnPatternMessageToLargeError(".*LARGE.*")
            .setSmsReturnPatternNumberError("NUMBER FORMAT ERROR");
    doReturn(httpMethod)
            .when(sender)
            .createHttpMethod(anyString());
    when(httpMethod.getResponseBodyAsString()).thenReturn(fakedResponseString);
    HttpClient httpClient = mock(HttpClient.class);
    doReturn(httpClient)
            .when(sender)
            .createHttpClient();
    when(httpClient.executeMethod(any(HttpMethod.class))).thenReturn(fakedReturnCode);
    return sender;
  }

  private void assertContains(String str, String part) {
    Assert.assertTrue(str.contains(part), "String '" + str + "' must contain '" + part + "'.");
  }

  private Map<String, String> createParams(String... params) {
    Assert.assertTrue(params.length % 2 == 0); // even number.
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < params.length; i += 2) {
      map.put(params[i], params[i + 1]);
    }
    return map;
  }
}
