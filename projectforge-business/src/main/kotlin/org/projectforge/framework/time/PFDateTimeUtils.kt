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
import org.apache.commons.lang3.Validate
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.i18n.UserException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.math.BigDecimal
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.*

class PFDateTimeUtils {
    companion object {
        @JvmStatic
        fun getBeginOfYear(dateTime: ZonedDateTime): ZonedDateTime {
            return getBeginOfDay(dateTime.withDayOfYear(1))
        }

        @JvmStatic
        fun getBeginOfWeek(date: ZonedDateTime): ZonedDateTime {
            val field = WeekFields.of(getFirstDayOfWeek(), 1).dayOfWeek()
            return getBeginOfDay(date.with(field, 1))
        }

        @JvmStatic
        fun getBeginOfWeek(date: LocalDate): LocalDate {
            val field = WeekFields.of(getFirstDayOfWeek(), 1).dayOfWeek()
            return return date.with(field, 1)
        }

        @JvmStatic
        fun getFirstDayOfWeek(): DayOfWeek {
            val firstDayOfWeek = ThreadLocalUserContext.getJodaFirstDayOfWeek()
            return getDayOfWeek(firstDayOfWeek)!!
        }

        @JvmStatic
        fun getBeginOfDay(dateTime: ZonedDateTime): ZonedDateTime {
            return dateTime.toLocalDate().atStartOfDay(dateTime.getZone())
        }

        @JvmStatic
        fun getEndOfDay(dateTime: ZonedDateTime): ZonedDateTime {
            return dateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1)
        }

        @JvmStatic
        fun convertToLocalDate(dateMidnight: org.joda.time.DateMidnight?): java.time.LocalDate? {
            if (dateMidnight == null)
                return null
            return java.time.LocalDate.of(dateMidnight.year, dateMidnight.monthOfYear, dateMidnight.dayOfMonth)
        }

        /**
         * dayNumber 1 - Monday, 2 - Tuesday, ..., 7 - Sunday
         */
        @JvmStatic
        fun getDayOfWeek(dayNumber: Int): DayOfWeek? {
            return when (dayNumber) {
                1 -> DayOfWeek.MONDAY
                2 -> DayOfWeek.TUESDAY
                3 -> DayOfWeek.WEDNESDAY
                4 -> DayOfWeek.THURSDAY
                5 -> DayOfWeek.FRIDAY
                6 -> DayOfWeek.SATURDAY
                7 -> DayOfWeek.SUNDAY
                else -> null
            }
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

        @JvmStatic
        fun getNumberOfWorkingDays(from: PFDateTime, to: PFDateTime): BigDecimal {
            Validate.notNull(from)
            Validate.notNull(to)
            return getNumberOfWorkingDays(PFDate.from(from)!!, PFDate.from(to)!!)
        }

        @JvmStatic
        fun getNumberOfWorkingDays(from: PFDate, to: PFDate): BigDecimal {
            Validate.notNull(from)
            Validate.notNull(to)
            val holidays = Holidays.getInstance()
            if (to.isBefore(from)) {
                return BigDecimal.ZERO
            }
            if (from.isSameDay(to)) {
                if (holidays.isWorkingDay(from)) {
                    val workFraction = holidays.getWorkFraction(from)
                    return workFraction ?: BigDecimal.ONE
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
                if (holidays.isWorkingDay(from)) {
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

        @JvmStatic
        fun addWorkingDays(date: PFDateTime, days: Int): PFDateTime {
            Validate.isTrue(days <= 10000)
            var currentDate = date
            val plus = days > 0
            for (counter in 0..9999) {
                if (counter == days) {
                    break
                }
                for (paranoia in 0..100) {
                    currentDate = if (plus) currentDate.plusDays(1) else currentDate.minusDays(1)
                    if (isWorkingDay(currentDate)) {
                        break
                    }
                }
            }
            return date
        }

        fun isWorkingDay(date: PFDateTime): Boolean {
            return Holidays.getInstance().isWorkingDay(date)
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
    }
}
