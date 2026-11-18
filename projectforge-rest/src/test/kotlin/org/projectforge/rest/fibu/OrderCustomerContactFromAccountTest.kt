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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KontoDO
import org.projectforge.rest.dto.Auftrag

/** The "add from account" button fills only what the order leaves empty: the order's own contact takes precedence. */
class OrderCustomerContactFromAccountTest {
    private val account = KontoDO().also {
        it.contactPerson = "Erika Mustermann"
        it.contactDepartment = "Einkauf"
        it.contactEmail = "einkauf@example.com"
    }

    @Test
    fun `an empty contact is taken from the account`() {
        val contact = OrderEntityRest.customerContactFromAccount(Auftrag(), account)
        assertEquals("Erika Mustermann", contact.customerContactPerson)
        assertEquals("Einkauf", contact.customerContactDepartment)
        assertEquals("einkauf@example.com", contact.customerContactEmail)
    }

    @Test
    fun `the contact of the order wins over the account`() {
        val order = Auftrag(customerContactPerson = "Max Muster", customerContactEmail = " ")
        val contact = OrderEntityRest.customerContactFromAccount(order, account)
        assertEquals("Max Muster", contact.customerContactPerson)
        assertEquals("Einkauf", contact.customerContactDepartment)
        // A blank value counts as empty.
        assertEquals("einkauf@example.com", contact.customerContactEmail)
    }

    @Test
    fun `without an account the contact stays as it is`() {
        val contact = OrderEntityRest.customerContactFromAccount(Auftrag(customerContactDepartment = "IT"), null)
        assertNull(contact.customerContactPerson)
        assertEquals("IT", contact.customerContactDepartment)
    }
}
