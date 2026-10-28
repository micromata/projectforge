/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.menu.builder.FavoritesMenuCreator
import org.projectforge.menu.builder.FavoritesMenuReaderWriter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

/**
 * REST service of the menu customizer (`/next/customizeMenu`): the user composes the favorites menu by drag and
 * drop. The current menus come from [MenuRest]; the texts from the generated i18n catalogs of the client.
 */
@RestController
@RequestMapping("${Rest.URL}/menu")
class MenuCustomizerRest {

    class CustomMenuData(val favoritesMenu: List<MenuItem>? = null)

    @Autowired
    private lateinit var favoritesMenuCreator: FavoritesMenuCreator

    /**
     * Saves the customized menu for the current user.
     * @param customMenuData The custom menu data with the favorites menu items.
     */
    @AccessChecked("Own user only (stores the logged-in user's menu pref)")
    @PostMapping("customized")
    fun saveCustomMenu(@RequestBody customMenuData: CustomMenuData): ResponseEntity<Any> {
        if (customMenuData.favoritesMenu == null) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Favorites menu cannot be null"))
        }

        log.info { "User saves new custom menu." }
        // Create a new menu with the provided items
        // For groups that have subMenus, we need to ensure they don't conflict with existing MenuItemDefs
        val newMenu = Menu()
        customMenuData.favoritesMenu.forEach { item ->
            if (item.subMenu?.isNotEmpty() == true) {
                // This is a group - create a custom group entry with a completely unique ID that won't conflict with any MenuItemDef
                val groupItem = MenuItem(key = "custom-group-${item.id ?: System.currentTimeMillis()}")
                groupItem.title = item.title
                groupItem.id = "CUSTOM_GROUP_${item.id}_${System.currentTimeMillis()}" // Completely unique ID
                item.subMenu!!.forEach { subItem ->
                    groupItem.add(subItem)
                }
                newMenu.add(groupItem)
            } else {
                // This is a regular menu item
                newMenu.add(item)
            }
        }

        // Store the menu in user preferences
        FavoritesMenuReaderWriter.storeAsUserPref(newMenu)

        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    /**
     * Returns the default menu without saving it to user preferences.
     */
    @AccessChecked("Own user only (the default menu of the logged-in user)")
    @GetMapping("default")
    fun getDefaultMenu(): ResponseEntity<Any> {
        val defaultMenu = favoritesMenuCreator.createDefaultFavoriteMenu()
        return ResponseEntity.ok(mapOf("favoritesMenu" to defaultMenu.menuItems))
    }
}
