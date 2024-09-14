/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.configuration.ConfigurationServiceAccessor
import java.math.BigDecimal

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

  const val KB = 1024

  val KB_BD = BigDecimal(KB)

  const val MB = KB * 1024

  val MB_BD = BigDecimal(MB)

  const val GB = MB * 1024

  val GB_BD = BigDecimal(GB)

  const val TB = GB * 1024L

  val TB_BD = BigDecimal(TB)

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
  const val REACT = "react"
  const val REACT_APP_PATH = "react/"
  const val WICKET_APPLICATION_PATH = "wa/"
  const val WICKET_REQUEST_TIMEOUT_MINUTES = 5

  /**
   * the name of the event class.
   */
  const val EVENT_CLASS_NAME = "timesheet"
  const val BREAK_EVENT_CLASS_NAME = "ts-break"
  const val TIMESHEET_CALENDAR_ID = -1L
  const val MINYEAR = 1900
  const val MAXYEAR = 2100

  const val WEB_HOME_PAGE_LINK = "https://projectforge.org"

  const val WEB_DOCS_LINK = "$WEB_HOME_PAGE_LINK/docs"

  const val WEB_DOCS_NEWS_LINK = "$WEB_HOME_PAGE_LINK/changelog-posts/"

  const val WEB_DOCS_USER_GUIDE_LINK = "$WEB_DOCS_LINK/userguide/"

  const val WEB_DOCS_LINK_HANDBUCH_LUCENE = "$WEB_DOCS_USER_GUIDE_LINK#full_indexed_search"

  const val WEB_DOCS_ADMIN_GUIDE_LINK = "$WEB_DOCS_LINK/adminguide/"

  const val WEB_DOCS_ADMIN_GUIDE_SECURITY_CONFIG_LINK = "$WEB_DOCS_ADMIN_GUIDE_LINK#securityconfig"

  const val SECONDS_PER_MINUTE = 60

  val SECONDS_PER_MINUTE_BD = BigDecimal(SECONDS_PER_MINUTE)

  const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60

  val SECONDS_PER_HOUR_BD = BigDecimal(SECONDS_PER_HOUR)

  const val SECONDS_PER_DAY = SECONDS_PER_HOUR * 24

  val SECONDS_PER_DAY_BD = BigDecimal(SECONDS_PER_DAY)

  const val SECONDS_PER_WEEK = SECONDS_PER_DAY * 7

  val SECONDS_PER_WEEK_BD = BigDecimal(SECONDS_PER_WEEK)

  const val MILLIS_PER_SECOND = 1_000L

  val MILLIS_PER_SECOND_BD = BigDecimal(MILLIS_PER_SECOND)

  const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND

  val MILLIS_PER_MINUTE_BD = BigDecimal(MILLIS_PER_MINUTE)

  const val MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60

  val MILLIS_PER_HOUR_BD = BigDecimal(MILLIS_PER_HOUR)

  const val MILLIS_PER_DAY = MILLIS_PER_HOUR * 24

  val MILLIS_PER_DAY_BD = BigDecimal(MILLIS_PER_DAY)

  const val MILLIS_PER_WEEK = MILLIS_PER_DAY * 7

  val MILLIS_PER_WEEK_BD = BigDecimal(MILLIS_PER_WEEK)

  val CURRENCY_SYMBOL = ConfigurationServiceAccessor.get()?.currencySymbol ?: "€"

  @JvmStatic
  fun isTimesheetCalendarId(id: Long?): Boolean {
    return id == TIMESHEET_CALENDAR_ID
  }
}
