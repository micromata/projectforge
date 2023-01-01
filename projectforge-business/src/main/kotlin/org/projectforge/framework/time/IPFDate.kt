/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.time

import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalUnit

interface IPFDate<T> : Comparable<T> {

  val year: Int

  val month: Month

  /**
   * The day represented by this object (time zone is considered, if any).
   */
  val localDate: LocalDate

  /**
   * Uses the locale configured in projectforge.properties. Ensures, that every user of ProjectForge uses same week-of-year-algorithm.
   */
  val weekOfYear: Int

  val dayOfMonth: Int

  val dayOfWeek: DayOfWeek

  val dayOfYear: Int

  val beginOfYear: T
  val endOfYear: T

  /**
   * 1 - January, ..., 12 - December.
   */
  val monthValue: Int
    get() = month.value

  val beginOfMonth: T
  val endOfMonth: T

  val beginOfWeek: T
  val endOfWeek: T

  val numberOfDaysInYear: Int

  val isBeginOfWeek: Boolean

  val isFirstDayOfWeek: Boolean
    get() = dayOfWeek == PFDayUtils.getFirstDayOfWeek()

  fun withYear(year: Int): T

  fun withMonth(month: Month): T

  /**
   * 1 (January) to 12 (December)
   */
  fun withMonth(month: Int): T

  fun withDayOfYear(dayOfYear: Int): T

  fun withDayOfMonth(dayOfMonth: Int): T

  /**
   * 1 - first day of week (locale dependent, e. g. Monday or Sunday).
   * 7 - last day of week.
   */
  fun withDayOfWeek(dayOfWeek: Int): T

  fun withDayOfWeek(dayOfWeek: DayOfWeek): T {
    return this.withDayOfWeek(dayOfWeek.value)
  }

  fun isBefore(other: T): Boolean
  fun isAfter(other: T): Boolean

  fun daysBetween(other: T): Long

  fun plusDays(days: Long): T
  fun minusDays(days: Long): T

  fun plusWeeks(weeks: Long): T
  fun minusWeeks(weeks: Long): T

  fun plusMonths(months: Long): T
  fun minusMonths(months: Long): T

  fun monthsBetween(other: T): Long

  fun plusYears(years: Long): T
  fun minusYears(years: Long): T

  fun plus(amountToAdd: Long, temporalUnit: TemporalUnit): T
  fun minus(amountToSubtract: Long, temporalUnit: TemporalUnit): T

  fun isSameDay(other: T): Boolean

  fun isWeekend(): Boolean {
    return DayOfWeek.SUNDAY == dayOfWeek || DayOfWeek.SATURDAY == dayOfWeek
  }

  fun isHoliday(): Boolean {
    return Holidays.instance.isHoliday(this)
  }

  fun isHolidayOrWeekend(): Boolean {
    return isWeekend() || isHoliday()
  }

  /**
   * If now formatter is given, the formatter for the thread local user will be used or, if not given, in default format.
   */
  fun format(formatter: DateTimeFormatter): String

  val dayOfWeekAsShortString: String?
    get() = dayOfWeek.getDisplayName(TextStyle.SHORT, ThreadLocalUserContext.locale)

  val dayOfWeekAsString: String?
    get() = dayOfWeek.getDisplayName(TextStyle.SHORT, ThreadLocalUserContext.locale)

  /**
   * Date part as ISO string: "yyyy-MM-dd" in UTC.
   */
  val isoString: String

  /**
   * @return The date as java.util.Date. java.util.Date is only calculated, if this getter is called and it
   * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
   */
  val utilDate: java.util.Date

  /**
   * @return The date as java.sql.Date. java.sql.Date is only calculated, if this getter is called and it
   * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
   */
  val sqlDate: java.sql.Date
}
