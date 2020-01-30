/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.common.DateFormatType
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
class PFDay(val date: LocalDate) : IPFDate<PFDay> {

    private constructor(instant: Instant) : this(LocalDate.from(instant))

    override val year: Int
        get() = date.year

    override val month: Month
        get() = date.month

    override val localDate: LocalDate
        get() = date

    /**
     * Uses the locale configured in projectforge.properties. Ensures, that every user of ProjectForge uses same week-of-year-algorithm.
     */
    override val weekOfYear: Int
        get() {
            val systemLocale = ConfigurationServiceAccessor.get().defaultLocale
            val weekFields = WeekFields.of(systemLocale)
            return date.get(weekFields.weekOfWeekBasedYear())
        }

    override val dayOfMonth: Int
        get() = date.dayOfMonth

    override val dayOfWeek: DayOfWeek
        get() = date.dayOfWeek

    override val dayOfYear: Int
        get() = date.dayOfYear

    override val beginOfYear: PFDay
        get() = PFDay(PFDayUtils.getBeginOfYear(date))

    override val endOfYear: PFDay
        get() = PFDay(PFDayUtils.getEndOfYear(date))

    /**
     * 1 - January, ..., 12 - December.
     */
    override val monthValue: Int
        get() = month.value

    override val beginOfMonth: PFDay
        get() = PFDay(PFDayUtils.getBeginOfMonth(date))

    override val endOfMonth: PFDay
        get() = PFDay(PFDayUtils.getEndOfMonth(date))

    override val beginOfWeek: PFDay
        get() = PFDay(PFDayUtils.getBeginOfWeek(date))

    override val endOfWeek: PFDay
        get() = PFDay(PFDayUtils.getEndOfWeek(date))

    override val numberOfDaysInYear: Int
        get() = Year.from(date).length()

    override val isBeginOfWeek: Boolean
        get() = date.dayOfWeek == PFDayUtils.getFirstDayOfWeek()

    override val isFirstDayOfWeek: Boolean
        get() = dayOfWeek == PFDayUtils.getFirstDayOfWeek()

    override fun withYear(year: Int): PFDay {
        return PFDay(date.withYear(year))
    }

    override fun withMonth(month: Month): PFDay {
        return PFDay(date.withMonth(month.value))
    }

    /**
     * 1 (January) to 12 (December)
     */
    override fun withMonth(month: Int): PFDay {
        return PFDay(date.withMonth(month))
    }

    override fun withDayOfYear(dayOfYear: Int): PFDay {
        return PFDay(date.withDayOfYear(dayOfYear))
    }

    override fun withDayOfMonth(dayOfMonth: Int): PFDay {
        return PFDay(date.withDayOfMonth(dayOfMonth))
    }

    override fun withDayOfWeek(dayOfWeek: Int): PFDay {
        return PFDayUtils.withDayOfWeek(this, dayOfWeek)
    }

    override fun isBefore(other: PFDay): Boolean {
        return date.isBefore(other.date)
    }

    fun isBefore(other: java.sql.Date): Boolean {
        return isBefore(from(other))
    }

    fun isBefore(other: LocalDate): Boolean {
        return date.isBefore(other)
    }

    override fun isAfter(other: PFDay): Boolean {
        return date.isAfter(other.date)
    }

    override fun daysBetween(other: PFDay): Long {
        return ChronoUnit.DAYS.between(date, other.date)
    }

    override fun plusDays(days: Long): PFDay {
        return PFDay(date.plusDays(days))
    }

    override fun minusDays(days: Long): PFDay {
        return PFDay(date.minusDays(days))
    }

    override fun plusWeeks(weeks: Long): PFDay {
        return PFDay(date.plusWeeks(weeks))
    }

    override fun minusWeeks(weeks: Long): PFDay {
        return PFDay(date.minusWeeks(weeks))
    }

    override fun plusMonths(months: Long): PFDay {
        return PFDay(date.plusMonths(months))
    }

    override fun minusMonths(months: Long): PFDay {
        return PFDay(date.minusMonths(months))
    }

    override fun monthsBetween(other: PFDay): Long {
        return ChronoUnit.MONTHS.between(date, other.date)
    }

    override fun plusYears(years: Long): PFDay {
        return PFDay(date.plusYears(years))
    }

    override fun minusYears(years: Long): PFDay {
        return PFDay(date.minusYears(years))
    }

    override fun plus(amountToAdd: Long, temporalUnit: TemporalUnit): PFDay {
        return PFDay(date.plus(amountToAdd, temporalUnit))
    }

