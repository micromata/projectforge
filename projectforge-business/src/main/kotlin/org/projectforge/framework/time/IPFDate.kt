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

package org.projectforge.framework.time

import java.time.DayOfWeek
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalUnit

interface IPFDate<T> : Comparable<T> {

    val year: Int

    val month: Month

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

    val beginOfMonth: T
    val endOfMonth: T

    val beginOfWeek: T
    val endOfWeek: T

    val numberOfDaysInYear: Int

    val isBeginOfWeek: Boolean

    val isFirstDayOfWeek: Boolean

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

    fun isWeekend(): Boolean

    fun format(formatter: DateTimeFormatter): String

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
