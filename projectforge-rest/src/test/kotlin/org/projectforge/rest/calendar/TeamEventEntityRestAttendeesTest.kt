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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventAttendee
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

/**
 * The attendees are read-only in the editor: they reach the client and come back unchanged, so a save
 * keeps them.
 */
class TeamEventEntityRestAttendeesTest : AbstractTestBase() {
    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    @Autowired
    private lateinit var teamEventEntityRest: TeamEventEntityRest

    @Test
    fun `attendees survive the round trip through the editor`() {
        val owner = logon(TEST_USER)
        val calId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Attendees test"
            it.owner = owner
        })
        val attendees = listOf(
            TeamEventAttendee("Jane Doe", "jane@example.org", TeamEventAttendeeStatus.ACCEPTED),
            TeamEventAttendee(null, "john@example.org", null),
        )
        val now = System.currentTimeMillis()
        val eventId = teamEventDao.insert(TeamEventDO().also {
            it.calendar = teamCalDao.find(calId)
            it.subject = "Kick-off meeting"
            it.startDate = Date(now)
            it.endDate = Date(now + 3600_000)
            it.storeAttendees(attendees)
        })
        val dto = teamEventEntityRest.transformFromDB(teamEventDao.find(eventId)!!, editMode = true)
        assertEquals(attendees, dto.attendees)

        dto.subject = "Kick-off meeting (moved)"
        teamEventDao.update(teamEventEntityRest.transformForDB(dto))
        val reloaded = teamEventDao.find(eventId)!!
        assertEquals("Kick-off meeting (moved)", reloaded.subject)
        assertEquals(attendees, reloaded.attendeeList())

        // Without any, the DTO carries none (NON_NULL drops the field).
        reloaded.storeAttendees(null)
        assertNull(teamEventEntityRest.transformFromDB(reloaded, editMode = true).attendees)
        // A user with minimal access sees none.
        val event = TeamEventDO().also { it.storeAttendees(attendees) }
        assertNull(event.clearFields().attendeesJson)
    }
}
