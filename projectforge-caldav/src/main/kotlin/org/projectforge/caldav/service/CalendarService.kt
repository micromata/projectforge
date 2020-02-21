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

package org.projectforge.caldav.service

import org.apache.commons.codec.binary.Base64
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.caldav.model.Calendar
import org.projectforge.caldav.model.Meeting
import org.projectforge.caldav.model.User
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.model.rest.CalendarEventObject
import org.projectforge.model.rest.RestPaths
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import java.util.function.Consumer

@Service
class CalendarService {
    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    fun getCalendarList(user: User): List<Calendar> {
        if (user.pk != ThreadLocalUserContext.getUserId().toLong()) {
            throw AccessException("Logged-in user differs from the user requested.")
        }
        val calendars = teamCalDao.getList(BaseSearchFilter())
        val result = calendars.map { cal ->
            Calendar(user, cal.id, cal.title ?: "untitled") }
        return result
    }

    fun getCalendarEvents(cal: Calendar): List<Meeting> {
        var result = mutableListOf<Meeting>()
        return result
        /*try {
            val url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.TEAMEVENTS)
            val headers = HttpHeaders()
            headers["Accept"] = MediaType.APPLICATION_JSON_VALUE
            headers["authenticationUserId"] = cal.user.pk.toString()
            headers["authenticationToken"] = cal.user.authenticationToken
            val builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("calendarIds", cal.id)
            val entity: HttpEntity<*> = HttpEntity<Any>(headers)
            val response: HttpEntity<Array<CalendarEventObject>> = restTemplate!!.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, Array<CalendarEventObject>::class.java)
            val calendarEventArray = response.body
            log.info("Result of rest call (" + RestPaths.TEAMEVENTS + ") (Size: " + calendarEventArray.size + ") : " + calendarEventArray)
            result = convertRestResponse(cal, calendarEventArray)
        } catch (e: Exception) {
            log.error("Exception while getting calendar events for calendar: " + cal.name, e)
        }
        return result*/
    }

    fun saveCalendarEvent(meeting: Meeting): Meeting? {
        return sendCalendarEvent(meeting, RestPaths.buildPath(RestPaths.TEAMEVENTS, RestPaths.SAVE))
    }

    fun updateCalendarEvent(meeting: Meeting): Meeting? {
        return sendCalendarEvent(meeting, RestPaths.buildPath(RestPaths.TEAMEVENTS, RestPaths.UPDATE))
    }

    private fun sendCalendarEvent(meeting: Meeting, path: String): Meeting? {
        /*try {
            val request = convertRestRequest(meeting)
            val mapper = ObjectMapper()
            val json = mapper.writeValueAsString(request)
            val url = "$projectforgeServerAddress:$projectforgeServerPort$path"
            val headers = HttpHeaders()
            headers["Accept"] = MediaType.APPLICATION_JSON_VALUE
            headers.contentType = MediaType.APPLICATION_JSON
            headers["authenticationUserId"] = meeting.calendar.user.pk.toString()
            headers["authenticationToken"] = meeting.calendar.user.authenticationToken
            val entity: HttpEntity<*> = HttpEntity(json, headers)
            val builder = UriComponentsBuilder.fromHttpUrl(url)
            val response = restTemplate
                    .exchange(builder.build().encode().toUri(), HttpMethod.PUT, entity, CalendarEventObject::class.java)
            val calendarEvent = response.body
            log.info("Result of rest call: $calendarEvent")
            return convertRestResponse(meeting.calendar, calendarEvent)
        } catch (e: Exception) {
            log.error("Exception while creating calendar event: " + meeting.name, e)
        }*/
        return null
    }

    fun deleteCalendarEvent(meeting: Meeting) {
        /* try {
             val request = convertRestRequest(meeting)
             val mapper = ObjectMapper()
             val json = mapper.writeValueAsString(request)
             val url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.TEAMEVENTS)
             val headers = HttpHeaders()
             headers["Accept"] = MediaType.APPLICATION_JSON_VALUE
             headers.contentType = MediaType.APPLICATION_JSON
             headers["authenticationUserId"] = meeting.calendar.user.pk.toString()
             headers["authenticationToken"] = meeting.calendar.user.authenticationToken
             val entity: HttpEntity<*> = HttpEntity(json, headers)
             val builder = UriComponentsBuilder.fromHttpUrl(url)
             val response = restTemplate
                     .exchange(builder.build().encode().toUri(), HttpMethod.DELETE, entity, CalendarEventObject::class.java)
             val calendarEvent = response.body
             log.info("Result of rest call: $calendarEvent")
         } catch (e: Exception) {
             log.error("Exception while creating calendar event: " + meeting.name, e)
         }*/
    }

    private fun convertRestResponse(cal: Calendar, calendarEventArray: Array<CalendarEventObject>): List<Meeting?> {
        val result: MutableList<Meeting?> = ArrayList()
        val calEventObjList = Arrays.asList(*calendarEventArray)
        calEventObjList.forEach(Consumer { calEventObj: CalendarEventObject -> result.add(convertRestResponse(cal, calEventObj)) })
        return result
    }

    private fun convertRestResponse(cal: Calendar, calendarEvent: CalendarEventObject): Meeting {
        val result = Meeting(cal)
        result.uniqueId = calendarEvent.uid
        result.createDate = calendarEvent.created
        result.modifiedDate = calendarEvent.lastUpdate
        result.name = calendarEvent.uid + ".ics"
        result.icalData = Base64.decodeBase64(calendarEvent.icsData)
        return result
    }

    private fun convertRestRequest(m: Meeting): CalendarEventObject {
        val result = CalendarEventObject()
        result.uid = m.uniqueId
        result.calendarId = m.calendar.id
        result.icsData = Base64.encodeBase64String(m.icalData)
        result.created = m.createDate
        result.lastUpdate = m.modifiedDate
        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(CalendarService::class.java)
    }
}
