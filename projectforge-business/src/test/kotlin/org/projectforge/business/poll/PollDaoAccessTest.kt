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
package org.projectforge.business.poll

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * Who may change a poll is decided by the stored poll (owner, full access users), not by the posted one.
 */
class PollDaoAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var pollDao: PollDao

    @Test
    fun `a foreign poll can't be taken over by posting the own user as owner or full access user`() {
        val owner = getUser(TEST_USER)
        val pollId = pollDao.insert(PollDO().also {
            it.title = "poll takeover test"
            it.deadline = LocalDate.now().plusDays(7)
            it.owner = owner
        }, checkAccess = false)

        val other = logon(TEST_USER2)
        val postedAsOwner = pollDao.find(pollId, checkAccess = false)!!.also {
            it.owner = other
            it.title = "taken over"
        }
        val postedAsFullAccessUser = pollDao.find(pollId, checkAccess = false)!!.also {
            it.fullAccessUserIds = "${other.id}"
            it.title = "taken over"
        }
        suppressErrorLogs {
            assertThrows(AccessException::class.java) { pollDao.update(postedAsOwner) }
            assertThrows(AccessException::class.java) { pollDao.update(postedAsFullAccessUser) }
        }

        logon(owner)
        assertEquals("poll takeover test", pollDao.find(pollId)!!.title)
        pollDao.update(pollDao.find(pollId)!!.also { it.title = "changed by the owner" })
        assertEquals("changed by the owner", pollDao.find(pollId)!!.title)
    }
}
