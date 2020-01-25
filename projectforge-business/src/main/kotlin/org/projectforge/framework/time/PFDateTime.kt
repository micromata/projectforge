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

import org.apache.commons.lang3.ObjectUtils
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
 * All date time acrobatics of ProjectForge should be done by [PFDateTime] or [PFDay]>.
 * Immutable holder of [ZonedDateTime] for transforming to [java.util.Date] (once) if used several times.
 * Zone date times will be generated automatically with the context user's time zone.
 */
class PFDateTime internal constructor(val dateTime: ZonedDateTime,
                                      val locale: Locale,
                                      val precision: DatePrecision?)
    : IPFDate<PFDateTime> {

    /**
     * For parsing dates from long values: how to interpret the number?
     */
    enum class NumberFormat { EPOCH_SECONDS, EPOCH_MILLIS }

    override val year: Int
        get() = dateTime.year

    override val month: Month
        get() = dateTime.month

    /**
     * Gets the month-of-year field from 1 (January) to 12 (December).
     */
    override val monthValue: Int
        get() = dateTime.monthValue

    /**
     * Gets the month-of-year field from 0 (January) to 11 (December).
     */
    val monthCompatibilityValue: Int
        get() = monthValue - 1

    override val dayOfYear: Int
        get() = dateTime.dayOfYear

    override val beginOfYear: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getBeginOfYear(dateTime), locale, precision)

    override val endOfYear: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getEndfYear(dateTime), locale, precision)

    override val dayOfMonth: Int
        get() = dateTime.dayOfMonth

    val hour: Int
        get() = dateTime.hour

    val minute: Int
        get() = dateTime.minute

    val second: Int
        get() = dateTime.second

    val nano: Int
        get() = dateTime.nano

    override val beginOfMonth: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getBeginOfMonth(dateTime), locale, precision)

    override val endOfMonth: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getEndOfMonth(dateTime), locale, precision)

    override val dayOfWeek: DayOfWeek
        get() = dateTime.dayOfWeek

    /**
     * 1 - MONDAY, ..., 7 - SUNDAY
     */
    val dayOfWeekNumber: Int
        get() = dayOfWeek.value

    /**
     * 1 - SUNDAY, 2 - MONDAY, ..., 7 - SATURDAY
     */
    val dayOfWeekCompatibilityNumber: Int
        get() = if (dayOfWeek == DayOfWeek.SUNDAY) 1 else dayOfWeekNumber + 1

    /**
     * Uses the locale configured in projectforge.properties. Ensures, that every user of ProjectForge uses same week-of-year-algorithm.
     */
    override val weekOfYear: Int
        get() {
            val systemLocale = ConfigurationServiceAccessor.get().defaultLocale
            val weekFields = WeekFields.of(systemLocale)
            return dateTime.get(weekFields.weekOfWeekBasedYear())
        }

    override val numberOfDaysInYear: Int
        get() = Year.from(dateTime).length()

    override val beginOfWeek: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getBeginOfWeek(dateTime), locale, precision)

    override val isBeginOfWeek: Boolean
        get() = dateTime.dayOfWeek == PFDayUtils.getFirstDayOfWeek() && dateTime.hour == 0 && dateTime.minute == 0 && dateTime.second == 0 && dateTime.nano == 0

    override val endOfWeek: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getEndOfWeek(dateTime), locale, precision)

    val beginOfDay: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getBeginOfDay(dateTime), locale, precision)

    val endOfDay: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getEndOfDay(dateTime), locale, precision)

    override val isFirstDayOfWeek: Boolean
        get() = dayOfWeek == PFDayUtils.getFirstDayOfWeek()

    override fun withYear(year: Int): PFDateTime {
        return PFDateTime(dateTime.withYear(year), locale, precision)
    }

    /**
     * 1 (January) to 12 (December)
     */
    override fun withMonth(month: Int): PFDateTime {
        return PFDateTime(dateTime.withMonth(month), locale, precision)
    }

    /**
     * 0-based: 0 (January) to 11 (December) for backward compability with [java.util.Calendar.MONTH]
     */
    fun withCompabilityMonth(month: Int): PFDateTime {
        return PFDateTime(dateTime.withMonth(month + 1), locale, precision)
    }

    override fun withMonth(month: Month): PFDateTime {
        return PFDateTime(dateTime.withMonth(month.value), locale, precision)
    }

    override fun withDayOfYear(dayOfYear: Int): PFDateTime {
        return PFDateTime(dateTime.withDayOfYear(dayOfYear), locale, precision)
    }

    override fun withDayOfMonth(dayOfMonth: Int): PFDateTime {
        return PFDateTime(dateTime.withDayOfMonth(dayOfMonth), locale, precision)
    }

    /**
     * 1 - first day of week (locale dependent, e. g. Monday or Sunday).
     * 7 - last day of week.
     */
    override fun withDayOfWeek(dayOfWeek: Int): PFDateTime {
        return PFDayUtils.withDayOfWeek(this, dayOfWeek)
    }

    fun withHour(hour: Int): PFDateTime {
        return PFDateTime(dateTime.withHour(hour), locale, precision)
    }

    fun withMinute(minute: Int): PFDateTime {
        return PFDateTime(dateTime.withMinute(minute), locale, precision)
    }

    fun withSecond(second: Int): PFDateTime {
        return PFDateTime(dateTime.withSecond(second), locale, precision)
    }

    /**
     * @return Milli seconds inside second (0..999).
     */
    fun getMilliSecond(): Int {
        return this.nano / 1000000
    }

    /**
     * @param millisOfSecond from 0 to 999
     */
    fun withMilliSecond(millisOfSecond: Int): PFDateTime {
        return withNano(millisOfSecond * 1000000)
    }

    fun withNano(nanoOfSecond: Int): PFDateTime {
        return PFDateTime(dateTime.withNano(nanoOfSecond), locale, precision)
    }

    val epochSeconds: Long
        get() = dateTime.toEpochSecond()

    val epochMilli: Long
        get() = dateTime.toInstant().toEpochMilli()

    @JvmOverloads
    fun format(dateFormatType: DateFormatType = DateFormatType.DATE_TIME_MINUTES): String {
        val formatter = DateFormats.getDateTimeFormatter(dateFormatType)
        return format(formatter)
    }

    override fun format(formatter: DateTimeFormatter): String {
        return dateTime.format(formatter)
    }

    /**
     * Date part as ISO string: "yyyy-MM-dd HH:mm" in UTC.
     */
    override val isoString: String
        get() = format(isoDateTimeFormatterMinutes)

    /**
     * Date part as ISO string: "yyyy-MM-dd HH:mm:ss" in UTC.
     */
    val isoStringSeconds: String
        get() = format(isoDateTimeFormatterSeconds)

    /**
     * Date part as ISO string: "yyyy-MM-dd HH:mm:ss.SSS" in UTC.
     */
    val isoStringMilli: String
        get() = format(isoDateTimeFormatterMilli)

    /**
     * Date as JavaScript string: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" (UTC).
     */
    val javaScriptString: String
        get() = format(jsDateTimeFormatter)

    val zone: ZoneId
        get() = dateTime.zone

    val timeZone: TimeZone
        get() = TimeZone.getTimeZone(dateTime.zone)

    override fun isBefore(other: PFDateTime): Boolean {
        return dateTime.isBefore(other.dateTime)
    }

    override fun isAfter(other: PFDateTime): Boolean {
        return dateTime.isAfter(other.dateTime)
    }

    override fun isSameDay(other: PFDateTime): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear
    }

    override fun isWeekend(): Boolean {
        return DayOfWeek.SUNDAY == dayOfWeek || DayOfWeek.SATURDAY == dayOfWeek
    }

    override fun monthsBetween(other: PFDateTime): Long {
        return ChronoUnit.MONTHS.between(dateTime, other.dateTime)
    }

    fun daysBetween(date: Date): Long {
        return daysBetween(from(date)!!)
    }

    override fun daysBetween(other: PFDateTime): Long {
        return ChronoUnit.DAYS.between(dateTime, other.dateTime)
    }

    override fun plus(amountToAdd: Long, temporalUnit: TemporalUnit): PFDateTime {
        return PFDateTime(dateTime.plus(amountToAdd, temporalUnit), locale, precision)
    }

    override fun minus(amountToSubtract: Long, temporalUnit: TemporalUnit): PFDateTime {
        return PFDateTime(dateTime.minus(amountToSubtract, temporalUnit), locale, precision)
    }

    override fun plusDays(days: Long): PFDateTime {
        return PFDateTime(dateTime.plusDays(days), locale, precision)
    }

    override fun minusDays(days: Long): PFDateTime {
        return PFDateTime(dateTime.minusDays(days), locale, precision)
    }

    override fun plusWeeks(weeks: Long): PFDateTime {
        return PFDateTime(dateTime.plusWeeks(weeks), locale, precision)
    }

    override fun minusWeeks(weeks: Long): PFDateTime {
        return PFDateTime(dateTime.minusWeeks(weeks), locale, precision)
    }

    override fun plusMonths(months: Long): PFDateTime {
        return PFDateTime(dateTime.plusMonths(months), locale, precision)
    }

    override fun minusMonths(months: Long): PFDateTime {
        return PFDateTime(dateTime.minusMonths(months), locale, precision)
    }

    override fun plusYears(years: Long): PFDateTime {
        return PFDateTime(dateTime.plusYears(years), locale, precision)
    }

    override fun minusYears(years: Long): PFDateTime {
        return PFDateTime(dateTime.minusYears(years), locale, precision)
    }

    /**
     * Ensure the given precision by setting / rounding fields such as minutes and seconds. If precision is MINUTE_15 then rounding the
     * minutes down: 00-14 -&gt; 00; 15-29 -&gt; 15, 30-44 -&gt; 30, 45-59 -&gt; 45.
     */
    fun withPrecision(precision: DatePrecision): PFDateTime {
        return PFDateTime(precision.ensurePrecision(dateTime), locale, precision)
    }

    override fun compareTo(other: PFDateTime): Int {
        return ObjectUtils.compare(dateTime, other.dateTime)
    }

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
            if (_utilDate == null)
                _utilDate = Date.from(dateTime.toInstant())
            return _utilDate!!
        }

    private var _calendar: Calendar? = null
    /**
     * @return The date as java.util.Date. java.util.Date is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    val calendar: Calendar
        get() {
            if (_calendar == null) {
                _calendar = Calendar.getInstance(timeZone, locale)
                _calendar!!.time = utilDate
            }
            return _calendar!!
        }

    private var _sqlTimestamp: java.sql.Timestamp? = null
    /**
     * @return The date as java.sql.Timestamp. java.sql.Timestamp is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    val sqlTimestamp: java.sql.Timestamp
        get() {
            if (_sqlTimestamp == null)
                _sqlTimestamp = java.sql.Timestamp.from(dateTime.toInstant())
            return _sqlTimestamp!!
        }

    private var _sqlDate: java.sql.Date? = null

    /**
     * @return The date as java.sql.Date. java.sql.Date is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    override val sqlDate: java.sql.Date
        get() {
            if (_sqlDate == null) {
                _sqlDate = PFDay.from(this)!!.sqlDate
            }
            return _sqlDate!!
        }

    private var _localDate: LocalDate? = null
    /**
     * @return The date as LocalDate. LocalDate is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    override val localDate: LocalDate
        get() {
            if (_localDate == null)
                _localDate = dateTime.toLocalDate()
            return _localDate!!
        }

    companion object {
        /**
         * Sets the user's time zone.
         */
        @JvmStatic
        @JvmOverloads
        fun from(value: Long?,
                 nowIfNull: Boolean = false,
                 zoneId: ZoneId = getUsersZoneId(),
                 locale: Locale = getUsersLocale(),
                 numberFormat: NumberFormat? = NumberFormat.EPOCH_MILLIS)
                : PFDateTime? {
            if (value == null)
                return if (nowIfNull) now() else null
            return if (numberFormat == NumberFormat.EPOCH_SECONDS) {
                from(Instant.ofEpochSecond(value), zoneId, locale)
            } else {
                from(Instant.ofEpochMilli(value), zoneId, locale)
            }
        }

        /**
         * Sets the user's time zone.
         */
        @JvmStatic
        @JvmOverloads
        fun fromMilli(epochMilli: Long?, nowIfNull: Boolean = false, zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime? {
            if (epochMilli == null)
                return if (nowIfNull) now() else null
            return from(Instant.ofEpochMilli(epochMilli), zoneId, locale)
        }

        private fun from(instant: Instant, zoneId: ZoneId, locale: Locale): PFDateTime {
            return PFDateTime(ZonedDateTime.ofInstant(instant, zoneId), locale, null)
        }

        /**
         * Sets the user's time zone.
         */
        @JvmStatic
        @JvmOverloads
        fun from(localDateTime: LocalDateTime?, nowIfNull: Boolean = false, zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime? {
            if (localDateTime == null)
                return if (nowIfNull) now() else null
            return PFDateTime(ZonedDateTime.of(localDateTime, zoneId), locale, null)
        }

        /**
         * Creates midnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        @JvmOverloads
        fun from(localDate: LocalDate?, nowIfNull: Boolean = false, zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime? {
            if (localDate == null)
                return if (nowIfNull) now() else null
            val localDateTime = LocalDateTime.of(localDate, LocalTime.MIDNIGHT)
            return PFDateTime(ZonedDateTime.of(localDateTime, zoneId), locale, null)
        }

        /**
         * @param timeZone: TimeZone to use, if not given, the user's time zone (from ThreadLocalUserContext) is used.
         * @return null if date is null.
         */
        @JvmStatic
        @JvmOverloads
        fun from(date: Date?, nowIfNull: Boolean = false, timeZone: TimeZone? = null, locale: Locale? = null): PFDateTime? {
            if (date == null)
                return if (nowIfNull) now() else null
            val zoneId = timeZone?.toZoneId() ?: getUsersZoneId()
            return if (date is java.sql.Date) { // Yes, this occurs!
                from(date.toLocalDate(), false, zoneId, locale ?: getUsersLocale())
            } else {
                PFDateTime(date.toInstant().atZone(zoneId), locale ?: getUsersLocale(), null)
            }
        }

        /**
         * Creates midnight [ZonedDateTime] from given [LocalDate].
         * @return null if date is null.
         */
        @JvmStatic
        @JvmOverloads
        fun from(date: java.sql.Date?, nowIfNull: Boolean = false, timeZone: TimeZone? = null, locale: Locale? = null): PFDateTime? {
            if (date == null)
                return if (nowIfNull) now() else null
            val zoneId = timeZone?.toZoneId() ?: getUsersZoneId()
            return from(date.toLocalDate(), false, zoneId, locale ?: getUsersLocale())
        }

        @JvmStatic
        @JvmOverloads
        fun now(zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime {
            return PFDateTime(ZonedDateTime.now(zoneId), locale, null)
        }

        internal fun getUsersZoneId(): ZoneId {
            return ThreadLocalUserContext.getTimeZone().toZoneId()
        }

        internal fun getUsersTimeZone(): TimeZone {
            return ThreadLocalUserContext.getTimeZone()
        }

        internal fun getUsersLocale(): Locale {
            return ThreadLocalUserContext.getLocale()
        }

        /**
         *  1-based Month: 1 (January) to 12 (December)
         */
        @JvmStatic
        @JvmOverloads
        fun withDate(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0, millisecond: Int = 0,
                     zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime {
            val dateTime = ZonedDateTime.of(year, month, day, hour, minute, second, millisecond * 1000000, zoneId)
            return PFDateTime(dateTime, locale, null)
        }

        @JvmStatic
        @JvmOverloads
        fun withDate(year: Int, month: Month, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0, millisecond: Int = 0,
                     zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime {
            return withDate(year, month.value, day, hour, minute, second, millisecond, zoneId, locale)
        }

        private val log = org.slf4j.LoggerFactory.getLogger(PFDateTime::class.java)

        internal val isoDateFormatter = PFDay.isoDateFormatter
        internal val isoDateTimeFormatterMinutes = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)
        internal val isoDateTimeFormatterSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
        internal val isoDateTimeFormatterMilli = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC)
        internal val jsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
        // private val jsonDateTimeFormatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern)
    }
}
