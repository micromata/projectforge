package org.projectforge.rest.menu

class MenuItem(val title: String,
               var url : String? = null,
               var badge : MenuBadge? = null) {

    var childs: MutableList<MenuItem>? = null

    fun add(menuItem: MenuItem) {
        if (childs == null) {
            childs = mutableListOf()
        }
        childs?.add(menuItem)
    }
}