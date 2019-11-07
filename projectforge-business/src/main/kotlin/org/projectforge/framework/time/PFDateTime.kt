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

import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Immutable holder of [ZonedDateTime] for transforming to [java.util.Date] (once) if used several times.
 * Zone date times will be generated automatically with the context user's time zone.
 */
class PFDateTime private constructor(val dateTime: ZonedDateTime) {

    private var _utilDate: java.util.Date? = null
    /**
     * @return The date as java.util.Date. java.util.Date is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    val utilDate: java.util.Date
        get() {
            if (_utilDate == null)
                _utilDate = java.util.Date.from(dateTime.toInstant())
            return _utilDate!!
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

    val month: Month
        get() = dateTime.month

    /**
     * Gets the month-of-year field from 1 to 12.
     */
    val monthValue: Int
        get() = dateTime.monthValue

    val dayOfMonth: Int
        get() = dateTime.dayOfMonth

    val beginOfMonth: PFDateTime
        get() = PFDateTime(PFDateTimeUtils.getBeginOfDay(dateTime.withDayOfMonth(1)))

    val endOfMonth: PFDateTime
        get() {
            val nextMonth = dateTime.plusMonths(1).withDayOfMonth(1)
            return PFDateTime(PFDateTimeUtils.getBeginOfDay(nextMonth.withDayOfMonth(1)))
        }

    val beginOfWeek: PFDateTime
        get() {
            val startOfWeek = PFDateTimeUtils.getBeginOfWeek(this.dateTime)
            return PFDateTime(startOfWeek)
        }

    val endOfWeek: PFDateTime
        get() {
            val startOfWeek = PFDateTimeUtils.getBeginOfWeek(this.dateTime).plusDays(7)
            return PFDateTime(startOfWeek)
        }

    val beginOfDay: PFDateTime
        get() {
            val startOfDay = PFDateTimeUtils.getBeginOfDay(dateTime)
            return PFDateTime(startOfDay)
        }

    val endOfDay: PFDateTime
        get() {
            val endOfDay = PFDateTimeUtils.getEndOfDay(dateTime)
            return PFDateTime(endOfDay)
        }

    val epochSeconds: Long
        get() = dateTime.toEpochSecond()

    /**
     * Date part as ISO string: "yyyy-MM-dd HH:mm" in UTC.
     */
    val isoString: String
        get() = isoDateTimeFormatterMinutes.format(dateTime)

    /**
     * Date as JavaScript string: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" (UTC).
     */
    val javaScriptString: String
        get() = jsDateTimeFormatter.format(dateTime)

    val zone: ZoneId
        get() = dateTime.zone

    val timeZone: java.util.TimeZone
        get() = java.util.TimeZone.getTimeZone(dateTime.zone)

    fun isBefore(other: PFDateTime): Boolean {
        return dateTime.isBefore(other.dateTime)
    }

    fun isAfter(other: PFDateTime): Boolean {
        return dateTime.isAfter(other.dateTime)
    }

    fun daysBetween(other: PFDateTime): Long {
        return ChronoUnit.DAYS.between(dateTime, other.dateTime)
    }

    fun plusDays(days: Long): PFDateTime {
        return PFDateTime(dateTime.plusDays(days))
    }

    fun minusDays(days: Long): PFDateTime {
        return PFDateTime(dateTime.minusDays(days))
    }

    fun plusMonths(months: Long): PFDateTime {
        return PFDateTime(dateTime.plusMonths(months))
    }

    fun minusMonths(months: Long): PFDateTime {
        return PFDateTime(dateTime.minusMonths(months))
    }

    fun plusYears(years: Long): PFDateTime {
        return PFDateTime(dateTime.plusYears(years))
    }

    fun minusYears(years: Long): PFDateTime {
        return PFDateTime(dateTime.minusYears(years))
    }

