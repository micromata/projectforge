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

import org.projectforge.framework.ToStringUtil.Companion.toJsonString
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class TimePeriod @JvmOverloads constructor(var fromDate: Date? = null, var toDate: Date? = null,
                                           /**
                                            * For storing time period collisions of time sheets.
                                            *
                                            * @return
                                            */
                                           var marker: Boolean = false) : Serializable {
    constructor( fromDay: LocalDate? = null,
                 toDate: LocalDate? = null,
                 marker: Boolean = false)
            : this(PFDateTime.fromOrNull(fromDay)?.beginOfDay?.utilDate, PFDateTime.fromOrNull(toDate)?.endOfDay?.utilDate, marker)

    var fromDay: LocalDate?
        get() = PFDay.fromOrNull(fromDate)?.localDate
        set(value) {
            if (value == null) {
                fromDate = null
            } else {
                fromDate = PFDateTime.from(value).beginOfDay.utilDate
            }
        }

    var toDay: LocalDate?
        get() = PFDay.fromOrNull(toDate)?.localDate
        set(value) {
            if (value == null) {
                toDate = null
            } else {
                toDate = PFDateTime.from(value).endOfDay.utilDate
            }
        }

    /**
     * hoursOfDay = 24; minHoursOfDaySeparation = 0;
     *
     * @see .getDurationFields
     */
    val durationFields: IntArray
        get() = getDurationFields(24)

    /**
     * minHoursOfDaySeparation = 0;
     *
     * @see .getDurationFields
     */
    fun getDurationFields(hoursOfDay: Int): IntArray {
        return getDurationFields(hoursOfDay, 0)
    }

    /**
     * @see .getDurationFields
     */
    fun getDurationFields(hoursOfDay: Int, minHours4DaySeparation: Int): IntArray {
        return getDurationFields(duration, hoursOfDay, minHours4DaySeparation)
    }

    /**
     * Duration in millis.
     *
     * @return
     */
    val duration: Long
        get() = getDuration(fromDate, toDate)

    override fun toString(): String {
        return toJsonString(this)
    }

    companion object {
        private const val serialVersionUID = -4928251035721502776L

        /**
         * @return duration in millis.
         */
        fun getDuration(fromDate: Date?, toDate: Date?): Long {
            return if (fromDate == null || toDate == null || toDate.before(fromDate)) {
                0
            } else toDate.time - fromDate.time
        }

        /**
         * @return duration in rounded hours.
         */
        @JvmStatic
        @JvmOverloads
        fun getDurationHours(fromDate: Date?, toDate: Date?, roundUnit: ROUND_UNIT = ROUND_UNIT.INT, roundingMode: RoundingMode = RoundingMode.HALF_UP ): BigDecimal {
            if (fromDate == null || toDate == null || toDate.before(fromDate)) {
                return BigDecimal.ZERO
            }
            val millis = toDate.time - fromDate.time
            return when (roundUnit) {
                ROUND_UNIT.INT -> BigDecimal(millis).divide(MILLIS_PER_HOUR, 0, roundingMode)
                ROUND_UNIT.HALF -> BigDecimal(millis).multiply(BD_2).divide(MILLIS_PER_HOUR, 0, roundingMode).divide(BD_2, 1, roundingMode)
                ROUND_UNIT.QUARTER -> BigDecimal(millis).multiply(BD_4).divide(MILLIS_PER_HOUR, 0, roundingMode).divide(BD_4, 2, roundingMode)
                ROUND_UNIT.FIFTH -> BigDecimal(millis).multiply(BD_5).divide(MILLIS_PER_HOUR, 0, roundingMode).divide(BD_5, 1, roundingMode)
                ROUND_UNIT.TENTH -> BigDecimal(millis).multiply(BigDecimal.TEN).divide(MILLIS_PER_HOUR, 0, roundingMode).divide(BigDecimal.TEN, 1, roundingMode)
            }
        }

        /**
         * hoursOfDay = 24; minHoursOfDaySeparation = 0;
         *
         * @see .getDurationFields
         */
        @JvmStatic
        fun getDurationFields(millis: Long): IntArray {
            return getDurationFields(millis, 24)
        }

        /**
         * minHoursOfDaySeparation = 0;
         *
         * @see .getDurationFields
         */
        @JvmStatic
        fun getDurationFields(millis: Long, hoursOfDay: Int): IntArray {
            return getDurationFields(millis, hoursOfDay, 0)
        }

        /**
         * Gets the duration of this time period.
         *
         * @param hoursOfDay Hours of day is for example 8 for a working day.
         * @param minHours4DaySeparation If minHours is e. g. 48 then 48 hours will result in 0 days and 48 hours independent
         * of the hoursOfDay. (Depending on the scope minHoursOfDay is more convenient to read.). If minHours is than
         * zero, no seperation will be done.
         * @param duration in millis.
         * @return int array { days, hours, minutes};
         */
        @JvmStatic
        fun getDurationFields(millis: Long, hoursOfDay: Int, minHours4DaySeparation: Int): IntArray {
            val duration = millis / 60000
            var hours = duration.toInt() / 60
            val minutes = duration.toInt() % 60
            var days = 0
            if (minHours4DaySeparation >= 0 && hours >= minHours4DaySeparation) { // Separate the days for more than 24 hours (=3 days):
                days = hours / hoursOfDay
                hours = hours % hoursOfDay
            }
            return intArrayOf(days, hours, minutes)
        }

        private val MILLIS_PER_HOUR = BigDecimal(1000 * 60 * 60)
        private val BD_2 = BigDecimal(2)
        private val BD_4 = BigDecimal(4)
        private val BD_5 = BigDecimal(5)
    }

    enum class ROUND_UNIT { INT, HALF, QUARTER, FIFTH, TENTH }
}
