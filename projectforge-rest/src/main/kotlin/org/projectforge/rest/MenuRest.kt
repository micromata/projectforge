/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import org.projectforge.SystemStatus
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.menu.builder.*
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("${Rest.URL}/menu")
class MenuRest {
    class Menus(val mainMenu: Menu, val favoritesMenu: Menu, val myAccountMenu: Menu)

    @Autowired
    private lateinit var menuCreator: MenuCreator

    @Autowired
    private lateinit var favoritesMenuCreator: FavoritesMenuCreator

    @Autowired
    private lateinit var systemStatus: SystemStatus;

    @GetMapping
    fun getMenu(): Menus {
        val mainMenu = menuCreator.build(MenuCreatorContext(ThreadLocalUserContext.getUser()))
        val favoritesMenu = favoritesMenuCreator.getFavoriteMenu()
        val goClassicsMenu = MenuItemDef(MenuItemDefId.GO_CLASSIC)
        favoritesMenu.add(goClassicsMenu)

        val myAccountMenu = Menu()
        val item = MenuItem("username", ThreadLocalUserContext.getUser()?.getFullname())
        myAccountMenu.add(item)
        item.add(MenuItem(MenuItemDefId.FEEDBACK))
        item.add(MenuItem(MenuItemDefId.MY_ACCOUNT))
        if (systemStatus.developmentMode) {
            val vacationAccountItem = MenuItem(MenuItemDefId.VACATION_ACCOUNT)
            vacationAccountItem.url = "${PREFIX}vacationAccount/dynamic"
            item.add(vacationAccountItem)
        } else {
            item.add(MenuItem(MenuItemDefId.VACATION_ACCOUNT))
        }
        item.add(MenuItem(MenuItemDefId.LOGOUT, type = MenuItemTargetType.RESTCALL))
        item.subMenu?.forEach { it.postProcess() }
        return Menus(mainMenu, favoritesMenu, myAccountMenu)
    }
}
