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

import org.apache.commons.lang3.StringUtils
import org.projectforge.common.DateFormatType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import java.sql.Timestamp
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.*

class PFDateTimeUtils {

    companion object {
        @JvmField
        val ZONE_UTC = ZoneId.of("UTC")

        @JvmField
        val ZONE_EUROPE_BERLIN = ZoneId.of("Europe/Berlin")

        @JvmField
        val TIMEZONE_UTC = TimeZone.getTimeZone("UTC")

        @JvmField
        val TIMEZONE_EUROPE_BERLIN = TimeZone.getTimeZone("Europe/Berlin")


        @JvmStatic
        fun getBeginOfYear(dateTime: ZonedDateTime): ZonedDateTime {
            return getBeginOfDay(dateTime.with(TemporalAdjusters.firstDayOfYear()))
        }

        @JvmStatic
        fun getEndfYear(dateTime: ZonedDateTime): ZonedDateTime {
            return getEndOfDay(dateTime.with(TemporalAdjusters.lastDayOfYear()))
        }

        @JvmStatic
        fun getBeginOfMonth(dateTime: ZonedDateTime): ZonedDateTime {
            return getBeginOfDay(dateTime.with(TemporalAdjusters.firstDayOfMonth()))
        }

        @JvmStatic
        fun getEndOfMonth(dateTime: ZonedDateTime): ZonedDateTime {
            return getEndOfDay(dateTime.with(TemporalAdjusters.lastDayOfMonth()))
        }

        @JvmStatic
        fun getBeginOfWeek(date: ZonedDateTime): ZonedDateTime {
            val field = WeekFields.of(PFDayUtils.getFirstDayOfWeek(), 1).dayOfWeek()
            return getBeginOfDay(date.with(field, 1))
        }

        @JvmStatic
        fun getEndOfWeek(dateTime: ZonedDateTime): ZonedDateTime {
            return getEndOfPreviousDay(getBeginOfWeek(dateTime.plusWeeks(1)))
        }

        @JvmStatic
        fun getBeginOfDay(dateTime: ZonedDateTime): ZonedDateTime {
            return dateTime.toLocalDate().atStartOfDay(dateTime.getZone())
        }

        @JvmStatic
        fun getEndOfDay(dateTime: ZonedDateTime): ZonedDateTime {
            return getEndOfPreviousDay(dateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1))
        }

        /**
         * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
         */
        @JvmStatic
        fun getUTCBeginOfDay(date: Date?): Date? {
            return getUTCBeginOfDay(date, ThreadLocalUserContext.getTimeZone()).utilDate
        }

        /**
         * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
         */
        @JvmStatic
        fun getUTCBeginOfDayTimestamp(date: Date?): Timestamp? {
            return getUTCBeginOfDay(date, ThreadLocalUserContext.getTimeZone()).sqlTimestamp
        }

        /**
         * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
         */
        @JvmStatic
        fun getUTCBeginOfDay(date: Date?, timeZone: TimeZone?): PFDateTime {
            val ud = from(date, false, timeZone)
            return withDate(ud!!.year, ud.month, ud.dayOfMonth, 0, 0, 0, 0, ZONE_UTC)
        }


        /**
         * Including limits.
         */
        @JvmStatic
        fun isBetween(date: PFDateTime, from: PFDateTime?, to: PFDateTime?): Boolean {
            if (from == null) {
                return if (to == null) {
                    false
                } else !date.isAfter(to)
            }
            return if (to == null) {
                !date.isBefore(from)
            } else !(date.isAfter(to) || date.isBefore(from))
        }

        /**
         * Parses the given date as UTC and converts it to the user's zoned date time.
         * @throws DateTimeParseException if the text cannot be parsed
         */
        @JvmStatic
        @JvmOverloads
        fun parseUTCDate(str: String?,
                         dateTimeFormatter: DateTimeFormatter,
                         zoneId: ZoneId = PFDateTime.getUsersZoneId(),
                         locale: Locale = PFDateTime.getUsersLocale())
                : PFDateTime? {
            if (str.isNullOrBlank())
                return null
            val local = LocalDateTime.parse(str, dateTimeFormatter) // Parses UTC as local date.
            val utcZoned = ZonedDateTime.of(local, ZONE_UTC)
            val userZoned = utcZoned.withZoneSameInstant(zoneId)
            return PFDateTime(userZoned, locale, null)
        }

