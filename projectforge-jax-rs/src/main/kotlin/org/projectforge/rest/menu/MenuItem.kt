package org.projectforge.rest.menu

import com.google.gson.annotations.SerializedName

class MenuItem(val title: String,
               var url : String? = null,
               /**
                * Unique key usable by React. It's also unique for multiple menu items (in different main categories).
                */
               var key: String? = null,
               var badge : MenuBadge? = null) {

    @SerializedName("sub-menu")
    var subMenu: MutableList<MenuItem>? = null

    fun add(menuItem: MenuItem) {
        if (subMenu == null) {
            subMenu = mutableListOf()
        }
        subMenu?.add(menuItem)
    }
}