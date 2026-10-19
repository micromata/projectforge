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


package org.projectforge.menu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MenuVisibilityTest {
    @Test
    fun `splits group lists by comma and semicolon`() {
        Assertions.assertEquals(listOf("PF_Finance", "PF_HR", "42"), MenuVisibility.splitEntries(" PF_Finance, PF_HR;42 "))
        Assertions.assertEquals(listOf("ALL"), MenuVisibility.splitEntries("ALL"))
        Assertions.assertEquals(emptyList<String>(), MenuVisibility.splitEntries(" ,; "))
        Assertions.assertEquals(emptyList<String>(), MenuVisibility.splitEntries(null))
    }
}
