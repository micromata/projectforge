package org.projectforge.sms;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.StringHelper;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class SMSSender {
  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SMSSender.class);

  public enum HttpResponseCode {SUCCESS, NUMBER_ERROR, MESSAGE_TO_LARGE, MESSAGE_ERROR, UNKNOWN_ERROR}

  private SMSSenderConfig config;

  public SMSSender(SMSSenderConfig config) {
    this.config = config;
  }

  /**
   * Variables #message and #number will be replaced in url as well as in parameter values.
   *
   * @return
   */
  public HttpResponseCode send(String phoneNumber, String message) {
    if (message == null) {
      log.error("Failed to send message to destination number: '" + StringHelper.hideStringEnding(phoneNumber, 'x', 3)
              + ". Message is null!");
      return HttpResponseCode.MESSAGE_ERROR;
    }
    if (message.length() > config.getSmsMaxMessageLength()) {
      log.error("Failed to send message to destination number: '" + StringHelper.hideStringEnding(phoneNumber, 'x', 3)
              + ". Message is to large, max length is " + config.getSmsMaxMessageLength() + ", but current message size is " + message.length());
      return HttpResponseCode.MESSAGE_TO_LARGE;
    }
    String proceededUrl = replaceVariables(config.getUrl(), phoneNumber, message, true);
    HttpMethodBase method = createHttpMethod(proceededUrl);
    if (config.getHttpMethodType() == SMSSenderConfig.HttpMethodType.GET) {
      if (MapUtils.isNotEmpty(config.getHttpParams())) {
        // Now build the query params list from the configured httpParams:
        NameValuePair[] params = new NameValuePair[config.getHttpParams().size()];
        int index = 0;
        for (Map.Entry<String, String> entry : config.getHttpParams().entrySet()) {
          String value = replaceVariables(entry.getValue(), phoneNumber, message, true);
          params[index++] = new NameValuePair(entry.getKey(), value);
        }
        ((GetMethod) method).setQueryString(params);
      }
    } else { // HTTP POST
      if (MapUtils.isNotEmpty(config.getHttpParams())) {
        // Now add all post params from the configured httpParams:
        for (Map.Entry<String, String> entry : config.getHttpParams().entrySet()) {
          String value = replaceVariables(entry.getValue(), phoneNumber, message, false);
          ((PostMethod) method).addParameter(entry.getKey(), value);
        }
      }
    }
    final HttpClient client = createHttpClient();
    try {
      int responseNumber = client.executeMethod(method);
      final String response = method.getResponseBodyAsString();
      log.info("Tried to send message to destination number: '" + StringHelper.hideStringEnding(phoneNumber, 'x', 3)
              + ". Response from service: " + response);
      HttpResponseCode responseCode = null;
      if (response == null || responseNumber != 200) {
        responseCode = HttpResponseCode.UNKNOWN_ERROR;
      } else if (matches(response, config.getSmsReturnPatternNumberError()) == true) {
        responseCode = HttpResponseCode.NUMBER_ERROR;
      } else if (matches(response, config.getSmsReturnPatternMessageToLargeError()) == true) {
        responseCode = HttpResponseCode.MESSAGE_TO_LARGE;
      } else if (matches(response, config.getSmsReturnPatternMessageError()) == true) {
        responseCode = HttpResponseCode.MESSAGE_ERROR;
      } else if (matches(response, config.getSmsReturnPatternError()) == true) {
        responseCode = HttpResponseCode.UNKNOWN_ERROR;
      } else if (matches(response, config.getSmsReturnPatternSuccess()) == true) {
        responseCode = HttpResponseCode.SUCCESS;
      } else {
        responseCode = HttpResponseCode.UNKNOWN_ERROR;
      }
      if (responseCode != HttpResponseCode.SUCCESS) {
        log.error("Unexpected response from sms gateway: " + responseNumber + ": " + response + " (if this call was successful, did you configured projectforge.sms.returnCodePattern.success?).");
      }
      return responseCode;
    } catch (final HttpException ex) {
      String errorKey = "Call failed. Please contact administrator.";
      log.error(errorKey + ": " + proceededUrl
              + StringHelper.hideStringEnding(String.valueOf(phoneNumber), 'x', 3));
      throw new RuntimeException(ex);
    } catch (final IOException ex) {
      String errorKey = "Call failed. Please contact administrator.";
      log.error(errorKey + ": " + proceededUrl
              + StringHelper.hideStringEnding(String.valueOf(phoneNumber), 'x', 3));
      throw new RuntimeException(ex);
    } finally {
      method.releaseConnection();
    }
  }

  private boolean matches(String str, String regexp) {
    if (regexp == null || str == null) {
      return false;
    }
    return str.matches(regexp);
  }

  /**
   * Variables #number and #message will be replaced by the user's form input.
   *
   * @param str    The string to proceed.
   * @param number The extracted phone number (already preprocessed...)
   * @return The given str with replaced vars (if exists).
   */
  private String replaceVariables(String str, String number, String message, boolean urlEncode) {
    if (number == null) return "";
    str = StringUtils.replaceOnce(str, "#number", urlEncode ? encode(number) : number);
    str = StringUtils.replaceOnce(str, "#message", urlEncode ? encode(message) : message);
    return str;
  }

  /**
   * Uses UTF-8
   *
   * @param str
   * @see URLEncoder#encode(String, String)
   */
  static String encode(final String str) {
    if (str == null) {
      return "";
    }
    try {
      return URLEncoder.encode(str, "UTF-8");
    } catch (final UnsupportedEncodingException ex) {
      log.info("Can't URL-encode '" + str + "': " + ex.getMessage());
      return "";
    }
  }

  /**
   * Used also for mocking {@link GetMethod} and {@link PostMethod}.
   *
   * @param url
   * @return
   */
  protected HttpMethodBase createHttpMethod(String url) {
    if (config.getHttpMethodType() == SMSSenderConfig.HttpMethodType.GET) {
      return new GetMethod(url);
    }
    return new PostMethod(url);
  }

  protected HttpClient createHttpClient() {
    return new HttpClient();
  }

  public SMSSender setConfig(SMSSenderConfig config) {
    this.config = config;
    return this;
  }
}
