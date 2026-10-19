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

package org.projectforge.business.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GroupNomenclatureConfigTest {
    @Test
    fun `parses the configured convention`() {
        val config = GroupNomenclatureConfig.parse(
            """{"description":"<p>Kunde-Projekt</p>","pattern":"{customer.identifier}-{project.identifier}",""" +
                    """"managerSuffix":"pm","externalSuffix":"extern","letterCase":"LOWER","unknown":1}"""
        )
        assertEquals("<p>Kunde-Projekt</p>", config.description)
        assertEquals("{customer.identifier}-{project.identifier}", config.pattern)
        assertEquals("pm", config.managerSuffix)
        assertNull(config.teamSuffix)
        assertEquals("extern", config.externalSuffix)
        assertEquals(GroupNomenclatureConfig.LETTER_CASE_LOWER, config.letterCase)
    }

    @Test
    fun `blank or broken values give the defaults`() {
        listOf(null, "", "  ", "{", "[1]").forEach { json ->
            val config = GroupNomenclatureConfig.parse(json)
            assertNull(config.pattern, "json=$json")
            assertNull(config.managerSuffix, "json=$json")
        }
    }
}
