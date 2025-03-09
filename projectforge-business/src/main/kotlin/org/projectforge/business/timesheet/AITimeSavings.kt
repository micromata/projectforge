/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.Constants
import org.projectforge.common.extensions.formatPercent
import org.projectforge.common.extensions.isZeroOrNull
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.utils.NumberHelper
import java.math.BigDecimal

/**
 * Central functions for calculating and formatting AI time savings.
 */
object AITimeSavings {
    class Stats {
        var totalDurationMillis: Long = 0
        var totalTimeSavedByAIMillis: Long = 0
        val percentageString: String
            get() {
                return getFormattedPercentage(totalDurationMillis, totalTimeSavedByAIMillis)
            }

        fun add(timesheet: TimesheetDO) {
            val duration = timesheet.duration
            totalDurationMillis += duration
            totalTimeSavedByAIMillis += getTimeSavedByAIMillis(timesheet, duration)
        }
    }

    /**
     * Set by TimesheetDao.
     */
    var timeSavingsByAIEnabled = false
        internal set

    /**
     * Returns the formatted percentage.
     * Example: "10 %"
     * @param duration
     * @param timeSavedByAI
     * @return The formatted percentage.
     */
    fun getFormattedPercentage(duration: Number, timeSavedByAI: Number): String {
        val percent = getPercentage(duration, timeSavedByAI)
        return getFormattedPercentage(percent)
    }

    /**
     * Returns the formatted percentage (scale = 1 for <10 %. Otherwise, scale = 0).
     * Example: "3,6 %", "10 %", etc.
     * @param percent
     * @return The formatted percentage. If the percent is null, "0 %" is returned.
     */
    fun getFormattedPercentage(percent: BigDecimal?): String {
        if (percent.isZeroOrNull()) {
            return "0 %"
        }
        val scale = if (percent!!.abs() < BigDecimal.TEN) 1 else 0
        return percent.formatPercent(true, scale = scale)
    }

    /**
     * Returns the percentage: Sum of duration and time saved by AI divided by time saved by AI.
     * @param duration
     * @param timeSavedByAI
     * @return The percentage.
     * @see NumberHelper.getPercent
     */
    fun getPercentage(duration: Number, timeSavedByAI: Number): BigDecimal {
        val savedByAI = timeSavedByAI.toLong()
        return NumberHelper.getPercent(part = savedByAI, total = duration.toLong() + savedByAI)
    }

    /**
     * Returns the faction: Sum of duration and time saved by AI divided by time saved by AI.
     * @param duration
     * @param timeSavedByAI
     * @return The percentage.
     * @see NumberHelper.getPercent
     */
    fun getFraction(duration: Number, timeSavedByAI: Number): BigDecimal {
        return NumberHelper.getFraction(timeSavedByAI, duration.toLong() + timeSavedByAI.toLong())
    }

    /**
     * Returns the formatted time saved by AI.
     * Example: "0:30h, 5,7 %", "1:30h, 10 %", etc.
     * @param timesheet
     * @param emptyStringIfNull If true, an empty string is returned if the time saved by AI is null.
     * @return The formatted time saved by AI.
     */
    @JvmStatic
    @JvmOverloads
    fun getFormattedTimeSavedByAI(timesheet: TimesheetDO, emptyStringIfNull: Boolean = true): String {
        val ms = getTimeSavedByAIMillisOrNull(timesheet, timesheet.duration)
        if (ms == null && emptyStringIfNull) {
            return ""
        }
        return "${
            DateTimeFormatter.instance().getFormattedDuration(ms ?: 0L)
        }, ${getFormattedPercentage(timesheet.duration, ms ?: 0L)}"
    }

    @JvmStatic
    fun getTimeSavedByAIMillis(timesheet: TimesheetDO): Long {
        return getTimeSavedByAIMillis(timesheet, timesheet.duration)
    }

    /**
     * Only for avoiding calculation of duration again.
     * @param timesheet
     * @param duration The duration of the timesheet in milliseconds.
     */
    fun getTimeSavedByAIMillis(timesheet: TimesheetDO, duration: Long): Long {
        return getTimeSavedByAIMillisOrNull(timesheet, duration) ?: 0L
    }

    /**
     * Returns the time saved by AI in milliseconds.
     * @param timesheet
     * @return The time saved by AI in milliseconds. If the time saved by AI is null, 0 is returned.
     */
    @JvmStatic
    fun getTimeSavedByAIMillisOrNull(timesheet: TimesheetDO): Long? {
        return getTimeSavedByAIMillisOrNull(timesheet, timesheet.duration)
    }

    /**
     * Returns the time saved by AI in milliseconds.
     * @param timesheet
     * @param duration The duration of the timesheet in milliseconds. Only for avoiding calculation of duration again.
     * @return The time saved by AI in milliseconds. If the time saved by AI is null, 0 is returned.
     */
    fun getTimeSavedByAIMillisOrNull(timesheet: TimesheetDO, duration: Long): Long? {
        timesheet.timeSavedByAI?.let { value ->
            return when (timesheet.timeSavedByAIUnit) {
                TimesheetDO.TimeSavedByAIUnit.HOURS -> value.multiply(Constants.MILLIS_PER_HOUR_BD).toLong()
                TimesheetDO.TimeSavedByAIUnit.PERCENTAGE -> {
                    val percentage = value.toLong()
                    if (percentage >= 100) {
                        0L
                    } else {
                        (percentage * duration) / (100 - percentage)
                    }
                }

                else -> 0 // nothing. Shouldn't happen.
            }
        }
        return null
    }

    @JvmStatic
    fun buildStats(timesheets: Collection<TimesheetDO>?): Stats {
        val stats = Stats()
        timesheets?.forEach { stats.add(it) }
        return stats
    }
}
