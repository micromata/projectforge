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

package org.projectforge.framework.configuration;

import java.util.TimeZone;

import org.projectforge.framework.configuration.entities.ConfigurationDO;

public enum ConfigurationParam implements IConfigurationParam
{
  // Global parameters:
  SYSTEM_ADMIN_E_MAIL("systemAdministratorEMail", ConfigurationType.STRING, true), //
  MESSAGE_OF_THE_DAY("messageOfTheDay", ConfigurationType.TEXT, true), //
  MULTI_TENANCY_ENABLED("admin.multiTenancyEnabled", ConfigurationType.BOOLEAN, true), //

  // Tenant specific parameters:
  CALENDAR_DOMAIN("calendarDomain", ConfigurationType.STRING, false), //
  ORGANIZATION("organization", ConfigurationType.TEXT, false), //
  DEFAULT_TIMEZONE("timezone", ConfigurationType.TIME_ZONE, false, TimeZone.getDefault().getID()), //
  DATE_FORMATS("dateFormats", ConfigurationType.STRING, false, "MM/dd/yyyy;dd/MM/yyyy;dd.MM.yyyy;yyyy-MM-dd"), //
  EXCEL_DATE_FORMATS("excelDateFormats", ConfigurationType.STRING, false, "MM/DD/YYYY;DD/MM/YYYY;DD.MM.YYYY"), //
  FEEDBACK_E_MAIL("feedbackEMail", ConfigurationType.STRING, false), //
  FIBU_DEFAULT_VAT("fibu.defaultVAT", ConfigurationType.PERCENT, false), //
  COST_CONFIGURED("fibu.costConfigured", ConfigurationType.BOOLEAN, false), //
  DEFAULT_TASK_ID_4_ADDRESSES("defaultTask4Addresses", ConfigurationType.TASK, false), //
  DEFAULT_TASK_ID_4_BOOKS("defaultTask4Books", ConfigurationType.TASK, false), //
  DEFAULT_COUNTRY_PHONE_PREFIX("countryPhonePrefix", ConfigurationType.STRING, false, "+49"), //
  MEB_SMS_RECEIVING_PHONE_NUMBER("mebSMSReceivingPhoneNumber", ConfigurationType.STRING, false), //
  PLUGIN_ACTIVATED("pluginsActivated", ConfigurationType.STRING, true), //
  HR_MAILADDRESS("hr.emailaddress", ConfigurationType.STRING, true),//
  VACATION_CAL_ID("vacation.cal.id", ConfigurationType.CALENDAR, true),//
  END_DATE_VACATION_LASTR_YEAR("vacation.lastyear.enddate", ConfigurationType.STRING, true, "31.03.");

  private String key;

  private ConfigurationType type;

  private String defaultStringValue;

  private boolean global;

  /**
   * The key will be used e. g. for i18n.
   *
   * @return
   */
  @Override
  public String getKey()
  {
    return key;
  }

  @Override
  public ConfigurationType getType()
  {
    return type;
  }

  /**
   * @return The full i18n key including the i18n prefix "administration.configuration.param.".
   */
  @Override
  public String getI18nKey()
  {
    return "administration.configuration.param." + key;
  }

  @Override
  public String getDefaultStringValue()
  {
    return defaultStringValue;
  }

  /**
   * @return The full i18n key including the i18n prefix "administration.configuration.param." and the suffix
   * ".description".
   */
  @Override
  public String getDescriptionI18nKey()
  {
    return "administration.configuration.param." + key + ".description";
  }

  /**
   * @return the global
   * @see ConfigurationDO#getGlobal()
   */
  @Override
  public boolean isGlobal()
  {
    return global;
  }

  ConfigurationParam(final String key, final ConfigurationType type, final boolean global)
  {
    this(key, type, global, null);
  }

  ConfigurationParam(final String key, final ConfigurationType type, final boolean global,
      final String defaultStringValue)
  {
    this.key = key;
    this.type = type;
    this.global = global;
    this.defaultStringValue = defaultStringValue;
  }
}
