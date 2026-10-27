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


package org.projectforge.rest.fibu.kost

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired

/**
 * The endpoints of the Kost2 reallocation are for the finance group only: every one of them refuses an ordinary user
 * (and also the controlling group, which may read the effective view only), and answers a finance member.
 */
class Kost2ReallocationRestAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var rest: Kost2ReallocationRest

    private val request = Kost2ReallocationRest.RunRequest(runId = 1L)

    @Test
    fun `an ordinary user is refused by every endpoint`() {
        logon(TEST_USER)
        assertRefused()
    }

    @Test
    fun `controlling is refused as well`() {
        logon(TEST_CONTROLLING_USER)
        assertRefused()
    }

    @Test
    fun `finance is allowed`() {
        logon(TEST_FINANCE_USER)
        assertTrue(rest.list().isEmpty())
        assertTrue(rest.defaultRules(Kost2ReallocationRest.DefaultRulesRequest()).isEmpty())
        // An unknown run is answered, not refused:
        assertEquals(406, rest.preview(request).statusCode.value())
        assertEquals(406, rest.apply(request).statusCode.value())
        assertEquals(406, rest.reapply(request).statusCode.value())
        assertEquals(406, rest.revert(request).statusCode.value())
        assertEquals(406, rest.delete(request).statusCode.value())
    }

    private fun assertRefused() {
        assertThrows<AccessException> { rest.list() }
        assertThrows<AccessException> { rest.getRun(1L) }
        assertThrows<AccessException> { rest.save(Kost2ReallocationRest.RunData(description = "x")) }
        assertThrows<AccessException> { rest.delete(request) }
        assertThrows<AccessException> { rest.defaultRules(Kost2ReallocationRest.DefaultRulesRequest(projektId = 1L)) }
        assertThrows<AccessException> { rest.timesheets(Kost2ReallocationRest.TimesheetsRequest(runId = 1L)) }
        assertThrows<AccessException> { rest.preview(request) }
        assertThrows<AccessException> { rest.apply(request) }
        assertThrows<AccessException> { rest.reapply(request) }
        assertThrows<AccessException> { rest.revert(request) }
    }
}
