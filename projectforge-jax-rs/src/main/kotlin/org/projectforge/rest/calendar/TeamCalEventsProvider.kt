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
                  displayFilter: CalendarsDisplayFilter) {
        val eventFilter = TeamEventFilter()
        eventFilter.teamCals = displayFilter.calendarColorMapping.keys
        eventFilter.startDate = start.asUtilDate()
        eventFilter.endDate = end.asUtilDate()
        eventFilter.user = ThreadLocalUserContext.getUser()
        val teamEvents = teamEventDao.getEventList(eventFilter, true)
        teamEvents?.forEach {
            val eventDO: TeamEventDO
            if (it is TeamEventDO) {
                eventDO = it
            } else {
                eventDO = (it as TeamRecurrenceEvent).master
            }

            val bgColor = displayFilter.calendarColorMapping.get(eventDO.getCalendarId())
            val link = "wa/editEvent?id=${eventDO.id}"
            val allDay = eventDO.isAllDay()
            events.add(BigCalendarEvent(eventDO.id, it.subject, it.startDate, it.endDate, allDay,
                    location = it.location, desc = it.note, link = link, bgColor = bgColor))
        }
    }
}
