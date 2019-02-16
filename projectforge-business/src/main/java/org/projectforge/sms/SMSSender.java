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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class SMSSender {
  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SMSSender.class);

  public enum HttpMethodType {POST, GET}

  public enum HttpResponseCode {SUCCESS, NUMBER_ERROR, MESSAGE_TO_LARGE, MESSAGE_ERROR, UNKNOWN_ERROR}

  private HttpMethodType httpMethodType;
  private String url;
  private Map<String, String> httpParams;
  private String smsReturnPatternSuccess;
  private String smsReturnPatternNumberError;
  private String smsReturnPatternMessageToLargeError;
  private String smsReturnPatternMessageError;
  private String smsReturnPatternError;

  /**
   * @param httpMethodType Uses {@link HttpMethodType#GET} for String "get" (ignore case), otherwise {@link HttpMethodType#POST}.
   * @param url
   * @param httpParams
   */
  public SMSSender(String httpMethodType, String url, Map<String, String> httpParams) {
    this.httpMethodType = StringUtils.equalsIgnoreCase("get", httpMethodType) ? HttpMethodType.GET : HttpMethodType.POST;
    this.url = url;
    this.httpParams = httpParams;
  }

  /**
   * Variables #message and #number will be replaced in url as well as in parameter values.
   *
   * @return
   */
  public HttpResponseCode send(String phoneNumber, String message) {
    String proceededUrl = replaceVariables(this.url, phoneNumber, message, true);
    HttpMethodBase method = createHttpMethod(proceededUrl);
    if (httpMethodType == HttpMethodType.GET) {
      if (MapUtils.isNotEmpty(httpParams)) {
        NameValuePair[] params = new NameValuePair[httpParams.size()];
        int index = 0;
        for (Map.Entry<String, String> entry : httpParams.entrySet()) {
          String value = replaceVariables(entry.getValue(), phoneNumber, message, true);
          params[index++] = new NameValuePair(entry.getKey(), value);
        }
        ((GetMethod) method).setQueryString(params);
      }
    } else {
      if (MapUtils.isNotEmpty(httpParams)) {
        for (Map.Entry<String, String> entry : httpParams.entrySet()) {
          String value = replaceVariables(entry.getValue(), phoneNumber, message, false);
          ((PostMethod) method).addParameter(entry.getKey(), value);
        }
      }
    }
    final HttpClient client = createHttpClient();
    try {
      int responseCode = client.executeMethod(method);
      final String response = method.getResponseBodyAsString();
      if (response == null) {
        return HttpResponseCode.UNKNOWN_ERROR;
      } else if (matches(response, this.smsReturnPatternSuccess) == true) {
        return HttpResponseCode.SUCCESS;
      } else if (matches(response, this.smsReturnPatternNumberError) == true) {
        return HttpResponseCode.NUMBER_ERROR;
      } else if (matches(response, this.smsReturnPatternMessageToLargeError) == true) {
        return HttpResponseCode.MESSAGE_TO_LARGE;
      } else if (matches(response, this.smsReturnPatternMessageError) == true) {
        return HttpResponseCode.MESSAGE_ERROR;
      } else if (matches(response, this.smsReturnPatternError) == true) {
        return HttpResponseCode.UNKNOWN_ERROR;
      } else {
        if (responseCode != 200) {
          return HttpResponseCode.UNKNOWN_ERROR;
        }
        return HttpResponseCode.SUCCESS;
      }
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
    if (httpMethodType == HttpMethodType.GET) {
      return new GetMethod(url);
    }
    return new PostMethod(url);
  }

  protected HttpClient createHttpClient() {
    return new HttpClient();
  }

  public SMSSender setSmsReturnPatternSuccess(String smsReturnPatternSuccess) {
    this.smsReturnPatternSuccess = smsReturnPatternSuccess;
    return this;
  }

  public SMSSender setSmsReturnPatternError(String smsReturnPatternError) {
    this.smsReturnPatternError = smsReturnPatternError;
    return this;
  }

  public SMSSender setSmsReturnPatternMessageError(String smsReturnPatternMessageError) {
    this.smsReturnPatternMessageError = smsReturnPatternMessageError;
    return this;
  }

  public SMSSender setSmsReturnPatternMessageToLargeError(String smsReturnPatternMessageToLargeError) {
    this.smsReturnPatternMessageToLargeError = smsReturnPatternMessageToLargeError;
    return this;
  }

  public SMSSender setSmsReturnPatternNumberError(String smsReturnPatternNumberError) {
    this.smsReturnPatternNumberError = smsReturnPatternNumberError;
    return this;
  }
}
