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

package org.projectforge.plugins.eed;

import org.apache.wicket.Page;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.eed.wicket.*;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.projectforge.framework.persistence.api.UserRightService.READONLY_READWRITE;

/**
 * @author Florian Blumenstein
 */
public class ExtendEmployeeDataPlugin extends AbstractPlugin {
  public static final String ID = "extendemployeedata";

  public static final String RESOURCE_BUNDLE_NAME = "ExtendEmployeeDataI18nResources";

  // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
  // each test).
  // The entities are inserted in ascending order and deleted in descending order.
  private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[]{};

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  @Autowired
  private EmployeeDao employeeDao;

  @Autowired
  private DatabaseService databaseService;

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize() {
    ExtendedEmployeeDataPluginUpdates.databaseService = databaseService;
    // Register it:
    register(ID, EmployeeDao.class, employeeDao, "plugins.extendemployeedata");

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(ID);

    // Register the menu entry as sub menu entry of the misc menu:
    register("eed_listcare", "plugins.eed.menu.listcare", EmployeeListEditPage.class, UserRightId.HR_EMPLOYEE, READONLY_READWRITE);
    register("eed_listcareimport", "plugins.eed.menu.listcareimport", EmployeeBillingImportPage.class, UserRightId.HR_EMPLOYEE, READONLY_READWRITE);
    register("eed_export", "plugins.eed.menu.export", ExportDataPage.class, UserRightId.HR_EMPLOYEE_SALARY, READONLY_READWRITE);
    register("eed_import", "plugins.eed.menu.import", EmployeeSalaryImportPage.class, UserRightId.HR_EMPLOYEE_SALARY, UserRightValue.READWRITE);
    register("eed_config", "plugins.eed.menu.config", EmployeeConfigurationPage.class, UserRightId.HR_EMPLOYEE_SALARY, READONLY_READWRITE);

    // Define the access management:
    registerRight(new ExtendEmployeeDataRight(accessChecker));

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);
  }

  private void register(String menuId, String i18nKey, Class<? extends Page> pageClass, UserRightId userRightId, UserRightValue... userRightValues) {
    MenuItemDef menuEntry = MenuItemDef.create(menuId, i18nKey);
    menuEntry.setRequiredUserRightId(userRightId);
    menuEntry.setRequiredUserRightValues(userRightValues);
    pluginWicketRegistrationService.registerMenuItem(MenuItemDefId.HR, menuEntry, pageClass);
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getInitializationUpdateEntry()
   */
  @Override
  public UpdateEntry getInitializationUpdateEntry() {
    return ExtendedEmployeeDataPluginUpdates.getInitializationUpdateEntry();
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getUpdateEntries()
   */
  @Override
  public List<UpdateEntry> getUpdateEntries() {
    return ExtendedEmployeeDataPluginUpdates.getUpdateEntries();
  }

}
