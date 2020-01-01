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

import java.time.DayOfWeek
import java.time.Month
import java.time.temporal.ChronoUnit
import java.util.*

class PFDateCompabilityUtils {
    companion object {
        @JvmStatic
        fun convertToLocalDate(dateMidnight: org.joda.time.DateMidnight?): java.time.LocalDate? {
            if (dateMidnight == null)
                return null
            return java.time.LocalDate.of(dateMidnight.year, dateMidnight.monthOfYear, dateMidnight.dayOfMonth)
        }

        /**
         * dayNumber 1 - Sunday, 2 - Monday, ..., 7 - Saturday
         */
        @JvmStatic
        fun getCompabilityDayOfWeek(dayNumber: Int?): DayOfWeek? {
            return when (dayNumber) {
                1 -> DayOfWeek.SUNDAY
                2 -> DayOfWeek.MONDAY
                3 -> DayOfWeek.TUESDAY
                4 -> DayOfWeek.WEDNESDAY
                5 -> DayOfWeek.THURSDAY
                6 -> DayOfWeek.FRIDAY
                7 -> DayOfWeek.SATURDAY
                else -> null
            }
        }

        /**
         * @return 1 - Sunday, 2 - Monday, ..., 7 - Saturday
         */
        @JvmStatic
        fun getCompabilityDayOfWeekValue(dayOfWeek: DayOfWeek?): Int? {
            return when (dayOfWeek) {
                DayOfWeek.SUNDAY -> 1
                DayOfWeek.MONDAY -> 2
                DayOfWeek.TUESDAY -> 3
                DayOfWeek.WEDNESDAY -> 4
                DayOfWeek.THURSDAY -> 5
                DayOfWeek.FRIDAY -> 6
                DayOfWeek.SATURDAY -> 7
                else -> null
            }
        }

        /**
         * monthNumber 0-based: 0 - January, ..., 11 - December
         */
        @JvmStatic
        fun getCompabilityMonth(monthNumber: Int?): Month? {
            return if (monthNumber != null) {
                Month.of(monthNumber + 1)
            } else {
                null
            }
        }

        /**
         * monthNumber 0-based: 0 - January, ..., 11 - December
         */
        @JvmStatic
        fun getCompabilityMonthValue(month: Month?): Int? {
            return if (month != null) {
                month.ordinal
            } else {
                null
            }
        }

        /**
         */
        @JvmStatic
        fun getCompabilityFields(calendarField: Int): ChronoUnit {
            return when (calendarField) {
                Calendar.DAY_OF_YEAR -> ChronoUnit.DAYS
                Calendar.DAY_OF_MONTH -> ChronoUnit.DAYS
                Calendar.DAY_OF_WEEK -> ChronoUnit.DAYS
                Calendar.MONTH -> ChronoUnit.MONTHS
                Calendar.WEEK_OF_YEAR -> ChronoUnit.WEEKS
                Calendar.HOUR -> ChronoUnit.HOURS
                Calendar.MINUTE -> ChronoUnit.MINUTES
                else -> throw IllegalArgumentException("Unsupported calendar field with number " + calendarField)
            }
        }
    }
}
