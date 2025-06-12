/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.datatransfer

import org.projectforge.Constants
import org.projectforge.business.admin.SystemStatistics
import org.projectforge.jcr.RepoBackupService
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.datatransfer.rest.DataTransferArea
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.plugins.datatransfer.rest.DataTransferPersonalBox
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.web.WicketSupport

/**
 * Your plugin initialization. Register all your components such as i18n files, data-access object etc.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class DataTransferPlugin :
    AbstractPlugin(
        ID,
        PluginAdminService.PLUGIN_DATA_TRANSFER_ID,
        "Data transfer tool for sharing files with other users or customers."
    ) {

    override fun initialize() {
        val dataTransferAreaPagesRest = WicketSupport.get(DataTransferAreaPagesRest::class.java)
        val dataTransferAreaDao = WicketSupport.get(DataTransferAreaDao::class.java)
        val menuCreator = WicketSupport.get(MenuCreator::class.java)
        val systemStatistics = WicketSupport.get(SystemStatistics::class.java)
        WicketSupport.get(RepoBackupService::class.java).registerNodePathToIgnore(dataTransferAreaPagesRest.jcrPath!!)

        // Register it:
        register(dataTransferAreaDao::class.java, dataTransferAreaDao, "plugins.datatransfer")

        menuCreator.register(
            MenuItemDefId.MISC,
            MenuItemDef(info.id, "plugins.datatransfer.menu", "${Constants.REACT_APP_PATH}datatransfer")
        )

        menuCreator.registerPluginMenu(
            "DataTransferPersonalBox",
            "plugins.datatransfer.personalBox",
            "${Constants.REACT_APP_PATH}datatransferfiles/dynamic/-1"
        )

        // All the i18n stuff:
        addResourceBundle(RESOURCE_BUNDLE_NAME)

        // Will only delivered to client but has to be ignored on sending back from client.
        JacksonConfiguration.registerAllowedUnknownProperties(
            DataTransferArea::class.java,
            "externalLink",
            "externalAccessEnabled",
            "lastUpdateTimeAgo",
            "maxUploadSizeFormatted"
        )
        JacksonConfiguration.registerAllowedUnknownProperties(
            DataTransferPersonalBox::class.java,
            "lastUpdateTimeAgo"
        )

        systemStatistics.registerStatisticsBuilder(
            DataTransferStatisticsBuilder()
        )
    }

    override fun createDefaultFavoriteMenu(menu: Menu, allMenuItems: List<MenuItem>) {
        if (allMenuItems.any { it.id == info.id }) {
            // DataTransfer menu already set in user's favorite menu.
            return
        }
        val menuCreator = WicketSupport.get(MenuCreator::class.java)
        menu.add(menuCreator.findById(PluginAdminService.PLUGIN_DATA_TRANSFER_ID))
    }

    companion object {
        const val ID = PluginAdminService.PLUGIN_DATA_TRANSFER_ID
        const val RESOURCE_BUNDLE_NAME = "DataTransferI18nResources"
    }
}
