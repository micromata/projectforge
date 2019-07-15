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

import java.time.*
import java.time.temporal.ChronoUnit

/**
 * Immutable holder of [LocalDate] for transforming to [java.sql.Date] (once) if used several times.
 * If you don't need to use [java.sql.Date] you may use [LocalDate] directly.
 */
class PFDate(val date: LocalDate) {

    constructor(instant: Instant) : this(LocalDate.from(instant))

    private var _sqlDate: java.sql.Date? = null
    /**
     * @return The date as java.sql.Date. java.sql.Date is only calculated, if this getter is called and it
     * will be calculated only once, so multiple calls of getter will not result in multiple calculations.
     */
    val sqlDate: java.sql.Date
        get() {
            if (_sqlDate == null) {
                _sqlDate = java.sql.Date.valueOf(date)
            }
            return _sqlDate!!
        }

    val month: Month
        get() = date.month

    fun isBefore(other: PFDate): Boolean {
        return date.isBefore(other.date)
    }

    fun isAfter(other: PFDate): Boolean {
        return date.isAfter(other.date)
    }

    fun daysBetween(other: PFDate): Long {
        return ChronoUnit.DAYS.between(date, other.date)
    }

    fun plusDays(days: Long): PFDate {
        return PFDate(date.plusDays(days))
    }

    companion object {
        /**
         * Creates mindnight [ZonedDateTime] from given [LocalDate].
         */
        @JvmStatic
        fun from(localDate: LocalDate?, nowIfNull: Boolean = false): PFDate? {
            if (localDate == null)
                return if (nowIfNull) now() else null
            return PFDate(localDate)
        }

        /**
         * @param date Date of type java.util.Date or java.sql.Date.
         * Creates mindnight [ZonedDateTime] from given [date].
         */
        @JvmStatic
        fun from(date: java.util.Date?, nowIfNull: Boolean = false): PFDate? {
            if (date == null)
                return if (nowIfNull) now() else null
            if (date is java.sql.Date) {
                return PFDate(date.toLocalDate())
            }
            return PFDate(date.toInstant()
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate())
        }


        @JvmStatic
        fun now(): PFDate {
            return PFDate(LocalDate.now())
        }
    }
}
