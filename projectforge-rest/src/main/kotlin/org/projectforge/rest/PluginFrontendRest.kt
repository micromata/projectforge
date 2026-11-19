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

import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.core.PluginFrontend
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Which activated plugins bring a frontend of their own (see [PluginFrontend]), so that the app can load it at
 * runtime without knowing the plugin. The answer is the same for every user: what a user may see or do inside the
 * pages is decided by the REST endpoints of the plugin, which check the access themselves.
 */
@RestController
@RequestMapping("${Rest.URL}/pluginFrontend")
class PluginFrontendRest {
    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    /** A frontend of a plugin as the app loads it. All urls are below [PluginFrontend.ASSETS_URL]. */
    class Manifest(
        val id: String,
        val name: String,
        val sdkVersion: String,
        val entry: String,
        val css: List<String>,
        /** Locale -> url of the json file with the texts. */
        val messages: Map<String, String>,
        /** The url of the pages of the plugin below the app, e.g. `/plugin/example`. */
        val route: String,
    )

    @AccessChecked("The same for every logged-in user; the REST endpoints of the plugins check their access themselves")
    @GetMapping
    fun getManifests(): List<Manifest> {
        return manifests(pluginAdminService.activePlugins)
    }

    companion object {
        internal fun manifests(plugins: List<AbstractPlugin>): List<Manifest> {
            return plugins.mapNotNull { plugin ->
                val frontend = plugin.frontend ?: return@mapNotNull null
                Manifest(
                    id = plugin.id,
                    name = plugin.info.name,
                    sdkVersion = frontend.sdkVersion,
                    entry = frontend.url(frontend.entry),
                    css = frontend.css.map { frontend.url(it) },
                    messages = frontend.messages.mapValues { frontend.url(it.value) },
                    route = "/${PluginFrontend.route(plugin.id)}",
                )
            }
        }
    }
}
