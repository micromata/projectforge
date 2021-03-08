/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.time.PFDateTime.Companion.from
import java.util.*

/**
 * Provides some helper methods.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object TimesheetUtils {
    /**
     * Analyzes the given time-sheets. The time-sheets will be filtered (by given day and given user).
     * @param timesheets
     * @param day
     * @param userId
     * @return
     */
    @JvmStatic
    fun getStats(timesheets: Collection<TimesheetDO>?, day: Date?, userId: Int?): TimesheetStats? {
        val dt = from(day!!).beginOfDay
        val startDate = dt.utilDate
        val stopDate = dt.plusDays(1).utilDate
        return getStats(timesheets, startDate, stopDate, userId)
    }

    /**
     * Analyzes the given time-sheets. The time-sheets will be filtered (by given time period and given user).
     * @param timesheets
     * @param from
     * @param to
     * @param userId
     * @return
     */
    fun getStats(timesheets: Collection<TimesheetDO>?, from: Date?, to: Date?, userId: Int?): TimesheetStats? {
        if (timesheets == null || timesheets.size == 0) {
            return null
        }
        val stats = TimesheetStats(from, to)
        for (timesheet in timesheets) {
            if (userId != timesheet.userId) {
                continue
            }
            stats.add(timesheet)
        }
        return stats
    }
}
