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

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

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
    val seconds = (System.currentTimeMillis() - date.time) / 1000
    if (seconds < 0 && allowFutureTimes) {
      return TimeLeft.getI18nKey(date, null)
    }
    return getUnit(seconds)
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

  internal fun getUnit(seconds: Long): Pair<String, Int> {
    if (seconds < 0) {
      return Pair("negative", -1)
    }
    val seconds10 = seconds * 10
    return getUnit(seconds10, YEAR)
      ?: getUnit(seconds10, MONTH)
      ?: getUnit(seconds10, WEEK)
      ?: getUnit(seconds10, DAY)
      ?: getUnit(seconds10, HOUR)
      ?: getUnit(seconds10, MINUTE)
      ?: Pair("afewseconds", -1)
  }

  private fun getUnit(seconds10: Long, unit: Unit): Pair<String, Int>? {
    return if (seconds10 >= 20 * unit.int) {
      Pair(unit.unitString, BigDecimal(seconds10).divide(unit.bigDecimal10Based, 0, RoundingMode.HALF_UP).toInt())
    } else if (seconds10 >= 10 * unit.int) {
      Pair("${unit.unitString}.one", -1)
    } else {
      null
    }
  }

  private class Unit(val int: Int, val unitString: String, val bigDecimal10Based: BigDecimal = BigDecimal(int * 10))

  private val MINUTE = Unit(60, "minutes")
  private val HOUR = Unit(60 * MINUTE.int, "hours")
  private val DAY = Unit(24 * HOUR.int, "days")
  private val WEEK = Unit(7 * DAY.int, "weeks")
  private val MONTH = Unit(30 * DAY.int, "months")
  private val YEAR = Unit(365 * DAY.int, "years")
}
