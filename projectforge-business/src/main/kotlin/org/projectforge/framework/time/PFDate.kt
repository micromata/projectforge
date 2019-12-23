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

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalUnit
import java.time.temporal.WeekFields
import java.util.*

/**
 * All date time acrobatics of ProjectForge should be done by PFDateTime or PFDate.
 * Immutable holder of [LocalDate] for transforming to [java.sql.Date] (once) if used several times.
 * If you don't need to use [java.sql.Date] you may use [LocalDate] directly.
 */
class PFDate(val date: LocalDate,
             /**
              * Needed for getting localized week of year.
              */
             val locale: Locale): Comparable<PFDate> {

    private constructor(instant: Instant, locale: Locale) : this(LocalDate.from(instant), locale)

    val year: Int
        get() = date.year

    val month: Month
        get() = date.month

    val weekOfYear: Int
        get() {
            val weekFields = WeekFields.of(locale)
            return date.get(weekFields.weekOfWeekBasedYear())
        }

    val dayOfMonth: Int
        get() = date.dayOfMonth

    val dayOfWeek: DayOfWeek
        get() = date.dayOfWeek

    val dayOfYear: Int
        get() = date.dayOfYear

    val beginOfYear: PFDate
        get() = PFDate(date.withMonth(1), locale).beginOfMonth

    /**
     * 1 - January, ..., 12 - December.
     */
    val monthValue: Int
        get() = month.value

    val beginOfMonth: PFDate
        get() = PFDate(date.withDayOfMonth(1), locale)

    val endOfMonth: PFDate
        get() = PFDate(date.with(TemporalAdjusters.lastDayOfMonth()), locale)

    val beginOfWeek: PFDate
        get() {
            val startOfWeek = PFDateTimeUtils.getBeginOfWeek(this.date)
            return PFDate(startOfWeek, locale)
        }

    val endOfWeek: PFDate
        get() {
            val startOfWeek = PFDateTimeUtils.getBeginOfWeek(this.date).plusDays(7)
            return PFDate(startOfWeek, locale)
        }

    fun withYear(year: Int): PFDate {
        return PFDate(date.withYear(year), locale)
    }

    /**
     * 1 (January) to 12 (December)
     */
    fun withMonth(month: Int): PFDate {
        return PFDate(date.withMonth(month), locale)
    }

    fun withDayOfYear(dayOfYear: Int): PFDate {
        return PFDate(date.withDayOfYear(dayOfYear), locale)
    }

    fun withDayOfMonth(dayOfMonth: Int): PFDate {
        return PFDate(date.withDayOfMonth(dayOfMonth), locale)
    }

    fun isBefore(other: PFDate): Boolean {
        return date.isBefore(other.date)
    }

    fun isBefore(other: java.sql.Date): Boolean {
        return isBefore(from(other)!!)
    }

    fun isAfter(other: PFDate): Boolean {
        return date.isAfter(other.date)
    }

    fun daysBetween(other: PFDate): Long {
        return ChronoUnit.DAYS.between(date, other.date)
    }

    fun plusDays(days: Long): PFDate {
        return PFDate(date.plusDays(days), locale)
    }

    fun plusWeeks(weeks: Long): PFDate {
        return PFDate(date.plusWeeks(weeks), locale)
    }

    fun plusMonths(months: Long): PFDate {
        return PFDate(date.plusMonths(months), locale)
    }

    fun plusYears(years: Long): PFDate {
        return PFDate(date.plusYears(years), locale)
    }

    fun plus(amountToAdd: Long, temporalUnit: TemporalUnit): PFDate {
        return PFDate(date.plus(amountToAdd, temporalUnit), locale)
    }

    fun isSameDay(other: PFDate): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear
    }

    fun isWeekend(): Boolean {
        return DayOfWeek.SUNDAY == dayOfWeek || DayOfWeek.SATURDAY == dayOfWeek
    }

    fun format(formatter: DateTimeFormatter): String {
        return date.format(formatter)
    }

    override fun compareTo(other: PFDate): Int {
        return date.compareTo(other.date)
    }

    /**
     * Date part as ISO string: "yyyy-MM-dd" in UTC.
     */
    val isoString: String
        get() = isoDateFormatter.format(date)


    private var _utilDate: java.util.Date? = null
    /**
     * @return The date as java.util.Date. java.util.Date is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    val utilDate: java.util.Date
        get() {
            if (_utilDate == null) {
                _utilDate = PFDateTime.from(date)!!.utilDate
            }
            return _utilDate!!
        }

    private var _sqlDate: java.sql.Date? = null
    /**
     * @return The date as java.sql.Date. java.sql.Date is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    val sqlDate: java.sql.Date
        get() {
            if (_sqlDate == null) {
                _sqlDate = java.sql.Date.valueOf(date)
            }
            return _sqlDate!!
        }

    companion object {
        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        @JvmOverloads
        fun from(localDate: LocalDate?, nowIfNull: Boolean = false, locale: Locale = PFDateTime.getUsersLocale()): PFDate? {
            if (localDate == null)
                return if (nowIfNull) now() else null
            return PFDate(localDate, locale)
        }

        /**
         * @param date Date of type java.util.Date or java.sql.Date.
         * Creates mindnight [ZonedDateTime] from given [date].
         */
        @JvmStatic
        @JvmOverloads
        fun from(date: java.util.Date?, nowIfNull: Boolean = false, timeZone: TimeZone? = PFDateTime.getUsersTimeZone(), locale: Locale = PFDateTime.getUsersLocale()): PFDate? {
            if (date == null)
                return if (nowIfNull) now() else null
            if (date is java.sql.Date) {
                return PFDate(date.toLocalDate(), locale)
            }
            val dateTime = PFDateTime.from(date, false, timeZone, locale)!!
            val localDate = LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
            return PFDate(localDate, locale)
        }

        /**
         * @param dateTime Date of type java.util.Date or java.sql.Date.
         * @param nowIfNull If true, then now will be returned as default date instead of null if dateTime is null.
         * Creates mindnight [ZonedDateTime] from given [date].
         */
        @JvmStatic
        @JvmOverloads
        fun from(dateTime: PFDateTime?, nowIfNull: Boolean = false, locale: Locale = PFDateTime.getUsersLocale()): PFDate? {
            if (dateTime == null)
                return if (nowIfNull) now() else null
            val localDate = LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
            return PFDate(localDate, locale)
        }

        @JvmStatic
        @JvmOverloads
        fun now(locale: Locale = PFDateTime.getUsersLocale()): PFDate {
            return PFDate(LocalDate.now(), locale)
        }

        /**
         *  1-based Month: 1 (January) to 12 (December)
         */
        @JvmStatic
        @JvmOverloads
        fun withDate(year: Int, month: Int, day: Int, locale: Locale = PFDateTime.getUsersLocale()): PFDate {
            return PFDate(LocalDate.of(year, month, day), locale)
        }

        @JvmStatic
        @JvmOverloads
        fun withDate(year: Int, month: Month, day: Int, locale: Locale = PFDateTime.getUsersLocale()): PFDate {
            return PFDate(LocalDate.of(year, month, day), locale)
        }

        internal fun getUsersLocale(): Locale {
            return ThreadLocalUserContext.getLocale()
        }

        private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
