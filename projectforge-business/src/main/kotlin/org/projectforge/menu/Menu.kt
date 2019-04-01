package org.projectforge.menu

import org.projectforge.menu.builder.MenuItemDef

class Menu() {
    val menuItems = mutableListOf<MenuItem>()

    /**
     * Removes all super menu items without children. This will happen, if the user hasn't the user rights to see
     * any children of a super menu item.
     */
    fun postProcess() {
        menuItems.forEach {
            it.postProcess()
        }
        menuItems.removeIf { !it.isLeaf() && it.subMenu.isNullOrEmpty() }
    }

    fun add(menuItem: MenuItem?) {
        if (menuItem == null)
            return
        menuItems.add(menuItem)
    }

    fun add(menuItemDef: MenuItemDef?) {
        if (menuItemDef == null)
            return // Do nothing.
        add(MenuItem(menuItemDef))
    }
}