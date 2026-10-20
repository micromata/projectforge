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


package org.projectforge.rest.scripting

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.TimesheetEntityRest
import org.projectforge.rest.dto.Script
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest

/**
 * The preset of a new script lists the variables and services a script can use: only for the users who may write
 * scripts (AbstractEntityRest.getNewEntry checks the insert access).
 */
class ScriptNewEntryAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var scriptEntityRest: ScriptEntityRest

    @Autowired
    private lateinit var myScriptEntityRest: MyScriptEntityRest

    @Autowired
    private lateinit var timesheetEntityRest: TimesheetEntityRest

    @Test
    fun `new script preset only for finance and controlling`() {
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { scriptEntityRest.getNewEntry(MockHttpServletRequest()) }
        assertThrows(AccessException::class.java) { myScriptEntityRest.getNewEntry(MockHttpServletRequest()) }

        logon(TEST_FINANCE_USER)
        val script = scriptEntityRest.getNewEntry(MockHttpServletRequest()).body as Script
        assertFalse(script.availableVariables.isNullOrBlank())

        logon(TEST_CONTROLLING_USER)
        assertTrue(scriptEntityRest.getNewEntry(MockHttpServletRequest()).statusCode.is2xxSuccessful)
    }

    @Test
    fun `entities without a user right stay open for every user`() {
        logon(TEST_USER)
        assertNotNull(timesheetEntityRest.getNewEntry(MockHttpServletRequest()).body)
    }
}
