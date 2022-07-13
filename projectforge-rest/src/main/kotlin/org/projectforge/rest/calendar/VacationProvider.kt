/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.business.vacation.VacationCache
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Provides the vacation days of the employees. You may filter the vacation by ProjectForge groups.
 */
@Component
open class VacationProvider {
  @Autowired
  private lateinit var vacationCache: VacationCache

  /**
   * @param groupIds Null items should only occur on (de)serialization issues.
   * @param userIds Null items should only occur on (de)serialization issues.
   */
  open fun addEvents(
    start: PFDateTime,
    end: PFDateTime,
    events: MutableList<FullCalendarEvent>,
    /**
     * Vacation days will only be displayed for employees (users) who are member of at least one of the following groups:
     */
    groupIds: Set<Int?>?,
    userIds: Set<Int?>?,
    bgColor: String? = null,
    fgColor: String? = null
  ) {
    if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
      return // Nothing to do
    }

    val vacations =
      vacationCache.getVacationForPeriodAndUsers(start.beginOfDay.localDate, end.localDate, groupIds, userIds)
    vacations.forEach { vacation ->
      val endDate = PFDay.fromOrNull(vacation.endDate)?.format() ?: ""
      val title = "${translate("vacation")}: ${vacation.employee?.user?.getFullname()} ${translate("date.until")} $endDate"
      if (!events.any {
          it.title == title && FullCalendarEvent.samePeriod(it, vacation.startDate, vacation.endDate) &&
              vacation.status != VacationStatus.REJECTED
        }) {
        // Event doesn't yet exist:
        events.add(
          FullCalendarEvent.createAllDayEvent(
            id = vacation.id,
            category = FullCalendarEvent.Category.VACATION,
            title = title,
            start = vacation.startDate!!,
            end = vacation.endDate!!,
            backgroundColor = bgColor,
            textColor = fgColor,
            dbId = vacation.id,
            classNames = "vacation-event"
          )
        )
      }
    }
  }
}
