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

package org.projectforge.framework.time

import org.apache.commons.lang3.StringUtils
import org.projectforge.common.DateFormatType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import java.sql.Timestamp
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.*

object PFDateTimeUtils {

    @JvmField
    val ZONE_UTC = ZoneId.of("UTC")

    @JvmField
    val ZONE_EUROPE_BERLIN = ZoneId.of("Europe/Berlin")

    @JvmField
    val TIMEZONE_UTC = TimeZone.getTimeZone("UTC")

    @JvmField
    val TIMEZONE_EUROPE_BERLIN = TimeZone.getTimeZone("Europe/Berlin")

    @JvmField
    val MILLIS_PER_DAY = 1000 * 60 * 60 * 24

    // some older database entries have this format: 2016-12-13 11:22:00:919.
    private val specialDateStringEndingRegex = """.*:\d{3}$""".toRegex()

    private val isoDateWithTimestampRegex = "[\\d]{4}-[\\d]{2}-[\\d]{2} .*".toRegex()

    // yyyy-MM-dd'T'HH:mm:ss.SSS+01:00'
    private val withTimeZoneregex = ".*[+-][\\d]{2}:[\\d]{2}".toRegex()

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
        return getUTCBeginOfDay(date, ThreadLocalUserContext.timeZone).utilDate
    }

    /**
     * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
     */
    @JvmStatic
    fun getUTCBeginOfDayTimestamp(date: Date?): Timestamp? {
        return getUTCBeginOfDay(date, ThreadLocalUserContext.timeZone).sqlTimestamp
    }

    /**
     * Converts a given date (in user's timeZone) to midnight of UTC timeZone.
     */
    @JvmStatic
    fun getUTCBeginOfDay(date: Date?, timeZone: TimeZone?): PFDateTime {
        val ud = PFDateTime.fromOrNow(date, timeZone)
        return withDate(ud.year, ud.month, ud.dayOfMonth, 0, 0, 0, 0, ZONE_UTC)
    }


    /**
     * Including limits.
     */
    @JvmStatic
    fun <T : IPFDate<T>> isBetween(date: T, from: T?, to: T?): Boolean {
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
     * Calls [parse] first.
     * @param str The string to parse.
     * @param parseWithZoneId The zone id to used while parsing, if now time zone offset isn't part of [str]. Default is UTC.
     * @param zoneId The user's zone id for the created [PFDateTime] (not used for parsing!). If not given, [PFDateTime.getUsersZoneId] is used.
     * @param locale The user's locale for the created [PFDateTime] (not used for parsing!). If not given, [PFDateTime.getUsersLocale] is used.
     * @param numberFormat If str is a number (long/int value), the value is interpreted as epoch seconds or millis. Epoch seconds is used as default.
     * @return The parsed [PFDateTime] with the given zone id and locale, or null, if given string was null or blank.
     * @throws DateTimeException if the text cannot be parsed
     * @see parse
     */
    @JvmStatic
    @JvmOverloads
    fun parseAndCreateDateTime(
        str: String?,
        parseWithZoneId: ZoneId? = null,
        zoneId: ZoneId = PFDateTime.getUsersZoneId(),
        locale: Locale = PFDateTime.getUsersLocale(),
        numberFormat: PFDateTime.NumberFormat? = PFDateTime.NumberFormat.EPOCH_SECONDS
    )
            : PFDateTime? {
        val utcDateTime = parse(str, parseWithZoneId, numberFormat, locale) ?: return null
        return utcDateTime.withZoneSameInstant(zoneId)
    }

    /**
     * Parses the given date and returns the LocalDateTime in UTC.
     * Tries the following formatters:
     * ### Supported formats
     * + number (epoch in millis or seconds, @see [numberFormat]) (number is always treated as UTC),
     * + "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS" (UTC),
     * + "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS" (UTC),
     * + "yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'" "yyyy-MM-dd'T'HH:mm:ss.SSS.'Z'" (UTC)
     * + "yyyy-MM-dd'T'HH:mm+01:30", "yyyy-MM-dd'T'HH:mm:ss-05:30", "yyyy-MM-dd'T'HH:mm:ss.SSS+01:00" (with given offset),
     * @param str The date string to parse. If no time zone or offset is given in the dat string, UTC is assumed.
     *
     * ### Examples
     * + "2020-01-25 18:00:00+01:00" (UTC+1) -> "2020-01-25 19:00:00" (UTC)
     * + "2020-01-25 18:00:00" (UTC) -> "2020-01-25 18:00:00" (UTC)
     *
     * @param defaultZoneId If the string is given without time zone offset, this zoneId is used. If not given UTC is used.
     * @param numberFormat If str is a number (long/int value), the value is interpreted as epoch seconds or millis. Epoch seconds is used as default.
     * @return The parsed [PFDateTime] representation of the given string with given [defaultZoneId] or specified time zone in date string or null, if given string was null or blank.
     * @throws DateTimeException if the text cannot be parsed
     */
    @JvmOverloads
    fun parse(
        str: String?,
        defaultZoneId: ZoneId? = null,
        numberFormat: PFDateTime.NumberFormat? = PFDateTime.NumberFormat.EPOCH_SECONDS,
        locale: Locale = PFDateTime.getUsersLocale()
    )
            : PFDateTime? {
        if (str.isNullOrBlank())
            return null
        if (StringUtils.isNumeric(str)) {
            val instant = if (numberFormat == PFDateTime.NumberFormat.EPOCH_SECONDS) {
                Instant.ofEpochSecond(str.toLong(), 0)
            } else {
                Instant.ofEpochMilli(str.toLong())
            }
            val dateTime = PFDateTime(ZonedDateTime.ofInstant(instant, ZONE_UTC), locale, null)
            return if (defaultZoneId != null) {
                dateTime.withZoneSameInstant(defaultZoneId)
            } else {
                dateTime
            }
        }
        var trimmedString = str.trim()
        if (trimmedString.startsWith('"') && trimmedString.endsWith('"')) {
            // String is quoted, remove the quotes first:
            trimmedString = trimmedString.substring(1..trimmedString.length - 2)
        }
        if (trimmedString.matches(specialDateStringEndingRegex)) { // special case in history entries of database.
            // Simply remove milliseconds for this special case:
            trimmedString = trimmedString.substring(0 until trimmedString.length - 4)
        }
        // [\d]{4}-[\d]{2}-[\d]{2} .*
        if (trimmedString.matches(isoDateWithTimestampRegex)) {
            trimmedString = trimmedString.replaceFirst(' ', 'T')
        }
        if (trimmedString.contains('Z') || trimmedString.matches(withTimeZoneregex)) {
            // yyyy-MM-dd'T'HH:mm:ss.SSS+01:00'
            val dateTime = ZonedDateTime.parse(trimmedString, DateTimeFormatter.ISO_DATE_TIME)
            return PFDateTime(dateTime, locale, null)
        }
        // yyyy-MM-dd'T'HH:mm:ss.SSS'
        val formatter = if (defaultZoneId != null) {
            DateTimeFormatter.ISO_DATE_TIME.withZone(defaultZoneId)
        } else {
            DateTimeFormatter.ISO_DATE_TIME.withZone(ZONE_UTC)
        }
        try {
            val dateTime = ZonedDateTime.parse(trimmedString, formatter) // Parses with given time zone.
            return PFDateTime(dateTime, locale = locale, precision = null)
        } catch (ex: DateTimeParseException) {
            // Can't parse it. Giving up.
        }
        return null
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
        return DateTimeFormatter.ofPattern(DateFormats.getFormatString(dateFormatType), ThreadLocalUserContext.locale)
    }

    /**
     * Substract 1 millisecond to get the end of last day.
     */
    private fun getEndOfPreviousDay(beginOfDay: ZonedDateTime): ZonedDateTime {
        return beginOfDay.minusNanos(1)
    }
}
