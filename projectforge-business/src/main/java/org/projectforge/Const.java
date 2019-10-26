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

package org.projectforge;

import java.util.Locale;

/**
 * Holds the consts for the PF Application.
 *
 * @author blumenstein
 */

public class Const
{
  public static final String REACT_APP_PATH = "react/";

  public static final String WICKET_APPLICATION_PATH = "wa/";

  public static final int WICKET_REQUEST_TIMEOUT_MINUTES = 5;

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

  public static final int MINYEAR = 1900;

  public static final int MAXYEAR = 2100;
}
