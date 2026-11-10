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
 * property `pf.extraPlugins` of projectforge-application), while their next pages and texts are here.
 *
 * A build without such a plugin (e.g. a community build from the sources) still has its pages: they are switched
 * off then like the pages of an inactive plugin ([org.projectforge.rest.feature.FeaturePathIndex]), and the
 * generated field metadata of its entities are kept (GenerateNextFieldMetadataMain).
 */
object ExternalPlugins {
    /**
     * @param id The plugin id, see [AbstractPlugin.id].
     * @param nextRoutes The routes of its next pages (without `next/`, e.g. `cveradar`).
     * @param metadataFilePrefix The prefix of the generated field metadata files of its entities in
     *   `projectforge-next/lib/metadata`.
     * @param entityPackage The package of its entities: the plugin is on the classpath, if any entity of it is.
     */
    class ExternalPlugin(
        val id: String,
        val nextRoutes: List<String>,
        val metadataFilePrefix: String,
        val entityPackage: String,
    )

    val CVE_RADAR = ExternalPlugin(
        id = PluginAdminService.PLUGIN_CVE_RADAR_ID,
        nextRoutes = listOf("cveradar", "cvrcase", "cvrvulnerability"),
        metadataFilePrefix = "cvr-",
        entityPackage = "org.projectforge.plugins.cveradar",
    )

    val ALL = listOf(CVE_RADAR)

    /**
     * The external plugins not contained in this build.
     * @param availableIds The ids of the plugins found, see [PluginAdminService.availablePlugins].
     */
    fun missing(availableIds: Collection<String>): List<ExternalPlugin> {
        return ALL.filter { it.id !in availableIds }
    }
}
