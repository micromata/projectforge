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

package org.projectforge.plugins.licensemanagement

import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.licensemanagement.rest.LicensePagesRest
import org.projectforge.registry.RegistryEntry
import org.projectforge.security.My2FAShortCut
import org.projectforge.web.WicketSupport
import org.projectforge.web.plugin.PluginWicketRegistrationService

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class LicenseManagementPlugin : AbstractPlugin(
    PluginAdminService.PLUGIN_LICENSE_MANAGEMENT_ID,
    "LicenseManagementPlugin",
    "For managing software licenses, keys and usage."
) {
    /**
     * @see org.projectforge.plugins.core.AbstractPlugin.initialize
     */
    override fun initialize() {
        val licenseDao = WicketSupport.get(LicenseDao::class.java)
        val pluginWicketRegistrationService = WicketSupport.get(
            PluginWicketRegistrationService::class.java
        )
        registerShortCutValues(My2FAShortCut.FINANCE_WRITE, "WRITE:license;/wa/licenseManagementEdit")
        registerShortCutValues(My2FAShortCut.FINANCE_WRITE, "/wa/licenseManagement")
        registerShortCutClasses(My2FAShortCut.FINANCE, LicensePagesRest::class.java)
        val entry = RegistryEntry(
            ID,
            LicenseDao::class.java, licenseDao,
            "plugins.licensemanagement"
        )
        // The LicenseDao is automatically available by the scripting engine!
        register(entry)

        // Register the web part:
        pluginWicketRegistrationService.registerWeb(
            ID,
            LicenseListPage::class.java,
            LicenseEditPage::class.java
        )

        val menuItemDef = MenuItemDef(
            ID, "plugins.licensemanagement.menu",
            checkAccess = {
                LicensePluginService.instance.hasAccess()
            })
        // Register the menu entry as sub menu entry of the misc menu:
        pluginWicketRegistrationService.registerMenuItem(
            MenuItemDefId.MISC, menuItemDef,
            LicenseListPage::class.java
        )

        // Define the access management:
        registerRight(LicenseManagementRight())

        // All the i18n stuff:
        addResourceBundle(RESOURCE_BUNDLE_NAME)
    }

    companion object {
        const val ID: String = "licenseManagement"

        const val RESOURCE_BUNDLE_NAME: String = "LicenseManagementI18nResources"
    }
}
