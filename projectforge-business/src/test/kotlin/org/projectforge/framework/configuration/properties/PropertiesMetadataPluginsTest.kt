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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** A plugin documents its settings in a file of its own, in the format of application.properties. */
class PropertiesMetadataPluginsTest {
    private val core = """
        ### General

        # Core setting.
        projectforge.domain=http://localhost:8080
    """.trimIndent()

    private val plugin = """
        #
        # The settings of the plugin Example.
        #

        ### Example

        # Prefix of the ids.
        projectforge.plugin.example.prefix=INC

        #projectforge.plugin.example.url=https://example.org
    """.trimIndent()

    @Test
    fun `the groups of a plugin follow the groups of the core`() {
        val metadata = PropertiesMetadata.parse(PropertiesMetadata.combine(core, listOf(plugin)))
        assertEquals(listOf("general", "example"), metadata.groups.map { it.id })
        assertNotNull(metadata.getParam("projectforge.plugin.example.prefix"))
        assertEquals("INC", metadata.getParam("projectforge.plugin.example.prefix")?.defaultValue)
        assertTrue(metadata.contains("projectforge.plugin.example.url"))
        assertTrue(metadata.contains("projectforge.domain"))
    }

    @Test
    fun `without plugins nothing changes`() {
        val metadata = PropertiesMetadata.parse(PropertiesMetadata.combine(core, emptyList()))
        assertEquals(listOf("general"), metadata.groups.map { it.id })
    }
}
