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
 * * timeago.inthefuture=in the future!
 * * timeago.minutes={0} minutes ago
 * * timeago.months={0} months ago
 * * timeago.weeks={0} weeks ago
 * * timeago.years={0} years ago
 * * timeago.yesterday=yesterday
 */
object TimeAgo {
  const val MINUTE = 60
  const val HOUR = 60 * MINUTE
  const val DAY = 24 * HOUR
  const val WEEK = 7 * DAY
  const val MONTH = 30 * DAY
  const val YEAR = 365 * DAY

  fun getI18nKey(date: Date): Pair<String, Long> {
    val seconds = (System.currentTimeMillis() - date.time) / 1000
    when {
      seconds < 0 -> return Pair("timeago.inthefuture", -1)
      seconds > 2 * YEAR -> return Pair("timeago.years", seconds / YEAR)
      seconds > YEAR -> return Pair("timeago.ayear", -1)
      seconds > 40 * DAY -> return Pair("timeago.months", seconds / MONTH)
      seconds > MONTH -> return Pair("timeago.amonth", -1)
      seconds > 2 * WEEK -> return Pair("timeago.weeks", seconds / WEEK)
      seconds > WEEK -> return Pair("timeago.aweek", -1)
      seconds > 2 * DAY -> return Pair("timeago.days", seconds / DAY)
      seconds > DAY -> return Pair("timeago.yesterday", -1)
      seconds > 2 * HOUR -> return Pair("timeago.hours", seconds / HOUR)
      seconds > HOUR -> return Pair("timeago.anhour", -1)
      seconds > 2 * MINUTE -> return Pair("timeago.minutes", seconds / MINUTE)
      seconds > MINUTE -> return Pair("timeago.aminute", -1)
      else -> return Pair("timeago.afewseconds", -1)
    }
  }

  fun getMessage(date: Date?, locale: Locale? = null): String {
    date ?: return ""
    val pair = getI18nKey(date)
    return if (pair.second < 0)
      translate(locale, pair.first)
    else
      translateMsg(locale, pair.first, pair.second)
  }
}
