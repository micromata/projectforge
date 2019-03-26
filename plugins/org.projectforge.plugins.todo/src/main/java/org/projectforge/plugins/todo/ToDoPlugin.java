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

package org.projectforge.plugins.todo;

import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.registry.RegistryEntry;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class ToDoPlugin extends AbstractPlugin
{
  public static final String ID = "toDo";

  public static final String ADDRESS = "address";

  public static final String RESOURCE_BUNDLE_NAME = "ToDoI18nResources";

  static UserPrefArea USER_PREF_AREA;

  // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
  // each test).
  // The entities are inserted in ascending order and deleted in descending order.
  private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[] { ToDoDO.class };

  @Autowired
  private ToDoDao toDoDao;

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize()
  {
    // DatabaseUpdateDao is needed by the updater:
    ToDoPluginUpdates.dao = myDatabaseUpdater;
    toDoDao = (ToDoDao) applicationContext.getBean("toDoDao");
    final RegistryEntry entry = new RegistryEntry(ID, ToDoDao.class, toDoDao, "plugins.todo");
    // The ToDoDao is automatically available by the scripting engine!
    register(entry); // Insert at second position after Address entry (for SearchPage).

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(ID, ToDoListPage.class, ToDoEditPage.class, ADDRESS, false); // Insert at second position after Address entry (for SearchPage).

    // Register the menu entry as sub menu entry of the misc menu:
    final MenuItemDef parentMenu = pluginWicketRegistrationService.getMenuItemDef(MenuItemDefId.MISC);
    MenuItemDef todomenu = new MenuItemDef( ID, "plugins.todo.menu");
    todomenu.setWicketPageClass(ToDoListPage.class);
    todomenu.setBadgeCounter(() -> new MenuCounterOpenToDos().getObject());
    pluginWicketRegistrationService.registerMenuItem(MenuItemDefId.MISC, todomenu);

    // Define the access management:
    registerRight(new ToDoRight(accessChecker));

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);

    // Register favorite entries (the user can modify these templates/favorites via 'own settings'):
    USER_PREF_AREA = registerUserPrefArea("TODO_FAVORITE", ToDoDO.class, "todo.favorite");
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getInitializationUpdateEntry()
   */
  @Override
  public UpdateEntry getInitializationUpdateEntry()
  {
    return ToDoPluginUpdates.getInitializationUpdateEntry();
  }
}
