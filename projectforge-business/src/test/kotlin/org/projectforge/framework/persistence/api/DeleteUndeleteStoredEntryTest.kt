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
package org.projectforge.framework.persistence.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.TeamEventDao
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

/**
 * Marking as deleted and undeleting check the stored entry as well as the posted one: posting a foreign entry with the
 * own user as owner (or into the own calendar) takes nothing over.
 */
class DeleteUndeleteStoredEntryTest : AbstractTestBase() {
    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var teamEventDao: TeamEventDao

    @Test
    fun `a foreign deleted calendar can't be undeleted by posting it with the own user as owner`() {
        val owner = logon(TEST_USER)
        val calId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Undelete test"
            it.owner = owner
        })
        teamCalDao.markAsDeleted(teamCalDao.find(calId)!!)

        val other = logon(TEST_USER2)
        val posted = teamCalDao.find(calId, checkAccess = false)!!.also {
            it.owner = other
            it.title = "taken over"
        }
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { teamCalDao.undelete(posted) }
        }
        assertTrue(teamCalDao.find(calId, checkAccess = false)!!.deleted)

        assertEquals("Undelete test", teamCalDao.find(calId, checkAccess = false)!!.title)

        // The owner may undelete (and change it on the way):
        logon(TEST_USER)
        teamCalDao.undelete(teamCalDao.find(calId, checkAccess = false)!!.also { it.title = "changed on undelete" })
        val stored = teamCalDao.find(calId, checkAccess = false)!!
        assertFalse(stored.deleted)
        assertEquals("changed on undelete", stored.title)
        assertEquals(owner.id, stored.owner?.id)
    }

    @Test
    fun `a foreign event can't be deleted by posting it with the own calendar`() {
        val owner = logon(TEST_USER)
        val calId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Delete test, owner"
            it.owner = owner
        })
        val eventId = insertEvent(calId, "Foreign event")
        val other = logon(TEST_USER2)
        val otherCalId = teamCalDao.insert(TeamCalDO().also {
            it.title = "Delete test, other"
            it.owner = other
        })
        val posted = teamEventDao.find(eventId, checkAccess = false)!!.also {
            it.calendar = teamCalDao.find(otherCalId)
        }
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { teamEventDao.markAsDeleted(posted) }
        }

        logon(TEST_USER)
        val stored = teamEventDao.find(eventId)!!
        assertFalse(stored.deleted)
        assertEquals(calId, stored.calendar?.id)
        // The owner of the calendar may delete it:
        teamEventDao.markAsDeleted(stored)
        assertTrue(teamEventDao.find(eventId, checkAccess = false)!!.deleted)
    }

    private fun insertEvent(calId: Long, subject: String): Long {
        val now = System.currentTimeMillis()
        return teamEventDao.insert(TeamEventDO().also {
            it.calendar = teamCalDao.find(calId)
            it.subject = subject
            it.startDate = Date(now)
            it.endDate = Date(now + 3600_000)
        })
    }
}
