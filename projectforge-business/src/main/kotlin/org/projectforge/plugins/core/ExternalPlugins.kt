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
 * Plugins whose backend is developed outside of this repository and joins the release jar as a binary (gradle
 * property `pf.extraPlugins` of projectforge-application), while their texts and the generated field metadata are here.
 *
 * The pages of such a plugin are its own frontend (loaded by the app at runtime, see [PluginFrontend]); a build without
 * the plugin has none. Here is left what the generator of the field metadata needs: the generated field metadata of its
 * entities are kept in a build without the plugin (GenerateNextFieldMetadataMain).
 */
object ExternalPlugins {
    /**
     * @param id The plugin id, see [AbstractPlugin.id].
     * @param metadataFilePrefix The prefix of the generated field metadata files of its entities in
     *   `projectforge-next/lib/metadata`.
     * @param entityPackage The package of its entities: the plugin is on the classpath, if any entity of it is.
     */
    class ExternalPlugin(
        val id: String,
        val metadataFilePrefix: String,
        val entityPackage: String,
    )

    val CVE_RADAR = ExternalPlugin(
        id = PluginAdminService.PLUGIN_CVE_RADAR_ID,
        metadataFilePrefix = "cvr-",
        entityPackage = "org.projectforge.plugins.cveradar",
    )

    val ALL = listOf(CVE_RADAR)
}
