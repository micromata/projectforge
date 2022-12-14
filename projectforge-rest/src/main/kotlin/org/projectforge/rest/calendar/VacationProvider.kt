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
import org.projectforge.business.calendar.CalendarStyle
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.vacation.VacationCache
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

/**
 * Provides the vacation days of the employees. You may filter the vacation by ProjectForge groups.
 */
@Component
open class VacationProvider {
  @Autowired
  private lateinit var userGroupCache: UserGroupCache

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
    settings: CalendarSettings,
    style: CalendarStyle? = null,
  ) {
    if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
      return // Nothing to do
    }
    val useStyle = style ?: CalendarStyle(settings.vacationsColorOrDefault)
    val vacations =
      vacationCache.getVacationForPeriodAndUsers(start.beginOfDay.localDate, end.localDate, groupIds, userIds)
    vacations.forEach { vacation ->
      val employeeUser = userGroupCache.getUser(vacation.employee)
      val title = "${translate("vacation")}: ${employeeUser?.getFullname()}"
      if (!events.any {
          it.title == title && FullCalendarEvent.samePeriod(it, vacation.startDate, vacation.endDate) &&
              vacation.status != VacationStatus.REJECTED
        }) {
        val duration = VacationService.getVacationDays(vacation)
        val unit = if (duration == BigDecimal.ONE) "fibu.common.workingDay" else "fibu.common.workingDays"
        // Event doesn't yet exist:
        val event = FullCalendarEvent.createAllDayEvent(
          id = vacation.id,
          category = FullCalendarEvent.Category.VACATION,
          title = title,
          start = vacation.startDate!!,
          end = vacation.endDate!!,
          style = useStyle,
          dbId = vacation.id,
          classNames = "vacation-event",
          formattedDuration = "$duration ${translate(unit)}",
          calendarSettings = settings,
        )
        val startDate = PFDay.fromOrNull(vacation.startDate)?.format() ?: ""
        val endDate = PFDay.fromOrNull(vacation.endDate)?.format() ?: ""
        val tb = TooltipBuilder()
          .addPropRow(translate("timePeriod"), "$startDate - $endDate")
          .addPropRow(
            translate("vacation.replacement"),
            vacation.allReplacements.joinToString { userGroupCache.getUser(it)?.displayName ?: "???" })
        if (!vacation.comment.isNullOrBlank()) {
          tb.addPropRow(translate("comment"), vacation.comment, abbreviate = true)
        }
        event.setTooltip(title, tb)
        events.add(event)
      }
    }
  }
}
