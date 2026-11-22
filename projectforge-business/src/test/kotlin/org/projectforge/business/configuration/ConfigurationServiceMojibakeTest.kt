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

package org.projectforge.business.configuration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConfigurationServiceMojibakeTest {
    @Test
    fun `a euro sign read as ISO-8859-1 is repaired`() {
        val broken = String("€".toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1)
        assertEquals("â\u0082¬", broken)
        assertEquals("€", ConfigurationService.fixMojibake(broken))
    }

    @Test
    fun `correct and other texts stay as they are`() {
        listOf("€", "$", "CHF", "£", "kr", "Fr.", "ä", "äö", "").forEach {
            assertEquals(it, ConfigurationService.fixMojibake(it), it)
        }
        assertNull(ConfigurationService.fixMojibake(null))
    }
}
