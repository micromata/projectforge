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
        timeAgo(-100, "negative", -1)
        timeAgo(-125, "minutes", 2, true)
        timeAgo(20, "afewseconds", -1)
        timeAgo(59, "afewseconds", -1)
        timeAgo(65, "aminute", -1)
        timeAgo(125, "minutes", 2)

        timeAgo(HOUR, "anhour", -1)
        timeAgo(2 * HOUR, "hours", 2)
        timeAgo(DAY, "day", -1)
        timeAgo(2 * DAY, "days", 2)
        timeAgo(7 * DAY, "aweek", -1)
        timeAgo(14 * DAY, "weeks", 2)
        timeAgo(31 * DAY, "amonth", -1)

        timeAgo(2 *  MONTH, "months", 2)
        timeAgo(11 * MONTH, "months", 11)
        timeAgo(365 * DAY, "ayear", -1)
        timeAgo(25 * MONTH, "years", 2)
    }

    private fun timeAgo(secondsOffset: Long, expectedI18nKey: String, expectedCounter: Long, allowFutureTimes: Boolean = false) {
        val pair = TimeAgo.getI18nKey(Date(System.currentTimeMillis() - secondsOffset * 1000 - 1000), allowFutureTimes)
        Assertions.assertEquals(expectedI18nKey, pair.first)
        Assertions.assertEquals(expectedCounter, pair.second)
    }
}
