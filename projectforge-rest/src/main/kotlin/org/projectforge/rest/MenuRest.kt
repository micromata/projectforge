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

package org.projectforge.rest

import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.menu.builder.*
import org.projectforge.rest.config.Rest
import org.projectforge.rest.my2fa.My2FASetupMenuBadge
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/menu")
class MenuRest {
  // favoritesMenu and myAccountMenu used by rest client.
  @Suppress("unused")
  class Menus(val mainMenu: Menu, val favoritesMenu: Menu, val myAccountMenu: Menu)

  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var menuCreator: MenuCreator

  @Autowired
  private lateinit var my2FASetupMenuBadge: My2FASetupMenuBadge

  @Autowired
  private lateinit var favoritesMenuCreator: FavoritesMenuCreator

  @GetMapping
  fun getMenu(): Menus {
    val mainMenu = menuCreator.build(MenuCreatorContext(ThreadLocalUserContext.loggedInUser!!))
    val favoritesMenu = favoritesMenuCreator.getFavoriteMenu()

    val myAccountMenu = Menu()
    val userNameItem = MenuItem("username", ThreadLocalUserContext.loggedInUser!!.getFullname(), key = "MY_MENU")
    myAccountMenu.add(userNameItem)
    userNameItem.add(MenuItem(MenuItemDefId.FEEDBACK))
    userNameItem.add(MenuItemDef(MenuItemDefId.MY_ACCOUNT))
    userNameItem.add(MenuItemDef(MenuItemDefId.MY_2FA_SETUP, badgeCounter = { my2FASetupMenuBadge.badgeCounter }))
    if (!accessChecker.isRestrictedUser) {
      if (ThreadLocalUserContext.userContext!!.employeeId != null) {
        userNameItem.add(MenuItem(MenuItemDefId.VACATION_ACCOUNT))
      }
      menuCreator.personalMenuPluginEntries.forEach { menuItemDef ->
        userNameItem.add(menuItemDef)
      }
    }

    userNameItem.add(MenuItem(MenuItemDefId.LOGOUT, type = MenuItemTargetType.RESTCALL))
    userNameItem.postProcess()
    return Menus(mainMenu, favoritesMenu, myAccountMenu)
  }
}
