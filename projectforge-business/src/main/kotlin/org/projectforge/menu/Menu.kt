package org.projectforge.menu

import org.projectforge.menu.builder.MenuItemDef

class Menu() {
    val menuItems = mutableListOf<MenuItem>()
    var badge: MenuBadge? = null

    /**
     * Removes all super menu items without children. This will happen, if the user hasn't the user rights to see
     * any children of a super menu item.
     *
     * Accumulates badge counter in parent menus by summarizing all badge counters from child menus.
     */
    fun postProcess() {
        menuItems.forEach {
            it.postProcess()
        }
        menuItems.removeIf { !it.isLeaf() && it.subMenu.isNullOrEmpty() }
        var badgeCounter = 0
        menuItems.forEach {
            if (it.badge?.counter ?: -1 > 0)
                badgeCounter += it.badge?.counter ?: 0
        }
        if (badgeCounter > 0)
            badge = MenuBadge(badgeCounter, style = "danger")
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