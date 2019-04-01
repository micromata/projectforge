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

package org.projectforge.menu.builder

import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.projectforge.business.user.UserXmlPreferencesDO
import org.projectforge.business.user.service.UserPreferencesHelper
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.UserException
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
    private lateinit var userPreferencesService: UserPreferencesService

    @Autowired
    private lateinit var menuCreator: MenuCreator

    @Autowired
    private lateinit var accessChecker: AccessChecker

    /**
     * Builds the standard favorite menu, if the use hasn't one yet.
     */
    fun getDefaultFavoriteMenu(): Menu {
        val favMenuAsUserPrefString = userPreferencesService.getEntry(USER_PREF_FAVORITES_MENU_ENTRIES_KEY) as String?
        return getDefaultFavoriteMenu(favMenuAsUserPrefString)
    }

    internal fun getDefaultFavoriteMenu(favMenuAsUserPrefString: String?): Menu {
        var menu = FavoritesMenuReaderWriter.read(menuCreator, favMenuAsUserPrefString)
        if (!menu.menuItems.isNullOrEmpty())
            return menu
        menu = Menu()
        if (accessChecker.isLoggedInUserMemberOfAdminGroup()) {
            val adminMenu = MenuItem(translate(MenuItemDefId.ADMINISTRATION.getI18nKey()))
            menu.add(adminMenu)
            adminMenu.add(menuCreator.findById(MenuItemDefId.ACCESS_LIST))
            adminMenu.add(menuCreator.findById(MenuItemDefId.USER_LIST))
            adminMenu.add(menuCreator.findById(MenuItemDefId.GROUP_LIST))
            adminMenu.add(menuCreator.findById(MenuItemDefId.SYSTEM))
        }
        if (accessChecker.isRestrictedUser()) {
            // Restricted users see only the change password menu entry (as favorite).
            val adminMenu = MenuItem(menuCreator.findById(MenuItemDefId.CHANGE_PASSWORD))
            menu.add(adminMenu)
        } else {
            val projectManagementMenu = MenuItem(translate(MenuItemDefId.PROJECT_MANAGEMENT.getI18nKey()))
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

    fun storeAsUserPref(menu: Menu?) {
        if (menu == null || menu.menuItems.isNullOrEmpty()) {
            UserPreferencesHelper.putEntry(USER_PREF_FAVORITES_MENU_ENTRIES_KEY, "", true)
            UserPreferencesHelper.removeEntry(USER_PREF_FAVORITES_MENU_KEY)
            return
        }
        val document = DocumentHelper.createDocument()
        val root = document.addElement("root")
        for (menuItem in menu.menuItems) {
            buildElement(root.addElement("item"), menuItem)
        }
        val xml = document.asXML()
        if (xml.length > UserXmlPreferencesDO.MAX_SERIALIZED_LENGTH) {
            throw UserException("menu.favorite.maxSizeExceeded")
        }
        UserPreferencesHelper.putEntry(USER_PREF_FAVORITES_MENU_ENTRIES_KEY, xml, true)
        UserPreferencesHelper.putEntry(USER_PREF_FAVORITES_MENU_KEY, this, false)
        log.info("Favorites menu stored: $xml")
    }

    private fun buildElement(element: Element, menuItem: MenuItem) {
        if (menuItem.id != null) {
            element.addAttribute("id", menuItem.id)
        }
        if (menuItem.title != null) {
            element.addText(menuItem.title)
        }
        if (!menuItem.subMenu.isNullOrEmpty()) {
            for (subItem in menuItem.subMenu!!) {
                buildElement(element.addElement("item"), subItem)
            }
        }
    }

    companion object {
        val USER_PREF_FAVORITES_MENU_KEY = "usersFavoritesMenu"

        internal val USER_PREF_FAVORITES_MENU_ENTRIES_KEY = "usersFavoriteMenuEntries"

        private val log = org.slf4j.LoggerFactory.getLogger(FavoritesMenuCreator::class.java)
    }
}
