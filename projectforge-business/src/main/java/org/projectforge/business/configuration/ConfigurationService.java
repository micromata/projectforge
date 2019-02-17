package org.projectforge.business.configuration;

import de.micromata.genome.util.runtime.config.MailSessionLocalSettingsConfigModel;
import org.projectforge.business.orga.ContractType;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.configuration.IConfigurationParam;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.mail.SendMailConfig;

import javax.net.ssl.SSLSocketFactory;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public interface ConfigurationService {
  Object[] getResourceContentAsString(String filename);

  Object[] getResourceAsInputStream(String filename);

  String getResourceDir();

  String getFontsDir();

  boolean isSendMailConfigured();

  SendMailConfig getSendMailConfiguration();

  String getTelephoneSystemUrl();

  boolean isTelephoneSystemUrlConfigured();

  List<ContractType> getContractTypes();

  boolean isSecurityConfigured();

  SecurityConfig getSecurityConfig();

  String getServletContextPath();

  String getLogoFile();

  String getDomain();

  String getPfBaseUrl();

  String getTelephoneSystemNumber();

  boolean isSmsConfigured();

  /**
   * Variables #number and #message will be replaced by the user's form input.
   *
   * @return The url to call the sms service.
   */
  String getSmsUrl();

  /**
   * @return POST or GET (default).
   */
  String getSmsHttpMethod();

  /**
   * Variables #number and #message will be replaced by the user's form input.
   *
   * @return Optional parameters for sms service (user, password.
   */
  Map<String, String> getSmsHttpParameters();

  /**
   * @return The pattern of the response string for successful service calls.
   */
  String getSmsReturnPatternSuccess();

  /**
   * @return The maximum length of a message (default is 160).
   */
  int getSmsMaxMessageLength();

  /**
   * @return The pattern of the response string for service calls with error in phone number (receiver).
   */
  String getSmsReturnPatternNumberError();

  /**
   * @return The pattern of the response string for service calls with error in message to send.
   */
  String getSmsReturnPatternMessageError();

  /**
   * @return The pattern of the response string for service calls with error caused by a to large message to send.
   */
  String getSmsReturnPatternMessageToLargeError();

  /**
   * @return The pattern of the response string for service calls with errors.
   */
  String getSmsReturnPatternError();

  String getReceiveSmsKey();

  String getPhoneLookupKey();

  String getKeystoreFile();

  SSLSocketFactory getUsersSSLSocketFactory();

  boolean isMebMailAccountConfigured();

  boolean isMultiTenancyConfigured();

  Object getDaoValue(final IConfigurationParam parameter, final ConfigurationDO configurationDO);

  List<ConfigurationDO> daoInternalLoadAll();

  List<ConfigurationDO> daoInternalLoadAll(TenantDO tenant);

  TimeZone getTimezone();

  String getPfSupportMailAddress();

  MailSessionLocalSettingsConfigModel createMailSessionLocalSettingsConfigModel();

  boolean isSqlConsoleAvailable();

  String getApplicationHomeDir();

  boolean getCompileCss();

  String getLoginHandlerClass();

  String getTeamCalCryptPassword();

  Calendar getEndDateVacationFromLastYear();

  String getHREmailadress();

  TeamCalDO getVacationCalendar();

  int getMinPasswordLength();

  boolean getFlagCheckPasswordChange();

  String getMaxFileSizeImage();

  String getMaxFileSizeDatev();

  String getMaxFileSizeXmlDumpImport();

  boolean isSnowEffectEnabled();
}
