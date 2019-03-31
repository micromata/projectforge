package org.projectforge.menu

import org.projectforge.framework.i18n.translate
import org.projectforge.menu.builder.MenuItemDef

class MenuItem(@Transient
               var id: String? = null,
               var title: String? = null,
               var url: String? = null,
               /**
                * Unique key usable by React. It's also unique for multiple menu items (in different main categories).
                */
               var key: String? = null,
               var badge: MenuBadge? = null) {
    constructor(menuItemDef: MenuItemDef?) : this() {
        if (menuItemDef == null)
            return
        id = menuItemDef.id
        title = translate(menuItemDef.i18nKey)
        key = menuItemDef.id
        url = menuItemDef.url
        if (menuItemDef.badgeCounter != null) {
            //badge = MenuBadge(counter = menuItemDef.badgeCounter?.invoke())
        }
    }

    var subMenu: MutableList<MenuItem>? = null

    fun add(menuItem: MenuItem) {
        if (subMenu == null) {
            subMenu = mutableListOf()
        }
        subMenu?.add(menuItem)
    }

    fun add(menuItemDef: MenuItemDef?) {
        if (menuItemDef == null)
            return // Do nothing.
        add(MenuItem(menuItemDef))
    }
}