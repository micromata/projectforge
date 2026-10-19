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

package org.projectforge.flyway.dbmigration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V9_0_11_2__Projekt_migrate_contactsTest {
    private fun build(vararg managers: Long?) = V9_0_11_2__Projekt_migrate_contacts.buildContacts(*managers)

    @Test
    fun `the project manager becomes the contact person, the others further contacts`() {
        assertEquals(V9_0_11_2__Projekt_migrate_contacts.Contacts(3, "1, 2"), build(3, 1, 2))
    }

    @Test
    fun `falls back to the next manager given and leaves out duplicates`() {
        assertEquals(V9_0_11_2__Projekt_migrate_contacts.Contacts(1, "2"), build(null, 1, 2))
        assertEquals(V9_0_11_2__Projekt_migrate_contacts.Contacts(2, null), build(null, null, 2))
        assertEquals(V9_0_11_2__Projekt_migrate_contacts.Contacts(1, "2"), build(1, 2, 1))
        assertEquals(V9_0_11_2__Projekt_migrate_contacts.Contacts(1, null), build(1, 1, 1))
    }

    @Test
    fun `no manager, no contact`() {
        assertEquals(V9_0_11_2__Projekt_migrate_contacts.Contacts(null, null), build(null, null, null))
    }
}
