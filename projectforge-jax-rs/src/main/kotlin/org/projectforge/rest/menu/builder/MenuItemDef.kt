package org.projectforge.rest.menu.builder

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.rest.menu.MenuItem
import org.projectforge.ui.translate

/**
 * Defines one menu item once. The [MenuCreator] creates the user menu item from this definition dynamically dependent
 * e. g. on the user's access.
 */
class MenuItemDef {
    /**
     * Usable for e. g. plugins without [MenuItemDef] available.
     * @param defId For getting the key and i18nKey.
     * @param url The target url.
     * @param checkAccess Dynamic check access for the logged in user. The menu is visible if [checkAccess] is null or returns true.
     */
    constructor(defId: MenuItemDefId, url: String? = null, checkAccess: (() -> Boolean)? = null) {
        this.key = defId.id
        this.i18nKey = defId.getI18nKey()
        this.url = url
        this.checkAccess = checkAccess
    }

    /**
     * Usable for e. g. plugins without [MenuItemDef] available.
     * @param key Should be unique inside one top menu.
     * @param i18nKey Used for translation.
     * @param url The target url.
     * @param checkAccess Dynamic check access for the logged in user. The menu is visible if [checkAccess] is null or returns true.
     */
    constructor(key : String, i18nKey: String, url: String? = null, checkAccess: (() -> Boolean)? = null) {
        this.key = key
        this.i18nKey = i18nKey
        this.url = url
        this.checkAccess = checkAccess
    }

    /**
     * Needed for unique keys for React frontend.
     */
    val key: String
    var title: String? = null
    var i18nKey: String? = null
    var url: String? = null
    var allowedGroups: List<ProjectForgeGroup>? = null

    var checkAccess: (() -> Boolean)? = null

    internal var childs: MutableList<MenuItemDef>? = null

    @Synchronized
    internal fun add(item: MenuItemDef): MenuItemDef {
        if (childs == null)
            childs = mutableListOf()
        childs!!.add(item)
        return this
    }

    internal fun get(id: MenuItemDefId): MenuItemDef? {
        if (this.key == id.id) {
            return this
        }
        if (childs == null) {
            return null
        }
        childs!!.forEach {
            if (it.key == id.id)
                return it
        }
        return null
    }

    /**
     * @param parentMenu Only needed for building unique keys
     * @param menuBuilderContext
     */
    internal fun createMenu(parentMenu: MenuItem, menuBuilderContext: MenuCreatorContext): MenuItem {
        val menuItem = MenuItem(translate(i18nKey), url = this.url)
        if (parentMenu?.title != "root")
            menuItem.key = "${parentMenu.key}.${key}"
        else
            menuItem.key = "${key}"
        return menuItem
    }
}
