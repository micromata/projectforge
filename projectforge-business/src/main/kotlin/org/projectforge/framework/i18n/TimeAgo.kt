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

package org.projectforge.framework.i18n

import org.projectforge.common.DateFormatType
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.TimeUnit
import java.util.*
import kotlin.math.round

/**
 * Time ago builds human readable localized strings for time ago events, such as: a few seconds ago, a minute ago, 19 minutes ago, an hour ago, ...
 *
 * Used i18n properties:
 * * timeago.afewseconds=a few seconds ago
 * * timeago.days={0} days ago
 * * timeago.days.one=yesterday
 * * timeago.hours={0} hours ago
 * * timeago.hours.one=an hour ago
 * * timeago.minutes={0} minutes ago
 * * timeago.minutes.one=a minute ago
 * * timeago.months={0} months ago
 * * timeago.months.one=a month ago
 * * timeago.negative=in the future!
 * * timeago.seconds={0} seconds ago
 * * timeago.weeks={0} weeks ago
 * * timeago.weeks.one=a week ago
 * * timeago.years={0} years ago
 * * timeago.years.one=a year ago
 */
object TimeAgo {
  /**
   * @param date Date in the past to compare with now. For future dates, a message will be returned: 'in the future!'
   * @param locale Locale to use for translation.
   * @param maxUnit If given (e. g. DAY then the highest unit used is days: "5 hours ago", "5 days ago", "720 day ago")
   * @return Time ago message or an empty string, if no date was given.
   */
  @JvmOverloads
  @JvmStatic
  fun getMessage(date: PFDateTime?, locale: Locale? = null, allowFutureTimes: Boolean = false, maxUnit: TimeUnit? = null): String {
    date ?: return ""
    return translate(getI18nKey(date.utilDate, allowFutureTimes, maxUnit), "timeago", locale)
  }

  /**
   * @param date Date in the past to compare with now. For future dates, a message will be returned: 'in the future!'
   * @param locale Locale to use for translation.
   * @param maxUnit If given (e. g. DAY then the highest unit used is days: "5 hours ago", "5 days ago", "720 day ago")
   * @return Time ago message or an empty string, if no date was given.
   */
  @JvmOverloads
  @JvmStatic
  fun getMessage(date: Date?, locale: Locale? = null, allowFutureTimes: Boolean = false, maxUnit: TimeUnit? = null): String {
    date ?: return ""
    return translate(getI18nKey(date, allowFutureTimes, maxUnit), "timeago", locale)
  }

  @JvmOverloads
  @JvmStatic
  fun getDateAndMessage(date: Date?, locale: Locale? = null, allowFutureTimes: Boolean = false, maxUnit: TimeUnit? = null): String {
    date ?: return ""
    val text = PFDateTime.from(date).format(DateFormatType.DATE_TIME_MINUTES)
    return "$text (${getMessage(date)})"
  }

  internal fun getI18nKey(date: Date, allowFutureTimes: Boolean, maxUnit: TimeUnit? = null): Pair<String, Int> {
    val millis = (System.currentTimeMillis() - date.time)
    if (millis < 0 && allowFutureTimes) {
      return TimeLeft.getI18nKey(date, null, maxUnit)
    }
    return getUnit(millis, maxUnit)
  }

  internal fun translate(pair: Pair<String, Int>, prefix: String, locale: Locale?): String {
    return if (pair.second < 0) {
      // Translates the i18n key:
      translate(locale, "$prefix.${pair.first}")
    } else {
      // Translates the message using the i18n key with parameter pair.second:
      translateMsg(locale, "$prefix.${pair.first}", pair.second)
    }
  }

  internal fun getUnit(millis: Long, maxUnit: TimeUnit?): Pair<String, Int> {
    if (millis < 0) {
      return Pair("negative", -1)
    }
    return getUnit(millis, 1, TimeUnit.YEAR.millis, "years", maxUnit)
      ?: getUnit(millis, 1, TimeUnit.MONTH.millis, "months", maxUnit)
      ?: getUnit(millis, 1, TimeUnit.WEEK.millis, "weeks", maxUnit)
      ?: getUnit(millis, 1, TimeUnit.DAY.millis, "days", maxUnit)
      ?: getUnit(millis, 1, TimeUnit.HOUR.millis, "hours", maxUnit)
      ?: getUnit(millis, 1, TimeUnit.MINUTE.millis, "minutes", maxUnit)
      ?: getUnit(millis, 10, TimeUnit.SECONDS.millis, "seconds", maxUnit)
      ?: Pair("afewseconds", -1)
  }

  private fun getUnit(millis: Long, minAmount: Int, unit: Long, unitString: String, maxUnit: TimeUnit?): Pair<String, Int>? {
    if (maxUnit != null && maxUnit.millis < unit) {
      // Current unit (e. g. YEAR) is higher than given maxUnit (e. g. DAY)
      return null
    }
    return if (millis >= minAmount * unit) {
      val amount = round(millis.toDouble() / unit).toInt()
      if (amount > 1) {
        Pair(unitString, amount)
      } else {
        Pair("$unitString.one", -1)
      }
    } else {
      null
    }
  }
}
