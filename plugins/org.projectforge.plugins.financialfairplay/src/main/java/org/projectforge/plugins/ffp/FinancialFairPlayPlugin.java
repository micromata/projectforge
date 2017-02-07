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

package org.projectforge.plugins.ffp;

import java.util.List;

import org.apache.wicket.model.Model;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.ffp.repository.FFPEventDao;
import org.projectforge.plugins.ffp.repository.FFPEventService;
import org.projectforge.plugins.ffp.wicket.FFPDebtListPage;
import org.projectforge.plugins.ffp.wicket.FFPEventListPage;
import org.projectforge.web.MenuBuilderContext;
import org.projectforge.web.MenuEntry;
import org.projectforge.web.MenuItemDef;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florian Blumenstein
 */
public class FinancialFairPlayPlugin extends AbstractPlugin
{
  public static final String ID = "financialfairplay";

  public static final String RESOURCE_BUNDLE_NAME = "FinancialFairPlayI18nResources";

  // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
  // each test).
  // The entities are inserted in ascending order and deleted in descending order.
  private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[] {};

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  @Autowired
  private FFPEventService eventService;

  @Autowired
  private InitDatabaseDao initDatabaseDao;

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize()
  {

    FinancialFairPlayPluginUpdates.databaseUpdateService = myDatabaseUpdater;
    FinancialFairPlayPluginUpdates.initDatabaseDao = initDatabaseDao;

    // Register it:
    register(ID, FFPEventDao.class, eventService.getEventDao(), "plugins.financialfairplay");

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(ID);

    final MenuItemDef parentMenu = new MenuItemDef(null, "FINANCIALFAIRPLAY", 120, "plugins.ffp.menu.financialfairplay");
    pluginWicketRegistrationService.registerMenuItem(parentMenu);

    pluginWicketRegistrationService
        .registerMenuItem(
            new MenuItemDef(parentMenu, ID, 121, "plugins.ffp.submenu.financialfairplay.eventlist", FFPEventListPage.class));
    final MenuItemDef debtViewPage = new MenuItemDef(parentMenu, ID, 122, "plugins.ffp.submenu.financialfairplay.dept", FFPDebtListPage.class)
    {
      @Override
      protected void afterMenuEntryCreation(final MenuEntry createdMenuEntry, final MenuBuilderContext context)
      {
        createdMenuEntry.setNewCounterModel(new Model<Integer>()
        {
          @Override
          public Integer getObject()
          {
            return eventService.getOpenDebts(ThreadLocalUserContext.getUser());
          }
        });
      }
    };
    pluginWicketRegistrationService.registerMenuItem(debtViewPage);

    // Define the access management:
    registerRight(new FinancialFairPlayDebtRight(accessChecker));

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getInitializationUpdateEntry()
   */
  @Override
  public UpdateEntry getInitializationUpdateEntry()
  {
    return FinancialFairPlayPluginUpdates.getInitializationUpdateEntry();
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getUpdateEntries()
   */
  @Override
  public List<UpdateEntry> getUpdateEntries()
  {
    return FinancialFairPlayPluginUpdates.getUpdateEntries();
  }

}
