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
import java.time.temporal.TemporalUnit
import java.time.temporal.WeekFields
import java.util.*

/**
 * All date time acrobatics of ProjectForge should be done by PFDateTime or PFDate.
 * Immutable holder of [LocalDate] for transforming to [java.sql.Date] (once) if used several times.
 * If you don't need to use [java.sql.Date] you may use [LocalDate] directly.
 */
class PFDay(val date: LocalDate,
            /**
              * Needed for getting localized week of year.
              */
             val locale: Locale) : IPFDate<PFDay> {

    private constructor(instant: Instant, locale: Locale) : this(LocalDate.from(instant), locale)

    override val year: Int
        get() = date.year

    override val month: Month
        get() = date.month

    override val weekOfYear: Int
        get() {
            val weekFields = WeekFields.of(locale)
            return date.get(weekFields.weekOfWeekBasedYear())
        }

    override val dayOfMonth: Int
        get() = date.dayOfMonth

    override val dayOfWeek: DayOfWeek
        get() = date.dayOfWeek

    override val dayOfYear: Int
        get() = date.dayOfYear

    override val beginOfYear: PFDay
        get() = PFDay(PFDayUtils.getBeginOfYear(date), locale)

    override val endOfYear: PFDay
        get() = PFDay(PFDayUtils.getEndOfYear(date), locale)

    /**
     * 1 - January, ..., 12 - December.
     */
    override val monthValue: Int
        get() = month.value

    override val beginOfMonth: PFDay
        get() = PFDay(PFDayUtils.getBeginOfMonth(date), locale)

    override val endOfMonth: PFDay
        get() = PFDay(PFDayUtils.getEndOfMonth(date), locale)

    override val beginOfWeek: PFDay
        get() = PFDay(PFDayUtils.getBeginOfWeek(date), locale)

    override val endOfWeek: PFDay
        get() = PFDay(PFDayUtils.getEndOfWeek(date), locale)

    override val numberOfDaysInYear: Int
        get() = Year.from(date).length()

    override val isBeginOfWeek: Boolean
        get() = date.dayOfWeek == PFDayUtils.getFirstDayOfWeek()

    override val isFirstDayOfWeek: Boolean
        get() = dayOfWeek == PFDayUtils.getFirstDayOfWeek()

    override fun withYear(year: Int): PFDay {
        return PFDay(date.withYear(year), locale)
    }

    override fun withMonth(month: Month): PFDay {
        return PFDay(date.withMonth(month.value), locale)
    }

    /**
     * 1 (January) to 12 (December)
     */
    override fun withMonth(month: Int): PFDay {
        return PFDay(date.withMonth(month), locale)
    }

    override fun withDayOfYear(dayOfYear: Int): PFDay {
        return PFDay(date.withDayOfYear(dayOfYear), locale)
    }

    override fun withDayOfMonth(dayOfMonth: Int): PFDay {
        return PFDay(date.withDayOfMonth(dayOfMonth), locale)
    }

    override fun withDayOfWeek(dayOfWeek: Int): PFDay {
        return PFDayUtils.withDayOfWeek(this, dayOfWeek)
    }

    override fun isBefore(other: PFDay): Boolean {
        return date.isBefore(other.date)
    }

    fun isBefore(other: java.sql.Date): Boolean {
        return isBefore(from(other)!!)
    }

    override fun isAfter(other: PFDay): Boolean {
        return date.isAfter(other.date)
    }

    override fun daysBetween(other: PFDay): Long {
        return ChronoUnit.DAYS.between(date, other.date)
    }

    override fun plusDays(days: Long): PFDay {
        return PFDay(date.plusDays(days), locale)
    }

    override fun minusDays(days: Long): PFDay {
        return PFDay(date.minusDays(days), locale)
    }

    override fun plusWeeks(weeks: Long): PFDay {
        return PFDay(date.plusWeeks(weeks), locale)
    }

    override fun minusWeeks(weeks: Long): PFDay {
        return PFDay(date.minusWeeks(weeks), locale)
    }

    override fun plusMonths(months: Long): PFDay {
        return PFDay(date.plusMonths(months), locale)
    }

    override fun minusMonths(months: Long): PFDay {
        return PFDay(date.minusMonths(months), locale)
    }

    override fun monthsBetween(other: PFDay): Long {
        return ChronoUnit.MONTHS.between(date, other.date)
    }

    override fun plusYears(years: Long): PFDay {
        return PFDay(date.plusYears(years), locale)
    }

    override fun minusYears(years: Long): PFDay {
        return PFDay(date.minusYears(years), locale)
    }

    override fun plus(amountToAdd: Long, temporalUnit: TemporalUnit): PFDay {
        return PFDay(date.plus(amountToAdd, temporalUnit), locale)
    }

    override fun minus(amountToSubtract: Long, temporalUnit: TemporalUnit): PFDay {
        return PFDay(date.minus(amountToSubtract, temporalUnit), locale)
    }

    override fun isSameDay(other: PFDay): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear
    }

    override fun isWeekend(): Boolean {
        return DayOfWeek.SUNDAY == dayOfWeek || DayOfWeek.SATURDAY == dayOfWeek
    }

    override fun compareTo(other: PFDay): Int {
        return date.compareTo(other.date)
    }

    override fun format(formatter: DateTimeFormatter): String {
        return date.format(formatter)
    }

    /**
     * Date part as ISO string: "yyyy-MM-dd" in UTC.
     */
    override val isoString: String
        get() = format(isoDateFormatter)

    override fun toString(): String {
        return isoString
    }

    private var _utilDate: Date? = null
    /**
     * @return The date as java.util.Date. java.util.Date is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    override val utilDate: Date
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
    override val sqlDate: java.sql.Date
        get() {
            if (_sqlDate == null) {
                _sqlDate = java.sql.Date.valueOf(date)
            }
            return _sqlDate!!
        }

    companion object {
        /**
         * Creates mindnight [LocalDate] from given [LocalDate].
         */
        @JvmStatic
        @JvmOverloads
        fun from(localDate: LocalDate?, nowIfNull: Boolean = false, locale: Locale = PFDateTime.getUsersLocale()): PFDay? {
            if (localDate == null)
                return if (nowIfNull) now() else null
            return PFDay(localDate, locale)
        }

        /**
         * @param date Date of type java.util.Date or java.sql.Date.
         * @param nowIfNull If date is null: If true now is returned, otherwise null.
         * @param timeZone If not given, the context user's time zone will be used.
         * @param locale If not given, the context user's locale will be used.
         * Creates mindnight [LocalDate] from given [date].
         */
        @JvmStatic
        @JvmOverloads
        fun from(date: Date?, nowIfNull: Boolean = false, timeZone: TimeZone? = PFDateTime.getUsersTimeZone(), locale: Locale = PFDateTime.getUsersLocale()): PFDay? {
            if (date == null)
                return if (nowIfNull) now() else null
            if (date is java.sql.Date) {
                return PFDay(date.toLocalDate(), locale)
            }
            val dateTime = PFDateTime.from(date, false, timeZone, locale)!!
            val localDate = LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
            return PFDay(localDate, locale)
        }

        /**
         * @param dateTime Date of type [Date] or [java.sql.Date].
         * Creates mindnight [LocalDate] from given [date].
         */
        @JvmStatic
        fun from(dateTime: PFDateTime?): PFDay? {
            if (dateTime == null)
                return null
            val localDate = LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
            return PFDay(localDate, dateTime.locale)
        }

        @JvmStatic
        @JvmOverloads
        fun now(locale: Locale = PFDateTime.getUsersLocale()): PFDay {
            return PFDay(LocalDate.now(), locale)
        }

        /**
         *  1-based Month: 1 (January) to 12 (December)
         */
        @JvmStatic
        @JvmOverloads
        fun withDate(year: Int, month: Int, day: Int, locale: Locale = PFDateTime.getUsersLocale()): PFDay {
            return PFDay(LocalDate.of(year, month, day), locale)
        }

        @JvmStatic
        @JvmOverloads
        fun withDate(year: Int, month: Month, day: Int, locale: Locale = PFDateTime.getUsersLocale()): PFDay {
            return PFDay(LocalDate.of(year, month, day), locale)
        }

        internal fun getUsersLocale(): Locale {
            return ThreadLocalUserContext.getLocale()
        }

        private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
