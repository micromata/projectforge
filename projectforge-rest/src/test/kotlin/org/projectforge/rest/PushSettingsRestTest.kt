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


package org.projectforge.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.push.PushContentPrefService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

class PushSettingsRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var pushSettingsRest: PushSettingsRest

    @Autowired
    private lateinit var pushContentPrefService: PushContentPrefService

    @Test
    fun `a user switches the content of their own pushes`() {
        logon(TEST_USER)
        val userId = getUserId(TEST_USER)
        val other = getUserId(TEST_USER2)
        assertTrue(pushSettingsRest.get().content, "With content by default.")
        assertTrue(pushSettingsRest.get().available)
        assertFalse(pushSettingsRest.save(PushSettingsRest.Request(content = false)).content)
        assertFalse(pushContentPrefService.contentAllowed(userId))
        assertTrue(pushContentPrefService.contentAllowed(other), "Only the own setting changed.")
        assertTrue(pushSettingsRest.save(PushSettingsRest.Request(content = true)).content)
        assertTrue(pushContentPrefService.contentAllowed(userId))
    }

    @Test
    fun `a restricted user may not save`() {
        logon(TEST_ADMIN_USER)
        val user = initTestDB.addUser(PFUserDO().also {
            it.username = "PushSettingsRestTest.restricted"
            it.restrictedUser = true
        })
        logon(user)
        assertFalse(pushSettingsRest.get().writeAccess)
        assertThrows<AccessException> { pushSettingsRest.save(PushSettingsRest.Request(content = false)) }
        assertEquals(true, pushContentPrefService.get(user.id!!).content)
    }
}
