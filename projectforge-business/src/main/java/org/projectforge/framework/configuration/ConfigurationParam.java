/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Configuration parameter class.
 */
public enum ConfigurationParam implements IConfigurationParam
{
  /**
   * System admin email.
   */
  SYSTEM_ADMIN_E_MAIL("systemAdministratorEMail", ConfigurationType.STRING), //
  /**
   * Message of the day configuration param.
   */
  MESSAGE_OF_THE_DAY("messageOfTheDay", ConfigurationType.TEXT), //

  /**
   * The Calendar domain.
   */
  CALENDAR_DOMAIN("calendarDomain", ConfigurationType.STRING), //
  /**
   * Organization configuration param.
   */
  ORGANIZATION("organization", ConfigurationType.TEXT), //
  /**
   * Default timezone configuration param.
   */
  DEFAULT_TIMEZONE("timezone", ConfigurationType.TIME_ZONE, TimeZone.getDefault().getID()), //
  /**
   * Date formats configuration param.
   */
  DATE_FORMATS("dateFormats", ConfigurationType.STRING, "MM/dd/yyyy;dd/MM/yyyy;dd.MM.yyyy;yyyy-MM-dd"), //
  /**
   * Excel date formats configuration param.
   */
  EXCEL_DATE_FORMATS("excelDateFormats", ConfigurationType.STRING, "MM/DD/YYYY;DD/MM/YYYY;DD.MM.YYYY"), //
  /**
   * Feedback e mail configuration param.
   */
  FEEDBACK_E_MAIL("feedbackEMail", ConfigurationType.STRING), //
  /**
   * Fibu default vat configuration param.
   */
  FIBU_DEFAULT_VAT("fibu.defaultVAT", ConfigurationType.PERCENT), //
  /**
   * Cost configured configuration param.
   */
  COST_CONFIGURED("fibu.costConfigured", ConfigurationType.BOOLEAN), //
  /**
   * Cost configured configuration param.
   */
  TIMESHEET_NOTE_SAVINGS_BY_AI("timesheet.noteSavingsByAI", ConfigurationType.TEXT), //
  /**
   * Default country phone prefix configuration param.
   */
  DEFAULT_COUNTRY_PHONE_PREFIX("countryPhonePrefix", ConfigurationType.STRING, "+49"), //
  /**
   * Plugin activated configuration param.
   */
  PLUGIN_ACTIVATED("pluginsActivated", ConfigurationType.STRING), //
  /**
   * Hr mailaddress configuration param.
   */
  HR_MAILADDRESS("hr.emailaddress", ConfigurationType.STRING),//
  /**
   * Globally defined, optional tags, the users may add to their time sheets.
   */
  TIMESHEET_TAGS("timesheetTags", ConfigurationType.STRING), //
  /**
   * End date vacation lastr year configuration param.
   */
  END_DATE_VACATION_LAST_YEAR("vacation.lastyear.enddate", ConfigurationType.STRING, "31.03."),
  /**
   * Minimum password length configuration param.
   */
  MIN_PASSWORD_LENGTH("minPasswordLength", ConfigurationType.LONG, 8),
  /**
   * Password Flag Check - configuration, that passwords will be checked that passwords have to change each time a new one is entered.
   */
  PASSWORD_FLAG_CHECK_CHANGE("password.flag.checkChange", ConfigurationType.BOOLEAN, true);

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
  public long getDefaultLongValue()
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
   * Instantiates a new Configuration param.
   *
   * @param key    the key
   * @param type   the type
   */
  ConfigurationParam(final String key, final ConfigurationType type)
  {
    this(key, type, null);
  }

  /**
   * Instantiates a new Configuration param.
   *
   * @param key                the key
   * @param type               the type
   * @param defaultStringValue the default string value
   */
  ConfigurationParam(final String key, final ConfigurationType type, final String defaultStringValue)
  {
    this(key, type, defaultStringValue, 0, false);

  }

  /**
   * Instantiates a new Configuration param.
   *
   * @param key             the key
   * @param type            the type
   * @param defaultIntValue the default int value
   */
  ConfigurationParam(final String key, final ConfigurationType type, final int defaultIntValue)
  {
    this(key, type, null, defaultIntValue, false);
  }

  /**
   * Instantiates a new Configuration param.
   *
   * @param key                 the key
   * @param type                the type
   * @param defaultBooleanValue the default boolean value
   */
  ConfigurationParam(final String key, final ConfigurationType type, final boolean defaultBooleanValue)
  {
    this(key, type, null, 0, defaultBooleanValue);
  }

  /**
   * Instantiates a new Configuration param.
   *
   * @param key                 the key
   * @param type                the type
   * @param defaultStringValue  the default string value
   * @param defaultIntValue     the default int value
   * @param defaultBooleanValue the default boolean value
   */
  ConfigurationParam(final String key, final ConfigurationType type, final String defaultStringValue, final int defaultIntValue,
      final boolean defaultBooleanValue)

  {
    this.key = key;
    this.type = type;
    this.defaultStringValue = defaultStringValue;
    this.defaultIntValue = defaultIntValue;
    this.defaultBooleanValue = defaultBooleanValue;
  }
}
