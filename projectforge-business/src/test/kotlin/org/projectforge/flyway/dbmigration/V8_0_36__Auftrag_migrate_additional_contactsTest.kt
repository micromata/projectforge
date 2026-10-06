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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class V8_0_36__Auftrag_migrate_additional_contactsTest {
    private fun build(contactPerson: Long?, vararg managers: Long?) =
        V8_0_36__Auftrag_migrate_additional_contacts.buildAdditionalContacts(contactPerson, *managers)

    @Test
    fun `keeps the order of the managers and drops nulls`() {
        assertEquals("3, 1, 2", build(null, 3, 1, 2))
        assertEquals("1, 2", build(null, 1, null, 2))
    }

    @Test
    fun `leaves out duplicates and the contact person`() {
        assertEquals("2", build(1, 1, 2, 2))
        assertEquals("2, 3", build(1, 2, 3, 1))
    }

    @Test
    fun `answers null if no further contact remains`() {
        assertNull(build(1, 1, null, 1))
        assertNull(build(null, null, null, null))
    }
}
