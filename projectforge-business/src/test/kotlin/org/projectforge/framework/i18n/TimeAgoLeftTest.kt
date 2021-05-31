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
import org.projectforge.framework.i18n.TimeLeft
import java.util.*

private const val HOUR = 3600L
private const val DAY = 24 * HOUR
private const val MONTH = 30 * DAY

class TimeAgoLeftTest {

  @Test
  fun i18nTest() {
    timeAgoLeft(-100, "negative", -1)
    timeAgoLeft(-125, "minutes", 2, true)
    timeAgoLeft(20, "afewseconds", -1)
    timeAgoLeft(59, "afewseconds", -1)
    timeAgoLeft(65, "aminute", -1)
    timeAgoLeft(125, "minutes", 2)

    timeAgoLeft(HOUR, "anhour", -1)
    timeAgoLeft(2 * HOUR, "hours", 2)
    timeAgoLeft(DAY, "day", -1)
    timeAgoLeft(2 * DAY, "days", 2)
    timeAgoLeft(7 * DAY, "aweek", -1)
    timeAgoLeft(14 * DAY, "weeks", 2)
    timeAgoLeft(31 * DAY, "amonth", -1)

    timeAgoLeft(2 * MONTH, "months", 2)
    timeAgoLeft(11 * MONTH, "months", 11)
    timeAgoLeft(365 * DAY, "ayear", -1)
    timeAgoLeft(25 * MONTH, "years", 2)
  }

  private fun timeAgoLeft(
    secondsOffset: Long,
    expectedI18nKey: String,
    expectedCounter: Long,
    allowNegativeTimes: Boolean = false
  ) {
    var pair = TimeAgo.getI18nKey(Date(System.currentTimeMillis() - secondsOffset * 1000 - 1000), allowNegativeTimes)
    Assertions.assertEquals(expectedI18nKey, pair.first)
    Assertions.assertEquals(expectedCounter, pair.second)

    pair = TimeLeft.getI18nKey(Date(System.currentTimeMillis() + secondsOffset * 1000 + 1000), if (allowNegativeTimes) null else "negative")
    Assertions.assertEquals(expectedI18nKey, pair.first)
    Assertions.assertEquals(expectedCounter, pair.second)
  }
}
