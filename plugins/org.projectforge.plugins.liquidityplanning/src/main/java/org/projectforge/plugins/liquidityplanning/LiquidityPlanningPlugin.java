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

package org.projectforge.plugins.liquidityplanning;

import org.projectforge.NextMigration;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.menu.builder.MenuCreator;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.plugins.liquidityplanning.rest.LiquidityEntityRest;
import org.projectforge.plugins.liquidityplanning.rest.LiquiditySeriesRest;
import org.projectforge.registry.RegistryEntry;
import org.projectforge.security.My2FAShortCut;
import org.projectforge.web.WicketSupport;

/**
 * @author Kai Reinhard
 */
public class LiquidityPlanningPlugin extends AbstractPlugin {
    public static final String ID = PluginAdminService.PLUGIN_LIQUIDITY_PLANNING_ID;

    public static final String RESOURCE_BUNDLE_NAME = "LiquidityPlanningI18nResources";

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
        // WRITE:liquidity is the category of LiquidityEntityRest (/rs/liquidity).
        registerShortCutValues(My2FAShortCut.FINANCE_WRITE, "WRITE:liquidity");
        registerShortCutClasses(My2FAShortCut.FINANCE, LiquidityEntityRest.class);
        // The series split (LiquiditySeriesRest, /rs/liquiditySeries/split) writes entries without passing the
        // save of /rs/liquidity, so WRITE:liquidity doesn't catch it. FINANCE covers it by its url prefix
        // (^/rs/liquidity.*), an installation configuring FINANCE_WRITE alone wouldn't gate it.
        registerShortCutClasses(My2FAShortCut.FINANCE_WRITE, LiquiditySeriesRest.class);
        final RegistryEntry entry = new RegistryEntry(ID, LiquidityEntryDao.class, liquidityEntryDao,
                "plugins.liquidityplanning");
        register(entry);

        // Register the menu entry as sub menu entry of the reporting menu. The pages are migrated to
        // projectforge-next (see NextMigration.MIGRATED), their Wicket pages are removed.
        MenuItemDef menuEntry = MenuItemDef.create(ID, "plugins.liquidityplanning.menu");
        menuEntry.setRequiredUserRightId(LiquidityplanningPluginUserRightId.PLUGIN_LIQUIDITY_PLANNING);
        menuEntry.setRequiredUserRightValues(UserRightService.READONLY_READWRITE);
        menuEntry.setUrl(NextMigration.INSTANCE.listUrl("liquidity"));
        WicketSupport.get(MenuCreator.class).register(MenuItemDefId.REPORTING, menuEntry);

        // Define the access management:
        registerRight(new LiquidityPlanningRight());

        // All the i18n stuff:
        addResourceBundle(RESOURCE_BUNDLE_NAME);
    }
}
