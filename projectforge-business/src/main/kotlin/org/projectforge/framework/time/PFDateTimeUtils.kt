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
        fun parseUTCDate(str: String?, dateTimeFormatter: DateTimeFormatter, zoneId: ZoneId = PFDateTime.getUsersZoneId(), locale: Locale = PFDateTime.getUsersLocale()): PFDateTime? {
            if (str.isNullOrBlank())
                return null
            val local = LocalDateTime.parse(str, dateTimeFormatter) // Parses UTC as local date.
            val utcZoned = ZonedDateTime.of(local, ZoneId.of("UTC"))
            val userZoned = utcZoned.withZoneSameInstant(zoneId)
            return PFDateTime(userZoned, locale, null)
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
        fun parseUTCDate(str: String?, zoneId: ZoneId = PFDateTime.getUsersZoneId(), locale: Locale = PFDateTime.getUsersLocale()): PFDateTime? {
            if (str.isNullOrBlank())
                return null
            if (StringUtils.isNumeric(str)) {
                return PFDateTime.from(str.toLong())
            }
            if (str.contains("T")) { // yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
                return parseUTCDate(str, PFDateTime.jsDateTimeFormatter)
            }
            val colonPos = str.indexOf(':')
            return when {
                colonPos < 0 -> {
                    throw DateTimeException("Can't parse date string '$str'. Supported formats are 'yyyy-MM-dd HH:mm', 'yyyy-MM-dd HH:mm:ss', 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'' and numbers as epoch seconds.")
                }
                str.indexOf(':', colonPos + 1) < 0 -> { // yyyy-MM-dd HH:mm
                    parseUTCDate(str, PFDateTime.isoDateTimeFormatterMinutes, zoneId, locale)
                }
                else -> { // yyyy-MM-dd HH:mm:ss
                    parseUTCDate(str, PFDateTime.isoDateTimeFormatterSeconds, zoneId, locale)
                }
            }
        }

        /**
         * Substract 1 millisecond to get the end of last day.
         */
        private fun getEndOfPreviousDay(beginOfDay: ZonedDateTime): ZonedDateTime {
            return beginOfDay.minusNanos(1)
        }
    }
}
