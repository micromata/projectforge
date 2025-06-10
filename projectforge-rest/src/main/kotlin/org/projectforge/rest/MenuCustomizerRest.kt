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

import mu.KotlinLogging
import org.projectforge.business.user.service.UserXmlPreferencesService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.menu.builder.FavoritesMenuCreator
import org.projectforge.menu.builder.FavoritesMenuReaderWriter
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

/**
 * REST service for customizing the user's menu via drag and drop interface.
 */
@RestController
@RequestMapping("${Rest.URL}/menu")
class MenuCustomizerRest {

    class CustomMenuData(val favoritesMenu: List<MenuItem>? = null)

    @Autowired
    private lateinit var favoritesMenuCreator: FavoritesMenuCreator
    
    @Autowired
    private lateinit var userXmlPreferencesService: UserXmlPreferencesService

    /**
     * Saves the customized menu for the current user.
     * @param customMenuData The custom menu data with the favorites menu items.
     */
    @PostMapping("customized")
    fun saveCustomMenu(@RequestBody customMenuData: CustomMenuData): ResponseEntity<Any> {
        log.info { "Saving customized menu via REST API for user: ${ThreadLocalUserContext.requiredLoggedInUser.username}" }
        log.info { "Received ${customMenuData.favoritesMenu?.size ?: 0} menu items: ${customMenuData.favoritesMenu?.map { "${it.id}:${it.title}" }}" }

        if (customMenuData.favoritesMenu == null) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Favorites menu cannot be null"))
        }

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
        log.info { "Menu successfully stored for user: ${ThreadLocalUserContext.requiredLoggedInUser.username}" }

        // Verify by reading it back
        val verifyMenu = favoritesMenuCreator.getFavoriteMenu()
        log.info { "Verification: Read back ${verifyMenu.menuItems.size} items: ${verifyMenu.menuItems.map { "${it.id}:${it.title}" }}" }
        
        // Also check what's actually stored in user preferences
        val storedPref = userXmlPreferencesService.getEntry("usersFavoriteMenuEntries") as String?
        log.info { "Stored preference length: ${storedPref?.length ?: 0}" }
        log.info { "Stored preference preview: ${storedPref?.take(200) ?: "null"}" }
        
        // Direct test: Read the stored XML directly
        val directReadMenu = favoritesMenuCreator.getFavoriteMenu(storedPref)
        log.info { "Direct read test: ${directReadMenu.menuItems.size} items: ${directReadMenu.menuItems.map { "${it.id}:${it.title}" }}" }

        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    /**
     * Resets the user's menu to the default menu.
     */
    @PostMapping("reset")
    fun resetMenu(): ResponseEntity<Any> {
        log.info { "Resetting menu to default via REST API for user: ${ThreadLocalUserContext.requiredLoggedInUser.username}" }

        // Clear the user preference for favorites menu
        FavoritesMenuReaderWriter.storeAsUserPref(null)

        return ResponseEntity.ok(mapOf("status" to "success"))
    }
}
