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

package org.projectforge.framework.persistence.user.api

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.Date

class UserContextPasswordChangeTest {
    @Test
    fun `a password change after the login ends the session, the own one not`() {
        val user = PFUserDO().also { it.id = 1; it.lastPasswordChange = Date(1_000) }
        val context = UserContext.createTestInstance(user)
        assertFalse(context.isPasswordChangedSinceLogin())
        // Changed elsewhere (another device, an admin, a password reset):
        user.lastPasswordChange = Date(2_000)
        assertTrue(context.isPasswordChangedSinceLogin())
        // Changed in this session:
        context.acceptOwnPasswordChange()
        assertFalse(context.isPasswordChangedSinceLogin())
    }

    @Test
    fun `the first password change of a user ends the session too`() {
        val user = PFUserDO().also { it.id = 1 }
        val context = UserContext.createTestInstance(user)
        assertFalse(context.isPasswordChangedSinceLogin())
        user.lastPasswordChange = Date(2_000)
        assertTrue(context.isPasswordChangedSinceLogin())
    }
}
