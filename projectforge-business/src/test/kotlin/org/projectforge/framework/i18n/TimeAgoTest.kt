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

package org.projectforge.web

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.i18n.TimeAgo
import java.util.*

private const val HOUR = 3600L
private const val DAY = 24 * HOUR
private const val MONTH = 30 * DAY

class TimeAgoTest {

    @Test
    fun i18nTest() {
        timeAgo(-100, "timeago.inthefuture", -1)
        timeAgo(20, "timeago.afewseconds", -1)
        timeAgo(59, "timeago.afewseconds", -1)
        timeAgo(65, "timeago.aminute", -1)
        timeAgo(125, "timeago.minutes", 2)

        timeAgo(HOUR, "timeago.anhour", -1)
        timeAgo(2 * HOUR, "timeago.hours", 2)
        timeAgo(DAY, "timeago.yesterday", -1)
        timeAgo(2 * DAY, "timeago.days", 2)
        timeAgo(7 * DAY, "timeago.aweek", -1)
        timeAgo(14 * DAY, "timeago.weeks", 2)
        timeAgo(31 * DAY, "timeago.amonth", -1)

        timeAgo(2 *  MONTH, "timeago.months", 2)
        timeAgo(11 * MONTH, "timeago.months", 11)
        timeAgo(365 * DAY, "timeago.ayear", -1)
        timeAgo(25 * MONTH, "timeago.years", 2)
    }

    private fun timeAgo(secondsOffset: Long, expectedI18nKey: String, expectedCounter: Long) {
        val pair = TimeAgo.getI18nKey(Date(System.currentTimeMillis() - secondsOffset * 1000 - 1000))
        Assertions.assertEquals(expectedI18nKey, pair.first)
        Assertions.assertEquals(expectedCounter, pair.second)
    }
}
