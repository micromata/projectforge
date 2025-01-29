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

    fun getFormattedPercentage(duration: Number, timeSavedByAI: Number): String {
        val savedByAI = timeSavedByAI.toLong()
        val percent = NumberHelper.getPercent(duration.toLong() + savedByAI, savedByAI)
        return getFormattedPercentage(percent)
    }

    fun getFormattedPercentage(percent: BigDecimal?): String {
        if (percent.isZeroOrNull()) {
            return "0 %"
        }
        val scale = if (percent!!.abs() < BigDecimal.TEN) 1 else 0
        return percent.formatPercent(true, scale = scale)
    }

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
     * Only for avoiding calculation of duration again.
     * @param timesheet
     * @param duration The duration of the timesheet in milliseconds.
     */
    @JvmStatic
    fun getTimeSavedByAIMillisOrNull(timesheet: TimesheetDO): Long? {
        return getTimeSavedByAIMillisOrNull(timesheet, timesheet.duration)
    }

    /**
     * Only for avoiding calculation of duration again.
     * @param timesheet
     * @param duration The duration of the timesheet in milliseconds.
     */
    fun getTimeSavedByAIMillisOrNull(timesheet: TimesheetDO, duration: Long): Long? {
        timesheet.timeSavedByAI?.let { value ->
            return when (timesheet.timeSavedByAIUnit) {
                TimesheetDO.TimeSavedByAIUnit.HOURS -> value.toLong() * Constants.MILLIS_PER_HOUR
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
