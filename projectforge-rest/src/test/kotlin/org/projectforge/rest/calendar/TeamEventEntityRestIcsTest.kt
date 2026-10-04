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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import java.util.Date

class TeamEventEntityRestIcsTest : AbstractTestBase() {
    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    @Autowired
    private lateinit var teamEventEntityRest: TeamEventEntityRest

    @Test
    fun `a stored event is exported as ics file`() {
        val owner = logon(TEST_USER)
        val calId = teamCalDao.insert(TeamCalDO().also {
            it.title = "ICS export test"
            it.owner = owner
        })
        val now = System.currentTimeMillis()
        val eventId = teamEventDao.insert(TeamEventDO().also {
            it.calendar = teamCalDao.find(calId)
            it.subject = "Kick-off meeting"
            it.startDate = Date(now)
            it.endDate = Date(now + 3600_000)
        })
        val response = teamEventEntityRest.exportIcs(eventId)
        assertEquals(200, response.statusCode.value())
        val content = String((response.body as Resource).contentAsByteArray)
        assertTrue(content.contains("BEGIN:VEVENT"), content)
        assertTrue(content.contains("SUMMARY:Kick-off meeting"), content)
        assertTrue(response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)!!.contains(".ics"))

        logon(TEST_USER2) // No access to the calendar.
        assertThrows<AccessException> { teamEventEntityRest.exportIcs(eventId) }
    }
}