    companion object {
        /**
         * Sets the user's time zone.
         */
        @JvmStatic
        fun from(epochSeconds: Long?, nowIfNull: Boolean = false): PFDateTime? {
            if (epochSeconds == null)
                return if (nowIfNull) now() else null
            val instant = Instant.ofEpochSecond(epochSeconds)
            return PFDateTime(ZonedDateTime.ofInstant(instant, getUsersZoneId()))
        }

        /**
         * Sets the user's time zone.
         */
        @JvmStatic
        fun from(localDateTime: LocalDateTime?, nowIfNull: Boolean = false): PFDateTime? {
            if (localDateTime == null)
                return if (nowIfNull) now() else null
            return PFDateTime(ZonedDateTime.of(localDateTime, getUsersZoneId()))
        }

        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        fun from(localDate: LocalDate?, nowIfNull: Boolean = false): PFDateTime? {
            if (localDate == null)
                return if (nowIfNull) now() else null
            val localDateTime = LocalDateTime.of(localDate, LocalTime.MIDNIGHT)
            return PFDateTime(ZonedDateTime.of(localDateTime, getUsersZoneId()))
        }

        /**
         * @param timeZone: TimeZone to use, if not given, the user's time zone (from ThreadLocalUserContext) is used.
         */
        @JvmStatic
        fun from(date: java.util.Date?, nowIfNull: Boolean = false, timeZone: TimeZone? = null): PFDateTime? {
            if (date == null)
                return if (nowIfNull) now() else null
            return if (date is java.sql.Date) { // Yes, this occurs!
                from(date.toLocalDate())
            } else {
                PFDateTime(date.toInstant().atZone(timeZone?.toZoneId() ?: getUsersZoneId()))
            }
        }

        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        fun from(date: java.sql.Date?, nowIfNull: Boolean = false): PFDateTime? {
            if (date == null)
                return if (nowIfNull) now() else null
            val dateTime = date.toInstant().atZone(getUsersZoneId())
            return PFDateTime(dateTime)
        }

        @JvmStatic
        fun now(): PFDateTime {
            return PFDateTime(ZonedDateTime.now(getUsersZoneId()))
        }

        @JvmStatic
        fun getUsersZoneId(): ZoneId {
            return ThreadLocalUserContext.getTimeZone().toZoneId()
        }

        /**
         * Parses the given date as UTC and converts it to the user's zoned date time.
         * @throws DateTimeParseException if the text cannot be parsed
         */
        @JvmStatic
        fun parseUTCDate(str: String?, dateTimeFormatter: DateTimeFormatter): PFDateTime? {
            if (str.isNullOrBlank())
                return null
            val local = LocalDateTime.parse(str, dateTimeFormatter) // Parses UTC as local date.
            val utcZoned = ZonedDateTime.of(local, ZoneId.of("UTC"))
            val userZoned = utcZoned.withZoneSameInstant(getUsersZoneId())
            return PFDateTime(userZoned)
        }

        /**
         * Parses the given date as UTC and converts it to the user's zoned date time.
         * Tries the following formatters:
         *
         * number (epoch in seconds), "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss.SSS.'Z'"
         * @throws DateTimeException if the text cannot be parsed
         */
        @JvmStatic
        fun parseUTCDate(str: String?): PFDateTime? {
            if (str.isNullOrBlank())
                return null
            if (StringUtils.isNumeric(str)) {
                return from(str.toLong())
            }
            if (str.contains("T")) { // yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
                return parseUTCDate(str, jsDateTimeFormatter)
            }
            val colonPos = str.indexOf(':')
            return if (colonPos < 0) {
                throw DateTimeException("Can't parse date string '$str'. Supported formats are 'yyyy-MM-dd HH:mm', 'yyyy-MM-dd HH:mm:ss', 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'' and numbers as epoch seconds.")
            } else if (str.indexOf(':', colonPos + 1) < 0) { // yyyy-MM-dd HH:mm
                parseUTCDate(str, isoDateTimeFormatterMinutes)
            } else { // yyyy-MM-dd HH:mm:ss
                parseUTCDate(str, isoDateTimeFormatterSeconds)
            }
        }

        private val log = org.slf4j.LoggerFactory.getLogger(PFDateTime::class.java)

        private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
        private val isoDateTimeFormatterMinutes = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)
        private val isoDateTimeFormatterSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
        private val jsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
        // private val jsonDateTimeFormatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern)
    }
}
