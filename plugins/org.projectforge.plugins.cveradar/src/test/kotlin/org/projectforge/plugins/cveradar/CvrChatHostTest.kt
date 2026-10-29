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

package org.projectforge.plugins.cveradar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.rest.CveRadarRest
import org.projectforge.plugins.cveradar.rest.CloseRequest
import org.projectforge.plugins.cveradar.rest.TeamCheckRequest
import org.springframework.beans.factory.annotation.Autowired

/**
 * Who reads, writes and moderates the discussion of a case (all readers) and of a team check (its group and the
 * security team only), and that a closed case is read-only (CTX-05).
 */
class CvrChatHostTest : CveRadarTestBase() {
    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var caseHost: CvrCaseChatHost

    @Autowired
    private lateinit var checkHost: CvrTeamCheckChatHost

    @Autowired
    private lateinit var rest: CveRadarRest

    @Autowired
    private lateinit var teamCheckDao: CvrTeamCheckDao

    @Test
    fun `all readers discuss a case, others don't, a closed case is read-only`() {
        val st = user("st", CvrRole.SECURITY_TEAM)
        val reader = user("reader", CvrRole.READER)
        val outsider = user("outsider", null)
        logon(st)
        val case = CvrCaseDO().also {
            it.kind = CvrCaseKind.INCIDENT
            it.title = "Chat"
            it.status = CvrCaseStatus.NEW
        }
        caseDao.insert(case)
        val id = case.id!!
        val channel = CvrCaseChatHost.CHANNEL
        assertEquals(listOf(channel), caseHost.channels(id))
        assertTrue(caseHost.channels(-1).isEmpty(), "an unknown case is refused")
        assertTrue(caseHost.hasReadAccess(reader, id, channel) && caseHost.hasWriteAccess(reader, id, channel))
        assertTrue(caseHost.hasWriteAccess(st, id, channel))
        assertFalse(caseHost.hasReadAccess(outsider, id, channel))
        assertFalse(caseHost.hasWriteAccess(outsider, id, channel))
        assertFalse(caseHost.hasReadAccess(reader, id, "other"), "unknown channel")
        assertTrue(caseHost.isModerator(st, id, channel))
        assertFalse(caseHost.isModerator(reader, id, channel))
        assertTrue(caseHost.notice(id, channel, reader) != null)

        rest.close(id, CloseRequest("done"))
        assertTrue(caseHost.hasReadAccess(reader, id, channel))
        assertFalse(caseHost.hasWriteAccess(reader, id, channel), "closed: read-only")
        assertFalse(caseHost.hasWriteAccess(st, id, channel))
    }

    @Test
    fun `the discussion of a team check is for its group and the security team`() {
        val st = user("st2", CvrRole.SECURITY_TEAM)
        val reader = user("reader2", CvrRole.READER)
        logon(st)
        val case = CvrCaseDO().also {
            it.kind = CvrCaseKind.INCIDENT
            it.title = "Chat 2"
            it.status = CvrCaseStatus.NEW
        }
        caseDao.insert(case)
        rest.createTeamCheck(case.id!!, TeamCheckRequest(getGroupId(TEST_GROUP), "Project D"))
        val id = teamCheckDao.selectByCase(case.id!!).single().id!!
        val channel = CvrTeamCheckChatHost.CHANNEL
        assertTrue(checkHost.hasWriteAccess(st, id, channel))
        assertTrue(checkHost.hasWriteAccess(getUser(TEST_USER), id, channel), "a member of the asked group")
        assertFalse(checkHost.hasReadAccess(reader, id, channel), "a reader outside of the group sees no project details")
        assertFalse(checkHost.hasReadAccess(user("outsider2", null), id, channel))
        assertFalse(checkHost.searchableAcrossConversations)
        assertTrue(getUserId(TEST_USER) in checkHost.participantIds(id, channel))
        assertTrue(checkHost.title(id, channel)!!.endsWith("Project D"))
    }
}
