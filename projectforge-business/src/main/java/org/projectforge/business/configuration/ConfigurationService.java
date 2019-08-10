/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.configuration;

import de.micromata.genome.util.runtime.config.MailSessionLocalSettingsConfigModel;
import org.projectforge.business.orga.ContractType;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.configuration.IConfigurationParam;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.time.TimeNotation;
import org.projectforge.mail.SendMailConfig;

import javax.net.ssl.SSLSocketFactory;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
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

  String getLogoFile();

  String getCurrencySymbol();

  String getTelephoneSystemNumber();

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

  /**
   * The default time notation (12-hour or 24-hour). This notation is used, if the user has not chosen his personal time
   * notation. Default is 24-hour for locales starting with "de" (German), otherwise 12-hour.
   */
  TimeNotation getDefaultTimeNotation();

  /**
   * The default first day of week (1 - Sunday, 2 - Monday, ...)
   *
   * @return the firstDayOfWeek
   */
  int getDefaultFirstDayOfWeek();


  String getPfSupportMailAddress();

  MailSessionLocalSettingsConfigModel createMailSessionLocalSettingsConfigModel();

  boolean isSqlConsoleAvailable();

  String getApplicationHomeDir();

  boolean getCompileCss();

  /**
   * The default locale is currently used for getting the week of year in Calendar.
   */
  Locale getDefaultLocale();

  /**
   * The paper size for excel exports.
   */
  String getExcelPaperSize();

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
