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

package org.projectforge.plugins.todo;

import org.projectforge.business.user.UserPrefAreaRegistry;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.registry.RegistryEntry;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.plugin.PluginWicketRegistrationService;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ToDoPlugin extends AbstractPlugin {
    public static final String ID = "toDo";

    public static final String RESOURCE_BUNDLE_NAME = "ToDoI18nResources";

    static UserPrefArea USER_PREF_AREA;

    // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
    // each test).
    // The entities are inserted in ascending order and deleted in descending order.
    private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[]{ToDoDO.class};

    public ToDoPlugin() {
        super(PluginAdminService.PLUGIN_TODO_ID, "To-do", "To-do's may shared by users, groups etc. with notification per e-mail on changes.");
    }

    /**
     * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
     */
    @Override
    protected void initialize() {
        ToDoDao toDoDao = WicketSupport.get(ToDoDao.class);
        PluginWicketRegistrationService pluginWicketRegistrationService = WicketSupport.get(PluginWicketRegistrationService.class);

        // DatabaseUpdateDao is needed by the updater:
        final RegistryEntry entry = new RegistryEntry(ID, ToDoDao.class, toDoDao, "plugins.todo");
        // The ToDoDao is automatically available by the scripting engine!
        register(entry);

        // Register the web part:
        pluginWicketRegistrationService.registerWeb(ID, ToDoListPage.class, ToDoEditPage.class); // Address entry no longer exists (Wicket pages removed).

        // Register the menu entry as sub menu entry of the misc menu:
        MenuItemDef todomenu = MenuItemDef.create(ID, "plugins.todo.menu");
        todomenu.setBadgeCounter(() -> toDoDao.getOpenToDoEntries(null));
        pluginWicketRegistrationService.registerMenuItem(MenuItemDefId.MISC, todomenu, ToDoListPage.class);

        // Define the access management:
        registerRight(new ToDoRight());

        // All the i18n stuff:
        addResourceBundle(RESOURCE_BUNDLE_NAME);

        USER_PREF_AREA = new UserPrefArea("TODO_FAVORITE", ToDoDO.class, "todo.favorite");
        UserPrefAreaRegistry.instance().register(USER_PREF_AREA);
    }
}
