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

@file:Suppress("DEPRECATION")

package org.projectforge.menu.builder

import org.projectforge.business.user.service.UserXmlPreferencesService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * The customizable menu of the user (stored in the data-base and customizable).
 */
// open only needed for Wicket (for using proxies)
@Component
open class FavoritesMenuCreator {
    @Autowired
    private lateinit var userXmlPreferencesService: UserXmlPreferencesService

    @Autowired
    private lateinit var menuCreator: MenuCreator

    @Autowired
    private lateinit var accessChecker: AccessChecker

    /**
     * Builds the standard favorite menu, if the use hasn't one yet.
     */
    fun getFavoriteMenu(): Menu {
        val favMenuAsUserPrefString = userXmlPreferencesService.getEntry(USER_PREF_FAVORITES_MENU_ENTRIES_KEY) as String?
        val menu = getFavoriteMenu(favMenuAsUserPrefString)
        menu.postProcess() // Build badges of top menus.
        return menu
    }

    internal fun getFavoriteMenu(favMenuAsUserPrefString: String?): Menu {
        var menu = FavoritesMenuReaderWriter.read(menuCreator, favMenuAsUserPrefString)
        if (!menu.menuItems.isNullOrEmpty())
            return menu
        menu = Menu()
        if (accessChecker.isLoggedInUserMemberOfAdminGroup) {
            val adminMenu = MenuItem(MenuItemDefId.ADMINISTRATION.id, translate(MenuItemDefId.ADMINISTRATION.getI18nKey()))
            menu.add(adminMenu)
            adminMenu.add(menuCreator.findById(MenuItemDefId.ACCESS_LIST))
            adminMenu.add(menuCreator.findById(MenuItemDefId.USER_LIST))
            adminMenu.add(menuCreator.findById(MenuItemDefId.GROUP_LIST))
            adminMenu.add(menuCreator.findById(MenuItemDefId.SYSTEM))
        }
        if (accessChecker.isRestrictedUser) {
            // Restricted users see only the change password menu entry (as favorite).
            val adminMenu = MenuItem(menuCreator.findById(MenuItemDefId.CHANGE_PASSWORD))
            menu.add(adminMenu)
        } else {
            val projectManagementMenu = MenuItem(MenuItemDefId.PROJECT_MANAGEMENT.id, translate(MenuItemDefId.PROJECT_MANAGEMENT.getI18nKey()))
            menu.add(projectManagementMenu)
            projectManagementMenu.add(menuCreator.findById(MenuItemDefId.MONTHLY_EMPLOYEE_REPORT))
            projectManagementMenu.add(menuCreator.findById(MenuItemDefId.TIMESHEET_LIST))

            menu.add(menuCreator.findById(MenuItemDefId.TASK_TREE))
            menu.add(menuCreator.findById(MenuItemDefId.CALENDAR))
            menu.add(menuCreator.findById(MenuItemDefId.ADDRESS_LIST))
        }
        return menu
    }

    fun read(favMenuAsString: String?): Menu {
        if (favMenuAsString.isNullOrBlank())
            return Menu()
        return FavoritesMenuReaderWriter.read(menuCreator, favMenuAsString)
    }

    companion object {
        const val USER_PREF_FAVORITES_MENU_KEY = "usersFavoritesMenu"

        internal const val USER_PREF_FAVORITES_MENU_ENTRIES_KEY = "usersFavoriteMenuEntries"
    }
}
