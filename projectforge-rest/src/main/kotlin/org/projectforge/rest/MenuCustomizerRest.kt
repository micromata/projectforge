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
import org.projectforge.framework.i18n.translate
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.menu.builder.FavoritesMenuReaderWriter
import org.projectforge.rest.config.Rest
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
    
    class MenuCustomizerPageData(
        val translations: Map<String, String>,
        val excelMenuUrl: String
    )

    /**
     * Returns page data including translations and Excel menu URL.
     */
    @GetMapping("customizer")
    fun getMenuCustomizerPageData(): ResponseEntity<MenuCustomizerPageData> {
        val translations = mapOf(
            "title" to translate("menu.customizer.title"),
            "customMenuSection" to translate("menu.customizer.customMenuSection"),
            "templateMenuSection" to translate("menu.customizer.templateMenuSection"),
            "addGroup" to translate("menu.customizer.addGroup"),
            "groupName" to translate("menu.customizer.groupName"),
            "add" to translate("add"),
            "cancel" to translate("cancel"),
            "save" to translate("save"),
            "reset" to translate("reset"),
            "saveChanges" to translate("menu.customizer.saveChanges"),
            "resetToDefault" to translate("menu.customizer.resetToDefault"),
            "dragItemsHere" to translate("menu.customizer.dragItemsHere"),
            "noItemsInCategory" to translate("menu.customizer.noItemsInCategory"),
            "dropItemsHere" to translate("menu.customizer.dropItemsHere"),
            "removeFromGroup" to translate("menu.customizer.removeFromGroup"),
            "removeFromFavorites" to translate("menu.customizer.removeFromFavorites"),
            "editGroupName" to translate("menu.customizer.editGroupName"),
            "removeGroup" to translate("menu.customizer.removeGroup"),
            "groupNameCannotBeEmpty" to translate("menu.customizer.groupNameCannotBeEmpty"),
            "menuSavedSuccessfully" to translate("menu.customizer.menuSavedSuccessfully"),
            "menuResetSuccessfully" to translate("menu.customizer.menuResetSuccessfully"),
            "errorLoadingMenu" to translate("menu.customizer.errorLoadingMenu"),
            "errorSavingMenu" to translate("menu.customizer.errorSavingMenu"),
            "errorResettingMenu" to translate("menu.customizer.errorResettingMenu"),
            "confirmReset" to translate("menu.customizer.confirmReset"),
            "excelMenu" to translate("menu.customizer.excelMenu")
        )
        
        val pageData = MenuCustomizerPageData(
            translations = translations,
            excelMenuUrl = "/react/myMenu/dynamic/"
        )
        
        return ResponseEntity.ok(pageData)
    }

    /**
     * Saves the customized menu for the current user.
     * @param customMenuData The custom menu data with the favorites menu items.
     */
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
     * Resets the user's menu to the default menu.
     */
    @PostMapping("reset")
    fun resetMenu(): ResponseEntity<Any> {
        log.info { "Clear the user preference for favorites menu." }
        FavoritesMenuReaderWriter.storeAsUserPref(null)

        return ResponseEntity.ok(mapOf("status" to "success"))
    }
}
