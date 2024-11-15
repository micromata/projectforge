/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.marketing

import org.projectforge.business.address.AddressDao
import org.projectforge.menu.builder.MenuItemDef.Companion.create
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.web.WicketSupport
import org.projectforge.web.plugin.PluginWicketRegistrationService

/**
 * Your plugin initialization. Register all your components such as i18n files, data-access object etc.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MarketingPlugin : AbstractPlugin("marketing", "Marketing", "Marketing plugin for address campaigns.") {
    override fun initialize() {
        val addressCampaignDao = WicketSupport.get(AddressCampaignDao::class.java)
        val addressCampaignValueDao = WicketSupport.get(AddressCampaignValueDao::class.java)
        // Register it:
        register(
            ADDRESS_CAMPAIGN_ID,
            AddressCampaignDao::class.java,
            addressCampaignDao,
            "plugins.marketing.addressCampaign"
        )
        register(
            ADDRESS_CAMPAIGN_VALUE_ID,
            AddressCampaignValueDao::class.java,
            addressCampaignValueDao,
            "plugins.marketing.addressCampaignValue"
        )
            .setSearchable(false)

        val pluginWicketRegistrationService = WicketSupport.get(PluginWicketRegistrationService::class.java)
        // Register the web part:
        pluginWicketRegistrationService!!.registerWeb(
            ADDRESS_CAMPAIGN_ID,
            AddressCampaignListPage::class.java,
            AddressCampaignEditPage::class.java
        )
        pluginWicketRegistrationService.registerWeb(
            ADDRESS_CAMPAIGN_VALUE_ID,
            AddressCampaignValueListPage::class.java,
            AddressCampaignValueEditPage::class.java
        )

        // Register the menu entry as sub menu entry of the misc menu:
        pluginWicketRegistrationService.registerMenuItem(
            MenuItemDefId.MISC, create(ADDRESS_CAMPAIGN_ID, "plugins.marketing.addressCampaign.menu"),
            AddressCampaignListPage::class.java
        )
        pluginWicketRegistrationService.registerMenuItem(
            MenuItemDefId.MISC, create(ADDRESS_CAMPAIGN_VALUE_ID, "plugins.marketing.addressCampaignValue.menu"),
            AddressCampaignValueListPage::class.java
        )

        // Define the access management:
        registerRight(AddressCampaignRight())
        registerRight(AddressCampaignValueRight())

        // All the i18n stuff:
        addResourceBundle(RESOURCE_BUNDLE_NAME)

        WicketSupport.get(AddressDao::class.java).register(MarketingPluginAddressDeletionListener(addressCampaignDao))
    }

    companion object {
        const val ADDRESS_CAMPAIGN_ID: String = "addressCampaign"

        const val ADDRESS_CAMPAIGN_VALUE_ID: String = "addressCampaignValues"

        const val RESOURCE_BUNDLE_NAME: String = "MarketingI18nResources"

        // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
        // each test).
        // The entities are inserted in ascending order and deleted in descending order.
        private val PERSISTENT_ENTITIES = arrayOf<Class<*>>(
            AddressCampaignDO::class.java,
            AddressCampaignValueDO::class.java
        )
    }
}
