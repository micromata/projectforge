/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.crm;

import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.plugins.core.AbstractPlugin;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class CrmPlugin extends AbstractPlugin
{
  public static final String ID = "crm";

  public static final String RESOURCE_BUNDLE_NAME = "CrmI18nResources";

  static UserPrefArea USER_PREF_AREA;

  // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
  // each test).
  // The entities are inserted in ascending order and deleted in descending order.
  private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[] { PersonalContactDO.class, ContactEntryDO.class,
      ContactDO.class };

  /**
   * This dao should be defined in pluginContext.xml (as resources) for proper initialization.
   */
  @Autowired
  private ContactDao contactDao;

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize()
  {
    // DatabaseUpdateDao is needed by the updater:
    // final RegistryEntry entry = new RegistryEntry(ID, LiquidityEntryDao.class, liquidityEntryDao, "plugins.liquidityplanning");
    // register(entry);
    //
    // // Register the web part:
    // // Insert at first position before accounting-record entry (for SearchPage).
    // registerWeb(ID, LiquidityEntryListPage.class, LiquidityEntryEditPage.class, DaoRegistry.ACCOUNTING_RECORD, true);
    //
    // addMountPage("liquidityForecast", LiquidityForecastPage.class);
    //
    // // Register the menu entry as sub menu entry of the reporting menu:
    // final MenuItemDef parentMenu = getMenuItemDef(MenuItemDefId.REPORTING);
    // registerMenuItem(new MenuItemDef(parentMenu, ID, 10, "plugins.liquidityplanning.menu", LiquidityEntryListPage.class,
    // LiquidityEntryDao.USER_RIGHT_ID, UserRightValue.READONLY, UserRightValue.READWRITE));
    //
    // // Define the access management:
    // registerRight(new LiquidityPlanningRight());
    //
    // // All the i18n stuff:
    // addResourceBundle(RESOURCE_BUNDLE_NAME);
  }

  /**
   * @param contactDao the contactDao to set
   * @return this for chaining.
   */
  public void setContactDao(final ContactDao contactDao)
  {
    this.contactDao = contactDao;
  }
}
