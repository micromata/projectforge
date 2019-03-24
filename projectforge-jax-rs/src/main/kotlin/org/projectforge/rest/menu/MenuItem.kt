package org.projectforge.rest.menu

import com.google.gson.annotations.SerializedName

class MenuItem(val title: String,
               var url : String? = null,
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