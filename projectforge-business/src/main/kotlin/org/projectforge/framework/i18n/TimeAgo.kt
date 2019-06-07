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

package org.projectforge.framework.i18n

import java.util.*

/**
 * Time ago builds human readable localized strings for time ago events, such as: a few seconds ago, a minute ago, 19 minutes ago, an hour ago, ...
 */
object TimeAgo {
    const val MINUTE = 60
    const val HOUR = 60 * MINUTE
    const val DAY = 24 * HOUR
    const val WEEK = 7 * DAY
    const val MONTH = 30 * DAY
    const val YEAR = 365 * DAY

    fun getI18nKey(date: Date): Pair<String, Long> {
        val seconds = (System.currentTimeMillis() - date.time) / 1000;
        if (seconds < 0)
            return Pair("timeago.inthefuture", -1)
        else if (seconds > 2 * YEAR)
            return Pair("timeago.years", seconds / YEAR)
        else if (seconds > YEAR)
            return Pair("timeago.ayear", -1)
        else if (seconds > 40 * DAY)
            return Pair("timeago.months", seconds / MONTH)
        else if (seconds > MONTH)
            return Pair("timeago.amonth", -1)
        else if (seconds > 2 * WEEK)
            return Pair("timeago.weeks", seconds / WEEK)
        else if (seconds > WEEK)
            return Pair("timeago.aweek", -1)
        else if (seconds > 2 * DAY)
            return Pair("timeago.days", seconds / DAY)
        else if (seconds > DAY)
            return Pair("timeago.yesterday", -1)
        else if (seconds > 2 * HOUR)
            return Pair("timeago.hours", seconds / HOUR)
        else if (seconds > HOUR)
            return Pair("timeago.anhour", -1)
        else if (seconds > 2 * MINUTE)
            return Pair("timeago.minutes", seconds / MINUTE)
        else if (seconds > MINUTE)
            return Pair("timeago.aminute", -1)
        return Pair("timeago.afewseconds", -1)
    }

    fun getMessage(date: Date): String {
        val pair = getI18nKey(date)
        if (pair.second < 0)
            return translate(pair.first)
        return translateMsg(pair.first, pair.second)
    }
}
