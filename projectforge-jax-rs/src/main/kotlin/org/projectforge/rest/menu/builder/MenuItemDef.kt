package org.projectforge.rest.menu.builder

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.rest.menu.MenuItem
import org.projectforge.ui.translate

internal class MenuItemDef {
    constructor(defId: MenuItemDefId, url: String? = null) {
        this.id = defId.id
        this.i18nKey = defId.getI18nKey()
        this.url = url
    }

    val id: String
    var title: String? = null
    var i18nKey: String? = null
    var url: String? = null
    var allowedGroups : List<ProjectForgeGroup>? = null

    internal var childs: MutableList<MenuItemDef>? = null

    @Synchronized
    fun add(item: MenuItemDef): MenuItemDef {
        if (childs == null)
            childs = mutableListOf()
        childs!!.add(item)
        return this
    }

    fun get(id: MenuItemDefId): MenuItemDef? {
        if (this.id == id.id) {
            return this
        }
        if (childs == null) {
            return null
        }
        childs!!.forEach {
            if (it.id == id.id)
                return it
        }
        return null
    }

    fun createMenu(menuBuilderContext: MenuCreatorContext) : MenuItem {
        val menuItem = MenuItem(translate(i18nKey), url = this.url)
        return menuItem
    }
}
