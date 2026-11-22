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

package org.projectforge.framework.configuration.properties

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PropertiesActiveDiffersTest {
    private fun differs(inFile: Boolean, active: String?, expected: String?) =
        ProjectForgePropertiesService.activeDiffers(inFile, active, expected)

    @Test
    fun `a default only known to the code of a plugin doesn't differ`() {
        assertFalse(differs(inFile = false, active = null, expected = "30"))
    }

    @Test
    fun `a default read by spring is compared`() {
        assertFalse(differs(inFile = false, active = "30", expected = "30"))
        assertTrue(differs(inFile = false, active = "20", expected = "30"))
    }

    @Test
    fun `a value of the file not yet active differs`() {
        assertTrue(differs(inFile = true, active = null, expected = "30"))
        assertTrue(differs(inFile = true, active = "20", expected = "30"))
        assertFalse(differs(inFile = true, active = "30", expected = "30"))
    }
}
