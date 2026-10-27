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

package org.projectforge.business.scripting.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.reallocation.BuchungssatzKost2ShareDao
import org.projectforge.business.fibu.kost.reallocation.BuchungssatzKost2ShareService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired

/** The script variable `kost2ShareService` is for finance and controlling only. */
class Kost2ShareScriptingServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var shareService: BuchungssatzKost2ShareService

    @Autowired
    private lateinit var shareDao: BuchungssatzKost2ShareDao

    @Test
    fun `refused for an ordinary user, allowed for finance and controlling`() {
        val scriptingService = Kost2ShareScriptingService(shareService, shareDao)
        val record = BuchungssatzDO().also { it.id = -1L }
        logon(TEST_USER)
        assertThrows<AccessException> { scriptingService.expand(listOf(record)) }
        assertThrows<AccessException> { scriptingService.getShares(record) }
        listOf(TEST_FINANCE_USER, TEST_CONTROLLING_USER).forEach { user ->
            logon(user)
            assertEquals(1, scriptingService.expand(listOf(record)).size, user)
            assertEquals(0, scriptingService.getShares(record).size, user)
        }
    }
}
