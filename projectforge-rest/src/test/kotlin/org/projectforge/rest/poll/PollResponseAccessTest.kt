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

package org.projectforge.rest.poll

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.poll.types.PollResponse
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * Only participants answer a poll, and only for themselves - or a user with full access on behalf of an
 * attendee. The poll and the stored response are taken from the database, never from the posted data.
 */
class PollResponseAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var pollResponsePageRest: PollResponsePageRest

    @Test
    fun `only participants answer, and only for themselves`() {
        val owner = getUser(TEST_USER)
        val attendee = getUser(TEST_USER2)
        val outsider = getUser(TEST_FINANCE_USER)
        val poll = PollDO().also {
            it.title = "poll access test"
            it.deadline = LocalDate.now().plusDays(7)
            it.owner = owner
            it.attendeeIds = "${attendee.id}"
        }
        pollDao.insert(poll, checkAccess = false)

        logon(outsider)
        assertThrows(AccessException::class.java) { answer(poll, null) }
        assertThrows(AccessException::class.java) { answer(poll, attendee.id) }

        logon(attendee)
        answer(poll, null)
        val responseId = responsesOf(poll).single().id
        // A second answer updates the response, whatever id is posted.
        answer(poll, null, postedId = 999_999_999L)
        assertEquals(listOf(responseId), responsesOf(poll).map { it.id })
        // Not on behalf of others without full access:
        assertThrows(AccessException::class.java) { answer(poll, owner.id) }

        logon(outsider)
        assertThrows(AccessException::class.java) { pollResponseDao.find(responseId) }

        logon(owner)
        assertEquals(attendee.id, pollResponseDao.find(responseId)?.owner?.id)
    }

    private fun answer(poll: PollDO, questionOwner: Long?, postedId: Long? = null) {
        val data = PollResponse().also {
            it.id = postedId
            it.poll = PollDO().also { stub -> stub.id = poll.id }
        }
        pollResponsePageRest.addResponse(PostData(data, watchFieldsTriggered = null, serverData = null), questionOwner)
    }

    private fun responsesOf(poll: PollDO) =
        pollResponseDao.selectAll(checkAccess = false).filter { it.poll?.id == poll.id }
}
