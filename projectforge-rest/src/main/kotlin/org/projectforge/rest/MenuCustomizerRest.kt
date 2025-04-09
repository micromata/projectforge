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

    /**
     * Saves the customized menu for the current user.
     * @param customMenuData The custom menu data with the favorites menu items.
     */
    @PostMapping("customized")
    fun saveCustomMenu(@RequestBody customMenuData: CustomMenuData): ResponseEntity<Any> {
        log.info { "Saving customized menu via REST API for user: ${ThreadLocalUserContext.requiredLoggedInUser.username}" }

        if (customMenuData.favoritesMenu == null) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Favorites menu cannot be null"))
        }

        // Create a new menu with the provided items
        val newMenu = Menu()
        customMenuData.favoritesMenu.forEach { newMenu.add(it) }

        // Store the menu in user preferences
        FavoritesMenuReaderWriter.storeAsUserPref(newMenu)

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
