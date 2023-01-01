/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.timesheet

import org.projectforge.framework.time.TimePeriod
import org.projectforge.framework.utils.RoundUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Stores some statistics of time sheets.
 * Doesn't support multiple users.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class TimesheetStats(fromDate: Date?, toDate: Date?) {
    /**
     * @return the time period of this stats.
     */
    val period: TimePeriod = TimePeriod(fromDate, toDate)

    /**
     * @return the earliestStartDate.
     */
    var earliestStartDateIgnoringPeriod: Date? = null
        private set

    /**
     * @return the latestStopDate
     */
    var latestStopDateIgnoringPeriod: Date? = null
        private set

    var timesheets = TreeSet<TimesheetDO>()
        private set

    /**
     * @return the earliestStartDate if not before fromDate, otherwise fromDate itself is returned. If no matching time sheet found, null is
     * returned.
     */
    fun getEarliestStartDate(): Date? {
        if (earliestStartDateIgnoringPeriod == null) {
            return null
        }
        return if (earliestStartDateIgnoringPeriod!!.before(period.fromDate)) {
            period.fromDate
        } else earliestStartDateIgnoringPeriod
    }

    /**
     * @param earliestStartDate the earliestStartDate to set
     * @return this for chaining.
     */
    fun setEarliestStartDate(earliestStartDate: Date?): TimesheetStats {
        earliestStartDateIgnoringPeriod = earliestStartDate
        return this
    }

    /**
     * @return the latestStopDate
     */
    fun getLatestStopDate(): Date? {
        if (latestStopDateIgnoringPeriod == null) {
            return null
        }
        return if (latestStopDateIgnoringPeriod!!.after(period.toDate)) {
            period.toDate
        } else latestStopDateIgnoringPeriod
    }

    /**
     * @param latestStopDate the latestStopDate to set
     * @return this for chaining.
     */
    fun setLatestStopDate(latestStopDate: Date?): TimesheetStats {
        latestStopDateIgnoringPeriod = latestStopDate
        return this
    }

    /**
     * @return the totalBreakHours
     */
    val totalBreakMillis: Long
        get() {
            if (timesheets.isEmpty()) {
                return 0L
            }
            var totalBreakMillis = 0L
            var lastStopTime: Date? = null
            timesheets.forEach { timesheet ->
                lastStopTime?.let { stopTime ->
                    if (stopTime.before(timesheet.startTime)) {
                        totalBreakMillis += timesheet.startTime!!.time - stopTime.time
                    }
                }
                lastStopTime = timesheet.stopTime
            }
            return totalBreakMillis
        }

    /**
     * @return the total millis of time sheets inside given period.
     */
    val totalMillis: Long
        get() {
            if (timesheets.isEmpty()) {
                return 0L
            }
            var total = 0L
            timesheets.forEach { timesheet ->
                var startTime = timesheet.startTime!!
                if (startTime.before(period.fromDate)) {
                    startTime = period.fromDate!!
                }
                var stopTime = timesheet.stopTime!!
                if (stopTime.after(period.toDate)) {
                    stopTime = period.toDate!!
                }
                total += stopTime.time - startTime.time
            }
            return total
        }

    /**
     * @return the totalHours of time sheets inside given period.
     */
    @JvmOverloads
    fun getTotal(unit: RoundUnit = RoundUnit.INT, roundingMode: RoundingMode = RoundingMode.HALF_UP): BigDecimal {
        if (timesheets.isEmpty()) {
            return BigDecimal.ZERO
        }
        var total = BigDecimal.ZERO
        timesheets.forEach { timesheet ->
            var startTime = timesheet.startTime!!
            if (period.fromDate != null && startTime.before(period.fromDate)) {
                startTime = period.fromDate!!
            }
            var stopTime = timesheet.stopTime!!
            if (period.toDate != null && stopTime.after(period.toDate)) {
                stopTime = period.toDate!!
            }
            total += TimePeriod.getDurationHours(startTime, stopTime, unit, roundingMode)
        }
        return total
    }


    /**
     * Adds the given time sheet only if the time sheet fits the period (full or partly).
     * @param timesheet the timesheet to add
     * @return this for chaining.
     */
    fun add(timesheet: TimesheetDO): TimesheetStats {
        val startTime = timesheet.startTime
        val stopTime = timesheet.stopTime
        if (startTime == null || stopTime == null) {
            return this
        }
        period.fromDate?.let {
            if (!it.before(stopTime)) {
                return this
            }
        }
        period.toDate?.let {
            if (!it.after(startTime)) {
                return this
            }
        }
        if (earliestStartDateIgnoringPeriod == null || earliestStartDateIgnoringPeriod!!.after(startTime)) {
            earliestStartDateIgnoringPeriod = startTime
        }
        if (latestStopDateIgnoringPeriod == null || latestStopDateIgnoringPeriod!!.before(stopTime)) {
            latestStopDateIgnoringPeriod = stopTime
        }
        timesheets.add(timesheet)
        return this
    }
}
