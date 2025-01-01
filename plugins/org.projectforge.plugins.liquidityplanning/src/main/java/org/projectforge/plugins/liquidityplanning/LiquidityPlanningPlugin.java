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

package org.projectforge.plugins.liquidityplanning;

import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.plugins.liquidityplanning.rest.LiquidityEntryPagesRest;
import org.projectforge.registry.RegistryEntry;
import org.projectforge.security.My2FAShortCut;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.plugin.PluginWicketRegistrationService;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LiquidityPlanningPlugin extends AbstractPlugin {
    public static final String ACCOUNTING_RECORD = "accountingRecord";

    public static final String ID = PluginAdminService.PLUGIN_LIQUIDITY_PLANNING_ID;

    public static final String RESOURCE_BUNDLE_NAME = "LiquidityPlanningI18nResources";

    static UserPrefArea USER_PREF_AREA;

    // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
    // each test).
    // The entities are inserted in ascending order and deleted in descending order.
    private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[]{LiquidityEntryDO.class};

    public LiquidityPlanningPlugin() {
        super("liquidplanning", "Liquidity planning", "Liquidity planning based on expected payments and invoices with probabilities.");
    }

    /**
     * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
     */
    @Override
    protected void initialize() {
        LiquidityEntryDao liquidityEntryDao = WicketSupport.get(LiquidityEntryDao.class);
        PluginWicketRegistrationService pluginWicketRegistrationService = WicketSupport.get(PluginWicketRegistrationService.class);
        registerShortCutValues(My2FAShortCut.FINANCE_WRITE, "WRITE:liquidityEntry;/wa/liquidityplanningEdit");
        registerShortCutValues(My2FAShortCut.FINANCE, "/wa/liquidityplanning;/wa/liquidityForecast");
        registerShortCutClasses(My2FAShortCut.FINANCE, LiquidityEntryPagesRest.class);
        final RegistryEntry entry = new RegistryEntry(ID, LiquidityEntryDao.class, liquidityEntryDao,
                "plugins.liquidityplanning");
        register(entry);

        // Register the web part:
        // Insert at first position before accounting-record entry (for SearchPage).
        pluginWicketRegistrationService.registerWeb(ID, LiquidityEntryListPage.class, LiquidityEntryEditPage.class,
                ACCOUNTING_RECORD, true);

        pluginWicketRegistrationService.addMountPage("liquidityForecast", LiquidityForecastPage.class);

        // Register the menu entry as sub menu entry of the reporting menu:
        MenuItemDef menuEntry = MenuItemDef.create(ID, "plugins.liquidityplanning.menu");
        menuEntry.setRequiredUserRightId(LiquidityplanningPluginUserRightId.PLUGIN_LIQUIDITY_PLANNING);
        menuEntry.setRequiredUserRightValues(UserRightService.READONLY_READWRITE);
        pluginWicketRegistrationService.registerMenuItem(MenuItemDefId.REPORTING, menuEntry, LiquidityEntryListPage.class);

        // Define the access management:
        registerRight(new LiquidityPlanningRight());

        // All the i18n stuff:
        addResourceBundle(RESOURCE_BUNDLE_NAME);
    }
}
