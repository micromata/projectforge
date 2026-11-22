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

package org.projectforge.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.user.entities.PFUserDO

class OidcUserMatcherTest {
    private fun user(name: String, idpId: String? = null) = PFUserDO().also {
        it.username = name
        it.idpExternalId = idpId
    }

    @Test
    fun `sub finds the user, a name is only a fallback for users without an IdP identity`() {
        val bound = user("admin", "sub-admin")
        val unbound = user("legacy")
        val dao = mock<UserDao>()
        whenever(dao.getUserByIdpExternalId("sub-admin")).thenReturn(bound)
        whenever(dao.getInternalByName("admin")).thenReturn(bound)
        whenever(dao.getInternalByName("legacy")).thenReturn(unbound)

        assertSame(bound, OidcUserMatcher.resolve(dao, "sub-admin", "whatever"))
        assertSame(unbound, OidcUserMatcher.resolve(dao, "sub-new", "legacy"), "Not yet bound: found by name.")
        assertNull(OidcUserMatcher.resolve(dao, "sub-evil", "admin"), "Bound to another identity: never taken over by a name.")
        assertNull(OidcUserMatcher.resolve(dao, "sub-x", null))
        assertNull(OidcUserMatcher.resolve(dao, "sub-x", " "))
    }

    @Test
    fun `a session belongs to one identity`() {
        assertTrue(OidcUserMatcher.isSameIdentity(user("a", "sub-a"), "sub-a", "x"))
        assertFalse(OidcUserMatcher.isSameIdentity(user("a", "sub-a"), "sub-b", "a"))
        assertTrue(OidcUserMatcher.isSameIdentity(user("a"), "sub-a", "a"))
        assertFalse(OidcUserMatcher.isSameIdentity(user("a"), "sub-b", "b"))
        assertFalse(OidcUserMatcher.isSameIdentity(null, "sub-a", "a"))
    }
}
