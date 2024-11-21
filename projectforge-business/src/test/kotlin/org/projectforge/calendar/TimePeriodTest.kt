/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.calendar

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.projectforge.framework.time.TimePeriod
import org.projectforge.framework.time.TimePeriod.Companion.getDurationHours
import org.projectforge.framework.utils.RoundUnit
import org.projectforge.business.test.TestSetup.init
import java.math.RoundingMode
import java.time.Month
import java.util.*

class TimePeriodTest {
  @Test
  fun testDurationHours() {
    checkDurationHours("1", 0.5, RoundUnit.INT, RoundingMode.HALF_UP)
    checkDurationHours("0", 0.4, RoundUnit.INT, RoundingMode.HALF_UP)
    checkDurationHours("0.0", 0.24, RoundUnit.HALF, RoundingMode.HALF_UP)
    checkDurationHours("0.5", 0.25, RoundUnit.HALF, RoundingMode.HALF_UP)
    checkDurationHours("0.5", 0.74, RoundUnit.HALF, RoundingMode.HALF_UP)
    checkDurationHours("1.0", 0.75, RoundUnit.HALF, RoundingMode.HALF_UP)
    checkDurationHours("0.00", 0.12, RoundUnit.QUARTER, RoundingMode.HALF_UP)
    checkDurationHours("0.25", 0.13, RoundUnit.QUARTER, RoundingMode.HALF_UP)
    checkDurationHours("7.75", 7.87, RoundUnit.QUARTER, RoundingMode.HALF_UP)
    checkDurationHours("8.00", 7.88, RoundUnit.QUARTER, RoundingMode.HALF_UP)
    checkDurationHours("0.0", 0.09, RoundUnit.FIFTH, RoundingMode.HALF_UP)
    checkDurationHours("0.2", 0.1, RoundUnit.FIFTH, RoundingMode.HALF_UP)
    checkDurationHours("7.8", 7.76, RoundUnit.FIFTH, RoundingMode.HALF_UP)
    checkDurationHours("0.0", 0.04, RoundUnit.TENTH, RoundingMode.HALF_UP)
    checkDurationHours("0.1", 0.05, RoundUnit.TENTH, RoundingMode.HALF_UP)
    checkDurationHours("1.0", 0.95, RoundUnit.TENTH, RoundingMode.HALF_UP)
  }

  private fun checkDurationHours(expected: String, hours: Double, rounUnit: RoundUnit, roundingMode: RoundingMode) {
    val start = Date()
    val end = Date(start.time + (hours * 1000 * 3600).toInt())
    Assertions.assertEquals(expected, getDurationHours(start, end, rounUnit, roundingMode).toString())
  }

  @Test
  fun testTimePeriod() {
    val dateTime1 = withDate(1970, Month.NOVEMBER, 21, 0, 0, 0)
    var dateTime2 = dateTime1.withHour(10)
    var timePeriod = TimePeriod(dateTime1.utilDate, dateTime2.utilDate)
    assertResultArray(intArrayOf(0, 10, 0), timePeriod.durationFields)
    assertResultArray(intArrayOf(1, 2, 0), timePeriod.getDurationFields(8))
    dateTime2 = dateTime2.withDayOfMonth(22).withHour(0)
    timePeriod = TimePeriod(dateTime1.utilDate, dateTime2.utilDate)
    assertResultArray(intArrayOf(1, 0, 0), timePeriod.durationFields)
    assertResultArray(intArrayOf(3, 0, 0), timePeriod.getDurationFields(8))
    assertResultArray(intArrayOf(0, 24, 0), timePeriod.getDurationFields(8, 25))
    assertResultArray(intArrayOf(3, 0, 0), timePeriod.getDurationFields(8, 24))
    dateTime2 = dateTime2.withDayOfMonth(21).withHour(23).withMinute(59).withSecond(59)
    timePeriod = TimePeriod(dateTime1.utilDate, dateTime2.utilDate)
    assertResultArray(intArrayOf(0, 23, 59), timePeriod.durationFields)
    assertResultArray(intArrayOf(2, 7, 59), timePeriod.getDurationFields(8))
    assertResultArray(intArrayOf(0, 23, 59), timePeriod.getDurationFields(8, 24))
    assertResultArray(intArrayOf(2, 7, 59), timePeriod.getDurationFields(8, 22))
    dateTime2 = dateTime2.withDayOfMonth(23).withHour(5).withMinute(30).withSecond(0)
    timePeriod = TimePeriod(dateTime1.utilDate, dateTime2.utilDate)
    assertResultArray(intArrayOf(2, 5, 30), timePeriod.durationFields)
    assertResultArray(intArrayOf(6, 5, 30), timePeriod.getDurationFields(8))
    assertResultArray(intArrayOf(0, 53, 30), timePeriod.getDurationFields(8, 54))
    assertResultArray(intArrayOf(6, 5, 30), timePeriod.getDurationFields(8, 53))
  }

  private fun assertResultArray(required: IntArray, result: IntArray) {
    Assertions.assertEquals(required[0], result[0], "days")
    Assertions.assertEquals(required[1], result[1], "hours")
    Assertions.assertEquals(required[2], result[2], "minutes")
  }

  @Test
  fun asStringTest() {
    Assertions.assertEquals("???-???", TimePeriod(null as Date?, null).formattedString)
    Assertions.assertEquals(
      "21.11.2021 08:30-???",
      TimePeriod(withDate(2021, Month.NOVEMBER, 21, 8, 30).utilDate, null).formattedString
    )
    Assertions.assertEquals(
      "21.11.2021 08:30-10:30",
      TimePeriod(withDate(2021, Month.NOVEMBER, 21, 8, 30).utilDate, withDate(2021, Month.NOVEMBER, 21, 10, 30).utilDate).formattedString
    )
    Assertions.assertEquals(
      "21.11.2021 08:30-22.11.2021 10:30",
      TimePeriod(withDate(2021, Month.NOVEMBER, 21, 8, 30).utilDate, withDate(2021, Month.NOVEMBER, 22, 10, 30).utilDate).formattedString
    )
    Assertions.assertEquals(
      "21.11.2021 22:30-10:30",
      TimePeriod(withDate(2021, Month.NOVEMBER, 21, 22, 30).utilDate, withDate(2021, Month.NOVEMBER, 22, 10, 30).utilDate).formattedString,
      "Less than 24 hours duration, don't show to date."
    )
    Assertions.assertEquals(
      "???-22.11.2021 10:30",
      TimePeriod(null, withDate(2021, Month.NOVEMBER, 22, 10, 30).utilDate).formattedString
    )
  }

  companion object {
    @BeforeAll
    @JvmStatic
    fun setUp() {
      // Needed if this tests runs before the ConfigurationTest.
      init()
    }
  }
}
