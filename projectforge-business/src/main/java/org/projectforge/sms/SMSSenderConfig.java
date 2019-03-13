package org.projectforge.sms;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;

@Configuration
public class SMSSenderConfig {

  public enum HttpMethodType {POST, GET}

  @Value("${projectforge.sms.httpMethod}")
  private HttpMethodType httpMethodType;
  @Value("${projectforge.sms.url}")
  private String url;
  @Value("#{${projectforge.sms.httpParameters}}")
  private Map<String, String> httpParams;
  @Value("${projectforge.sms.returnCodePattern.success}")
  private String smsReturnPatternSuccess;
  @Value("${projectforge.sms.returnCodePattern.numberError}")
  private String smsReturnPatternNumberError;
  @Value("${projectforge.sms.returnCodePattern.messageToLargeError}")
  private String smsReturnPatternMessageToLargeError;
  @Value("${projectforge.sms.returnCodePattern.messageError}")
  private String smsReturnPatternMessageError;
  @Value("${projectforge.sms.returnCodePattern.error}")
  private String smsReturnPatternError;
  @Value("${projectforge.sms.smsMaxMessageLength}")
  private int smsMaxMessageLength = 160;

  public String getSmsReturnPatternSuccess() {
    return smsReturnPatternSuccess;
  }

  public SMSSenderConfig setSmsReturnPatternSuccess(String smsReturnPatternSuccess) {
    this.smsReturnPatternSuccess = smsReturnPatternSuccess;
    return this;
  }

  public String getSmsReturnPatternError() {
    return smsReturnPatternError;
  }

  public SMSSenderConfig setSmsReturnPatternError(String smsReturnPatternError) {
    this.smsReturnPatternError = smsReturnPatternError;
    return this;
  }

  public String getSmsReturnPatternMessageError() {
    return smsReturnPatternMessageError;
  }

  public SMSSenderConfig setSmsReturnPatternMessageError(String smsReturnPatternMessageError) {
    this.smsReturnPatternMessageError = smsReturnPatternMessageError;
    return this;
  }

  public String getSmsReturnPatternMessageToLargeError() {
    return smsReturnPatternMessageToLargeError;
  }

  public SMSSenderConfig setSmsReturnPatternMessageToLargeError(String smsReturnPatternMessageToLargeError) {
    this.smsReturnPatternMessageToLargeError = smsReturnPatternMessageToLargeError;
    return this;
  }

  public String getSmsReturnPatternNumberError() {
    return smsReturnPatternNumberError;
  }

  public SMSSenderConfig setSmsReturnPatternNumberError(String smsReturnPatternNumberError) {
    this.smsReturnPatternNumberError = smsReturnPatternNumberError;
    return this;
  }

  public HttpMethodType getHttpMethodType() {
    return httpMethodType;
  }

  public SMSSenderConfig setHttpMethodType(HttpMethodType httpMethodType) {
    this.httpMethodType = httpMethodType;
    return this;
  }

  public SMSSenderConfig setHttpMethodType(String httpMethodType) {
    this.httpMethodType = StringUtils.equalsIgnoreCase("get", httpMethodType) ? HttpMethodType.GET : HttpMethodType.POST;
    return this;
  }

  public int getSmsMaxMessageLength() {
    return smsMaxMessageLength;
  }

  public SMSSenderConfig setSmsMaxMessageLength(int smsMaxMessageLength) {
    this.smsMaxMessageLength = smsMaxMessageLength;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public SMSSenderConfig setUrl(String url) {
    this.url = url;
    return this;
  }

  public Map<String, String> getHttpParams() {
    return httpParams;
  }

  public SMSSenderConfig setHttpParams(Map<String, String> httpParams) {
    this.httpParams = httpParams;
    return this;
  }

  public boolean isSmsConfigured() {
    return StringUtils.isNotBlank(url);
  }
}
