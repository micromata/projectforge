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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.user.entities.PFUserDO

class ContactUserIdsTest {
    @Test
    fun `normalize keeps the order and drops nulls, duplicates and the main contact`() {
        assertEquals("3, 1, 2", ContactUserIds.normalize(null, listOf(3, null, 1, 2, 1)))
        assertEquals("2", ContactUserIds.normalize(1, listOf(1, 2, 2)))
        assertNull(ContactUserIds.normalize(1, listOf(1, null)))
        assertNull(ContactUserIds.normalize(null, null))
    }

    @Test
    fun `main contact and further contacts are contacts alike`() {
        val project = ProjektDO().also {
            it.contactPerson = PFUserDO().also { user -> user.id = 1 }
            it.additionalContactUserIds = "2, 3"
        }
        assertTrue(project.isContact(1))
        assertTrue(project.isContact(3))
        assertFalse(project.isContact(4))
        assertFalse(project.isContact(null))
        assertFalse(ProjektDO().isContact(1))
    }
}
