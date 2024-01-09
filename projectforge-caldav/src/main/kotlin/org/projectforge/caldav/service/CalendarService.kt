/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.caldav.service

import mu.KotlinLogging
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.TeamEventFilter
import org.projectforge.business.teamcal.event.TeamEventService
import org.projectforge.business.teamcal.event.ical.ICalGenerator
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.caldav.model.Calendar
import org.projectforge.caldav.model.Meeting
import org.projectforge.caldav.model.User
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class CalendarService {
    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    @Autowired
    private lateinit var teamEventService: TeamEventService

    fun getCalendarList(user: User): List<Calendar> {
        if (user.id != ThreadLocalUserContext.userId!!.toLong()) {
            throw AccessException("Logged-in user differs from the user requested.")
        }
        val calendars = teamCalDao.getList(BaseSearchFilter())
        val result = calendars.map { cal ->
            Calendar(user, cal.id, cal.title ?: "untitled")
        }
        return result
    }

    fun getCalendarEvents(cal: Calendar): List<Meeting> {
        val result = mutableListOf<Meeting>()
        cal.id ?: return result
        val filter = TeamEventFilter().setTeamCals(listOf(cal.id))
        filter.startDate = now().minusDays(1000.toLong()).utilDate
        val generator = ICalGenerator.exportAllFields()
        generator.editableVEvent(true)
        teamEventService.getTeamEventDOList(filter).forEach {
            result.add(convert(generator, cal, it))
        }
        return result
    }

    @Suppress("UNUSED_PARAMETER")
    fun createCalendarEvent(meeting: Meeting): Meeting? {
        log.warn { "Creating of meetings not supported." }
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    fun updateCalendarEvent(meeting: Meeting): Meeting? {
        log.warn { "Updating of meetings not supported." }
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    fun deleteCalendarEvent(meeting: Meeting) {
        log.warn { "Deleting of meetings not supported." }
    }

    private fun convert(generator: ICalGenerator, cal: Calendar, event: TeamEventDO): Meeting {
        val result = Meeting(cal)
        result.uniqueId = event.uid
        result.createDate = event.created
        result.modifiedDate = event.lastUpdate
        result.name = event.uid + ".ics"
        result.icalData = generator.calendarAsByteStream.toByteArray()
        return result
    }

    private fun convertRestRequest(meeting: Meeting): TeamEventDO {
        val event = TeamEventDO()
        event.uid = meeting.uniqueId
        teamEventDao.setCalendar(event, meeting.calendar.id)
        //event.icsData = Base64.encodeBase64String(meeting.icalData)
        event.created = meeting.createDate
        event.lastUpdate = meeting.modifiedDate
        return event
    }
}
