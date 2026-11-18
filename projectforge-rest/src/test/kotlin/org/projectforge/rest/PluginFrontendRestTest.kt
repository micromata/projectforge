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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginFrontend

class PluginFrontendRestTest {
    private class Plugin(id: String, override val frontend: PluginFrontend?) : AbstractPlugin(id, "Plugin $id", "Test.") {
        override fun initialize() {}
    }

    @Test
    fun `lists only plugins with a frontend, with urls below the assets`() {
        val frontend = PluginFrontend(
            sdkVersion = "1",
            assetDir = "example/1.0.0",
            css = listOf("plugin.css"),
            messages = mapOf("de" to "messages/de.json", "en" to "messages/en.json"),
        )
        val manifests = PluginFrontendRest.manifests(listOf(Plugin("plain", null), Plugin("example", frontend)))
        Assertions.assertEquals(listOf("example"), manifests.map { it.id })
        val manifest = manifests.single()
        Assertions.assertEquals("/next/plugins-assets/example/1.0.0/plugin.js", manifest.entry)
        Assertions.assertEquals(listOf("/next/plugins-assets/example/1.0.0/plugin.css"), manifest.css)
        Assertions.assertEquals("/next/plugins-assets/example/1.0.0/messages/de.json", manifest.messages["de"])
        Assertions.assertEquals("/plugin/example", manifest.route)
        Assertions.assertEquals("1", manifest.sdkVersion)
    }

    @Test
    fun `no plugin without a frontend, no manifest`() {
        Assertions.assertTrue(PluginFrontendRest.manifests(emptyList()).isEmpty())
        Assertions.assertTrue(PluginFrontendRest.manifests(listOf(Plugin("plain", null))).isEmpty())
    }

    @Test
    fun `a frontend cannot leave the assets`() {
        for (dir in listOf("../etc", "a/../b", "/abs", "a//b", "a b", "")) {
            Assertions.assertThrows(IllegalArgumentException::class.java, { PluginFrontend("1", dir) }, "dir '$dir'")
        }
        for (file in listOf("../plugin.js", "/plugin.js", "plugin", "a/../plugin.js", "http://x/plugin.js")) {
            Assertions.assertThrows(IllegalArgumentException::class.java, { PluginFrontend("1", "ok", entry = file) }, "file '$file'")
        }
    }
}
