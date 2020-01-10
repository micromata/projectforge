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

package org.projectforge.rest.calendar

import org.projectforge.business.vacation.VacationCache
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.time.PFDateTime

/**
 * Provides the vacation days of the employees. You may filter the vacation by ProjectForge groups.
 */
object VacationProvider {
    private val log = org.slf4j.LoggerFactory.getLogger(VacationProvider::class.java)
    private val holidays = Holidays.instance

    fun addEvents(vacationCache: VacationCache,
                  start: PFDateTime,
                  end: PFDateTime,
                  events: MutableList<BigCalendarEvent>,
                  /**
                   * Vacation days will only be displayed for employees (users) who are member of at least one of the following groups:
                   */
                  groupIds: Set<Int>?,
                  userIds: Set<Int>?) {
        if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
            return // Nothing to do
        }
        val vacations = vacationCache.getVacationForPeriodAndUsers(start.beginOfDay.localDate, end.localDate, groupIds, userIds)
        vacations.forEach {
            val bgColor= "#ffa500"
            val fgColor= "#ffffff"

            events.add(BigCalendarEvent(
                    title = it.employee?.user?.getFullname(),
                    start = it.startDate!!,
                    end = it.endDate!!,
                    allDay = true,
                    category = "vacation",
                    bgColor = bgColor,
                    fgColor = fgColor,
                    dbId = it.id))
        }
    }
}
