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
    var fromDay: LocalDate?
        get() = PFDay.from(fromDate)?.localDate
        set(value) {
            if (value == null) {
                fromDate = null
            } else {
                fromDate = PFDateTime.from(value)!!.beginOfDay.utilDate
            }
        }

    var toDay: LocalDate?
        get() = PFDay.from(toDate)?.localDate
        set(value) {
            if (value == null) {
                toDate = null
            } else {
                toDate = PFDateTime.from(value)!!.endOfDay.utilDate
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
        fun getDuration(fromDate: Date?, toDate: Date?): Long {
            return if (fromDate == null || toDate == null || toDate.before(fromDate)) {
                0
            } else toDate.time - fromDate.time
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
    }

}
