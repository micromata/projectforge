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

import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.i18n.UserException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.math.BigDecimal
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.time.temporal.WeekFields
import java.util.*


/**
 * All date time acrobatics of ProjectForge should be done by PFDateTime or PFDate.
 * Immutable holder of [ZonedDateTime] for transforming to [java.util.Date] (once) if used several times.
 * Zone date times will be generated automatically with the context user's time zone.
 */
class PFDateTime private constructor(val dateTime: ZonedDateTime,
                                     val locale: Locale)
    : Comparable<PFDateTime> {

    val year: Int
        get() = dateTime.year

    val month: Month
        get() = dateTime.month

    /**
     * Gets the month-of-year field from 1 (January) to 12 (December).
     */
    val monthValue: Int
        get() = dateTime.monthValue

    /**
     * Gets the month-of-year field from 0 (January) to 11 (December).
     */
    val monthCompatibilityValue: Int
        get() = monthValue - 1

    val dayOfYear: Int
        get() = dateTime.dayOfYear

    val beginOfYear: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getBeginOfYear(dateTime.withDayOfMonth(1)), locale)

    val dayOfMonth: Int
        get() = dateTime.dayOfMonth

    val hour: Int
        get() = dateTime.hour

    val minute: Int
        get() = dateTime.minute

    val second: Int
        get() = dateTime.second

    val nano: Int
        get() = dateTime.nano

    val beginOfMonth: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getBeginOfDay(dateTime.withDayOfMonth(1)), locale)

    val endOfMonth: PFDateTime
        get() {
            val nextMonth = dateTime.plusMonths(1).withDayOfMonth(1)
            return PFDateTime(PFDateTimeUtils.getBeginOfDay(nextMonth.withDayOfMonth(1)), locale)
        }

    val dayOfWeek: DayOfWeek
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

    val weekOfYear: Int
        get() {
            val weekFields = WeekFields.of(locale)
            return dateTime.get(weekFields.weekOfWeekBasedYear())
        }

    val numberOfDaysInYear: Int
        get() = Year.from(dateTime).length()

    val beginOfWeek: PFDateTime
        get() {
            val startOfWeek = PFDateTimeUtils.getBeginOfWeek(this.dateTime)
            return PFDateTime(startOfWeek, locale)
        }

    val endOfWeek: PFDateTime
        get() {
            val startOfWeek = PFDateTimeUtils.getBeginOfWeek(this.dateTime).plusDays(7)
            return PFDateTime(startOfWeek, locale)
        }

    val beginOfDay: PFDateTime
        get() {
            val startOfDay = PFDateTimeUtils.getBeginOfDay(dateTime)
            return PFDateTime(startOfDay, locale)
        }

    val endOfDay: PFDateTime
        get() {
            val endOfDay = PFDateTimeUtils.getEndOfDay(dateTime)
            return PFDateTime(endOfDay, locale)
        }

    val isFirstDayOfWeek: Boolean
        get() = dayOfWeek == PFDateTimeUtils.getFirstDayOfWeek()

    fun withYear(year: Int): PFDateTime {
        return PFDateTime(dateTime.withYear(year), locale)
    }

    /**
     * 1 (January) to 12 (December)
     */
    fun withMonth(month: Int): PFDateTime {
        return PFDateTime(dateTime.withMonth(month), locale)
    }

    fun withDayOfYear(dayOfYear: Int): PFDateTime {
        return PFDateTime(dateTime.withDayOfYear(dayOfYear), locale)
    }

    fun withDayOfMonth(dayOfMonth: Int): PFDateTime {
        return PFDateTime(dateTime.withDayOfMonth(dayOfMonth), locale)
    }

    /**
     * 1 - first day of week (locale dependent, e. g. Monday or Sunday).
     * 7 - last day of week.
     */
    fun withDayOfWeek(dayOfWeek: Int): PFDateTime {
        if (dayOfWeek in 1..7) {
            return if (dayOfWeek == 1) beginOfWeek else beginOfWeek.plusDays((dayOfWeek - 1).toLong())
        } else {
            throw IllegalArgumentException("withDayOfWeek accepts only day of weeks from 1 (first day of week) to 7 (last day of week).")
        }
    }

    fun withHour(hour: Int): PFDateTime {
        return PFDateTime(dateTime.withHour(hour), locale)
    }

    fun withMinute(minute: Int): PFDateTime {
        return PFDateTime(dateTime.withMinute(minute), locale)
    }

    fun withSecond(second: Int): PFDateTime {
        return PFDateTime(dateTime.withSecond(second), locale)
    }

    fun withMilliSecond(millisOfSecond: Int): PFDateTime {
        return PFDateTime(dateTime.withNano(millisOfSecond * 1000), locale)
    }

    fun withNano(nanoOfSecond: Int): PFDateTime {
        return PFDateTime(dateTime.withNano(nanoOfSecond), locale)
    }

    val epochSeconds: Long
        get() = dateTime.toEpochSecond()

    val epochMilli: Long
        get() = dateTime.toInstant().toEpochMilli()

    /**
     * Date part as ISO string: "yyyy-MM-dd HH:mm" in UTC.
     */
    val isoString: String
        get() = isoDateTimeFormatterMinutes.format(dateTime)

    /**
     * Date part as ISO string: "yyyy-MM-dd HH:mm:ss" in UTC.
     */
    val isoStringSeconds: String
        get() = isoDateTimeFormatterSeconds.format(dateTime)

    /**
     * Date as JavaScript string: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" (UTC).
     */
    val javaScriptString: String
        get() = jsDateTimeFormatter.format(dateTime)

    val zone: ZoneId
        get() = dateTime.zone

    val timeZone: TimeZone
        get() = TimeZone.getTimeZone(dateTime.zone)

    fun isBefore(other: PFDateTime): Boolean {
        return dateTime.isBefore(other.dateTime)
    }

    fun isAfter(other: PFDateTime): Boolean {
        return dateTime.isAfter(other.dateTime)
    }

    fun isSameDay(other: PFDateTime): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear
    }

    fun isWeekend(): Boolean {
        return DayOfWeek.SUNDAY == dayOfWeek || DayOfWeek.SATURDAY == dayOfWeek
    }

    fun daysBetween(date: Date): Long {
        return daysBetween(from(date)!!)
    }

    fun daysBetween(other: PFDateTime): Long {
        return ChronoUnit.DAYS.between(dateTime, other.dateTime)
    }

    fun plus(amountToAdd: Long, temporalUnit: TemporalUnit): PFDateTime {
        return PFDateTime(dateTime.plus(amountToAdd, temporalUnit), locale)
    }

    fun minus(amountToSubtract: Long, temporalUnit: TemporalUnit): PFDateTime {
        return PFDateTime(dateTime.minus(amountToSubtract, temporalUnit), locale)
    }

    fun plusDays(days: Long): PFDateTime {
        return PFDateTime(dateTime.plusDays(days), locale)
    }

    fun minusDays(days: Long): PFDateTime {
        return PFDateTime(dateTime.minusDays(days), locale)
    }

    fun plusWeeks(weeks: Long): PFDateTime {
        return PFDateTime(dateTime.plusWeeks(weeks), locale)
    }

    fun minusWeeks(weeks: Long): PFDateTime {
        return PFDateTime(dateTime.minusWeeks(weeks), locale)
    }

    fun plusMonths(months: Long): PFDateTime {
        return PFDateTime(dateTime.plusMonths(months), locale)
    }

    fun minusMonths(months: Long): PFDateTime {
        return PFDateTime(dateTime.minusMonths(months), locale)
    }

    fun plusYears(years: Long): PFDateTime {
        return PFDateTime(dateTime.plusYears(years), locale)
    }

    fun minusYears(years: Long): PFDateTime {
        return PFDateTime(dateTime.minusYears(years), locale)
    }

    /**
     * Ensure the given precision by setting / rounding fields such as minutes and seconds. If precision is MINUTE_15 then rounding the
     * minutes down: 00-14 -&gt; 00; 15-29 -&gt; 15, 30-44 -&gt; 30, 45-59 -&gt; 45.
     */
    fun withPrecision(precision: DatePrecision): PFDateTime {
        return PFDateTime(precision.ensurePrecision(dateTime), locale)
    }

    override fun compareTo(other: PFDateTime): Int {
        return ObjectUtils.compare(dateTime, other.dateTime)
    }

    private var _utilDate: Date? = null
    /**
     * @return The date as java.util.Date. java.util.Date is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    val utilDate: Date
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
    val sqlDate: java.sql.Date
        get() {
            if (_sqlDate == null) {
                _sqlDate = PFDate.from(this)!!.sqlDate
            }
            return _sqlDate!!
        }

    private var _localDate: LocalDate? = null
    /**
     * @return The date as LocalDate. LocalDate is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    val localDate: LocalDate
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
        fun from(epochSeconds: Long?, nowIfNull: Boolean = false, zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime? {
            if (epochSeconds == null)
                return if (nowIfNull) now() else null
            val instant = Instant.ofEpochSecond(epochSeconds)
            return PFDateTime(ZonedDateTime.ofInstant(instant, zoneId), locale)
        }

        /**
         * Sets the user's time zone.
         */
        @JvmStatic
        @JvmOverloads
        fun from(localDateTime: LocalDateTime?, nowIfNull: Boolean = false, zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime? {
            if (localDateTime == null)
                return if (nowIfNull) now() else null
            return PFDateTime(ZonedDateTime.of(localDateTime, zoneId), locale)
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
            return PFDateTime(ZonedDateTime.of(localDateTime, zoneId), locale)
        }

        /**
         * @param timeZone: TimeZone to use, if not given, the user's time zone (from ThreadLocalUserContext) is used.
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
                PFDateTime(date.toInstant().atZone(zoneId), locale ?: getUsersLocale())
            }
        }

        /**
         * Creates midnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        @JvmOverloads
        fun from(date: java.sql.Date?, nowIfNull: Boolean = false, timeZone: TimeZone? = null, locale: Locale? = null): PFDateTime? {
            if (date == null)
                return if (nowIfNull) now() else null
            val zoneId = timeZone?.toZoneId() ?: getUsersZoneId()
            val dateTime = date.toInstant().atZone(zoneId)
            return PFDateTime(dateTime, locale ?: getUsersLocale())
        }

        @JvmStatic
        @JvmOverloads
        fun now(zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime {
            return PFDateTime(ZonedDateTime.now(zoneId), locale)
        }

        private fun getUsersZoneId(): ZoneId {
            return ThreadLocalUserContext.getTimeZone().toZoneId()
        }

        private fun getUsersTimeZone(): TimeZone {
            return ThreadLocalUserContext.getTimeZone()
        }

        private fun getUsersLocale(): Locale {
            return ThreadLocalUserContext.getLocale()
        }

        /**
         * Parses the given date as UTC and converts it to the user's zoned date time.
         * @throws DateTimeParseException if the text cannot be parsed
         */
        @JvmStatic
        @JvmOverloads
        fun parseUTCDate(str: String?, dateTimeFormatter: DateTimeFormatter, zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime? {
            if (str.isNullOrBlank())
                return null
            val local = LocalDateTime.parse(str, dateTimeFormatter) // Parses UTC as local date.
            val utcZoned = ZonedDateTime.of(local, ZoneId.of("UTC"))
            val userZoned = utcZoned.withZoneSameInstant(zoneId)
            return PFDateTime(userZoned, locale)
        }

        /**
         * Parses the given date as UTC and converts it to the user's zoned date time.
         * Tries the following formatters:
         *
         * number (epoch in seconds), "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss.SSS.'Z'"
         * @throws DateTimeException if the text cannot be parsed
         */
        @JvmStatic
        @JvmOverloads
        fun parseUTCDate(str: String?, zoneId: ZoneId = getUsersZoneId(), locale: Locale = getUsersLocale()): PFDateTime? {
            if (str.isNullOrBlank())
                return null
            if (StringUtils.isNumeric(str)) {
                return from(str.toLong())
            }
            if (str.contains("T")) { // yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
                return parseUTCDate(str, jsDateTimeFormatter)
            }
            val colonPos = str.indexOf(':')
            return when {
                colonPos < 0 -> {
                    throw DateTimeException("Can't parse date string '$str'. Supported formats are 'yyyy-MM-dd HH:mm', 'yyyy-MM-dd HH:mm:ss', 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'' and numbers as epoch seconds.")
                }
                str.indexOf(':', colonPos + 1) < 0 -> { // yyyy-MM-dd HH:mm
                    parseUTCDate(str, isoDateTimeFormatterMinutes, zoneId, locale)
                }
                else -> { // yyyy-MM-dd HH:mm:ss
                    parseUTCDate(str, isoDateTimeFormatterSeconds, zoneId, locale)
                }
            }
        }

        @JvmStatic
        fun getNumberOfWorkingDays(from: PFDateTime, to: PFDateTime): BigDecimal {
            Validate.notNull(from)
            Validate.notNull(to)
            val holidays = Holidays()
            if (to.isBefore(from)) {
                return BigDecimal.ZERO
            }
            if (from.isSameDay(to)) {
                if (holidays.isWorkingDay(from.dateTime)) {
                    val workFraction = holidays.getWorkFraction(from)
                    return workFraction?: BigDecimal.ONE
                } else {
                    return BigDecimal.ZERO
                }
            }
            var numberOfWorkingDays = BigDecimal.ZERO
            var numberOfFullWorkingDays = 0
            var dayCounter = 1
            do {
                if (dayCounter++ > 740) { // Endless loop protection, time period greater 2 years.
                    throw UserException(
                            "getNumberOfWorkingDays does not support calculation of working days for a time period greater than two years!")
                }
                if (holidays.isWorkingDay(from.dateTime)) {
                    val workFraction = holidays.getWorkFraction(from)
                    if (workFraction != null) {
                        numberOfWorkingDays = numberOfWorkingDays.add(workFraction)
                    } else {
                        numberOfFullWorkingDays++
                    }
                }
                from.plusDays(1)
            } while (!from.isSameDay(to))
            numberOfWorkingDays = numberOfWorkingDays.add(BigDecimal(numberOfFullWorkingDays))
            return numberOfWorkingDays
        }

        private val log = org.slf4j.LoggerFactory.getLogger(PFDateTime::class.java)

        private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
        private val isoDateTimeFormatterMinutes = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)
        private val isoDateTimeFormatterSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
        private val jsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
        // private val jsonDateTimeFormatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern)
    }
}
