/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge

import java.math.BigDecimal
import java.util.*

/**
 * Defines different constants (typical length of string columns) usable by plugins and core package.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object Constants {
  /** The default length of comment strings in the data base. Used by data base definition and front-end validation.  */
  const val COMMENT_LENGTH = 4000

  @JvmField
  val TEN_BILLION = BigDecimal("10000000000")

  @JvmField
  val TEN_BILLION_NEGATIVE = BigDecimal("-10000000000")

  /**
   * Default length of text fields in the data-base (4,000).
   */
  const val LENGTH_TEXT = 4000

  /**
   * Default length of comment fields in the data-base (4,000).
   */
  const val LENGTH_COMMENT = 4000

  /**
   * Default length of text fields in the data-base (1,000).
   */
  const val LENGTH_SUBJECT = 1000

  /**
   * Default length of title fields in the data-base (1,000).
   */
  const val LENGTH_TITLE = 1000
  const val RESOURCE_BUNDLE_NAME = "I18nResources"
  const val REACT_APP_PATH = "react/"
  const val WICKET_APPLICATION_PATH = "wa/"
  const val WICKET_REQUEST_TIMEOUT_MINUTES = 5

  // Available Loacles for external i18n-files
  @JvmField
  val I18NSERVICE_LANGUAGES = arrayOf(Locale.GERMAN, Locale.ENGLISH, Locale.ROOT)

  /**
   * Available Localization for the wicket module
   * If you add new languages don't forget to add the I18nResources_##.properties also for all used plugins.
   * You need also to add the language to I18nResources*.properties such as<br></br>
   * locale.de=German<br></br>
   * locale.en=English<br></br>
   * locale.zh=Chinese
   */
  @JvmField
  val LOCALIZATIONS = arrayOf("en", "de")

  /**
   * the name of the event class.
   */
  const val EVENT_CLASS_NAME = "timesheet"
  const val BREAK_EVENT_CLASS_NAME = "ts-break"
  const val TIMESHEET_CALENDAR_ID = -1
  const val MINYEAR = 1900
  const val MAXYEAR = 2100

  const val WEB_HOME_PAGE_LINK = "https://projectforge.org"

  const val WEB_DOCS_LINK = "$WEB_HOME_PAGE_LINK/docs"

  const val WEB_DOCS_NEWS_LINK = "$WEB_HOME_PAGE_LINK/changelog-posts/"

  const val WEB_DOCS_USER_GUIDE_LINK = "$WEB_DOCS_LINK/userguide/"

  const val WEB_DOCS_LINK_HANDBUCH_LUCENE = "$WEB_DOCS_USER_GUIDE_LINK#full_indexed_search"

  const val WEB_DOCS_ADMIN_GUIDE_LINK = "$WEB_DOCS_LINK/adminguide/"

  const val WEB_DOCS_ADMIN_GUIDE_SECURITY_CONFIG_LINK = "$WEB_DOCS_ADMIN_GUIDE_LINK#securityconfig"

  @JvmStatic
  fun isTimesheetCalendarId(id: Int): Boolean {
    return id == TIMESHEET_CALENDAR_ID
  }
}
