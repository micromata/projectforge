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

object AITimeSavings {
    class Stats {
        var totalDurationMillis: Long = 0
        var totalTimeSavedByAIMillis: Long = 0
        fun add(timesheet: TimesheetDO) {
            val duration = timesheet.duration
            totalDurationMillis += duration
            totalTimeSavedByAIMillis += getTimeSavedByAIMs(timesheet, duration)
        }
    }

    @JvmStatic
    fun getTimeSavedByAIMs(timesheet: TimesheetDO): Long {
        return getTimeSavedByAIMs(timesheet, timesheet.duration)
    }

    /**
     * Only for avoiding calculation of duration again.
     * @param timesheet
     * @param duration The duration of the timesheet in milliseconds.
     */
    fun getTimeSavedByAIMs(timesheet: TimesheetDO, duration: Long): Long {
        timesheet.timeSavedByAI?.let {
            return when (timesheet.timeSavedByAIUnit) {
                TimesheetDO.TimeSavedByAIUnit.HOURS -> it.toLong() * Constants.MILLIS_PER_HOUR
                TimesheetDO.TimeSavedByAIUnit.PERCENTAGE -> it.toLong() * duration / 100
                else -> 0 // nothing. Shouldn't happen.
            }
        }
        return 0L
    }

    fun buildStats(timesheets: Collection<TimesheetDO>): Stats {
        val stats = Stats()
        timesheets.forEach { stats.add(it) }
        return stats
    }
}
