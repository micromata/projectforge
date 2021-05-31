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

/**
 * Time ago builds human readable localized strings for time ago events, such as: a few seconds ago, a minute ago, 19 minutes ago, an hour ago, ...
 *
 * Used i18n properties:
 * * timeago.afewseconds=a few seconds ago
 * * timeago.aminute=a minute ago
 * * timeago.amonth=a month ago
 * * timeago.anhour=an hour ago
 * * timeago.aweek=a week ago
 * * timeago.ayear=a year ago
 * * timeago.days={0} days ago
 * * timeago.hours={0} hours ago
 * * timeago.negative=in the future!
 * * timeago.minutes={0} minutes ago
 * * timeago.months={0} months ago
 * * timeago.weeks={0} weeks ago
 * * timeago.years={0} years ago
 * * timeago.day=yesterday
 */
object TimeAgo {
  const val MINUTE = 60
  const val HOUR = 60 * MINUTE
  const val DAY = 24 * HOUR
  const val WEEK = 7 * DAY
  const val MONTH = 30 * DAY
  const val YEAR = 365 * DAY

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

  internal fun getI18nKey(date: Date, allowFutureTimes: Boolean): Pair<String, Long> {
    val seconds = (System.currentTimeMillis() - date.time) / 1000
    if (seconds < 0 && allowFutureTimes) {
      return TimeLeft.getI18nKey(date, false)
    }
    return getUnit(seconds)
  }

  internal fun translate(pair: Pair<String, Long>, prefix: String, locale: Locale?): String {
    return if (pair.second < 0) {
      // Translates the i18n key:
      translate(locale, "$prefix.${pair.first}")
    } else {
      // Translates the message using the i18n key with parameter pair.second:
      translateMsg(locale, "$prefix.${pair.first}", pair.second)
    }
  }

  internal fun getUnit(seconds: Long): Pair<String, Long> {
    return when {
      seconds < 0 -> Pair("negative", -1)
      seconds > 2 * TimeLeft.YEAR -> Pair("years", seconds / TimeLeft.YEAR)
      seconds > TimeLeft.YEAR -> Pair("ayear", -1)
      seconds > 40 * TimeLeft.DAY -> Pair("months", seconds / TimeLeft.MONTH)
      seconds > TimeLeft.MONTH -> Pair("amonth", -1)
      seconds > 2 * TimeLeft.WEEK -> Pair("weeks", seconds / TimeLeft.WEEK)
      seconds > TimeLeft.WEEK -> Pair("aweek", -1)
      seconds > 2 * TimeLeft.DAY -> Pair("days", seconds / TimeLeft.DAY)
      seconds > TimeLeft.DAY -> Pair("day", -1)
      seconds > 2 * TimeLeft.HOUR -> Pair("hours", seconds / TimeLeft.HOUR)
      seconds > TimeLeft.HOUR -> Pair("anhour", -1)
      seconds > 2 * TimeLeft.MINUTE -> Pair("minutes", seconds / TimeLeft.MINUTE)
      seconds > TimeLeft.MINUTE -> Pair("aminute", -1)
      else -> Pair("afewseconds", -1)
    }
  }
}