    override fun minus(amountToSubtract: Long, temporalUnit: TemporalUnit): PFDay {
        return PFDay(date.minus(amountToSubtract, temporalUnit))
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

    fun format(): String {
        val formatter = DateFormats.getDateTimeFormatter(DateFormatType.DATE)
        return format(formatter)
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
                _utilDate = PFDateTime.from(date).utilDate
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
         * @param localDate Date to use (not null).
         * @return PFDay from given value...
         * @throws java.lang.IllegalStateException if date is null.
         */
        @JvmStatic
        fun from(localDate: LocalDate): PFDay {
            return PFDay(localDate)
        }

        /**
         * @param localDate Date to use (or null).
         * @return PFDay from given value or today if [localDate] is null...
         */
        @JvmStatic
        fun fromOrNow(localDate: LocalDate?): PFDay {
            localDate ?: return now()
            return PFDay(localDate)
        }

        /**
         * @param localDate Date to use (or null).
         * @return PFDay from given value or null if [localDate] is null...
         */
        @JvmStatic
        fun fromOrNull(localDate: LocalDate?): PFDay? {
            localDate ?: return null
            return PFDay(localDate)
        }

        /**
         * @param date Date of type java.util.Date or java.sql.Date (not null).
         * @param timeZone If not given, the context user's time zone will be used.
         * @return PFDay (midnight) from given value...
         * @throws java.lang.IllegalStateException if date is null.
         */
        @JvmStatic
        @JvmOverloads
        fun from(date: Date, timeZone: TimeZone? = null): PFDay {
            if (date is java.sql.Date) {
                return PFDay(date.toLocalDate())
            }
            val dateTime = PFDateTime.from(date, timeZone)
            val localDate = LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
            return PFDay(localDate)
        }

            /**
         * @param date Date of type java.util.Date or java.sql.Date (or null).
         * @param timeZone If not given, the context user's time zone will be used.
         * @return PFDay (midnight) from given value or now if [localDate] is null...
         */
        @JvmStatic
        @JvmOverloads
        fun fromOrNow(date: Date?, timeZone: TimeZone? = null): PFDay {
            date ?: return now()
            return from(date, timeZone)
        }

        /**
         * @param date Date of type java.util.Date or java.sql.Date (or null).
         * @param timeZone If not given, the context user's time zone will be used.
         * @return PFDay (midnight) from given value or null if [localDate] is null...
         */
        @JvmStatic
        @JvmOverloads
        fun fromOrNull(date: Date?, timeZone: TimeZone? = null): PFDay? {
            date ?: return null
            return from(date, timeZone)
        }

        /**
         * @param dateTime Date to convert (not null).
         * @return PFDay (midnight) from given value...
         * @throws java.lang.IllegalStateException if date is null.
         */
        @JvmStatic
        fun from(dateTime: PFDateTime): PFDay {
            val localDate = LocalDate.of(dateTime.year, dateTime.month, dateTime.dayOfMonth)
            return PFDay(localDate)
        }

        /**
         * @param dateTime Date to convert (or null).
         * @return PFDay (midnight) from given value or now if [dateTime] is null...
         */
        @JvmStatic
        fun fromOrNow(dateTime: PFDateTime?): PFDay {
            dateTime ?: return now()
            return from(dateTime)
        }

        /**
         * @param dateTime Date to convert (or null).
         * @return PFDay (midnight) from given value or null if [dateTime] is null...
         */
        @JvmStatic
        fun fromOrNull(dateTime: PFDateTime?): PFDay? {
            dateTime ?: return null
            return from(dateTime)
        }

        @JvmStatic
        fun now(): PFDay {
            return PFDay(LocalDate.now())
        }

        /**
         *  1-based Month: 1 (January) to 12 (December)
         */
        @JvmStatic
        fun withDate(year: Int, month: Int, day: Int): PFDay {
            return PFDay(LocalDate.of(year, month, day))
        }

        /**
         *  1-based Month: 1 (January) to 12 (December)
         */
        @JvmStatic
        fun of(year: Int, month: Int, day: Int): PFDay {
            return PFDay(LocalDate.of(year, month, day))
        }

        @JvmStatic
        fun withDate(year: Int, month: Month, day: Int): PFDay {
            return PFDay(LocalDate.of(year, month, day))
        }

        @JvmStatic
        fun of(year: Int, month: Month, day: Int): PFDay {
            return PFDay(LocalDate.of(year, month, day))
        }

        internal fun getUsersLocale(): Locale {
            return ThreadLocalUserContext.getLocale()
        }

        internal val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
