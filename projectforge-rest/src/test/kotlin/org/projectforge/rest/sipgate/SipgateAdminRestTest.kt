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

package org.projectforge.rest.sipgate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired

/**
 * The Sipgate actions of the System page are refused for an ordinary user. An admin passes the access check and,
 * as Sipgate isn't configured in the test context, gets the clear "not configured" message instead of a client error.
 */
class SipgateAdminRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var sipgateAdminRest: SipgateAdminRest

    @Autowired
    private lateinit var sipgateConfiguration: SipgateConfiguration

    @Test
    fun `sipgate actions are refused for an ordinary user`() {
        logon(TEST_USER)
        assertThrows<AccessException> { sipgateAdminRest.downloadConfiguration() }
        assertThrows<AccessException> { sipgateAdminRest.synchronize() }
        assertThrows<AccessException> { sipgateAdminRest.resetContacts() }
    }

    @Test
    fun `an admin passes the access check, but an unconfigured sipgate is refused`() {
        assertFalse(sipgateConfiguration.isConfigured())
        logon(TEST_ADMIN_USER)
        listOf(
            { sipgateAdminRest.downloadConfiguration() },
            { sipgateAdminRest.synchronize() },
            { sipgateAdminRest.resetContacts() },
        ).forEach { action ->
            val ex = assertThrows<UserException> { action() }
            assertEquals("system.admin.sipgate.notConfigured", ex.i18nKey)
        }
    }
}