        /**
         * Parses the given date as UTC and converts it to the user's zoned date time.
         * Tries the following formatters:
         * ### Supported formats
         * + number (epoch in millis or seconds, @see [numberFormat]),
         * + "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS" (UTC),
         * + "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS" (UTC),
         * + "yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'" "yyyy-MM-dd'T'HH:mm:ss.SSS.'Z'" (UTC)
         * + "yyyy-MM-dd'T'HH:mm+01:30", "yyyy-MM-dd'T'HH:mm:ss-05:30", "yyyy-MM-dd'T'HH:mm:ss.SSS+01:00" (with given offset),
         * @param str The string to parse.
         * @param zoneId The user's zone id for the returned [PFDateTime] (not used for parsing!). If not given, [PFDateTime.getUsersZoneId] is used.
         * @param locale The user's locale for the returned [PFDateTime] (not used for parsing!). If not given, [PFDateTime.getUsersLocale] is used.
         * @param numberFormat If str is a number (long/int value), the value is interpreted as epoch seconds or millis. Epoch seconds is used as default.
         * @return The parsed [PFDateTime] with the given zone id and locale, or null, if given string was null or blank.
         * @throws DateTimeException if the text cannot be parsed
         */
        @JvmStatic
        @JvmOverloads
        fun parseUTCDate(str: String?,
                         zoneId: ZoneId = PFDateTime.getUsersZoneId(),
                         locale: Locale = PFDateTime.getUsersLocale(),
                         numberFormat: PFDateTime.NumberFormat? = PFDateTime.NumberFormat.EPOCH_SECONDS)
                : PFDateTime? {
            if (str.isNullOrBlank())
                return null
            if (StringUtils.isNumeric(str)) {
                return from(str.toLong(), false, zoneId, locale, numberFormat)
            }
            var trimmedString = str.trim()
            if (trimmedString.matches("[\\d]{4}-[\\d]{2}-[\\d]{2} .*".toRegex())) {
                trimmedString = trimmedString.replaceFirst(' ', 'T')
            }
            val utcZoned = if (trimmedString.contains('Z') || trimmedString.matches(".*[+-][\\d]{2}:[\\d]{2}".toRegex())) {
                // yyyy-MM-dd'T'HH:mm:ss.SSS+01:00'
                val dateTime = ZonedDateTime.parse(trimmedString, DateTimeFormatter.ISO_DATE_TIME) // Parses UTC as local date.
                dateTime.withZoneSameInstant(ZONE_UTC)
            } else {
                // yyyy-MM-dd'T'HH:mm:ss.SSS'
                val localTime = LocalDateTime.parse(trimmedString, DateTimeFormatter.ISO_LOCAL_DATE_TIME) // Parses UTC as local date.
                ZonedDateTime.of(localTime, ZONE_UTC)
            }
            val userZoned = utcZoned.withZoneSameInstant(zoneId)
            return PFDateTime(userZoned, locale, null)
        }

        @JvmStatic
        fun formatUTCDate(date: Date): String {
            return if (date is java.sql.Date) {
                PFDateTime.isoDateFormatter.format(date.toLocalDate())
            } else {
                val ldt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                PFDateTime.isoDateTimeFormatterMilli.format(ldt)
            }
        }

        /**
         * Creates DateTimeFormatter with user's date format and user's locale.
         */
        @JvmStatic
        fun ensureUsersDateTimeFormat(dateFormatType: DateFormatType): DateTimeFormatter {
            return DateTimeFormatter.ofPattern(DateFormats.getFormatString(dateFormatType), ThreadLocalUserContext.getLocale())
        }

        /**
         * Substract 1 millisecond to get the end of last day.
         */
        private fun getEndOfPreviousDay(beginOfDay: ZonedDateTime): ZonedDateTime {
            return beginOfDay.minusNanos(1)
        }
    }
}
