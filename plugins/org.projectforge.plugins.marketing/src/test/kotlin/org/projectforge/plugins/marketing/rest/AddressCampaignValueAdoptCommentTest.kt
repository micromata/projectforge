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

package org.projectforge.plugins.marketing.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AddressCampaignValueAdoptCommentTest {
    private fun append(own: String?, comparison: String?) =
        AddressCampaignValueMassUpdateRest.appendComment(own, comparison)

    @Test
    fun `the comparison comment is taken as it is without one of the row's own`() {
        assertEquals("Karte 2025", append(null, "Karte 2025"))
        assertEquals("Karte 2025", append("  ", " Karte 2025 "))
    }

    @Test
    fun `the comparison comment is appended to the row's own on a new line`() {
        assertEquals("Anruf im Mai\nKarte 2025", append("Anruf im Mai\n", "Karte 2025"))
    }

    @Test
    fun `nothing is adopted without a comparison comment or when it is already there`() {
        assertNull(append("Anruf", null))
        assertNull(append("Anruf", " "))
        assertNull(append("Anruf im Mai\nKarte 2025", "Karte 2025"))
    }
}
