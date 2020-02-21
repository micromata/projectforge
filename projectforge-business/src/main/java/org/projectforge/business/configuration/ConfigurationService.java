/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import java.time.DayOfWeek;
import java.time.LocalDate;
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
   * @return the firstDayOfWeek
   */
  DayOfWeek getDefaultFirstDayOfWeek();


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

  /**
   * 31.03. of this year if today is after 31.03, otherwise 31.03. of last year.
   * Example (if 31.03. is configured):
   * <ul>
   * <li>Today = 02.01.2020 then this method returns 31.03.2019.</li>
   * <li>Today = 31.12.2019 then this method returns 31.03.2019 as well.</li>
   * </ul>
   */
  LocalDate getEndDateVacationFromLastYear();

  /**
   * 31.03. of the given year, if not configured different. This date determine when vacation days of an employee
   * from the last year will be invalid, if not used.
   */
  LocalDate getEndOfCarryVacationOfPreviousYear(int year);

  String getHREmailadress();

  TeamCalDO getVacationCalendar();

  int getMinPasswordLength();

  boolean getFlagCheckPasswordChange();

  String getMaxFileSizeImage();

  String getMaxFileSizeDatev();

  String getMaxFileSizeXmlDumpImport();

  boolean isDAVServicesAvailable();

  void setDAVServicesAvailable(boolean dAVServicesAvailable);
}
