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

import org.apache.commons.lang3.Validate
import org.projectforge.common.DateFormatType
import org.projectforge.framework.calendar.Holidays
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.*
import kotlin.math.absoluteValue

class PFDayUtils {
    companion object {
        @JvmStatic
        fun getBeginOfYear(date: LocalDate): LocalDate {
            return date.with(TemporalAdjusters.firstDayOfYear())
        }

        @JvmStatic
        fun getEndOfYear(date: LocalDate): LocalDate {
            return date.with(TemporalAdjusters.lastDayOfYear())
        }

        @JvmStatic
        fun getBeginOfMonth(date: LocalDate): LocalDate {
            return date.with(TemporalAdjusters.firstDayOfMonth())
        }

        @JvmStatic
        fun getEndOfMonth(date: LocalDate): LocalDate {
            return date.with(TemporalAdjusters.lastDayOfMonth())
        }

        @JvmStatic
        fun getBeginOfWeek(date: LocalDate): LocalDate {
            val field = WeekFields.of(getFirstDayOfWeek(), 1).dayOfWeek()
            return date.with(field, 1)
        }

        @JvmStatic
        fun getEndOfWeek(date: LocalDate): LocalDate {
            return getBeginOfWeek(date).plusDays(6)
        }

        @JvmStatic
        fun getFirstDayOfWeek(): DayOfWeek {
            return ThreadLocalUserContext.firstDayOfWeek!!
        }

        /**
         * 1 - first day of week (locale dependent, e. g. Monday or Sunday).
         * 7 - last day of week.
         */
        @JvmStatic
        fun <T : IPFDate<T>> withDayOfWeek(date: T, dayOfWeek: Int): T {
            if (dayOfWeek in 1..7) {
                return if (dayOfWeek == 1) date.beginOfWeek else date.beginOfWeek.plusDays((dayOfWeek - 1).toLong())
            } else {
                throw IllegalArgumentException("withDayOfWeek accepts only day of weeks from 1 (first day of week) to 7 (last day of week), but $dayOfWeek was given.")
            }
        }

        /**
         * dayNumber 0 - Sunday,  1 - Monday, 2 - Tuesday, ..., 7 - Sunday
         */
        @JvmStatic
        fun getISODayOfWeek(dayNumber: Int?): DayOfWeek? {
            return when (dayNumber) {
                null -> {
                    null
                }
                in 1..7 -> {
                    DayOfWeek.of(dayNumber)
                }
                0 -> {
                    DayOfWeek.SUNDAY
                }
                else -> {
                    throw IllegalArgumentException("getDayOfWeek accepts only day of weeks from 1 (first day of week) to 7 (last day of week), but $dayNumber was given.")
                }
            }
        }

        /**
         * ISO-8601 standard
         * @return 1 - Monday, 2 - Tuesday, ..., 7 - Sunday
         */
        @JvmStatic
        fun getISODayOfWeekValue(dayOfWeek: DayOfWeek?): Int? {
            return dayOfWeek?.value
        }

        /**
         * monthNumber 1-based: 1 - January, ..., 12 - December
         */
        @JvmStatic
        fun getMonth(monthNumber: Int?): Month? {
            return if (monthNumber != null) {
                Month.of(monthNumber)
            } else {
                null
            }
        }

        /**
         * Convenient function for Java (is equivalent to Kotlin month?.value).
         * @return monthNumber 1-based: 1 - January, ..., 12 - December or null if given month is null.
         */
        @JvmStatic
        fun getMonthValue(month: Month?): Int? {
            return month?.value
        }

        /**
         * Validates if the given param is null or in 1..12. Otherwise an IllegalArgumentException will be thrown.
         */
        @Throws(IllegalArgumentException::class)
        @JvmStatic
        @JvmOverloads
        fun validateMonthValue(month: Int?, autoFix: Boolean = true): Int? {
            if (month != null && month !in 1..12) {
                if (autoFix) {
                    if (month < 1)
                        return 1
                    else
                        return 12
                }
                throw IllegalArgumentException("Month value out of range 1..12: $month")
            }
            return month
        }

        /**
         * Including limits.
         */
        @JvmStatic
        fun <T : IPFDate<T>> isBetween(date: T, from: T?, to: T?): Boolean {
            return PFDateTimeUtils.isBetween(date, from, to)
        }


        /**
         * Determines the number of working days in the given period. Please note: there might be also half working days
         * (e. g. on Xmas or New Years Eve), so a BigDecimal is returned.
         */
        @JvmStatic
        fun getNumberOfWorkingDays(from: LocalDate, to: LocalDate): BigDecimal {
            return getNumberOfWorkingDays(PFDay.from(from), PFDay.from(to))
        }

        /**
         * Determines the number of working days in the given period. Please note: there might be also half working days
         * (e. g. on Xmas or New Years Eve), so a BigDecimal is returned.
         */
        @JvmStatic
        fun <T : IPFDate<T>> getNumberOfWorkingDays(from: T, to: T): BigDecimal {
            Validate.notNull(from)
            Validate.notNull(to)
            val holidays = Holidays.instance
            if (to.isBefore(from)) {
                return BigDecimal.ZERO
            }
            var numberOfWorkingDays = BigDecimal.ZERO
            var numberOfFullWorkingDays = 0
            var dayCounter = 1
            var day = from
            do {
                if (dayCounter++ > 740) { // Endless loop protection, time period greater 2 years.
                    throw UserException(
                            "getNumberOfWorkingDays does not support calculation of working days for a time period greater than two years!")
                }
                if (holidays.isWorkingDay(day)) {
                    val workFraction = holidays.getWorkFraction(day)
                    if (workFraction != null) {
                        numberOfWorkingDays = numberOfWorkingDays.add(workFraction)
                    } else {
                        numberOfFullWorkingDays++
                    }
                }
                day = day.plusDays(1)
            } while (!day.isAfter(to))
            numberOfWorkingDays = numberOfWorkingDays.add(BigDecimal(numberOfFullWorkingDays))
            return numberOfWorkingDays
        }

        @JvmStatic
        fun <T : IPFDate<T>> addWorkingDays(date: T, days: Int): T {
            Validate.isTrue(days <= 10000)
            var currentDate = date
            val plus = days > 0
            val absDays = days.absoluteValue
            for (counter in 0..9999) {
                if (counter == absDays) {
                    break
                }
                for (paranoia in 0..100) {
                    currentDate = if (plus) currentDate.plusDays(1) else currentDate.minusDays(1)
                    if (isWorkingDay(currentDate)) {
                        break
                    }
                }
            }
            return currentDate
        }

        fun <T : IPFDate<T>> isWorkingDay(date: T): Boolean {
            return Holidays.instance.isWorkingDay(date)
        }

        fun isWorkingDay(date: LocalDate): Boolean {
            return Holidays.instance.isWorkingDay(date)
        }

        /**
         * @return The given date, if already a working day, otherwise the first working day after given date.
         */
        fun <T : IPFDate<T>> getNextWorkingDay(date: T): T {
            var nextWorkingDay = date
            while (!isWorkingDay(nextWorkingDay)) {
                nextWorkingDay = nextWorkingDay.plusDays(1)
            }
            return nextWorkingDay
        }

        /**
         * @return The given date, if already a working day, otherwise the first working day after given date.
         */
        fun getNextWorkingDay(date: LocalDate): LocalDate {
            return getNextWorkingDay(PFDay.from(date)).localDate
        }

        /**
         * Parses the given date (of iso type yyyy-MM-dd or user's date format [DateFormatType.DATE] or [DateFormatType.DATE_SHORT].
         * @throws DateTimeParseException if the text cannot be parsed
         */
        @JvmStatic
        fun parseDate(str: String?): LocalDate? {
            if (str.isNullOrBlank())
                return null
            val dateString = str.trim()
            var date = parseDate(dateString, PFDay.isoDateFormatter) // Parses iso date.
            if (date != null) {
                return date
            }
            date = parseDate(dateString, PFDateTimeUtils.ensureUsersDateTimeFormat(DateFormatType.DATE))
            if (date != null) {
                return date
            }
            date = parseDate(dateString, PFDateTimeUtils.ensureUsersDateTimeFormat(DateFormatType.DATE_SHORT))
            if (date != null) {
                return date
            }
            // Try to parse with time of day, but use local date in user's time zone independant of given time zone.
            // e. g. Fronend sends 2019-10-04T22:00:00.000Z in user's time zone Europe/Berlin. This should result in 1999-10-05.
            date = PFDateTimeUtils.parse(dateString)?.withZoneSameInstant(ThreadLocalUserContext.zoneId)?.localDate
            return date
        }

        fun parseDate(str: String?, dateTimeFormatter: DateTimeFormatter): LocalDate? {
            str ?: return null
            try {
                return LocalDate.parse(str, dateTimeFormatter)
            } catch (ex: DateTimeParseException) {
                // OK
                return null
            }
        }

        fun format(date: LocalDate?, dateTimeFormat: DateFormatType): String? {
            return PFDateTimeUtils.ensureUsersDateTimeFormat(dateTimeFormat).format(date)
        }

        /**
         * return year of given LocalDate or -1 if LocalDate is null.
         */
        @JvmStatic
        fun getYear(date: LocalDate?): Int {
            return date?.year ?: -1
        }

        @JvmStatic
        fun convertToUtilDate(date: LocalDate): Date {
            return PFDateTime.from(date).utilDate
        }

        /**
         * Substract 1 millisecond to get the end of last day.
         */
        private fun getEndOfPreviousDay(beginOfDay: ZonedDateTime): ZonedDateTime {
            return beginOfDay.minusNanos(1)
        }
    }
}
