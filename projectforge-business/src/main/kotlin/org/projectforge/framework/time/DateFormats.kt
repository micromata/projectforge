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

package org.projectforge.framework.time

import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.common.DateFormatType
import org.projectforge.framework.configuration.Configuration.Companion.instance
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Date formats. All the formats base on the given defaultDateFormat. Default date formats are e. g. "dd.MM.yyyy", "dd.MM.yy", "dd/MM/yyyy",
 * "dd/MM/yy", "MM/dd/yyyy", "MM/dd/yy".
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object DateFormats {
  const val COMPACT_DATE = "yyyyMMdd"
  const val ISO_DATE = "yyyy-MM-dd"
  const val ISO_TIMESTAMP_MINUTES = "yyyy-MM-dd HH:mm"
  const val ISO_TIMESTAMP_SECONDS = "yyyy-MM-dd HH:mm:ss"
  const val ISO_TIMESTAMP_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS"
  const val EXCEL_ISO_DATE = "YYYY-MM-DD"
  const val LUCENE_TIMESTAMP_MINUTE = "yyyyMMddHHmm"
  const val ICAL_DATETIME_FORMAT = "yyyyMMdd'T'HHmmss"

  /**
   * Check weather the given dateString has month or day first. If not analyzable then true is returned as default value.
   *
   * @param dateString
   * @return true if month is used before day of month.
   */
  @JvmStatic
  fun isFormatMonthFirst(dateString: String?): Boolean {
    if (dateString == null) {
      return true
    }
    val monthPos = dateString.indexOf('M')
    val dayPos = dateString.indexOf('d')
    return monthPos <= dayPos // '=': if none of both found, true is the default.
  }

  /**
   * Tries to get the separator char in dates ('/' is the default if nothing found). <br></br>
   * Example: "dd.MM.yyyy ..." results in '.', "MM/dd/yyy ..." results in '/'. <br></br>
   * Only '/', '-' and '.' are supported for now.
   *
   * @param dateString
   * @return the separator char.
   */
  @JvmStatic
  fun getDateSeparatorChar(dateString: String?): Char {
    if (dateString == null) {
      return '/'
    }
    if (dateString.indexOf('/') > 0) {
      return '/'
    } else if (dateString.indexOf('.') > 0) {
      return '.'
    } else if (dateString.indexOf('-') > 0) {
      return '-'
    }
    return '/'
  }

  /**
   * @param dateString
   * @return true if the dateString starts with "yyyy-MM-dd", otherwise false.
   */
  @JvmStatic
  fun isIsoFormat(dateString: String?): Boolean {
    return dateString?.startsWith("yyyy-MM-dd") ?: false
  }

  /**
   * Uses default format of the logged-in user.
   */
  @JvmStatic
  val dateParseFormats: Array<String?>
    get() = getDateParseFormats(ensureAndGetDefaultDateFormat())

  /**
   * DefaultDateFormat with yyyy and yy and ISO format yyyy-MM-dd.
   *
   * @param defaultDateFormat
   */
  @JvmStatic
  fun getDateParseFormats(defaultDateFormat: String): Array<String?> {
    // # Date/time formats (important: don't use spaces after separator char, e. g. correct is dd.MMM yyyy instead of dd. MMM yyyy):
    val sa = arrayOfNulls<String>(4)
    if (defaultDateFormat.contains("yyyy")) {
      sa[0] = defaultDateFormat.replace("yyyy", "yy") // First, try "yy"
      sa[1] = defaultDateFormat
    } else {
      sa[0] = defaultDateFormat
      sa[1] = defaultDateFormat.replace("yy", "yyyy")
    }
    sa[2] = getFormatString(defaultDateFormat, null, DateFormatType.DATE_WITHOUT_YEAR)
    sa[3] = ISO_DATE
    return sa
  }

  /**
   * Gets the format string for the logged-in user. Uses the date format of the logged in user and if not given, it'll be set.
   *
   * @param format
   * @see Configuration.getDateFormats
   * @see PFUserDO.getExcelDateFormat
   */
  @JvmStatic
  @JvmOverloads
  fun getFormatString(format: DateFormatType?, user: PFUserDO? = ThreadLocalUserContext.loggedInUser): String {
    return getFormatString(ensureAndGetDefaultDateFormat(user), ensureAndGetDefaultTimeNotation(user), format)
  }

  /**
   * Gets the format string for the logged-in user. Uses the date format of the logged in user and if not given, it'll be set.
   *
   * @param format
   * @see Configuration.getDateFormats
   * @see PFUserDO.getExcelDateFormat
   */
  @JvmStatic
  @JvmOverloads
  fun getDateTimeFormatter(
    format: DateFormatType?,
    user: PFUserDO? = ThreadLocalUserContext.loggedInUser
  ): DateTimeFormatter {
    val formatString = getFormatString(ensureAndGetDefaultDateFormat(user), ensureAndGetDefaultTimeNotation(user), format)
    return DateTimeFormatter.ofPattern(formatString, ThreadLocalUserContext.locale)
      .withZone(ThreadLocalUserContext.zoneId)
  }

  /**
   * Ensures and gets the default date format of the logged-in user.
   *
   * @return
   */
  private fun ensureAndGetDefaultDateFormat(user: PFUserDO? = ThreadLocalUserContext.loggedInUser): String {
    var defaultDateFormat = user?.dateFormat
    if (defaultDateFormat == null) {
      defaultDateFormat = instance.defaultDateFormat
      if (user != null) {
        user.dateFormat = defaultDateFormat
      }
    }
    return defaultDateFormat
  }

  /**
   * Ensures and gets the default time notation of the logged-in user.
   *
   * @return
   */
  @JvmStatic
  @JvmOverloads
  fun ensureAndGetDefaultTimeNotation(user: PFUserDO? = ThreadLocalUserContext.loggedInUser): TimeNotation? {
    var defaultTimeNotation = user?.timeNotation
    if (defaultTimeNotation == null) {
      defaultTimeNotation = if (ConfigurationServiceAccessor.get().defaultTimeNotation != null) {
        ConfigurationServiceAccessor.get().defaultTimeNotation
      } else {
        val locale = ThreadLocalUserContext.locale
        if (locale != null && locale.toString().lowercase(Locale.getDefault()).startsWith("de")) {
          TimeNotation.H24
        } else {
          TimeNotation.H12
        }
      }
      if (user != null) {
        user.timeNotation = defaultTimeNotation
      }
    }
    return defaultTimeNotation
  }

  /**
   * Ensures and gets the default excel date format of the logged-in user.
   *
   * @return
   */
  private fun ensureAndGetDefaultExcelDateFormat(): String {
    val user = ThreadLocalUserContext.loggedInUser
    var defaultExcelDateFormat = user?.excelDateFormat
    if (defaultExcelDateFormat == null) {
      defaultExcelDateFormat = instance.defaultExcelDateFormat
      if (user != null) {
        user.excelDateFormat = defaultExcelDateFormat
      }
    }
    return defaultExcelDateFormat
  }

  /**
   * Gets the format string for the logged-in user. Uses the date format of the logged in user and if not given, a default format is
   * returned.
   *
   * @param format
   * @see Configuration.getExcelDateFormats
   * @see PFUserDO.getExcelDateFormat
   */
  @JvmStatic
  fun getExcelFormatString(format: DateFormatType?): String {
    return getExcelFormatString(ensureAndGetDefaultExcelDateFormat(), format)
  }

  fun getExcelFormatString(defaultExcelDateFormat: String, format: DateFormatType?): String {
    return when (format) {
      DateFormatType.DATE -> defaultExcelDateFormat
      DateFormatType.DATE_TIME_MINUTES -> "$defaultExcelDateFormat hh:mm"
      DateFormatType.DATE_TIME_SECONDS -> "$defaultExcelDateFormat hh:mm:ss"
      DateFormatType.DATE_TIME_MILLIS -> "$defaultExcelDateFormat hh:mm:ss.fff"
      else -> "$defaultExcelDateFormat hh:mm:ss"
    }
  }

  fun getFormatString(defaultDateFormat: String, timeNotation: TimeNotation?, format: DateFormatType?): String {
    return when (format) {
      DateFormatType.DATE -> defaultDateFormat
      DateFormatType.DATE_WITH_DAY_NAME -> "E, " + getFormatString(
        defaultDateFormat,
        timeNotation,
        DateFormatType.DATE
      )
      DateFormatType.DATE_WITHOUT_YEAR -> {
        val pattern: String
        pattern = if (defaultDateFormat.contains("yyyy")) {
          defaultDateFormat.replace("yyyy", "")
        } else {
          defaultDateFormat.replace("yy", "")
        }
        if (pattern.endsWith("/")) {
          pattern.substring(0, pattern.length - 1)
        } else if (pattern.startsWith("-")) {
          pattern.substring(1)
        } else {
          pattern
        }
      }
      DateFormatType.DATE_SHORT -> {
        if (!defaultDateFormat.contains("yyyy")) {
          defaultDateFormat
        } else defaultDateFormat.replace("yyyy", "yy")
      }
      DateFormatType.ISO_DATE -> "yyyy-MM-dd"
      DateFormatType.ISO_TIMESTAMP_MINUTES -> "yyyy-MM-dd HH:mm"
      DateFormatType.ISO_TIMESTAMP_SECONDS -> "yyyy-MM-dd HH:mm:ss"
      DateFormatType.ISO_TIMESTAMP_MILLIS -> "yyyy-MM-dd HH:mm:ss.SSS"
      DateFormatType.DAY_OF_WEEK_SHORT -> "EE"
      DateFormatType.DATE_TIME_MINUTES -> getFormatString(
        defaultDateFormat,
        timeNotation,
        DateFormatType.DATE
      ) + if (timeNotation == TimeNotation.H24) " HH:mm" else " hh:mm a"
      DateFormatType.DATE_TIME_SECONDS -> getFormatString(
        defaultDateFormat,
        timeNotation,
        DateFormatType.DATE
      ) + if (timeNotation == TimeNotation.H24) " HH:mm:ss" else " hh:mm:ss a"
      DateFormatType.DATE_TIME_MILLIS -> getFormatString(
        defaultDateFormat,
        timeNotation,
        DateFormatType.DATE
      ) + if (timeNotation == TimeNotation.H24) " HH:mm:ss.SSS" else " hh:mm:ss.SSS a"
      DateFormatType.DATE_TIME_SHORT_MINUTES -> getFormatString(
        defaultDateFormat,
        timeNotation,
        DateFormatType.DATE_SHORT
      ) + if (timeNotation == TimeNotation.H24) " HH:mm" else " hh:mm a"
      DateFormatType.DATE_TIME_SHORT_SECONDS -> getFormatString(
        defaultDateFormat,
        timeNotation,
        DateFormatType.DATE_SHORT
      ) + if (timeNotation == TimeNotation.H24) " HH:mm:ss" else " hh:mm:ss a"
      DateFormatType.TIME_OF_DAY_MINUTES -> if (timeNotation == TimeNotation.H24) "HH:mm" else "hh:mm a"
      DateFormatType.TIME_OF_DAY_SECONDS -> if (timeNotation == TimeNotation.H24) "HH:mm:ss" else "hh:mm:ss a"
      else -> defaultDateFormat
    }
  }
}
