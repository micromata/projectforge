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

package org.projectforge.framework.configuration;

import java.util.TimeZone;

import org.projectforge.framework.configuration.entities.ConfigurationDO;

/**
 * Configuration parameter class.
 */
public enum ConfigurationParam implements IConfigurationParam
{
  /**
   * System admin email.
   */
  // Global parameters:
  SYSTEM_ADMIN_E_MAIL("systemAdministratorEMail", ConfigurationType.STRING, true), //
  /**
   * Message of the day configuration param.
   */
  MESSAGE_OF_THE_DAY("messageOfTheDay", ConfigurationType.TEXT, true), //
  /**
   * Multi tenancy enabled configuration param.
   */
  MULTI_TENANCY_ENABLED("admin.multiTenancyEnabled", ConfigurationType.BOOLEAN, true), //

  /**
   * The Calendar domain.
   */
  // Tenant specific parameters:
  CALENDAR_DOMAIN("calendarDomain", ConfigurationType.STRING, false), //
  /**
   * Organization configuration param.
   */
  ORGANIZATION("organization", ConfigurationType.TEXT, false), //
  /**
   * Default timezone configuration param.
   */
  DEFAULT_TIMEZONE("timezone", ConfigurationType.TIME_ZONE, false, TimeZone.getDefault().getID()), //
  /**
   * Date formats configuration param.
   */
  DATE_FORMATS("dateFormats", ConfigurationType.STRING, false, "MM/dd/yyyy;dd/MM/yyyy;dd.MM.yyyy;yyyy-MM-dd"), //
  /**
   * Excel date formats configuration param.
   */
  EXCEL_DATE_FORMATS("excelDateFormats", ConfigurationType.STRING, false, "MM/DD/YYYY;DD/MM/YYYY;DD.MM.YYYY"), //
  /**
   * Feedback e mail configuration param.
   */
  FEEDBACK_E_MAIL("feedbackEMail", ConfigurationType.STRING, false), //
  /**
   * Fibu default vat configuration param.
   */
  FIBU_DEFAULT_VAT("fibu.defaultVAT", ConfigurationType.PERCENT, false), //
  /**
   * Cost configured configuration param.
   */
  COST_CONFIGURED("fibu.costConfigured", ConfigurationType.BOOLEAN, false), //
  /**
   * Default country phone prefix configuration param.
   */
  DEFAULT_COUNTRY_PHONE_PREFIX("countryPhonePrefix", ConfigurationType.STRING, false, "+49"), //
  /**
   * Meb sms receiving phone number configuration param.
   */
  MEB_SMS_RECEIVING_PHONE_NUMBER("mebSMSReceivingPhoneNumber", ConfigurationType.STRING, false), //
  /**
   * Plugin activated configuration param.
   */
  PLUGIN_ACTIVATED("pluginsActivated", ConfigurationType.STRING, true), //
  /**
   * Hr mailaddress configuration param.
   */
  HR_MAILADDRESS("hr.emailaddress", ConfigurationType.STRING, true),//
  /**
   * Vacation cal id configuration param.
   */
  VACATION_CAL_ID("vacation.cal.id", ConfigurationType.CALENDAR, true),//
  /**
   * End date vacation lastr year configuration param.
   */
  END_DATE_VACATION_LASTR_YEAR("vacation.lastyear.enddate", ConfigurationType.STRING, true, "31.03."),
  /**
   * Minimum password length configuration param.
   */
  MIN_PASSWORD_LENGTH("minPasswordLength", ConfigurationType.INTEGER, true, 10),
  /**
   * Password Flag Check - configuration, that passwords will be checked that passwords have to change each time a new one is entered.
   */
  PASSWORD_FLAG_CHECK_CHANGE("password.flag.checkChange", ConfigurationType.BOOLEAN, true, true),

  /**
   * Will enable / disable snow effect.
   */
  SNOW_EFFECT_ENABLED("snoweffect.enabled", ConfigurationType.BOOLEAN, true, false);

  /**
   * Key.
   */
  private final String key;

  /**
   * Configuration Type.
   */
  private final ConfigurationType type;

  /**
   * Default string value.
   */
  private final String defaultStringValue;

  /**
   * Default int value.
   */
  private final int defaultIntValue;

  /**
   * Default boolean value.
   */
  private final boolean defaultBooleanValue;

  /**
   * Global.
   */
  private final boolean global;

  /**
   * Key will be used e. g. for i18n.
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

  @Override
  public int getDefaultIntValue()
  {
    return defaultIntValue;
  }

  @Override
  public boolean getDefaultBooleanValue()
  {
    return defaultBooleanValue;
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

  /**
   * Instantiates a new Configuration param.
   *
   * @param key    the key
   * @param type   the type
   * @param global the global
   */
  ConfigurationParam(final String key, final ConfigurationType type, final boolean global)
  {
    this(key, type, global, null);
  }

  /**
   * Instantiates a new Configuration param.
   *
   * @param key                the key
   * @param type               the type
   * @param global             the global
   * @param defaultStringValue the default string value
   */
  ConfigurationParam(final String key, final ConfigurationType type, final boolean global, final String defaultStringValue)
  {
    this(key, type, global, defaultStringValue, 0, false);

  }

  /**
   * Instantiates a new Configuration param.
   *
   * @param key             the key
   * @param type            the type
   * @param global          the global
   * @param defaultIntValue the default int value
   */
  ConfigurationParam(final String key, final ConfigurationType type, final boolean global, final int defaultIntValue)
  {
    this(key, type, global, null, defaultIntValue, false);
  }

  /**
   * Instantiates a new Configuration param.
   *
   * @param key                 the key
   * @param type                the type
   * @param global              the global
   * @param defaultBooleanValue the default boolean value
   */
  ConfigurationParam(final String key, final ConfigurationType type, final boolean global, final boolean defaultBooleanValue)
  {
    this(key, type, global, null, 0, defaultBooleanValue);
  }

  /**
   * Instantiates a new Configuration param.
   *
   * @param key                 the key
   * @param type                the type
   * @param global              the global
   * @param defaultStringValue  the default string value
   * @param defaultIntValue     the default int value
   * @param defaultBooleanValue the default boolean value
   */
  ConfigurationParam(final String key, final ConfigurationType type, final boolean global, final String defaultStringValue, final int defaultIntValue,
      final boolean defaultBooleanValue)

  {
    this.key = key;
    this.type = type;
    this.global = global;
    this.defaultStringValue = defaultStringValue;
    this.defaultIntValue = defaultIntValue;
    this.defaultBooleanValue = defaultBooleanValue;
  }
}
