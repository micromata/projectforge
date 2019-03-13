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

package org.projectforge.messaging;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.projectforge.sms.SmsSenderConfig;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class SmsSenderTest {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SmsSenderTest.class);

  @Test
  public void testSmsService() throws Exception {
    Assert.assertEquals(SmsSender.HttpResponseCode.SUCCESS,
            testGetCall("hurzel", "0123456", "Hello_world", 200, "OK. Perfect."));
    Assert.assertEquals(SmsSender.HttpResponseCode.UNKNOWN_ERROR,
            testGetCall("hurzel", "0123456", "Hello_world", 200, "ERROR"));
    Assert.assertEquals(SmsSender.HttpResponseCode.MESSAGE_TO_LARGE,
            testGetCall("hurzel", "0123456", "Hello_world", 200, "MESSAGE TO LARGE"));
    Assert.assertEquals(SmsSender.HttpResponseCode.UNKNOWN_ERROR,
            testPostCall("hurzel", "0123456", "Hello_world", 100, "Ingore this message."));
  }

  /**
   * Configuration of live test in ~/ProjectForge/smsTestConfig.yaml
   * <pre>
   *   projectforge:
   *     sms:
   *         httpMethod: POST
   *         httpParameters:
   *             mode: 'number'
   *             password: 'mypassword'
   *             text: '#message'
   *             to: '#number'
   *             username: 'projectforge'
   *         returnCodePattern:
   *             error: ''
   *             messageError: ''
   *             messageToLargeError: ''
   *             numberError: ''
   *             success: ''
   *         url: 'http://smsgateway.acme.com/api.php'
   *         testNumber: '0123456789'
   * </pre>
   *
   * @throws IOException
   */
  @Test
  public void liveTest() throws IOException {
    // ~/ProjectForge/smsTestConfig.yaml
    File configFile = new File(System.getProperty("user.home"), "ProjectForge" + File.separatorChar + "smsTestConfig.yaml");
    if (!configFile.exists()) {
      log.info("Skipping live testing of sms service (OK). For live testing, please refer this code for the required config file: " + configFile.getAbsolutePath());
      return;
    }
    Yaml yaml = new Yaml();
    Map<String, Object> map = yaml.load(new FileReader(configFile));
    String httpMethodType = (String) getFromMap(map, "projectforge", "sms", "httpMethod");
    String url = (String) getFromMap(map, "projectforge", "sms", "url");
    String testNumber = (String) getFromMap(map, "projectforge", "sms", "testNumber");
    Map<String, String> httpParams = (Map<String, String>) getFromMap(map, "projectforge", "sms", "httpParameters");
    SmsSenderConfig config = new SmsSenderConfig();
    config.setHttpMethodType(httpMethodType).setUrl(url);
    config.setHttpParams(httpParams);
    config.setSmsMaxMessageLength((int) getFromMap(map, "projectforge", "sms", "smsMaxMessageLength"));
    config.setSmsReturnPatternSuccess((String) getFromMap(map, "projectforge", "sms", "returnCodePattern", "success"));
    config.setSmsReturnPatternNumberError((String) getFromMap(map, "projectforge", "sms", "returnCodePattern", "numberError"));
    config.setSmsReturnPatternMessageToLargeError((String) getFromMap(map, "projectforge", "sms", "returnCodePattern", "messageToLargeError"));
    config.setSmsReturnPatternMessageError((String) getFromMap(map, "projectforge", "sms", "returnCodePattern", "messageError"));
    config.setSmsReturnPatternError((String) getFromMap(map, "projectforge", "sms", "returnCodePattern", "error"));
    SmsSender sender = new SmsSender(config);
    sender.send(testNumber, "Hello world! With love by ProjectForge: " + new Date());
  }

  // Helper for getting variables from YAML object map.
  private Object getFromMap(Map<String, Object> map, String... keys) {
    for (String key : keys) {
      Object obj = map.get(key);
      if (obj instanceof Map) {
        map = (Map) obj;
      } else {
        return obj;
      }
    }
    return map;
  }

  private SmsSender.HttpResponseCode testPostCall(String url, String phoneNumber, String message, int fakedReturnCode, String fakedResponseString) throws Exception {
    PostMethod httpMethod = spy(new PostMethod(url));
    SmsSender sender = createSender(url, httpMethod, fakedReturnCode, fakedResponseString);
    SmsSender.HttpResponseCode responseCode = sender.send(phoneNumber, message);
    Assert.assertEquals("smsUser", httpMethod.getParameter("user").getValue());
    Assert.assertEquals("smsPassword", httpMethod.getParameter("password").getValue());
    Assert.assertEquals(message, httpMethod.getParameter("message").getValue());
    Assert.assertEquals(phoneNumber, httpMethod.getParameter("to").getValue());
    verify(sender).createHttpMethod(url);
    return responseCode;
  }

  private SmsSender.HttpResponseCode testGetCall(String url, String phoneNumber, String message, int fakedReturnCode, String fakedResponseString) throws Exception {
    GetMethod httpMethod = spy(new GetMethod(url));
    SmsSender sender = createSender(url, httpMethod, fakedReturnCode, fakedResponseString);
    SmsSender.HttpResponseCode responseCode = sender.send(phoneNumber, message);
    String queryString = httpMethod.getQueryString();
    assertContains(queryString, "user=smsUser");
    assertContains(queryString, "password=smsPassword");
    assertContains(queryString, "message=" + message);
    assertContains(queryString, "to=" + phoneNumber);
    verify(sender).createHttpMethod(url);
    return responseCode;
  }

  private SmsSender createSender(String url, HttpMethod httpMethod, int fakedReturnCode, String fakedResponseString) throws Exception {
    Map<String, String> params = createParams("user", "smsUser", "password", "smsPassword", "message", "#message", "to", "#number");
    SmsSenderConfig config = new SmsSenderConfig();
    config.setHttpMethodType(httpMethod instanceof GetMethod ? "get" : "post")
            .setUrl(url);
    config.setHttpParams(params);
    config.setSmsReturnPatternSuccess("^OK.*");
    config.setSmsReturnPatternError("ERROR");
    config.setSmsReturnPatternMessageError("MESSAGE ERROR");
    config.setSmsReturnPatternMessageToLargeError(".*LARGE.*");
    config.setSmsReturnPatternNumberError("NUMBER FORMAT ERROR");
    SmsSender sender = spy(new SmsSender(config));
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
