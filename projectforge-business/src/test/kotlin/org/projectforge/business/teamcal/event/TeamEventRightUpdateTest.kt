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
package org.projectforge.business.teamcal.event

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

/**
 * On update the calendar of the stored event decides as well: a foreign event posted with the own calendar is
 * neither changed nor moved.
 */
class TeamEventRightUpdateTest : AbstractTestBase() {
    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    @Test
    fun `a foreign event can't be moved into the own calendar`() {
        val owner = logon(TEST_USER)
        val calId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Update test, owner"
            it.owner = owner
        })
        val now = System.currentTimeMillis()
        val eventId = teamEventDao.insert(TeamEventDO().also {
            it.calendar = teamCalDao.find(calId)
            it.subject = "Foreign event"
            it.startDate = Date(now)
            it.endDate = Date(now + 3600_000)
        })
        val other = logon(TEST_USER2)
        val otherCalId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Update test, other"
            it.owner = other
        })
        val posted = teamEventDao.find(eventId, checkAccess = false)!!.also {
            it.calendar = teamCalDao.find(otherCalId)
            it.subject = "taken over"
        }
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { teamEventDao.update(posted) }
        }

        logon(TEST_USER)
        teamEventDao.update(teamEventDao.find(eventId)!!.also { it.subject = "changed by the owner" })
        val stored = teamEventDao.find(eventId)!!
        assertEquals("changed by the owner", stored.subject)
        assertEquals(calId, stored.calendar?.id)
    }
}
