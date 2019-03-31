package org.projectforge.menu

class MenuItem(@Transient
               var id: String?,
               val title: String,
               var url: String? = null,
               /**
                * Unique key usable by React. It's also unique for multiple menu items (in different main categories).
                */
               var key: String? = null,
               var badge: MenuBadge? = null) {

    var subMenu: MutableList<MenuItem>? = null

    fun add(menuItem: MenuItem) {
        if (subMenu == null) {
            subMenu = mutableListOf()
        }
        subMenu?.add(menuItem)
    }
}