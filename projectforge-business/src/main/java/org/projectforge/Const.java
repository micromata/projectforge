package org.projectforge;

import java.util.Locale;

/**
 * Holds the consts for the PF Application.
 *
 * @author blumenstein
 */

public class Const
{
  public static final String WICKET_APPLICATION_PATH = "wa/";

  public static final int WICKET_REQUEST_TIMEOUT_MINUTES = 5;

  public static final String MESSAGE_KEY_PASSWORD_QUALITY_CHECK = "user.changePassword.error.passwordQualityCheck";

  public static final String COOKIE_NAME_FOR_STAY_LOGGED_IN = "stayLoggedIn";

  // Available Loacles for external i18n-files
  public static final Locale[] I18NSERVICE_LANGUAGES = new Locale[] { Locale.GERMAN, Locale.ENGLISH, Locale.ROOT };

  /**
   * Available Localization for the wicket module
   * If you add new languages don't forget to add the I18nResources_##.properties also for all used plugins.
   * You need also to add the language to I18nResources*.properties such as<br/>
   * locale.de=German<br/>
   * locale.en=English<br/>
   * locale.zh=Chinese
   */
  public static final String[] LOCALIZATIONS = { "en", "de" };
  /**
   * the name of the event class.
   */
  public static final String EVENT_CLASS_NAME = "timesheet";

  public static final String BREAK_EVENT_CLASS_NAME = "ts-break";

  public static final Integer TIMESHEET_CALENDAR_ID = -1;

  // if you change this, check the multipart.maxFileSize value in the properties file
  public static final int ADDRESS_EDITPAGE_MAX_IMAGE_UPLOAD_SIZE = 1024 * 1024;
}
