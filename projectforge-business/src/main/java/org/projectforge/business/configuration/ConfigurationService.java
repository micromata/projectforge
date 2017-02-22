package org.projectforge.business.configuration;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.net.ssl.SSLSocketFactory;

import org.projectforge.business.orga.ContractType;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.configuration.IConfigurationParam;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.mail.SendMailConfig;

import de.micromata.genome.util.runtime.config.MailSessionLocalSettingsConfigModel;

public interface ConfigurationService
{
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

  String getSmsUrl();

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
}
