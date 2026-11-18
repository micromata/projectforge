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

package org.projectforge.plugins.core

/**
 * The frontend of a plugin: a bundle of native ES modules built independently of projectforge-next, which the app
 * loads at runtime (no rebuild of the app). A plugin declares it by [AbstractPlugin.frontend]; the app learns it from
 * `GET /rs/pluginFrontend` and renders the pages of the plugin below `/next/plugin/<plugin id>/`.
 *
 * The files are served by Spring from the plugin jar, `static/next/plugins-assets/<[assetDir]>/` (same origin,
 * so the content security policy needs no change). A bundle is built against one version of the plugin sdk of the
 * app ([sdkVersion]); the app refuses a bundle of a version it doesn't know.
 *
 * @param sdkVersion The version of the plugin sdk (the bridge of the app) the bundle was built against.
 * @param assetDir The directory of the files below `plugins-assets`, e.g. `cveradar/1.2.3`. Put the version in it:
 *   the urls are cached immutably.
 * @param entry The module with the `activate` function, relative to [assetDir].
 * @param css Style sheets, relative to [assetDir], loaded before the first page of the plugin is rendered.
 * @param messages The texts of the plugin per locale (`de`, `en`): a json file below [assetDir] of the shape
 *   `{ "plugins.example.title": "Title" }`, merged into the texts of the app.
 */
class PluginFrontend(
    val sdkVersion: String,
    val assetDir: String,
    val entry: String = "plugin.js",
    val css: List<String> = emptyList(),
    val messages: Map<String, String> = emptyMap(),
) {
    init {
        require(ASSET_PART.matches(assetDir) && noDots(assetDir)) {
            "Invalid asset directory of a plugin frontend: '$assetDir'."
        }
        (listOf(entry) + css + messages.values).forEach {
            require(FILE_PART.matches(it) && noDots(it)) { "Invalid file of a plugin frontend: '$it'." }
        }
    }

    /** The url below the app, e.g. `/next/plugins-assets/cveradar/1.2.3/plugin.js`. */
    fun url(file: String): String = "$ASSETS_URL$assetDir/$file"

    companion object {
        const val ASSETS_URL = "/next/plugins-assets/"

        /** The url of the pages of a plugin below the app, without leading slash. */
        fun route(pluginId: String) = "plugin/$pluginId"

        /** No `.` or `..` segment: a file never leaves the directory of the plugin. */
        private fun noDots(path: String) = path.split('/').none { it == "." || it == ".." }

        private val ASSET_PART = Regex("[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*")
        private val FILE_PART = Regex("[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*\\.[A-Za-z0-9]+")
    }
}
