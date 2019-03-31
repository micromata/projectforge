package org.projectforge.menu

import org.projectforge.menu.builder.MenuItemDef

class Menu() {
    val menuItems = mutableListOf<MenuItem>()

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