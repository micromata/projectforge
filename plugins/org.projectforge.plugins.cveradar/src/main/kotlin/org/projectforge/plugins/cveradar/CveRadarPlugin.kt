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
import org.projectforge.web.WicketSupport

/**
 * CVE-Radar (vulnerability management, see doc/requirements/PF-CVR_CVE-Radar_Vulnerability_Management.md).
 *
 * For now only a UI prototype for the review of the specification: the pages under /next/cveradar work on test
 * data bundled with the frontend, so the plugin registers nothing but its menu entry and texts. It is not
 * activated initially; an admin switches it on in the plugin administration.
 */
class CveRadarPlugin : AbstractPlugin(ID, "CVE-Radar", "Vulnerability management (prototype with test data).") {

    override fun initialize() {
        val menuCreator = WicketSupport.get(MenuCreator::class.java)
        // A new page of projectforge-next without a legacy version, so no NextMigration entry:
        menuCreator.register(MenuItemDefId.MISC, MenuItemDef(info.id, "plugins.cveradar.menu", "next/cveradar"))
        addResourceBundle(RESOURCE_BUNDLE_NAME)
    }

    companion object {
        const val ID = PluginAdminService.PLUGIN_CVE_RADAR_ID
        const val RESOURCE_BUNDLE_NAME = "CveRadarI18nResources"
    }
}
