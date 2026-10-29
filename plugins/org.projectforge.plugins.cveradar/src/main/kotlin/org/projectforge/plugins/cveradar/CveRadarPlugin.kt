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

import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.framework.SpringBeans

/**
 * CVE-Radar (vulnerability management, see dev-docs/requirements/PF-CVR_CVE-Radar_Vulnerability_Management.md).
 *
 * Real data: the vulnerabilities imported from the NVD (start page /next/cveradar), cases (vulnerabilities and
 * incidents) with fix criteria and closure overview, team checks, devices from the vulnerability scanner and the log
 * server ([CveRadarJobs]) and the employees' confirmations. The UI prototype on test data bundled with the frontend
 * is at /next/cveradar/demo. The plugin is not activated initially; an admin switches it on in the plugin
 * administration.
 */
class CveRadarPlugin : AbstractPlugin(ID, "CVE-Radar", "Vulnerability management.") {

    override fun initialize() {
        val caseDao = SpringBeans.get(CvrCaseDao::class.java)
        val teamCheckDao = SpringBeans.get(CvrTeamCheckDao::class.java)
        register(CASE_CATEGORY, CvrCaseDao::class.java, caseDao, "plugins.cveradar.case")
        register("cvrteamcheck", CvrTeamCheckDao::class.java, teamCheckDao, "plugins.cveradar.teamCheck")
        register(
            VULNERABILITY_CATEGORY, CvrVulnerabilityDao::class.java, SpringBeans.get(CvrVulnerabilityDao::class.java),
            "plugins.cveradar.vulnerability",
        )
        val menuCreator = SpringBeans.get(MenuCreator::class.java)
        // A new page of projectforge-next without a legacy version, so no PageRoutes entry:
        menuCreator.register(MenuItemDefId.MISC, MenuItemDef(info.id, "plugins.cveradar.menu", "next/cveradar"))
        addResourceBundle(RESOURCE_BUNDLE_NAME)
    }

    /** The start page and the prototype's pages have no REST category to derive them from. */
    override val featureNextRoutes: List<String>
        get() = listOf("cveradar")

    companion object {
        const val ID = PluginAdminService.PLUGIN_CVE_RADAR_ID
        const val RESOURCE_BUNDLE_NAME = "CveRadarI18nResources"

        /** The REST category of the cases (`/rs/cvrcase`) and the route of their next pages. */
        const val CASE_CATEGORY = "cvrcase"

        /** The vulnerabilities imported from the NVD (`/rs/cvrvulnerability`), listed on the start page. */
        const val VULNERABILITY_CATEGORY = "cvrvulnerability"
    }
}
