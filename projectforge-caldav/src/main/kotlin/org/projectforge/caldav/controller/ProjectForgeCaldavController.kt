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

package org.projectforge.caldav.controller

import io.milton.annotations.*
import org.projectforge.caldav.model.*
import org.projectforge.caldav.model.Calendar
import org.projectforge.caldav.service.CalendarService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

@ResourceController
open class ProjectForgeCaldavController : BaseAuthenticationController() {
    @Autowired
    private lateinit var calendarService: CalendarService

    @get:Root
    val root: ProjectForgeCaldavController
        get() = this

    @ChildrenOf
    fun getUsersHome(root: ProjectForgeCaldavController?): UsersHome {
        if (usersHome == null) {
            log.info("Create new UsersHome")
            usersHome = UsersHome()
        }
        return usersHome!!
    }

    @ChildrenOf
    fun getCalendarsHome(user: User): CalendarsHome {
        log.info("Creating CalendarsHome for user:$user")
        return CalendarsHome(user)
    }

    @ChildrenOf
    @Calendars
    fun getCalendarsHome(cals: CalendarsHome): List<Calendar> {
        return calendarService.getCalendarList(cals.user)
    }

    @ChildrenOf
    fun getCalendarEvents(cal: Calendar?): List<Meeting> {
        if (cal == null) {
            return emptyList()
        }
        return calendarService.getCalendarEvents(cal)
    }

    @Get
    @ICalData
    fun getMeetingData(m: Meeting): ByteArray? {
        return m.icalData
    }

    @PutChild
    fun createMeeting(cal: Calendar, ical: ByteArray, newName: String?): Meeting? {
        val requestMeeting = Meeting(cal)
        requestMeeting.icalData = ical
        val now = Date()
        requestMeeting.createDate = now
        requestMeeting.modifiedDate = now
        requestMeeting.name = newName
        return calendarService.saveCalendarEvent(requestMeeting)
    }

    @PutChild
    fun updateMeeting(m: Meeting, ical: ByteArray): Meeting? {
        m.icalData = ical
        val meetingUpdated = calendarService.updateCalendarEvent(m) ?: return null
        // update modification date in event parameter, required for computing eTag!
        m.modifiedDate = meetingUpdated.modifiedDate
        return meetingUpdated
    }

    @Delete
    fun deleteMeeting(m: Meeting) {
        calendarService.deleteCalendarEvent(m)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProjectForgeCaldavController::class.java)
    }

    init {
        log.info("init")
    }
}
