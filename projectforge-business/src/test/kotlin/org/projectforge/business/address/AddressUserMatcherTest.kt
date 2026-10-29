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

package org.projectforge.business.address

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.user.entities.PFUserDO

class AddressUserMatcherTest {
    private val user = PFUserDO().also {
        it.firstname = "Kai"
        it.lastname = "Reinhard"
        it.email = "k.reinhard@acme.test"
        it.alternativeEmails = "kai@acme.test"
    }

    private fun address(
        id: Long, firstName: String = "Kai", name: String = "Reinhard", organization: String? = null,
        email: String? = null,
    ) = AddressDO().also {
        it.id = id
        it.firstName = firstName
        it.name = name
        it.organization = organization
        it.email = email
    }

    @Test
    fun `names are compared trimmed and ignoring case`() {
        assertTrue(AddressUserMatcher.nameMatches(address(1, " kai ", "REINHARD"), user))
        assertFalse(AddressUserMatcher.nameMatches(address(1, "Karl"), user))
        assertFalse(AddressUserMatcher.nameMatches(address(1, name = "Reinhardt"), user))
    }

    @Test
    fun `e-mail also matches alternative addresses`() {
        assertTrue(AddressUserMatcher.emailMatches(address(1, email = "KAI@acme.test"), user))
        assertFalse(AddressUserMatcher.emailMatches(address(1, email = "other@acme.test"), user))
        assertFalse(AddressUserMatcher.emailMatches(address(1), user))
    }

    @Test
    fun `suggestions are ranked by e-mail, organization, name and skip inactive ones`() {
        val nameOnly = address(1)
        val organization = address(2, organization = "ACME GmbH")
        val email = address(3, email = "kai@acme.test")
        val other = address(4, firstName = "Anna")
        val outdated = address(5, email = "kai@acme.test").also { it.addressStatus = AddressStatus.OUTDATED }
        val inactive = address(6).also { it.contactStatus = ContactStatus.DEPARTED }
        val result = AddressUserMatcher.suggest(
            user, listOf(nameOnly, organization, email, other, outdated, inactive), "acme gmbh"
        )
        assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
    }
}
