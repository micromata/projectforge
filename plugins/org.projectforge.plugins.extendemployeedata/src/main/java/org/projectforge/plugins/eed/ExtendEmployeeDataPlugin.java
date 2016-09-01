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

package org.projectforge.plugins.eed;

import static org.projectforge.framework.persistence.api.UserRightService.READONLY_READWRITE;

import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.eed.wicket.EmployeeBillingImportPage;
import org.projectforge.plugins.eed.wicket.EmployeeConfigurationPage;
import org.projectforge.plugins.eed.wicket.EmployeeListEditPage;
import org.projectforge.plugins.eed.wicket.ExportDataPage;
import org.projectforge.web.MenuItemDef;
import org.projectforge.web.MenuItemDefId;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florian Blumenstein
 */
public class ExtendEmployeeDataPlugin extends AbstractPlugin
{
  public static final String ID = "extendemployeedata";

  public static final String RESOURCE_BUNDLE_NAME = "ExtendEmployeeDataI18nResources";

  // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
  // each test).
  // The entities are inserted in ascending order and deleted in descending order.
  private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[] {};

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  @Autowired
  private EmployeeDao employeeDao;

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize()
  {

    ExtendedEmployeeDataPluginUpdates.dao = myDatabaseUpdater;
    // Register it:
    register(ID, EmployeeDao.class, employeeDao, "plugins.extendemployeedata");

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(ID);

    // Register the menu entry as sub menu entry of the misc menu:
    final MenuItemDef parentMenu = pluginWicketRegistrationService.getMenuItemDef(MenuItemDefId.HR);
    pluginWicketRegistrationService
        .registerMenuItem(new MenuItemDef(parentMenu, ID, 21, "plugins.eed.menu.listcare", EmployeeListEditPage.class,
            UserRightId.HR_EMPLOYEE, READONLY_READWRITE));
    pluginWicketRegistrationService
        .registerMenuItem(
            new MenuItemDef(parentMenu, ID, 22, "plugins.eed.menu.listcareimport", EmployeeBillingImportPage.class,
                UserRightId.HR_EMPLOYEE, READONLY_READWRITE));
    pluginWicketRegistrationService
        .registerMenuItem(new MenuItemDef(parentMenu, ID, 23, "plugins.eed.menu.export", ExportDataPage.class,
            UserRightId.HR_EMPLOYEE_SALARY, READONLY_READWRITE));
    pluginWicketRegistrationService
        .registerMenuItem(
            new MenuItemDef(parentMenu, ID, 24, "plugins.eed.menu.config", EmployeeConfigurationPage.class));

    // Define the access management:
    registerRight(new ExtendEmployeeDataRight(accessChecker));

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getInitializationUpdateEntry()
   */
  @Override
  public UpdateEntry getInitializationUpdateEntry()
  {
    return ExtendedEmployeeDataPluginUpdates.getInitializationUpdateEntry();
  }

}
