/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.calendar.CalendarStyleMap
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.TeamEventFilter
import org.projectforge.business.teamcal.event.TeamRecurrenceEvent
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.utils.HtmlHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper.greaterZero
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Provides the events of a calendar (team calendar).
 */
@Component
open class TeamCalEventsProvider() {

  @Autowired
  private lateinit var teamCalDao: TeamCalDao

  @Autowired
  private lateinit var teamEventDao: TeamEventDao

  @Autowired
  private lateinit var vacationProvider: VacationProvider

  /**
   * @param teamCalendarIds Null items should only occur on (de)serialization issues.
   */
  open fun addEvents(
    start: PFDateTime,
    end: PFDateTime,
    events: MutableList<FullCalendarEvent>,
    teamCalendarIds: List<Long?>?,
    styleMap: CalendarStyleMap,
    settings: CalendarSettings,
  ) {
    if (teamCalendarIds.isNullOrEmpty())
      return
    val eventFilter = TeamEventFilter()
    eventFilter.teamCals = teamCalendarIds
    eventFilter.startDate = start.utilDate
    eventFilter.endDate = end.utilDate
    eventFilter.user = ThreadLocalUserContext.loggedInUser
    val teamEvents = teamEventDao.getEventList(eventFilter, true)
    teamEvents.forEach {
      val eventDO: TeamEventDO
      //val recurrentEvent: Boolean
      if (it is TeamEventDO) {
        eventDO = it
        //recurrentEvent = false
      } else {
        eventDO = (it as TeamRecurrenceEvent).master
        //recurrentEvent = true
      }
      //val recurrentDate = if (recurrentEvent) "?recurrentDate=${it.startDate!!.time / 1000}" else ""
      //val link = "teamEvent/edit/${eventDO.id}$recurrentDate"
      val allDay = eventDO.allDay
      val style = styleMap.get(eventDO.calendarId)
      val dbId: Long?
      val uid: String?
      if (eventDO.id!! > 0) {
        dbId = eventDO.id
        uid = null
      } else {
        dbId = null
        uid = "${eventDO.calendarId}-${eventDO.uid}"
      }
      val duration = it.endDate!!.time - it.startDate!!.time
      val event = if (allDay) {
        val useStart = PFDay.fromOrNow(it.startDate).localDate
        val useEnd = PFDay.fromOrNow(it.endDate).localDate
        FullCalendarEvent.createAllDayEvent(
          id = dbId ?: uid,
          category = FullCalendarEvent.Category.TEAM_CAL_EVENT,
          title = it.subject,
          calendarSettings = settings,
          start = useStart,
          end = useEnd,
        )
      } else {
        FullCalendarEvent.createEvent(
          id = dbId ?: uid,
          category = FullCalendarEvent.Category.TEAM_CAL_EVENT,
          title = it.subject,
          calendarSettings = settings,
          start = it.startDate!!,
          end = it.endDate!!,
        )
      }
      event.setDuration(duration)
      event.editable = true
      event.ensureExtendedProps().let { props ->
        props.dbId = dbId
        props.uid = uid
      }
      event.withColor(settings, style = style)

      val tooltipBuilder = TooltipBuilder()
      val title = eventDO.calendar?.title ?: ""
      eventDO.subject?.let {
        if (it.isNotBlank()) {
          tooltipBuilder.addPropRow(translate("plugins.teamcal.event.subject"), it, abbreviate = true)
        }
      }
      eventDO.location?.let {
        if (it.isNotBlank()) {
          tooltipBuilder.addPropRow(translate("plugins.teamcal.event.location"), it, abbreviate = true)
        }
      }
      eventDO.note?.let {
        if (it.isNotBlank()) {
          tooltipBuilder.addPropRow(translate("plugins.teamcal.event.note"), it, abbreviate = true)
        }
      }
      if (eventDO.hasRecurrence()) {
        val recurrenceData = eventDO.getRecurrenceData(ThreadLocalUserContext.timeZone)
        val frequency = recurrenceData.frequency
        if (frequency != null) {
          val unitI18nKey = frequency.unitI18nKey
          if (unitI18nKey != null) {
            tooltipBuilder.addPropRow(
              translate("plugins.teamcal.event.recurrence"),
              recurrenceData.interval.toString() + " " + translate(unitI18nKey),
            )
          }
        }
      }

      if (eventDO.reminderActionType != null && greaterZero(eventDO.reminderDuration) && eventDO.reminderDurationUnit != null) {
        tooltipBuilder.addPropRow(
          translate("plugins.teamcal.event.reminder"),
          translate(eventDO.reminderActionType!!.i18nKey)
              + " "
              + eventDO.reminderDuration
              + " "
              + translate(eventDO.reminderDurationUnit!!.i18nKey)
        )
      }
      eventDO.attendees?.let { attendees ->
        if (attendees.isNotEmpty()) {
          val sb = StringBuilder()
          sb.append("<ul>")
          attendees.forEach { teamEventAttendeeDO ->
            sb.append("<li>")
            if (teamEventAttendeeDO.user != null) {
              sb.append(HtmlHelper.escapeHtml(teamEventAttendeeDO.user!!.getFullname()))
            } else if (teamEventAttendeeDO.url != null) {
              sb.append(HtmlHelper.escapeHtml(teamEventAttendeeDO.url))
            } else {
              sb.append(HtmlHelper.escapeHtml(teamEventAttendeeDO.address!!.fullName))
            }
            teamEventAttendeeDO.status.let { status ->
              if (status != null) {
                sb.append("  [")
                  .append(HtmlHelper.escapeHtml(translate(status.i18nKey)))
                  .append("]")
              } else {
                sb.append("  [")
                  .append(HtmlHelper.escapeHtml(translate(TeamEventAttendeeStatus.IN_PROCESS.i18nKey)))
                  .append("]")
              }
            }
            sb.append("</li>")
          }
          sb.append("</ul>")
          tooltipBuilder.addPropRow(translate("plugins.teamcal.attendees"), sb.toString(), escapeHtml = false)
        }
      }
      event.setTooltip(title, tooltipBuilder)
      events.add(event)
    }
    for (calId in teamCalendarIds) {
      val cal = teamCalDao.find(calId, checkAccess = false) ?: continue
      if (cal.includeLeaveDaysForGroups.isNullOrBlank() && cal.includeLeaveDaysForUsers.isNullOrBlank()) {
        continue
      }
      val userIds = User.toLongArray(cal.includeLeaveDaysForUsers)?.toSet()
      val groupIds = Group.toLongArray(cal.includeLeaveDaysForGroups)?.toSet()
      val style = styleMap.get(calId)
      vacationProvider.addEvents(start, end, events, groupIds, userIds, settings, style)
    }
  }
}
