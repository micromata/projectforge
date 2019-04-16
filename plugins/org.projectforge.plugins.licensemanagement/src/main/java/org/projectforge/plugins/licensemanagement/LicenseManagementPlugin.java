/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.plugins.licensemanagement;

import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.registry.RegistryEntry;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class LicenseManagementPlugin extends AbstractPlugin {
  public static final String ID = "licenseManagement";

  public static final String RESOURCE_BUNDLE_NAME = "LicenseManagementI18nResources";

  static UserPrefArea USER_PREF_AREA;

  // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
  // each test).
  // The entities are inserted in ascending order and deleted in descending order.
  private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[]{LicenseDO.class};

  @Autowired
  private LicenseDao licenseDao;

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize() {
    // DatabaseUpdateDao is needed by the updater:
    LicenseManagementPluginUpdates.dao = myDatabaseUpdater;
    final RegistryEntry entry = new RegistryEntry(ID, LicenseDao.class, licenseDao,
            "plugins.licensemanagement");
    // The LicenseDao is automatically available by the scripting engine!
    register(entry);

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(ID, LicenseListPage.class, LicenseEditPage.class);

    // Register the menu entry as sub menu entry of the misc menu:
    pluginWicketRegistrationService.registerMenuItem(MenuItemDefId.MISC, new MenuItemDef(ID, "plugins.licensemanagement.menu"),
            LicenseListPage.class);

    // Define the access management:
    registerRight(new LicenseManagementRight(accessChecker));

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getUpdateEntries()
   */
  @Override
  public List<UpdateEntry> getUpdateEntries() {
    return LicenseManagementPluginUpdates.getUpdateEntries();
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getInitializationUpdateEntry()
   */
  @Override
  public UpdateEntry getInitializationUpdateEntry() {
    return LicenseManagementPluginUpdates.getInitializationUpdateEntry();
  }
}
