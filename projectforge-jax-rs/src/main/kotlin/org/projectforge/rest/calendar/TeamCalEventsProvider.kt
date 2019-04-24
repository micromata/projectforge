package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.TeamEventFilter
import org.projectforge.business.teamcal.event.TeamRecurrenceEvent
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Provides the events of a calendar (team calendar).
 */
@Component
class TeamCalEventsProvider() {

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    fun addEvents(start: PFDateTime,
                  end: PFDateTime,
                  events: MutableList<BigCalendarEvent>,
                  teamCalendarIds: List<Int>?,
                  styleMap: CalendarStyleMap) {
        if (teamCalendarIds.isNullOrEmpty())
            return
        val eventFilter = TeamEventFilter()
        eventFilter.teamCals = teamCalendarIds
        eventFilter.startDate = start.asUtilDate()
        eventFilter.endDate = end.asUtilDate()
        eventFilter.user = ThreadLocalUserContext.getUser()
        val teamEvents = teamEventDao.getEventList(eventFilter, true)
        teamEvents?.forEach {
            val eventDO: TeamEventDO
            val recurrentEvent: Boolean
            if (it is TeamEventDO) {
                eventDO = it
                recurrentEvent = false
            } else {
                eventDO = (it as TeamRecurrenceEvent).master
                recurrentEvent = true
            }
            val recurrentDate = if (recurrentEvent) "?recurrentDate=${it.startDate.time / 1000}" else ""
            val link = "teamEvent/edit/${eventDO.id}$recurrentDate"
            val allDay = eventDO.isAllDay()
            val style = styleMap.get(eventDO.calendarId)
            events.add(BigCalendarEvent(it.subject, it.startDate, it.endDate, allDay,
                    location = it.location, desc = it.note, category = "event", dbId = eventDO.id,
                    bgColor = style?.bgColor, fgColor = style?.fgColor))
        }
    }
}
