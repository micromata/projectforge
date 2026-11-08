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

package org.projectforge.plugins.cveradar

import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.cveradar.sync.DependencyTrackClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Dependency-Track, if the plugin is active and the connection configured: the state of the last probe
 * (`cveradar.deptrack.probe`, see [CveRadarJobs]), the host and the version of Dependency-Track (NFR-05).
 */
@Component
class DependencyTrackSubsystemStatusProvider : SubsystemStatusProvider {
    @Autowired
    private lateinit var client: DependencyTrackClient

    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    override val id = "dependencyTrack"

    override val titleKey = "system.admin.adminErrors.subsystem.dependencyTrack"

    override val problems = SubsystemProblemMatch(
        locationPrefixes = listOf("DependencyTrack"),
        syncTypes = listOf(DependencyTrackClient.SYNC_TYPE),
    )

    override fun status(): SubsystemStatus? {
        if (!config.deptrack.isConfigured() || !pluginAdminService.isActive(PluginAdminService.PLUGIN_CVE_RADAR_ID)) {
            return null
        }
        val host = SubsystemStatus.hostOf(config.deptrack.url)
        val detail = client.lastVersion?.let { "$host, Dependency-Track $it" } ?: host
        return SubsystemStatus.ofSyncs(problems.syncTypes, detail)
    }
}
