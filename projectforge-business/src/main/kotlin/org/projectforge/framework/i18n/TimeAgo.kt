/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
 * * timeago.weeks={0} weeks ago
 * * timeago.weeks.one=a week ago
 * * timeago.years={0} years ago
 * * timeago.years.one=a year ago
 */
object TimeAgo {
  /**
   * @param date Date in the past to compare with now. For future dates, a message will be returned: 'in the future!'
   * @param locale Locale to use for translation.
   * @return Time ago message or an empty string, if no date was given.
   */
  @JvmOverloads
  @JvmStatic
  fun getMessage(date: Date?, locale: Locale? = null, allowFutureTimes: Boolean = false): String {
    date ?: return ""
    return translate(getI18nKey(date, allowFutureTimes), "timeago", locale)
  }

  internal fun getI18nKey(date: Date, allowFutureTimes: Boolean): Pair<String, Int> {
    val millis = (System.currentTimeMillis() - date.time)
    if (millis < 0 && allowFutureTimes) {
      return TimeLeft.getI18nKey(date, null)
    }
    return getUnit(millis)
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

  internal fun getUnit(millis: Long): Pair<String, Int> {
    if (millis < 0) {
      return Pair("negative", -1)
    }
    return getUnit(millis, 1, YEAR, "years")
      ?: getUnit(millis, 1, MONTH, "months")
      ?: getUnit(millis, 1, WEEK, "weeks")
      ?: getUnit(millis, 1, DAY, "days")
      ?: getUnit(millis, 1, HOUR, "hours")
      ?: getUnit(millis, 1, MINUTE, "minutes")
      ?: Pair("afewseconds", -1)
  }

  private fun getUnit(millis: Long, minAmount: Int, unit: Long, unitString: String): Pair<String, Int>? {
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

  const val MINUTE = 60 * 1000L
  const val HOUR = 60 * MINUTE
  const val DAY = 24 * HOUR
  const val WEEK = 7 * DAY
  const val MONTH = 30 * DAY
  const val YEAR = 365 * DAY
}
