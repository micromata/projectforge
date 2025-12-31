/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.time.TimeUnit
import java.util.*

internal const val SECOND = 1000L
internal const val MINUTE = 60 * SECOND
internal const val HOUR = 60 * MINUTE
internal const val DAY = 24 * HOUR
private const val MONTH = 30 * DAY

class TimeAgoLeftTest {

  @Test
  fun i18nTest() {
    timeAgoLeft(-100 * SECOND, "negative", -1)
    timeAgoLeft(-125 * SECOND, "minutes", 2, true)
    timeAgoLeft(5 * SECOND, "afewseconds", -1)
    timeAgoLeft(20 * SECOND, "seconds", 21)
    timeAgoLeft(30 * SECOND, "seconds", 31)
    timeAgoLeft(31 * SECOND, "seconds", 32)
    timeAgoLeft(49 * SECOND, "seconds", 50)
    timeAgoLeft(50 * SECOND, "seconds", 51)
    timeAgoLeft(65 * SECOND, "minutes.one", -1)
    timeAgoLeft(125 * SECOND, "minutes", 2)

    timeAgoLeft(HOUR, "hours.one", -1)
    timeAgoLeft(2 * HOUR, "hours", 2)
    timeAgoLeft(DAY, "days.one", -1)
    timeAgoLeft(2 * DAY, "days", 2)
    timeAgoLeft(7 * DAY, "weeks.one", -1)
    timeAgoLeft(14 * DAY, "weeks", 2)
    timeAgoLeft(29 * DAY, "weeks", 4)
    timeAgoLeft(30 * DAY, "months.one", -1)
    for (days in 31..44) {
      timeAgoLeft(days * DAY, "months.one", -1, message = "for $days days")
    }
    for (days in 45..74) {
      timeAgoLeft(days * DAY, "months", 2, message = "for $days days")
    }
    for (days in 75..104) {
      timeAgoLeft(days * DAY, "months", 3, message = "for $days days")
    }

    for (months in 2..12) {
      timeAgoLeft(months * MONTH, "months", months, message = "for $months months")
    }
    for (months in 13..18) {
      timeAgoLeft(months * MONTH, "years.one", -1, message = "for $months months")
    }
    for (months in 19..30) {
      timeAgoLeft(months * MONTH, "years", 2, message = "for $months months")
    }
    timeAgoLeft(365 * DAY, "years.one", -1)
    timeAgoLeft(25 * MONTH, "years", 2)
  }

  private fun timeAgoLeft(
    millissOffset: Long,
    expectedI18nKey: String,
    expectedCounter: Int,
    allowNegativeTimes: Boolean = false,
    message: String? = null,
    maxUnit: TimeUnit? = null,
  ) {
    var pair = TimeAgo.getI18nKey(Date(System.currentTimeMillis() - millissOffset - 1000), allowNegativeTimes)
    Assertions.assertEquals(expectedI18nKey, pair.first, message)
    Assertions.assertEquals(expectedCounter, pair.second, message)

    pair = TimeLeft.getI18nKey(
      Date(System.currentTimeMillis() + millissOffset + 1000),
      if (allowNegativeTimes) null else "negative",
      maxUnit,
    )
    Assertions.assertEquals(expectedI18nKey, pair.first, message)
    Assertions.assertEquals(expectedCounter, pair.second, message)
  }
}
