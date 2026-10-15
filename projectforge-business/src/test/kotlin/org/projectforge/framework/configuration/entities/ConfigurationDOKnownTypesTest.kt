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

package org.projectforge.framework.configuration.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.framework.configuration.ConfigurationType

class ConfigurationDOKnownTypesTest {
    @Test
    fun `restriction lists exactly the known configuration types`() {
        val listed = Regex("'([A-Z_]+)'").findAll(ConfigurationDO.KNOWN_TYPES_RESTRICTION).map { it.groupValues[1] }.toSet()
        assertEquals(
            ConfigurationType.entries.map { it.name }.toSet(),
            listed,
            "ConfigurationDO.KNOWN_TYPES_RESTRICTION must list every ConfigurationType, otherwise its rows are hidden.",
        )
    }
}
